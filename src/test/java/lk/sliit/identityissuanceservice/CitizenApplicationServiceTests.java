package lk.sliit.identityissuanceservice;

import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Dto.CitizenEligibilityDto;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model.ApplicationDocument;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model.CitizenApplication;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model.GovernmentIdentityRecord;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Repository.ApplicationDocumentRepository;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Repository.CitizenApplicationRepository;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Repository.GovernmentIdentityRecordRepository;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Repository.NicApplicationRepository;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Repository.PassportApplicationRepository;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Service.CitizenApplicationService;
import lk.sliit.identityissuanceservice.User_Mgmt.Model.CitizenAccount;
import lk.sliit.identityissuanceservice.User_Mgmt.Model.UserAccount;
import lk.sliit.identityissuanceservice.User_Mgmt.Repository.CitizenAccountRepository;
import lk.sliit.identityissuanceservice.User_Mgmt.Repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CitizenApplicationServiceTests {

    @Mock UserAccountRepository userRepository;
    @Mock CitizenAccountRepository citizenRepository;
    @Mock GovernmentIdentityRecordRepository governmentRepository;
    @Mock CitizenApplicationRepository applicationRepository;
    @Mock NicApplicationRepository nicRepository;
    @Mock PassportApplicationRepository passportRepository;
    @Mock ApplicationDocumentRepository documentRepository;

    private final Path uploadDirectory = Path.of("target", "test-uploads").toAbsolutePath();

    private CitizenApplicationService service;
    private UserAccount user;
    private CitizenAccount citizen;

    @BeforeEach
    void setUp() throws Exception {
        Files.createDirectories(uploadDirectory);
        service = new CitizenApplicationService(
                userRepository, citizenRepository, governmentRepository,
                applicationRepository, nicRepository, passportRepository,
                documentRepository, uploadDirectory.toString()
        );
        user = new UserAccount();
        user.setUserId(25);
        user.setEmail("citizen@example.com");
        user.setPhone("+94771234567");
        citizen = new CitizenAccount(25);
    }

    @Test
    void exactEmailAndPhoneMatchFindsExistingDocuments() {
        GovernmentIdentityRecord record = governmentRecord();
        stubAccountAndIdentity(Optional.of(record), Optional.of(record), Optional.of(record));

        CitizenEligibilityDto result = service.getEligibility(25);

        assertTrue(result.isHasNic());
        assertTrue(result.isHasPassport());
        assertFalse(result.isIdentityConflict());
        verify(citizenRepository).save(citizen);
    }

    @Test
    void partialMatchIsBlockedAsIdentityConflict() {
        GovernmentIdentityRecord record = governmentRecord();
        stubAccountAndIdentity(Optional.empty(), Optional.of(record), Optional.empty());

        CitizenEligibilityDto result = service.getEligibility(25);

        assertTrue(result.isIdentityConflict());
        assertFalse(result.isHasNic());
    }

    @Test
    void newNicIsRejectedForExistingNicHolder() {
        GovernmentIdentityRecord record = governmentRecord();
        stubAccountAndIdentity(Optional.of(record), Optional.of(record), Optional.of(record));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.prepareNewNicForm(25)
        );
        assertTrue(exception.getMessage().contains("already have"));
    }

    @Test
    void uploadStoresAFileAndSavesOnlyItsRelativePath() throws Exception {
        CitizenApplication application = application();
        when(applicationRepository.findByApplicationIdAndCitizenId(101, 25))
                .thenReturn(Optional.of(application));
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", new byte[]{1, 2, 3}
        );

        service.uploadNewDocument(25, 101, "PHOTOGRAPH", file);

        ArgumentCaptor<ApplicationDocument> captor = ArgumentCaptor.forClass(ApplicationDocument.class);
        verify(documentRepository).save(captor.capture());
        Path storedPath = Path.of(captor.getValue().getFilePath());
        assertFalse(storedPath.isAbsolute());
        assertTrue(Files.exists(uploadDirectory.resolve(storedPath)));
    }

    @Test
    void submissionCannotContinueWhenRequiredDocumentIsMissing() {
        CitizenApplication application = application();
        when(applicationRepository.findByApplicationIdAndCitizenId(101, 25))
                .thenReturn(Optional.of(application));
        ApplicationDocument photograph = new ApplicationDocument();
        photograph.setDocumentType("PHOTOGRAPH");
        when(documentRepository.findAllByApplicationIdOrderByUploadedAtAsc(101))
                .thenReturn(List.of(photograph));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.submitNewApplication(25, 101)
        );
        assertTrue(exception.getMessage().contains("BIRTH_CERTIFICATE"));
        assertTrue("DRAFT".equals(application.getCurrentStatus()));
    }

    private void stubAccountAndIdentity(
            Optional<GovernmentIdentityRecord> exact,
            Optional<GovernmentIdentityRecord> email,
            Optional<GovernmentIdentityRecord> phone) {
        when(userRepository.findById(25)).thenReturn(Optional.of(user));
        when(citizenRepository.findById(25)).thenReturn(Optional.of(citizen));
        when(governmentRepository.findByRegisteredEmailIgnoreCaseAndRegisteredPhone(
                user.getEmail(), user.getPhone())).thenReturn(exact);
        when(governmentRepository.findByRegisteredEmailIgnoreCase(user.getEmail())).thenReturn(email);
        when(governmentRepository.findByRegisteredPhone(user.getPhone())).thenReturn(phone);
    }

    private GovernmentIdentityRecord governmentRecord() {
        GovernmentIdentityRecord record = new GovernmentIdentityRecord();
        record.setGovernmentRecordId(10);
        record.setNicNo("200012345678");
        record.setPassportNo("N1234567");
        record.setRegisteredEmail(user.getEmail());
        record.setRegisteredPhone(user.getPhone());
        record.setIdentityStatus("ACTIVE");
        return record;
    }

    private CitizenApplication application() {
        CitizenApplication application = new CitizenApplication();
        application.setApplicationId(101);
        application.setApplicationNo("LK-NIC-2026-123456");
        application.setCitizenId(25);
        application.setServiceType("NIC");
        application.setApplicationKind("NEW");
        application.setCurrentStatus("DRAFT");
        return application;
    }
}
