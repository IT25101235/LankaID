package lk.sliit.identityissuanceservice;

import lk.sliit.identityissuanceservice.Admin_Mgmt.Service.ApplicationFeeSettingsService;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model.CitizenApplication;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Repository.ApplicationDocumentRepository;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Repository.NicApplicationRepository;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Repository.PassportApplicationRepository;
import lk.sliit.identityissuanceservice.Notification_Mgmt.Service.NotificationService;
import lk.sliit.identityissuanceservice.User_Mgmt.Repository.CitizenAccountRepository;
import lk.sliit.identityissuanceservice.User_Mgmt.Repository.UserAccountRepository;
import lk.sliit.identityissuanceservice.Verification_And_Approval_Management.Repository.ApplicationReviewRepository;
import lk.sliit.identityissuanceservice.Verification_And_Approval_Management.Repository.VerificationApplicationRepository;
import lk.sliit.identityissuanceservice.Verification_And_Approval_Management.Service.VerificationReviewService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerificationReviewFeeNotificationTests {

    @Mock VerificationApplicationRepository applicationRepository;
    @Mock ApplicationDocumentRepository documentRepository;
    @Mock CitizenAccountRepository citizenRepository;
    @Mock UserAccountRepository userRepository;
    @Mock NicApplicationRepository nicRepository;
    @Mock PassportApplicationRepository passportRepository;
    @Mock ApplicationReviewRepository reviewRepository;
    @Mock NotificationService notificationService;
    @Mock ApplicationFeeSettingsService feeSettingsService;

    @Test
    void approvalNotificationUsesTheConfiguredFee() {
        CitizenApplication application = new CitizenApplication();
        application.setApplicationId(42);
        application.setApplicationNo("NIC-TEST-42");
        application.setCitizenId(7);
        application.setServiceType("NIC");
        application.setCurrentStatus("UNDER_REVIEW");

        when(applicationRepository.findByApplicationNoIgnoreCase("NIC-TEST-42"))
                .thenReturn(Optional.of(application));
        when(documentRepository.findAllByApplicationIdOrderByUploadedAtAsc(42))
                .thenReturn(List.of());
        when(reviewRepository.findTopByApplicationIdOrderByReviewSequenceDesc(42))
                .thenReturn(Optional.empty());
        when(feeSettingsService.getFee("NIC")).thenReturn(new BigDecimal("3456.78"));

        VerificationReviewService service = new VerificationReviewService(
                applicationRepository,
                documentRepository,
                citizenRepository,
                userRepository,
                nicRepository,
                passportRepository,
                reviewRepository,
                notificationService,
                feeSettingsService,
                "target/test-uploads"
        );

        service.approve("NIC-TEST-42", "NIC", 9);

        verify(notificationService).sendToCitizen(
                eq(7),
                eq(9),
                eq("APPLICATION_APPROVED"),
                contains("Pay LKR 3,456.78")
        );
    }
}
