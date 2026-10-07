package lk.sliit.identityissuanceservice;

import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Dto.CitizenEligibilityDto;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model.CitizenApplication;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Repository.ApplicationDocumentRepository;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Repository.CitizenApplicationRepository;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Repository.NicApplicationRepository;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Repository.PassportApplicationRepository;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Service.CitizenApplicationService;
import lk.sliit.identityissuanceservice.Document_Renewal_Replacement_And_Delivery_Mgmt.Service.DocumentRenewalReplacementService;
import lk.sliit.identityissuanceservice.Notification_Mgmt.Observer.NotificationPublisher;
import lk.sliit.identityissuanceservice.User_Mgmt.Model.CitizenAccount;
import lk.sliit.identityissuanceservice.User_Mgmt.Repository.CitizenAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentRenewalReplacementServiceTests {

    @Mock CitizenApplicationService eligibilityService;
    @Mock CitizenAccountRepository citizenRepository;
    @Mock CitizenApplicationRepository applicationRepository;
    @Mock NicApplicationRepository nicRepository;
    @Mock PassportApplicationRepository passportRepository;
    @Mock ApplicationDocumentRepository documentRepository;
    @Mock NotificationPublisher notificationPublisher;

    private DocumentRenewalReplacementService renewalService;

    @BeforeEach
    void setUp() {
        renewalService = new DocumentRenewalReplacementService(
                eligibilityService,
                citizenRepository,
                applicationRepository,
                nicRepository,
                passportRepository,
                documentRepository,
                notificationPublisher,
                "target/test-renewal-uploads"
        );
    }

    @Test
    void renewalServicePrefillsExistingNicNumber() {
        CitizenAccount citizen = new CitizenAccount(25);
        when(eligibilityService.getEligibility(25))
                .thenReturn(new CitizenEligibilityDto(true, false, false, "200012345678", null));
        when(citizenRepository.findById(25)).thenReturn(Optional.of(citizen));

        var form = renewalService.prepareNicForm(25, "renewal");

        assertEquals("200012345678", form.getLastNicNo());
    }

    @Test
    void renewalServiceDoesNotAcceptNewApplicationType() {
        assertThrows(
                IllegalArgumentException.class,
                () -> renewalService.prepareNicForm(25, "new")
        );
    }

    @Test
    void submittedRenewalDocumentFormCannotBeOpened() {
        CitizenApplication application = new CitizenApplication();
        application.setApplicationId(90);
        application.setCitizenId(25);
        application.setServiceType("NIC");
        application.setApplicationKind("RENEWAL");
        application.setCurrentStatus("SUBMITTED");
        when(applicationRepository.findByApplicationIdAndCitizenId(90, 25))
                .thenReturn(Optional.of(application));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> renewalService.getDocuments(25, 90)
        );

        assertEquals(
                "This application is locked after submission. It can only be edited when an officer requests a correction.",
                exception.getMessage()
        );
    }
}
