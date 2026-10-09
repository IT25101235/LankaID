package lk.sliit.identityissuanceservice.Verification_And_Approval_Management.Service;

import lk.sliit.identityissuanceservice.Admin_Mgmt.Service.OfficerAccountService;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model.CitizenApplication;
import lk.sliit.identityissuanceservice.Verification_And_Approval_Management.Dto.DirectorReferralQueueItem;
import lk.sliit.identityissuanceservice.Verification_And_Approval_Management.Model.DirectorReviewReferral;
import lk.sliit.identityissuanceservice.Verification_And_Approval_Management.Repository.DirectorReviewReferralRepository;
import lk.sliit.identityissuanceservice.Verification_And_Approval_Management.Repository.VerificationApplicationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Business rules for creating, answering and logically deleting Director referrals. */
@Service
public class DirectorReviewReferralService {

    public static final String PENDING = "PENDING";
    public static final String RESPONDED = "RESPONDED";
    public static final String WITHDRAWN = "WITHDRAWN";

    private static final Set<String> CATEGORIES = Set.of(
            "IDENTITY_CONFLICT", "DOCUMENT_CONCERN", "POSSIBLE_DUPLICATE", "POLICY_EXCEPTION", "OTHER"
    );
    private static final Set<String> RECOMMENDATIONS = Set.of(
            "APPROVE", "REJECT", "REQUEST_CORRECTION"
    );

    private final DirectorReviewReferralRepository referralRepository;
    private final VerificationApplicationRepository applicationRepository;

    public DirectorReviewReferralService(
            DirectorReviewReferralRepository referralRepository,
            VerificationApplicationRepository applicationRepository
    ) {
        this.referralRepository = referralRepository;
        this.applicationRepository = applicationRepository;
    }

    @Transactional
    public DirectorReviewReferral refer(
            String reference,
            String serviceType,
            Integer staffId,
            String staffRole,
            String category,
            String reason
    ) {
        requireServiceOfficer(staffRole, serviceType);
        requireStaffId(staffId);
        CitizenApplication application = findApplication(reference, serviceType);
        if (!"UNDER_REVIEW".equals(application.getCurrentStatus())) {
            throw new IllegalArgumentException("Open the application for review before referring it to the Director.");
        }

        String cleanCategory = cleanCode(category);
        if (!CATEGORIES.contains(cleanCategory)) {
            throw new IllegalArgumentException("Select a valid reason for Director review.");
        }
        String cleanReason = requireText(reason, 20, 1000,
                "Explain the exceptional issue in at least 20 characters.");

        referralRepository.findByApplicationIdAndReferralStatus(application.getApplicationId(), PENDING)
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("This application already has a pending Director referral.");
                });
        referralRepository.findTopByApplicationIdOrderByCreatedAtDescReferralIdDesc(application.getApplicationId())
                .filter(previous -> RESPONDED.equals(previous.getReferralStatus()))
                .ifPresent(previous -> {
                    throw new IllegalArgumentException("The Director has already responded to this application.");
                });

        DirectorReviewReferral referral = new DirectorReviewReferral();
        referral.setApplicationId(application.getApplicationId());
        referral.setReferredByStaffId(staffId);
        referral.setReferralCategory(cleanCategory);
        referral.setReferralReason(cleanReason);
        referral.setReferralStatus(PENDING);
        return referralRepository.save(referral);
    }

    @Transactional
    public void withdraw(
            Integer referralId,
            String reference,
            String serviceType,
            Integer staffId,
            String staffRole,
            String reason
    ) {
        requireServiceOfficer(staffRole, serviceType);
        requireStaffId(staffId);
        DirectorReviewReferral referral = findReferralForUpdate(referralId);
        CitizenApplication application = findApplication(reference, serviceType);

        if (!application.getApplicationId().equals(referral.getApplicationId())) {
            throw new IllegalArgumentException("The referral does not belong to this application.");
        }
        if (!PENDING.equals(referral.getReferralStatus())) {
            throw new IllegalArgumentException("Only a pending Director referral can be withdrawn.");
        }
        if (!staffId.equals(referral.getReferredByStaffId())) {
            throw new IllegalArgumentException("Only the officer who created this referral can withdraw it.");
        }

        referral.setReferralStatus(WITHDRAWN);
        referral.setWithdrawnByStaffId(staffId);
        referral.setWithdrawalReason(requireText(reason, 10, 500,
                "Give a withdrawal reason of at least 10 characters."));
        referral.setWithdrawnAt(LocalDateTime.now());
        referralRepository.save(referral);
    }

    @Transactional
    public void respond(Integer referralId, Integer directorId, String recommendation, String comment) {
        requireStaffId(directorId);
        DirectorReviewReferral referral = findReferralForUpdate(referralId);
        if (!PENDING.equals(referral.getReferralStatus())) {
            throw new IllegalArgumentException("This referral is no longer awaiting a Director response.");
        }

        CitizenApplication application = applicationRepository.findById(referral.getApplicationId())
                .orElseThrow(() -> new IllegalArgumentException("The referred application was not found."));
        if (!"UNDER_REVIEW".equals(application.getCurrentStatus())) {
            throw new IllegalArgumentException("This application has already left the review stage.");
        }

        String cleanRecommendation = cleanCode(recommendation);
        if (!RECOMMENDATIONS.contains(cleanRecommendation)) {
            throw new IllegalArgumentException("Select a valid recommendation.");
        }

        referral.setReferralStatus(RESPONDED);
        referral.setDirectorId(directorId);
        referral.setRecommendation(cleanRecommendation);
        referral.setDirectorComment(requireText(comment, 10, 1000,
                "Explain the recommendation in at least 10 characters."));
        referral.setRespondedAt(LocalDateTime.now());
        referralRepository.save(referral);
    }

    @Transactional(readOnly = true)
    public List<DirectorReferralQueueItem> getPendingQueue() {
        return referralRepository.findPendingQueue();
    }

    @Transactional(readOnly = true)
    public DirectorReviewReferral getPendingReferral(Integer referralId) {
        DirectorReviewReferral referral = findReferral(referralId);
        if (!PENDING.equals(referral.getReferralStatus())) {
            throw new IllegalArgumentException("This referral is no longer pending.");
        }
        return referral;
    }

    @Transactional(readOnly = true)
    public CitizenApplication getApplication(DirectorReviewReferral referral) {
        return applicationRepository.findById(referral.getApplicationId())
                .orElseThrow(() -> new IllegalArgumentException("The referred application was not found."));
    }

    private DirectorReviewReferral findReferral(Integer referralId) {
        return referralRepository.findById(referralId)
                .orElseThrow(() -> new IllegalArgumentException("Director referral was not found."));
    }

    private DirectorReviewReferral findReferralForUpdate(Integer referralId) {
        return referralRepository.findByIdForUpdate(referralId)
                .orElseThrow(() -> new IllegalArgumentException("Director referral was not found."));
    }

    private CitizenApplication findApplication(String reference, String serviceType) {
        String normalizedService = cleanCode(serviceType);
        CitizenApplication application = applicationRepository.findByApplicationNoIgnoreCase(reference)
                .orElseThrow(() -> new IllegalArgumentException("Application was not found."));
        if (!normalizedService.equals(application.getServiceType())) {
            throw new IllegalArgumentException("Application does not belong to this verification queue.");
        }
        return application;
    }

    private void requireServiceOfficer(String role, String serviceType) {
        String expectedRole = VerificationReviewService.NIC.equals(cleanCode(serviceType))
                ? OfficerAccountService.NIC_OFFICER : OfficerAccountService.PASSPORT_OFFICER;
        if (!expectedRole.equals(role)) {
            throw new IllegalArgumentException("Only the assigned application officer can manage this referral.");
        }
    }

    private void requireStaffId(Integer staffId) {
        if (staffId == null) throw new IllegalArgumentException("Your officer session has expired. Sign in again.");
    }

    private String requireText(String value, int minimum, int maximum, String message) {
        String text = value == null ? "" : value.trim();
        if (text.length() < minimum || text.length() > maximum) throw new IllegalArgumentException(message);
        return text;
    }

    private String cleanCode(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
