package lk.sliit.identityissuanceservice.Verification_And_Approval_Management.Service;

import lk.sliit.identityissuanceservice.Admin_Mgmt.Service.ApplicationFeeSettingsService;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Service.OfficerAccountService;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model.ApplicationDocument;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model.CitizenApplication;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Repository.ApplicationDocumentRepository;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Repository.NicApplicationRepository;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Repository.PassportApplicationRepository;
import lk.sliit.identityissuanceservice.Notification_Mgmt.Observer.NotificationPublisher;
import lk.sliit.identityissuanceservice.User_Mgmt.Model.CitizenAccount;
import lk.sliit.identityissuanceservice.User_Mgmt.Model.UserAccount;
import lk.sliit.identityissuanceservice.User_Mgmt.Repository.CitizenAccountRepository;
import lk.sliit.identityissuanceservice.User_Mgmt.Repository.UserAccountRepository;
import lk.sliit.identityissuanceservice.Verification_And_Approval_Management.Dto.VerificationDocumentFile;
import lk.sliit.identityissuanceservice.Verification_And_Approval_Management.Dto.VerificationReviewDetails;
import lk.sliit.identityissuanceservice.Verification_And_Approval_Management.Model.ApplicationReview;
import lk.sliit.identityissuanceservice.Verification_And_Approval_Management.Repository.ApplicationReviewRepository;
import lk.sliit.identityissuanceservice.Verification_And_Approval_Management.Repository.VerificationApplicationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/**
 * Core business service for verification and approval decisions.
 *
 * <p>This class acts as the workflow coordinator: it validates the application's current state,
 * updates document and application records, writes an immutable audit entry, and publishes the
 * corresponding citizen notification. Transaction boundaries keep those related changes atomic.</p>
 *
 * <p>OOP concepts: repository and notification collaborators are composed through constructor
 * injection; callers use high-level operations such as {@code approve} without knowing the
 * persistence steps, demonstrating abstraction and encapsulation.</p>
 */
@Service
public class VerificationReviewService {

    public static final String NIC = "NIC";
    public static final String PASSPORT = "PASSPORT";

    private final VerificationApplicationRepository applicationRepository;
    private final ApplicationDocumentRepository documentRepository;
    private final CitizenAccountRepository citizenRepository;
    private final UserAccountRepository userRepository;
    private final NicApplicationRepository nicRepository;
    private final PassportApplicationRepository passportRepository;
    private final ApplicationReviewRepository reviewRepository;
    private final NotificationPublisher notificationPublisher;
    private final ApplicationFeeSettingsService feeSettingsService;
    private final Path uploadRoot;

    public VerificationReviewService(
            VerificationApplicationRepository applicationRepository,
            ApplicationDocumentRepository documentRepository,
            CitizenAccountRepository citizenRepository,
            UserAccountRepository userRepository,
            NicApplicationRepository nicRepository,
            PassportApplicationRepository passportRepository,
            ApplicationReviewRepository reviewRepository,
            NotificationPublisher notificationPublisher,
            ApplicationFeeSettingsService feeSettingsService,
            @Value("${app.upload.directory:uploads}") String uploadDirectory
    ) {
        this.applicationRepository = applicationRepository;
        this.documentRepository = documentRepository;
        this.citizenRepository = citizenRepository;
        this.userRepository = userRepository;
        this.nicRepository = nicRepository;
        this.passportRepository = passportRepository;
        this.reviewRepository = reviewRepository;
        this.notificationPublisher = notificationPublisher;
        this.feeSettingsService = feeSettingsService;
        this.uploadRoot = Path.of(uploadDirectory).toAbsolutePath().normalize();
    }

    @Transactional
    public CitizenApplication startReview(String reference, String serviceType) {
        // Only a submitted or resubmitted application may enter the officer-review state.
        CitizenApplication application = findApplication(reference, serviceType);
        String status = application.getCurrentStatus();

        if ("SUBMITTED".equals(status) || "RESUBMITTED".equals(status)) {
            application.setCurrentStatus("UNDER_REVIEW");
            application = applicationRepository.save(application);
            notificationPublisher.sendToCitizen(
                    application.getCitizenId(),
                    null,
                    "APPLICATION_UNDER_REVIEW",
                    application.getServiceType() + " application " + application.getApplicationNo()
                            + " is now being reviewed by an application officer."
            );
            return application;
        }
        if ("UNDER_REVIEW".equals(status) || "CORRECTION_REQUIRED".equals(status)) {
            return application;
        }
        throw new IllegalArgumentException("This application is not available for verification.");
    }

    @Transactional(readOnly = true)
    public VerificationReviewDetails getReviewDetails(String reference, String serviceType) {
        // Compose one view object from the common application, citizen, service detail and documents.
        CitizenApplication application = findApplication(reference, serviceType);
        CitizenAccount citizen = citizenRepository.findById(application.getCitizenId())
                .orElseThrow(() -> new IllegalArgumentException("Citizen details were not found."));
        UserAccount user = userRepository.findById(application.getCitizenId())
                .orElseThrow(() -> new IllegalArgumentException("Citizen account was not found."));

        return new VerificationReviewDetails(
                application,
                citizen,
                user,
                nicRepository.findById(application.getApplicationId()).orElse(null),
                passportRepository.findById(application.getApplicationId()).orElse(null),
                documentRepository.findAllByApplicationIdOrderByUploadedAtAsc(application.getApplicationId()),
                buildApplicantName(citizen),
                buildPermanentAddress(citizen)
        );
    }

    @Transactional
    public void approve(String reference, String serviceType, Integer staffId) {
        // A final decision is legal only while an officer has the application under review.
        CitizenApplication application = findApplicationUnderReview(reference, serviceType);
        LocalDateTime now = LocalDateTime.now();

        List<ApplicationDocument> documents = documentRepository
                .findAllByApplicationIdOrderByUploadedAtAsc(application.getApplicationId());
        // Approval verifies every supporting document and records who performed the verification.
        for (ApplicationDocument document : documents) {
            document.setVerificationStatus("VERIFIED");
            document.setVerifiedByStaffId(staffId);
            document.setVerifiedAt(now);
        }
        documentRepository.saveAll(documents);

        application.setCurrentStatus("APPROVED");
        applicationRepository.save(application);
        // The audit record preserves the decision independently of the application's latest status.
        saveReview(application, staffId, "APPROVED", null);
        String fee = String.format(Locale.US, "%,.2f",
                feeSettingsService.getFee(application.getServiceType()));
        notificationPublisher.sendToCitizen(
                application.getCitizenId(),
                staffId,
                "APPLICATION_APPROVED",
                serviceType + " application " + application.getApplicationNo()
                        + " was approved. Pay LKR " + fee + " to book biometric collection."
        );
    }

    @Transactional
    public void reject(String reference, String serviceType, Integer staffId) {
        // Rejection updates both the parent application and all documents in one transaction.
        CitizenApplication application = findApplicationUnderReview(reference, serviceType);
        LocalDateTime now = LocalDateTime.now();

        List<ApplicationDocument> documents = documentRepository
                .findAllByApplicationIdOrderByUploadedAtAsc(application.getApplicationId());
        for (ApplicationDocument document : documents) {
            document.setVerificationStatus("REJECTED");
            document.setVerifiedByStaffId(staffId);
            document.setVerifiedAt(now);
        }
        documentRepository.saveAll(documents);

        application.setCurrentStatus("REJECTED");
        applicationRepository.save(application);
        saveReview(application, staffId, "REJECTED", null);
        notificationPublisher.sendToCitizen(
                application.getCitizenId(),
                staffId,
                "APPLICATION_REJECTED",
                serviceType + " application " + application.getApplicationNo() + " was rejected."
        );
    }

    @Transactional
    public void requestCorrection(
            String reference,
            String serviceType,
            boolean correctForm,
            List<Integer> documentIds,
            Integer staffId
    ) {
        // Correction is a reversible decision: the citizen edits and resubmits the same application.
        CitizenApplication application = findApplicationUnderReview(reference, serviceType);
        List<Integer> selectedDocumentIds = documentIds == null ? List.of() : documentIds;

        if (!correctForm && selectedDocumentIds.isEmpty()) {
            throw new IllegalArgumentException("Select the application form or at least one document.");
        }

        List<ApplicationDocument> documents = documentRepository
                .findAllByApplicationIdOrderByUploadedAtAsc(application.getApplicationId());
        List<ApplicationDocument> selectedDocuments = documents.stream()
                .filter(document -> selectedDocumentIds.contains(document.getDocumentId()))
                .toList();
        // Filtering against saved documents prevents IDs from another application being modified.
        if (!correctForm && selectedDocuments.isEmpty()) {
            throw new IllegalArgumentException("Select the application form or at least one document.");
        }

        List<String> requestedItems = new java.util.ArrayList<>();
        if (correctForm) requestedItems.add("Application form details");
        selectedDocuments.stream()
                .map(document -> displayDocumentType(document.getDocumentType()))
                .forEach(requestedItems::add);
        String correctionMessage = String.join(", ", requestedItems);

        for (ApplicationDocument document : selectedDocuments) {
            // Selected files must be replaced, so their earlier verification metadata is cleared.
            document.setVerificationStatus("REJECTED");
            document.setVerifiedByStaffId(null);
            document.setVerifiedAt(null);
        }
        documentRepository.saveAll(documents);

        application.setCurrentStatus("CORRECTION_REQUIRED");
        applicationRepository.save(application);
        saveReview(application, staffId, "CORRECTION_REQUIRED", correctionMessage);
        notificationPublisher.sendToCitizen(
                application.getCitizenId(),
                staffId,
                "CORRECTION_REQUIRED",
                "Corrections are required for " + application.getApplicationNo() + ": "
                        + String.join(", ", requestedItems) + ". Update and resubmit the same application."
        );
    }

    private void saveReview(
            CitizenApplication application,
            Integer staffId,
            String decision,
            String correctionMessage
    ) {
        // Sequence numbers retain the complete order when an application is corrected and reviewed again.
        ApplicationReview review = new ApplicationReview();
        review.setApplicationId(application.getApplicationId());
        review.setStaffId(staffId);
        review.setReviewSequence(reviewRepository
                .findTopByApplicationIdOrderByReviewSequenceDesc(application.getApplicationId())
                .map(previous -> previous.getReviewSequence() + 1)
                .orElse(1));
        review.setDecision(decision);
        review.setCorrectionMessage(correctionMessage);
        reviewRepository.save(review);
    }

    @Transactional(readOnly = true)
    public VerificationDocumentFile getDocumentForOfficer(Integer documentId, String officerRole) {
        // Authorization depends on the owning application's service type, not on a browser parameter.
        ApplicationDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document was not found."));
        CitizenApplication application = applicationRepository.findById(document.getApplicationId())
                .orElseThrow(() -> new IllegalArgumentException("Application was not found."));

        checkOfficerRole(officerRole, application.getServiceType());

        Path file = uploadRoot.resolve(document.getFilePath()).normalize();
        // Normalization plus startsWith blocks path traversal outside the configured upload directory.
        if (!file.startsWith(uploadRoot) || !Files.isRegularFile(file)) {
            throw new IllegalArgumentException("The uploaded file is not available.");
        }

        String contentType;
        try {
            contentType = Files.probeContentType(file);
        } catch (IOException exception) {
            contentType = null;
        }
        if (contentType == null) {
            contentType = contentTypeFromName(file.getFileName().toString());
        }

        return new VerificationDocumentFile(
                new FileSystemResource(file),
                contentType,
                document.getDocumentType().toLowerCase(Locale.ROOT).replace('_', '-')
                        + fileExtension(file.getFileName().toString())
        );
    }

    private CitizenApplication findApplication(String reference, String serviceType) {
        // Checking the service prevents a NIC reference from being opened through passport routes.
        CitizenApplication application = applicationRepository.findByApplicationNoIgnoreCase(reference)
                .orElseThrow(() -> new IllegalArgumentException("Application was not found."));
        if (!serviceType.equals(application.getServiceType())) {
            throw new IllegalArgumentException("Application does not belong to this verification queue.");
        }
        return application;
    }

    private CitizenApplication findApplicationUnderReview(String reference, String serviceType) {
        // This state guard prevents duplicate or out-of-order approval decisions.
        CitizenApplication application = findApplication(reference, serviceType);
        if (!"UNDER_REVIEW".equals(application.getCurrentStatus())) {
            throw new IllegalArgumentException("Open the submitted application for review before making a decision.");
        }
        return application;
    }

    private void checkOfficerRole(String officerRole, String serviceType) {
        // Polymorphic role behaviour: directors handle both services; officers handle one service.
        boolean director = OfficerAccountService.DIRECTOR.equals(officerRole);
        boolean serviceOfficer = NIC.equals(serviceType)
                ? OfficerAccountService.NIC_OFFICER.equals(officerRole)
                : OfficerAccountService.PASSPORT_OFFICER.equals(officerRole);
        if (!director && !serviceOfficer) {
            throw new IllegalArgumentException("You do not have access to this document.");
        }
    }

    private String buildApplicantName(CitizenAccount citizen) {
        String name = (clean(citizen.getGivenName()) + " " + clean(citizen.getFamilyName())).trim();
        if (!name.isBlank()) return name;
        String surname = clean(citizen.getSurname());
        return surname.isBlank() ? "Citizen #" + citizen.getUserId() : surname;
    }

    private String buildPermanentAddress(CitizenAccount citizen) {
        return String.join(", ", List.of(
                        clean(citizen.getPermanentHouseNo()),
                        clean(citizen.getPermanentStreet()),
                        clean(citizen.getPermanentCity()),
                        clean(citizen.getPermanentPostalCode())
                ).stream().filter(value -> !value.isBlank()).toList());
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String displayDocumentType(String documentType) {
        String label = clean(documentType).toLowerCase(Locale.ROOT).replace('_', ' ');
        return label.isEmpty() ? "Document" : Character.toUpperCase(label.charAt(0)) + label.substring(1);
    }

    private String contentTypeFromName(String fileName) {
        String lowerName = fileName.toLowerCase(Locale.ROOT);
        if (lowerName.endsWith(".pdf")) return "application/pdf";
        if (lowerName.endsWith(".png")) return "image/png";
        return "image/jpeg";
    }

    private String fileExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? "" : fileName.substring(dot);
    }
}
