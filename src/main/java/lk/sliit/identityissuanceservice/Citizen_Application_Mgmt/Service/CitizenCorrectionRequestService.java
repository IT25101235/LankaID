package lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Service;

import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model.CitizenApplication;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Repository.CitizenApplicationRepository;
import lk.sliit.identityissuanceservice.Verification_And_Approval_Management.Model.ApplicationReview;
import lk.sliit.identityissuanceservice.Verification_And_Approval_Management.Repository.ApplicationReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
// Reads the latest officer correction message for citizen applications.
public class CitizenCorrectionRequestService {

    private static final String CORRECTION_REQUIRED = "CORRECTION_REQUIRED";

    private final CitizenApplicationRepository applicationRepository;
    private final ApplicationReviewRepository reviewRepository;

    // Stores the repositories used to find applications and reviews.
    public CitizenCorrectionRequestService(
            CitizenApplicationRepository applicationRepository,
            ApplicationReviewRepository reviewRepository
    ) {
        this.applicationRepository = applicationRepository;
        this.reviewRepository = reviewRepository;
    }

    @Transactional(readOnly = true)
    // Returns the latest correction message for one citizen-owned application.
    public String getCurrentCorrectionMessage(Integer citizenId, Integer applicationId) {
        return applicationRepository
                .findByApplicationIdAndCitizenId(applicationId, citizenId)
                .map(this::currentCorrectionMessage)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    // Returns current correction messages indexed by application ID.
    public Map<Integer, String> getCurrentCorrectionMessages(Integer citizenId) {
        Map<Integer, String> messages = new LinkedHashMap<>();
        applicationRepository.findAllByCitizenIdOrderByCreatedAtDesc(citizenId).stream()
                .filter(application -> CORRECTION_REQUIRED.equals(application.getCurrentStatus()))
                .forEach(application -> {
                    String message = currentCorrectionMessage(application);
                    if (message != null) {
                        messages.put(application.getApplicationId(), message);
                    }
                });
        return messages;
    }

    // Extracts a non-empty message from the latest correction review.
    private String currentCorrectionMessage(CitizenApplication application) {
        if (!CORRECTION_REQUIRED.equals(application.getCurrentStatus())) {
            return null;
        }
        return reviewRepository
                .findTopByApplicationIdOrderByReviewSequenceDesc(application.getApplicationId())
                .filter(review -> CORRECTION_REQUIRED.equals(review.getDecision()))
                .map(ApplicationReview::getCorrectionMessage)
                .map(String::trim)
                .filter(message -> !message.isEmpty())
                .orElse(null);
    }
}
