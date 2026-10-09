package lk.sliit.identityissuanceservice;

import lk.sliit.identityissuanceservice.Admin_Mgmt.Service.PrintingExportService;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model.CitizenApplication;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model.GovernmentIdentityRecord;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Repository.ApplicationDocumentRepository;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Repository.CitizenApplicationRepository;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Repository.GovernmentIdentityRecordRepository;
import lk.sliit.identityissuanceservice.Document_Renewal_Replacement_And_Delivery_Mgmt.Service.DocumentDeliveryService;
import lk.sliit.identityissuanceservice.Notification_Mgmt.Observer.NotificationPublisher;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
    @Mock NotificationPublisher notificationPublisher;
    @Mock DocumentDeliveryService deliveryService;

    private PrintingExportService printingService;
    private GovernmentIdentityRecord savedRecord;

    @BeforeEach
    void setUp() {
        printingService = new PrintingExportService(
                applicationRepository,
                citizenRepository,
                userRepository,
                governmentRepository,
                documentRepository,
                notificationPublisher,
                deliveryService
        );
        when(governmentRepository.save(any(GovernmentIdentityRecord.class))).thenAnswer(invocation -> {
            GovernmentIdentityRecord record = invocation.getArgument(0);
            if (record.getGovernmentRecordId() == null) record.setGovernmentRecordId(91);
            savedRecord = record;
            return record;
        });
    }

    @Test
    void nicPrintingCreatesTwelveDigitNumberAndUpdatesExistingTables() {
        TestData data = prepareApplication("NIC", 123, "MALE");
        when(governmentRepository.existsByNicNo(anyString())).thenReturn(false);

        byte[] csv = printingService.printAndCreateCsv(List.of(123), 70);

        assertNotNull(savedRecord.getNicNo());
        assertNotNull(savedRecord.getNicIssuedDate());
        assertTrue(savedRecord.getNicNo().matches("2000\\d{8}"));
        assertEquals(12, savedRecord.getNicNo().length());
        assertEquals("ISSUED", data.application.getCurrentStatus());
        assertEquals(savedRecord.getNicNo(), data.application.getIssuedDocumentNumber());
        assertEquals(91, data.citizen.getGovernmentRecordId());
        assertTrue(new String(csv, StandardCharsets.UTF_8).contains(savedRecord.getNicNo()));
        verify(notificationPublisher).sendToCitizen(
                data.application.getCitizenId(), 70, "DOCUMENT_PRINTED",
                "NIC application APP-123 has been printed. Document number: " + savedRecord.getNicNo() + "."
        );
    }

    @Test
    void passportPrintingCreatesCurrentNSeriesNumber() {
        TestData data = prepareApplication("PASSPORT", 456, "FEMALE");
        when(governmentRepository.existsByPassportNo(anyString())).thenReturn(false);

        printingService.printAndCreateCsv(List.of(456), 70);

        assertNotNull(savedRecord.getPassportNo());
        assertTrue(savedRecord.getPassportNo().matches("N\\d{7}"));
        assertEquals("ISSUED", data.application.getCurrentStatus());
        assertEquals(savedRecord.getPassportNo(), data.application.getIssuedDocumentNumber());
    }

    @Test
    void passportRenewalKeepsEarlierPrintedNumberAndRegisterRows() {
        TestData original = prepareApplication("PASSPORT", 456, "FEMALE");
        when(governmentRepository.existsByPassportNo(anyString())).thenReturn(false);
        printingService.printAndCreateCsv(List.of(456), 70);
        String originalNumber = original.application.getIssuedDocumentNumber();

        TestData renewal = prepareApplication("PASSPORT", 457, "FEMALE");
        renewal.application.setApplicationKind("RENEWAL");
        renewal.citizen.setGovernmentRecordId(original.citizen.getGovernmentRecordId());
        when(governmentRepository.findById(original.citizen.getGovernmentRecordId()))
                .thenAnswer(invocation -> Optional.of(savedRecord));
        byte[] renewedCsv = printingService.printAndCreateCsv(List.of(457), 70);
        String renewedNumber = renewal.application.getIssuedDocumentNumber();

        assertNotEquals(originalNumber, renewedNumber);
        assertEquals(renewedNumber, savedRecord.getPassportNo());
        assertEquals(originalNumber, original.application.getIssuedDocumentNumber());
        assertEquals("ISSUED", original.application.getCurrentStatus());
        assertEquals("ISSUED", renewal.application.getCurrentStatus());
        assertTrue(new String(renewedCsv, StandardCharsets.UTF_8)
                .contains("\"APP-457\",\"PASSPORT\",\"RENEWAL\",\"" + renewedNumber + "\""));

        when(applicationRepository.findAllByCurrentStatusOrderByUpdatedAtDesc("ISSUED"))
                .thenReturn(List.of(renewal.application, original.application));
        var rows = printingService.getPrintedApplications();
        assertEquals(renewedNumber, rows.get(0).documentNumber());
        assertEquals(originalNumber, rows.get(1).documentNumber());
        String register = new String(printingService.createPrintedRegisterCsv(), StandardCharsets.UTF_8);
        assertTrue(register.contains("\"APP-456\",\"PASSPORT\",\"NEW\",\"" + originalNumber + "\""));
        assertTrue(register.contains("\"APP-457\",\"PASSPORT\",\"RENEWAL\",\"" + renewedNumber + "\""));
        assertFalse(register.contains("\"APP-456\",\"PASSPORT\",\"NEW\",\"" + renewedNumber + "\""));
    }

    @Test
    void printedHistoryWithoutRecordedNumberLeavesCsvNumberBlank() {
        TestData data = prepareApplication("PASSPORT", 456, "FEMALE");
        when(governmentRepository.existsByPassportNo(anyString())).thenReturn(false);
        printingService.printAndCreateCsv(List.of(456), 70);
        when(applicationRepository.findAllByCurrentStatusOrderByUpdatedAtDesc("ISSUED"))
                .thenReturn(List.of(data.application));

        // Older issued applications may have no saved number; the latest identity is not proof of it.
        for (String missingNumber : new String[]{null, "", "   "}) {
            data.application.setIssuedDocumentNumber(missingNumber);
            String register = new String(printingService.createPrintedRegisterCsv(), StandardCharsets.UTF_8);
            assertTrue(register.contains("\"APP-456\",\"PASSPORT\",\"NEW\",\"\","));
            assertFalse(register.contains(savedRecord.getPassportNo()));
        }
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

        when(applicationRepository.findAllById(List.of(applicationId))).thenReturn(List.of(application));
        when(citizenRepository.findById(25)).thenReturn(Optional.of(citizen));
        when(userRepository.findById(25)).thenReturn(Optional.of(user));
        when(documentRepository.findByApplicationIdAndDocumentType(applicationId, "PHOTOGRAPH"))
                .thenReturn(Optional.empty());
        return new TestData(application, citizen);
    }

    private record TestData(
            CitizenApplication application,
            CitizenAccount citizen
    ) {
    }
}
