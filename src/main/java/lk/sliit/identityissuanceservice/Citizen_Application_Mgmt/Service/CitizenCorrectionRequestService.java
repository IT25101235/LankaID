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
public class CitizenCorrectionRequestService {

    private static final String CORRECTION_REQUIRED = "CORRECTION_REQUIRED";

    private final CitizenApplicationRepository applicationRepository;
    private final ApplicationReviewRepository reviewRepository;

    public CitizenCorrectionRequestService(
            CitizenApplicationRepository applicationRepository,
            ApplicationReviewRepository reviewRepository
    ) {
        this.applicationRepository = applicationRepository;
        this.reviewRepository = reviewRepository;
    }

    @Transactional(readOnly = true)
    public String getCurrentCorrectionMessage(Integer citizenId, Integer applicationId) {
        return applicationRepository
                .findByApplicationIdAndCitizenId(applicationId, citizenId)
                .map(this::currentCorrectionMessage)
                .orElse(null);
    }

    @Transactional(readOnly = true)
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
