package lk.sliit.identityissuanceservice.Verification_And_Approval_Management.Service;

import lk.sliit.identityissuanceservice.Admin_Mgmt.Service.OfficerAccountService;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model.ApplicationDocument;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model.CitizenApplication;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Repository.ApplicationDocumentRepository;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Repository.NicApplicationRepository;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Repository.PassportApplicationRepository;
import lk.sliit.identityissuanceservice.Notification_Mgmt.Service.NotificationService;
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
    private final NotificationService notificationService;
    private final Path uploadRoot;

    public VerificationReviewService(
            VerificationApplicationRepository applicationRepository,
            ApplicationDocumentRepository documentRepository,
            CitizenAccountRepository citizenRepository,
            UserAccountRepository userRepository,
            NicApplicationRepository nicRepository,
            PassportApplicationRepository passportRepository,
            ApplicationReviewRepository reviewRepository,
            NotificationService notificationService,
            @Value("${app.upload.directory:uploads}") String uploadDirectory
    ) {
        this.applicationRepository = applicationRepository;
        this.documentRepository = documentRepository;
        this.citizenRepository = citizenRepository;
        this.userRepository = userRepository;
        this.nicRepository = nicRepository;
        this.passportRepository = passportRepository;
        this.reviewRepository = reviewRepository;
        this.notificationService = notificationService;
        this.uploadRoot = Path.of(uploadDirectory).toAbsolutePath().normalize();
    }

    @Transactional
    public CitizenApplication startReview(String reference, String serviceType) {
        CitizenApplication application = findApplication(reference, serviceType);
        String status = application.getCurrentStatus();

        if ("SUBMITTED".equals(status) || "RESUBMITTED".equals(status)) {
            application.setCurrentStatus("UNDER_REVIEW");
            application = applicationRepository.save(application);
            notificationService.sendToCitizen(
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
        CitizenApplication application = findApplicationUnderReview(reference, serviceType);
        LocalDateTime now = LocalDateTime.now();

        List<ApplicationDocument> documents = documentRepository
                .findAllByApplicationIdOrderByUploadedAtAsc(application.getApplicationId());
        for (ApplicationDocument document : documents) {
            document.setVerificationStatus("VERIFIED");
            document.setVerifiedByStaffId(staffId);
            document.setVerifiedAt(now);
        }
        documentRepository.saveAll(documents);

        application.setCurrentStatus("APPROVED");
        applicationRepository.save(application);
        saveReview(application, staffId, "APPROVED", null);
        String fee = NIC.equals(serviceType) ? "2,000" : "10,000";
        notificationService.sendToCitizen(
                application.getCitizenId(),
                staffId,
                "APPLICATION_APPROVED",
                serviceType + " application " + application.getApplicationNo()
                        + " was approved. Pay LKR " + fee + " to book biometric collection."
        );
    }

    @Transactional
    public void reject(String reference, String serviceType, Integer staffId) {
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
        notificationService.sendToCitizen(
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
            document.setVerificationStatus("REJECTED");
            document.setVerifiedByStaffId(null);
            document.setVerifiedAt(null);
        }
        documentRepository.saveAll(documents);

        application.setCurrentStatus("CORRECTION_REQUIRED");
        applicationRepository.save(application);
        saveReview(application, staffId, "CORRECTION_REQUIRED", correctionMessage);
        notificationService.sendToCitizen(
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
        ApplicationDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document was not found."));
        CitizenApplication application = applicationRepository.findById(document.getApplicationId())
                .orElseThrow(() -> new IllegalArgumentException("Application was not found."));

        checkOfficerRole(officerRole, application.getServiceType());

        Path file = uploadRoot.resolve(document.getFilePath()).normalize();
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
        CitizenApplication application = applicationRepository.findByApplicationNoIgnoreCase(reference)
                .orElseThrow(() -> new IllegalArgumentException("Application was not found."));
        if (!serviceType.equals(application.getServiceType())) {
            throw new IllegalArgumentException("Application does not belong to this verification queue.");
        }
        return application;
    }

    private CitizenApplication findApplicationUnderReview(String reference, String serviceType) {
        CitizenApplication application = findApplication(reference, serviceType);
        if (!"UNDER_REVIEW".equals(application.getCurrentStatus())) {
            throw new IllegalArgumentException("Open the submitted application for review before making a decision.");
        }
        return application;
    }

    private void checkOfficerRole(String officerRole, String serviceType) {
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
