package lk.sliit.identityissuanceservice;

import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model.CitizenApplication;
import lk.sliit.identityissuanceservice.User_Mgmt.Model.CitizenAccount;
import lk.sliit.identityissuanceservice.User_Mgmt.Repository.CitizenAccountRepository;
import lk.sliit.identityissuanceservice.Verification_And_Approval_Management.Dto.VerificationQueueItem;
import lk.sliit.identityissuanceservice.Verification_And_Approval_Management.Repository.VerificationApplicationRepository;
import lk.sliit.identityissuanceservice.Verification_And_Approval_Management.Service.VerificationQueueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VerificationQueueServiceTests {

    private VerificationApplicationRepository applicationRepository;
    private CitizenAccountRepository citizenAccountRepository;
    private VerificationQueueService verificationQueueService;

    @BeforeEach
    void setUp() {
        applicationRepository = mock(VerificationApplicationRepository.class);
        citizenAccountRepository = mock(CitizenAccountRepository.class);
        verificationQueueService = new VerificationQueueService(applicationRepository, citizenAccountRepository);
    }

    @Test
    void nicQueueCombinesApplicationAndCitizenDetails() {
        CitizenApplication application = new CitizenApplication();
        application.setApplicationId(21);
        application.setApplicationNo("LK-NIC-2026-000021");
        application.setCitizenId(8);
        application.setServiceType("NIC");
        application.setApplicationKind("NEW");
        application.setCurrentStatus("SUBMITTED");
        application.setSubmittedAt(LocalDateTime.of(2026, 8, 28, 9, 30));

        CitizenAccount citizen = new CitizenAccount(8);
        citizen.setGivenName("Nimal");
        citizen.setFamilyName("Perera");
        citizen.setDateOfBirth(LocalDateTime.of(2000, 4, 12, 0, 0));

        when(applicationRepository
                .findByServiceTypeAndCurrentStatusInOrderBySubmittedAtAscApplicationIdAsc(eq("NIC"), any()))
                .thenReturn(List.of(application));
        when(citizenAccountRepository.findAllById(any())).thenReturn(List.of(citizen));

        List<VerificationQueueItem> queue = verificationQueueService.getNicQueue();

        assertEquals(1, queue.size());
        assertEquals("Nimal Perera", queue.get(0).applicantName());
        assertEquals("NP", queue.get(0).applicantInitials());
        assertEquals("New NIC", queue.get(0).applicationKindLabel());
        assertEquals("Submitted", queue.get(0).statusLabel());
        verify(applicationRepository)
                .findByServiceTypeAndCurrentStatusInOrderBySubmittedAtAscApplicationIdAsc(eq("NIC"), any());
    }

    @Test
    void passportQueueUsesSafeNameWhenCitizenDetailsAreUnavailable() {
        CitizenApplication application = new CitizenApplication();
        application.setApplicationId(31);
        application.setApplicationNo("LK-PASS-2026-000031");
        application.setCitizenId(14);
        application.setServiceType("PASSPORT");
        application.setApplicationKind("RENEWAL");
        application.setCurrentStatus("UNDER_REVIEW");

        when(applicationRepository
                .findByServiceTypeAndCurrentStatusInOrderBySubmittedAtAscApplicationIdAsc(eq("PASSPORT"), any()))
                .thenReturn(List.of(application));
        when(citizenAccountRepository.findAllById(any())).thenReturn(List.of());

        VerificationQueueItem queueItem = verificationQueueService.getPassportQueue().get(0);

        assertEquals("Citizen #14", queueItem.applicantName());
        assertEquals("C", queueItem.applicantInitials());
        assertEquals("Renewal", queueItem.applicationKindLabel());
        assertEquals("Under review", queueItem.statusLabel());
    }
}
