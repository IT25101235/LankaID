package lk.sliit.identityissuanceservice;

import lk.sliit.identityissuanceservice.Admin_Mgmt.Service.PrintingExportService;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model.CitizenApplication;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model.GovernmentIdentityRecord;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Repository.ApplicationDocumentRepository;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Repository.CitizenApplicationRepository;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Repository.GovernmentIdentityRecordRepository;
import lk.sliit.identityissuanceservice.Notification_Mgmt.Service.NotificationService;
import lk.sliit.identityissuanceservice.User_Mgmt.Model.CitizenAccount;
import lk.sliit.identityissuanceservice.User_Mgmt.Model.UserAccount;
import lk.sliit.identityissuanceservice.User_Mgmt.Repository.CitizenAccountRepository;
import lk.sliit.identityissuanceservice.User_Mgmt.Repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrintingExportServiceTests {

    @Mock CitizenApplicationRepository applicationRepository;
    @Mock CitizenAccountRepository citizenRepository;
    @Mock UserAccountRepository userRepository;
    @Mock GovernmentIdentityRecordRepository governmentRepository;
    @Mock ApplicationDocumentRepository documentRepository;
    @Mock NotificationService notificationService;

    private PrintingExportService printingService;

    @BeforeEach
    void setUp() {
        printingService = new PrintingExportService(
                applicationRepository,
                citizenRepository,
                userRepository,
                governmentRepository,
                documentRepository,
                notificationService
        );
        when(governmentRepository.save(any(GovernmentIdentityRecord.class))).thenAnswer(invocation -> {
            GovernmentIdentityRecord record = invocation.getArgument(0);
            if (record.getGovernmentRecordId() == null) record.setGovernmentRecordId(91);
            return record;
        });
    }

    @Test
    void nicPrintingCreatesTwelveDigitNumberAndUpdatesExistingTables() {
        TestData data = prepareApplication("NIC", 123, "MALE");
        when(governmentRepository.existsByNicNo(anyString())).thenReturn(false);

        byte[] csv = printingService.printAndCreateCsv(List.of(123), 70);

        assertNotNull(data.record.getNicNo());
        assertTrue(data.record.getNicNo().matches("2000\\d{8}"));
        assertEquals(12, data.record.getNicNo().length());
        assertEquals("PRINTED", data.application.getCurrentStatus());
        assertEquals(91, data.citizen.getGovernmentRecordId());
        assertTrue(new String(csv, StandardCharsets.UTF_8).contains(data.record.getNicNo()));
        verify(notificationService).sendToCitizen(
                data.application.getCitizenId(), 70, "DOCUMENT_PRINTED",
                "NIC application APP-123 has been printed. Document number: " + data.record.getNicNo() + "."
        );
    }

    @Test
    void passportPrintingCreatesCurrentNSeriesNumber() {
        TestData data = prepareApplication("PASSPORT", 456, "FEMALE");
        when(governmentRepository.existsByPassportNo(anyString())).thenReturn(false);

        printingService.printAndCreateCsv(List.of(456), 70);

        assertNotNull(data.record.getPassportNo());
        assertTrue(data.record.getPassportNo().matches("N\\d{7}"));
        assertEquals("PRINTED", data.application.getCurrentStatus());
    }

    private TestData prepareApplication(String serviceType, int applicationId, String gender) {
        CitizenApplication application = new CitizenApplication();
        application.setApplicationId(applicationId);
        application.setApplicationNo("APP-" + applicationId);
        application.setCitizenId(25);
        application.setServiceType(serviceType);
        application.setApplicationKind("NEW");
        application.setCurrentStatus("BIOMETRIC_COLLECTED");

        CitizenAccount citizen = new CitizenAccount(25);
        citizen.setGivenName("Nimal");
        citizen.setFamilyName("Perera");
        citizen.setGender(gender);
        citizen.setDateOfBirth(LocalDateTime.of(2000, 1, 15, 0, 0));

        UserAccount user = new UserAccount();
        user.setUserId(25);
        user.setEmail("citizen@example.com");
        user.setPhone("+94771234567");

        GovernmentIdentityRecord record = new GovernmentIdentityRecord();
        when(applicationRepository.findAllById(List.of(applicationId))).thenReturn(List.of(application));
        when(citizenRepository.findById(25)).thenReturn(Optional.of(citizen));
        when(userRepository.findById(25)).thenReturn(Optional.of(user));
        when(governmentRepository.findByRegisteredEmailIgnoreCaseAndRegisteredPhone(
                user.getEmail(), user.getPhone())).thenReturn(Optional.of(record));
        when(governmentRepository.findById(91)).thenReturn(Optional.of(record));
        when(documentRepository.findByApplicationIdAndDocumentType(applicationId, "PHOTOGRAPH"))
                .thenReturn(Optional.empty());
        return new TestData(application, citizen, record);
    }

    private record TestData(
            CitizenApplication application,
            CitizenAccount citizen,
            GovernmentIdentityRecord record
    ) {
    }
}
