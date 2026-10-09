package lk.sliit.identityissuanceservice;

import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Dto.CitizenEligibilityDto;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Dto.NewNicApplicationForm;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Dto.NewPassportApplicationForm;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model.ApplicationDocument;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model.CitizenApplication;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model.GovernmentIdentityRecord;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Repository.ApplicationDocumentRepository;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Repository.CitizenApplicationRepository;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Repository.GovernmentIdentityRecordRepository;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Repository.NicApplicationRepository;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Repository.PassportApplicationRepository;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Service.CitizenApplicationService;
import lk.sliit.identityissuanceservice.Notification_Mgmt.Observer.NotificationPublisher;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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
    @Mock NotificationPublisher notificationPublisher;

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
                documentRepository, notificationPublisher, uploadDirectory.toString()
        );
        user = new UserAccount();
        user.setUserId(25);
        user.setEmail("citizen@example.com");
        user.setEmailVerified("Y");
        user.setPhone("+94771234567");
        citizen = new CitizenAccount(25);
        citizen.setDateOfBirth(LocalDateTime.of(2000, 1, 1, 0, 0));
    }

    @Test
    void matchingUniqueContactsLinkExistingDocumentsAfterEmailVerification() {
        GovernmentIdentityRecord record = governmentRecord();
        stubAccountAndIdentity(Optional.of(record), Optional.of(record));

        CitizenEligibilityDto result = service.getEligibility(25);

        assertTrue(result.isHasNic());
        assertTrue(result.isHasPassport());
        assertFalse(result.isIdentityConflict());
        verify(citizenRepository).save(citizen);
    }

    @Test
    void partialMatchIsBlockedAsIdentityConflict() {
        GovernmentIdentityRecord record = governmentRecord();
        record.setRegisteredPhone("+94770000000");
        stubAccountAndIdentity(Optional.of(record), Optional.empty());

        CitizenEligibilityDto result = service.getEligibility(25);

        assertTrue(result.isIdentityConflict());
        assertFalse(result.isHasNic());
    }

    @Test
    void newNicIsRejectedForExistingNicHolder() {
        GovernmentIdentityRecord record = governmentRecord();
        citizen.setGovernmentRecordId(record.getGovernmentRecordId());
        when(userRepository.findById(25)).thenReturn(Optional.of(user));
        when(citizenRepository.findById(25)).thenReturn(Optional.of(citizen));
        when(governmentRepository.findById(10)).thenReturn(Optional.of(record));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.prepareNewNicForm(25)
        );
        assertTrue(exception.getMessage().contains("already have"));
    }

    @Test
    void newNicFormIsRejectedWhenADraftAlreadyExists() {
        stubAccountAndIdentity(Optional.empty(), Optional.empty());
        when(applicationRepository.existsByCitizenIdAndServiceTypeAndCurrentStatusIn(
                eq(25),
                eq("NIC"),
                anyCollection()
        )).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.prepareNewNicForm(25)
        );

        assertTrue(exception.getMessage().contains("already in progress"));
        assertTrue(exception.getMessage().contains("Continue"));
    }

    @Test
    void underageCitizenCannotCreateANicApplication() {
        stubAccountAndIdentity(Optional.empty(), Optional.empty());
        NewNicApplicationForm form = new NewNicApplicationForm();
        form.setDateOfBirth(LocalDate.now().minusYears(15).plusDays(1));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.createNewNicApplication(25, form)
        );

        assertTrue(exception.getMessage().contains("at least 15"));
        verify(applicationRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void passportCannotUseParentNicWhenCitizenHasAnNicRecord() {
        GovernmentIdentityRecord record = governmentRecord();
        record.setPassportNo(null);
        citizen.setGovernmentRecordId(record.getGovernmentRecordId());
        when(userRepository.findById(25)).thenReturn(Optional.of(user));
        when(citizenRepository.findById(25)).thenReturn(Optional.of(citizen));
        when(governmentRepository.findById(10)).thenReturn(Optional.of(record));
        NewPassportApplicationForm form = new NewPassportApplicationForm();
        setValidSriLankanBirthLocation(form);
        form.setParentNicNo("197012345678");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.createNewPassportApplication(25, form)
        );

        assertTrue(exception.getMessage().contains("parent NIC cannot be used"));
        verify(applicationRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void underageNicDraftCannotBeSubmitted() {
        citizen.setDateOfBirth(LocalDateTime.now().minusYears(14));
        stubAccountAndIdentity(Optional.empty(), Optional.empty());
        CitizenApplication application = application();
        when(applicationRepository.findEditableForUpdate(101, 25)).thenReturn(Optional.of(application));
        when(applicationRepository.findByApplicationIdAndCitizenId(101, 25)).thenReturn(Optional.of(application));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.submitNewApplication(25, 101)
        );

        assertTrue(exception.getMessage().contains("at least 15"));
        verify(applicationRepository, never()).save(application);
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
        stubAccountAndIdentity(Optional.empty(), Optional.empty());
        CitizenApplication application = application();
        when(applicationRepository.findByApplicationIdAndCitizenId(101, 25))
                .thenReturn(Optional.of(application));
        when(applicationRepository.findEditableForUpdate(101, 25)).thenReturn(Optional.of(application));
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

    @Test
    void citizenCanDeleteAnUnsubmittedDraft() {
        CitizenApplication application = application();
        when(applicationRepository.findByApplicationIdAndCitizenId(101, 25))
                .thenReturn(Optional.of(application));
        when(documentRepository.findAllByApplicationIdOrderByUploadedAtAsc(101))
                .thenReturn(List.of());

        CitizenApplication deleted = service.deleteDraftApplication(25, 101);

        assertTrue(deleted == application);
        verify(documentRepository).deleteAll(List.of());
        verify(nicRepository).deleteById(101);
        verify(passportRepository, never()).deleteById(101);
        verify(applicationRepository).delete(application);
    }

    @Test
    void citizenCannotDeleteAnApplicationAfterSubmission() {
        CitizenApplication application = application();
        application.setCurrentStatus("SUBMITTED");
        when(applicationRepository.findByApplicationIdAndCitizenId(101, 25))
                .thenReturn(Optional.of(application));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.deleteDraftApplication(25, 101)
        );

        assertTrue(exception.getMessage().contains("unsubmitted draft"));
        verify(applicationRepository, never()).delete(application);
    }

    @Test
    void dualCitizenMustUploadCitizenshipCertificate() {
        CitizenApplication application = application();
        citizen.setDualCitizen("Y");
        when(citizenRepository.findById(25)).thenReturn(Optional.of(citizen));

        assertEquals(
                List.of("PHOTOGRAPH", "BIRTH_CERTIFICATE", "CITIZENSHIP_CERTIFICATE"),
                service.requiredNewDocumentTypes(application)
        );
    }

    @Test
    void nonDualCitizenCannotUploadCitizenshipCertificate() {
        CitizenApplication application = application();
        when(applicationRepository.findByApplicationIdAndCitizenId(101, 25))
                .thenReturn(Optional.of(application));
        when(citizenRepository.findById(25)).thenReturn(Optional.of(citizen));
        MockMultipartFile file = new MockMultipartFile(
                "file", "citizenship.pdf", "application/pdf", new byte[]{1, 2, 3}
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.uploadNewDocument(25, 101, "CITIZENSHIP_CERTIFICATE", file)
        );

        assertTrue(exception.getMessage().contains("not required"));
        verify(documentRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void submittedApplicationDocumentFormCannotBeOpened() {
        CitizenApplication application = application();
        application.setCurrentStatus("SUBMITTED");
        when(applicationRepository.findByApplicationIdAndCitizenId(101, 25))
                .thenReturn(Optional.of(application));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.getNewDocuments(25, 101)
        );

        assertTrue(exception.getMessage().contains("locked after submission"));
    }

    private void stubAccountAndIdentity(
            Optional<GovernmentIdentityRecord> email,
            Optional<GovernmentIdentityRecord> phone) {
        when(userRepository.findById(25)).thenReturn(Optional.of(user));
        when(citizenRepository.findById(25)).thenReturn(Optional.of(citizen));
        List<GovernmentIdentityRecord> matches = java.util.stream.Stream.concat(email.stream(), phone.stream()).distinct().toList();
        when(governmentRepository.findAllByRegisteredEmailIgnoreCaseOrRegisteredPhone(user.getEmail(), user.getPhone()))
                .thenReturn(matches);
        if (matches.size() == 1) {
            when(citizenRepository.findByIdForIdentityLink(25)).thenReturn(Optional.of(citizen));
            when(governmentRepository.findByIdForIdentityLink(matches.get(0).getGovernmentRecordId()))
                    .thenReturn(Optional.of(matches.get(0)));
        }
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

    private void setValidSriLankanBirthLocation(NewPassportApplicationForm form) {
        form.setCountryOfBirth("Sri Lanka");
        form.setBirthDistrict("Colombo");
        form.setBirthDivision("Colombo");
        form.setBirthCity("Colombo");
    }
}
