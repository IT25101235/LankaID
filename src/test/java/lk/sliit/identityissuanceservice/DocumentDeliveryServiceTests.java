package lk.sliit.identityissuanceservice;

import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Repository.CentreRepository;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model.CitizenApplication;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Repository.CitizenApplicationRepository;
import lk.sliit.identityissuanceservice.Document_Renewal_Replacement_And_Delivery_Mgmt.Model.DocumentDelivery;
import lk.sliit.identityissuanceservice.Document_Renewal_Replacement_And_Delivery_Mgmt.Repository.DocumentDeliveryRepository;
import lk.sliit.identityissuanceservice.Document_Renewal_Replacement_And_Delivery_Mgmt.Service.DocumentDeliveryService;
import lk.sliit.identityissuanceservice.Notification_Mgmt.Observer.NotificationPublisher;
import lk.sliit.identityissuanceservice.User_Mgmt.Model.CitizenAccount;
import lk.sliit.identityissuanceservice.User_Mgmt.Model.UserAccount;
import lk.sliit.identityissuanceservice.User_Mgmt.Repository.CitizenAccountRepository;
import lk.sliit.identityissuanceservice.User_Mgmt.Repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DocumentDeliveryServiceTests {

    private DocumentDeliveryService deliveryService;
    private DocumentDeliveryRepository deliveryRepository;
    private CitizenApplicationRepository applicationRepository;
    private CitizenAccountRepository citizenRepository;
    private UserAccountRepository userRepository;

    @BeforeEach
    void setUp() {
        deliveryRepository = mock(DocumentDeliveryRepository.class);
        applicationRepository = mock(CitizenApplicationRepository.class);
        citizenRepository = mock(CitizenAccountRepository.class);
        userRepository = mock(UserAccountRepository.class);
        deliveryService = new DocumentDeliveryService(
                deliveryRepository,
                applicationRepository,
                citizenRepository,
                userRepository,
                mock(CentreRepository.class),
                mock(NotificationPublisher.class)
        );
    }

    @Test
    void homeDeliveryFeesFollowTheRequiredNicAndPassportRates() {
        assertEquals(new BigDecimal("500.00"), deliveryService.homeDeliveryFee("NIC"));
        assertEquals(new BigDecimal("2000.00"), deliveryService.homeDeliveryFee("PASSPORT"));
        assertEquals(
                BigDecimal.ZERO,
                deliveryService.deliveryFee("PASSPORT", DocumentDeliveryService.CENTRE_COLLECTION)
        );
    }

    @Test
    void uncollectedPassportKeepsItsOwnNumberAfterTheCitizenRenews() {
        DocumentDelivery original = prepareCollection(456, "NEW", "N0000456", "DEL-FIRST");
        assertEquals("N0000456", deliveryService.findCollection("DEL-FIRST").documentNumber());
        DocumentDelivery renewal = prepareCollection(457, "RENEWAL", "N0000457", "DEL-RENEWAL");

        assertEquals("N0000456", deliveryService.findCollection("DEL-FIRST").documentNumber());
        assertEquals("N0000457", deliveryService.findCollection("DEL-RENEWAL").documentNumber());
        assertEquals(DocumentDeliveryService.READY_FOR_COLLECTION, original.getDeliveryStatus());
        when(deliveryRepository.findAllByDeliveryMethodAndDeliveryStatusOrderByCreatedAtAsc(
                DocumentDeliveryService.CENTRE_COLLECTION, DocumentDeliveryService.READY_FOR_COLLECTION))
                .thenReturn(List.of(original, renewal));
        var collections = deliveryService.getReadyCollections();
        assertEquals("N0000456", collections.get(0).documentNumber());
        assertEquals("N0000457", collections.get(1).documentNumber());
    }

    @Test
    void collectionHistoryWithoutRecordedNumberSaysNotRecorded() {
        DocumentDelivery delivery = prepareCollection(456, "NEW", null, "DEL-OLD");
        CitizenApplication application = applicationRepository.findById(delivery.getApplicationId()).orElseThrow();
        for (String missingNumber : new String[]{null, "", "   "}) {
            application.setIssuedDocumentNumber(missingNumber);
            assertEquals("Not recorded", deliveryService.findCollection("DEL-OLD").documentNumber());
        }
    }

    private DocumentDelivery prepareCollection(int applicationId, String kind, String number, String reference) {
        CitizenApplication application = new CitizenApplication();
        application.setApplicationId(applicationId);
        application.setApplicationNo("APP-" + applicationId);
        application.setCitizenId(25);
        application.setServiceType("PASSPORT");
        application.setApplicationKind(kind);
        application.setCurrentStatus("ISSUED");
        application.setIssuedDocumentNumber(number);
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        CitizenAccount citizen = new CitizenAccount(25);
        citizen.setGovernmentRecordId(91);
        citizen.setGivenName("Nimal");
        citizen.setFamilyName("Perera");
        when(citizenRepository.findById(25)).thenReturn(Optional.of(citizen));
        UserAccount user = new UserAccount();
        user.setUserId(25);
        user.setEmail("citizen@example.com");
        user.setPhone("+94771234567");
        when(userRepository.findById(25)).thenReturn(Optional.of(user));
        DocumentDelivery delivery = new DocumentDelivery();
        delivery.setApplicationId(applicationId);
        delivery.setDeliveryMethod(DocumentDeliveryService.CENTRE_COLLECTION);
        delivery.setDeliveryStatus(DocumentDeliveryService.READY_FOR_COLLECTION);
        delivery.setTrackingReference(reference);
        when(deliveryRepository.findByTrackingReferenceIgnoreCase(reference)).thenReturn(Optional.of(delivery));
        return delivery;
    }
}
