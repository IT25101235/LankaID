package lk.sliit.identityissuanceservice.Admin_Mgmt;

import jakarta.persistence.EntityManager;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Service.PrintingExportService;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Model.Centre;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Repository.CentreRepository;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model.CitizenApplication;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Repository.CitizenApplicationRepository;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Repository.GovernmentIdentityRecordRepository;
import lk.sliit.identityissuanceservice.Document_Renewal_Replacement_And_Delivery_Mgmt.Service.DocumentDeliveryService;
import lk.sliit.identityissuanceservice.User_Mgmt.Model.CitizenAccount;
import lk.sliit.identityissuanceservice.User_Mgmt.Model.UserAccount;
import lk.sliit.identityissuanceservice.User_Mgmt.Repository.CitizenAccountRepository;
import lk.sliit.identityissuanceservice.User_Mgmt.Repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Rollback
class PassportIssuanceHistoryIntegrationTests {

    @Autowired private PrintingExportService printingService;
    @Autowired private DocumentDeliveryService deliveryService;
    @Autowired private EntityManager entityManager;
    @Autowired private UserAccountRepository userRepository;
    @Autowired private CitizenAccountRepository citizenRepository;
    @Autowired private CitizenApplicationRepository applicationRepository;
    @Autowired private CentreRepository centreRepository;
    @Autowired private GovernmentIdentityRecordRepository governmentRepository;

    @Test
    void renewedPassportKeepsPersistedPrintedAndUncollectedHistory() {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        UserAccount user = new UserAccount();
        user.setEmail("passport-history-" + unique + "@example.com");
        user.setPhone("+947" + String.format("%08d", Integer.toUnsignedLong(unique.hashCode()) % 100_000_000));
        user.setPasswordHash("test-only");
        user = userRepository.saveAndFlush(user);
        CitizenAccount citizen = new CitizenAccount(user.getUserId());
        citizen.setGivenName("Passport History");
        citizen.setFamilyName("Perera");
        citizen.setDateOfBirth(LocalDateTime.of(2000, 1, 15, 0, 0));
        citizenRepository.saveAndFlush(citizen);
        Centre centre = new Centre();
        centre.setCentreName("Passport History " + unique);
        centre.setCity("Colombo");
        centre.setDistrict("Colombo");
        centre.setDailyCapacity(100);
        centre.setCentreStatus("ACTIVE");
        centre = centreRepository.saveAndFlush(centre);

        CitizenApplication original = application(user.getUserId(), "NEW", unique);
        Integer originalId = original.getApplicationId();
        String originalReference = deliveryService.chooseDelivery(user.getUserId(), originalId,
                DocumentDeliveryService.CENTRE_COLLECTION, centre.getCentreId()).getTrackingReference();
        printingService.printAndCreateCsv(List.of(originalId), null);
        String originalNumber = original.getIssuedDocumentNumber();
        CitizenApplication renewal = application(user.getUserId(), "RENEWAL", unique);
        Integer renewalId = renewal.getApplicationId();
        String renewalReference = deliveryService.chooseDelivery(user.getUserId(), renewalId,
                DocumentDeliveryService.CENTRE_COLLECTION, centre.getCentreId()).getTrackingReference();
        printingService.printAndCreateCsv(List.of(renewalId), null);
        String renewalNumber = renewal.getIssuedDocumentNumber();
        entityManager.flush();
        entityManager.clear();

        original = applicationRepository.findById(originalId).orElseThrow();
        renewal = applicationRepository.findById(renewalId).orElseThrow();
        citizen = citizenRepository.findById(user.getUserId()).orElseThrow();
        assertTrue(originalNumber.matches("N\\d{7}"));
        assertTrue(renewalNumber.matches("N\\d{7}"));
        assertNotEquals(originalNumber, renewalNumber);
        assertEquals(originalNumber, original.getIssuedDocumentNumber());
        assertEquals(renewalNumber, renewal.getIssuedDocumentNumber());
        assertEquals("ISSUED", original.getCurrentStatus());
        assertEquals("ISSUED", renewal.getCurrentStatus());
        assertEquals(renewalNumber, governmentRepository.findById(citizen.getGovernmentRecordId())
                .orElseThrow().getPassportNo());
        var printedRows = printingService.getPrintedApplications();
        assertEquals(originalNumber, printedRows.stream().filter(row -> originalId.equals(row.applicationId()))
                .findFirst().orElseThrow().documentNumber());
        assertEquals(renewalNumber, printedRows.stream().filter(row -> renewalId.equals(row.applicationId()))
                .findFirst().orElseThrow().documentNumber());
        String csv = new String(printingService.createPrintedRegisterCsv(), StandardCharsets.UTF_8);
        assertTrue(csv.contains("\"" + original.getApplicationNo() + "\",\"PASSPORT\",\"NEW\",\"" + originalNumber + "\""));
        assertFalse(csv.contains("\"" + original.getApplicationNo() + "\",\"PASSPORT\",\"NEW\",\"" + renewalNumber + "\""));
        assertTrue(csv.contains("\"" + renewal.getApplicationNo() + "\",\"PASSPORT\",\"RENEWAL\",\"" + renewalNumber + "\""));
        var originalCollection = deliveryService.findCollection(originalReference);
        assertEquals(originalNumber, originalCollection.documentNumber());
        assertEquals(DocumentDeliveryService.READY_FOR_COLLECTION, originalCollection.delivery().getDeliveryStatus());
        assertEquals(renewalNumber, deliveryService.findCollection(renewalReference).documentNumber());
        assertEquals(originalNumber, deliveryService.getReadyCollections().stream()
                .filter(row -> originalId.equals(row.delivery().getApplicationId()))
                .findFirst().orElseThrow().documentNumber());
    }

    private CitizenApplication application(Integer citizenId, String kind, String unique) {
        CitizenApplication application = new CitizenApplication();
        application.setCitizenId(citizenId);
        application.setApplicationNo("HISTORY-" + kind + "-" + unique);
        application.setServiceType("PASSPORT");
        application.setApplicationKind(kind);
        application.setCurrentStatus("BIOMETRIC_COLLECTED");
        application.setDeclarationAccepted("Y");
        return applicationRepository.saveAndFlush(application);
    }
}
