package lk.sliit.identityissuanceservice;

import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Repository.CentreRepository;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Repository.CitizenApplicationRepository;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Repository.GovernmentIdentityRecordRepository;
import lk.sliit.identityissuanceservice.Document_Delivery_Mgmt.Repository.DocumentDeliveryRepository;
import lk.sliit.identityissuanceservice.Document_Delivery_Mgmt.Service.DocumentDeliveryService;
import lk.sliit.identityissuanceservice.Notification_Mgmt.Service.NotificationService;
import lk.sliit.identityissuanceservice.User_Mgmt.Repository.CitizenAccountRepository;
import lk.sliit.identityissuanceservice.User_Mgmt.Repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class DocumentDeliveryServiceTests {

    private DocumentDeliveryService deliveryService;

    @BeforeEach
    void setUp() {
        deliveryService = new DocumentDeliveryService(
                mock(DocumentDeliveryRepository.class),
                mock(CitizenApplicationRepository.class),
                mock(CitizenAccountRepository.class),
                mock(UserAccountRepository.class),
                mock(CentreRepository.class),
                mock(GovernmentIdentityRecordRepository.class),
                mock(NotificationService.class)
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
}
