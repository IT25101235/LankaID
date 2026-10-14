package lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Service;

import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronization;

import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Dto.CitizenEligibilityDto;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Dto.CitizenDocumentFile;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Dto.CitizenProfileMapper;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Dto.NewNicApplicationForm;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Dto.NewPassportApplicationForm;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model.ApplicationDocument;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model.CitizenApplication;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model.GovernmentIdentityRecord;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model.NicApplication;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model.PassportApplication;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Repository.ApplicationDocumentRepository;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Repository.CitizenApplicationRepository;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Repository.GovernmentIdentityRecordRepository;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Repository.NicApplicationRepository;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Repository.PassportApplicationRepository;
import lk.sliit.identityissuanceservice.Notification_Mgmt.Observer.NotificationPublisher;
import lk.sliit.identityissuanceservice.User_Mgmt.Model.CitizenAccount;
import lk.sliit.identityissuanceservice.User_Mgmt.Model.UserAccount;
import lk.sliit.identityissuanceservice.User_Mgmt.Repository.CitizenAccountRepository;
import lk.sliit.identityissuanceservice.User_Mgmt.Repository.UserAccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
// Handles the business rules for new NIC and passport applications.
public class CitizenApplicationService {

    // Values used to identify the supported services and application kind.
    public static final String NIC = "NIC";
    public static final String PASSPORT = "PASSPORT";
    public static final String NEW = "NEW";

    // Statuses that mean a citizen already has an application in progress.
    private static final Set<String> ACTIVE_STATUSES = Set.of(
            "DRAFT", "SUBMITTED", "UNDER_REVIEW", "CORRECTION_REQUIRED", "RESUBMITTED",
            "APPROVED", "PAYMENT_PENDING", "APPOINTMENT_BOOKED", "BIOMETRIC_COLLECTED", "PROCESSING"
    );
    // Document types and file rules accepted by the upload workflow.
    private static final Set<String> ALLOWED_DOCUMENT_TYPES = Set.of(
            "PHOTOGRAPH", "BIRTH_CERTIFICATE", "POLICE_REPORT",
            "NIC_PROOF", "CITIZENSHIP_CERTIFICATE"
    );
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "jpg", "jpeg", "png");
    private static final long MAX_FILE_SIZE = 5L * 1024L * 1024L;

    // Repositories and collaborators are supplied by Spring through the constructor.
    private final UserAccountRepository userRepository;
    private final CitizenAccountRepository citizenRepository;
    private final GovernmentIdentityRecordRepository governmentRepository;
    private final CitizenApplicationRepository applicationRepository;
    private final NicApplicationRepository nicRepository;
    private final PassportApplicationRepository passportRepository;
    private final ApplicationDocumentRepository documentRepository;
    private final NotificationPublisher notificationPublisher;
    private final Path uploadRoot;

    // Stores the dependencies used by the application workflow.
    public CitizenApplicationService(
            UserAccountRepository userRepository,
            CitizenAccountRepository citizenRepository,
            GovernmentIdentityRecordRepository governmentRepository,
            CitizenApplicationRepository applicationRepository,
            NicApplicationRepository nicRepository,
            PassportApplicationRepository passportRepository,
            ApplicationDocumentRepository documentRepository,
            NotificationPublisher notificationPublisher,
            @Value("${app.upload.directory:uploads}") String uploadDirectory) {
        this.userRepository = userRepository;
        this.citizenRepository = citizenRepository;
        this.governmentRepository = governmentRepository;
        this.applicationRepository = applicationRepository;
        this.nicRepository = nicRepository;
        this.passportRepository = passportRepository;
        this.documentRepository = documentRepository;
        this.notificationPublisher = notificationPublisher;
        this.uploadRoot = Path.of(uploadDirectory).toAbsolutePath().normalize();
    }

    @Transactional
    // Checks the government registry and returns the citizen's NIC and passport eligibility.
    public CitizenEligibilityDto getEligibility(Integer citizenId) {
        UserAccount user = findUser(citizenId);
        CitizenAccount citizen = findCitizen(citizenId);
        IdentityMatch match = findGovernmentIdentity(user, citizen);

        if (match.conflict()) {
            return new CitizenEligibilityDto(false, false, true, null, null);
        }

        GovernmentIdentityRecord record = match.record();
        boolean usableRecord = record != null && !"CANCELLED".equals(record.getIdentityStatus());
        String nicNumber = usableRecord ? CitizenProfileMapper.blankToNull(record.getNicNo()) : null;
        String passportNumber = usableRecord ? CitizenProfileMapper.blankToNull(record.getPassportNo()) : null;
        return new CitizenEligibilityDto(
                nicNumber != null,
                passportNumber != null,
                false,
                nicNumber,
                passportNumber,
                usableRecord ? record.getNicIssuedDate() : null
        );
    }

    @Transactional
    // Creates a pre-filled form for a first-time NIC application.
    public NewNicApplicationForm prepareNewNicForm(Integer citizenId) {
        checkNewEligibility(getEligibility(citizenId), NIC);
        checkForActiveApplication(citizenId, NIC);
        NewNicApplicationForm form = new NewNicApplicationForm();
        CitizenProfileMapper.copyToForm(findCitizen(citizenId), form);
        return form;
    }

    @Transactional
    // Creates a pre-filled form for a first-time passport application.
    public NewPassportApplicationForm prepareNewPassportForm(Integer citizenId) {
        CitizenEligibilityDto eligibility = getEligibility(citizenId);
        checkNewEligibility(eligibility, PASSPORT);
        checkForActiveApplication(citizenId, PASSPORT);
        NewPassportApplicationForm form = new NewPassportApplicationForm();
        CitizenProfileMapper.copyToForm(findCitizen(citizenId), form);
        if (eligibility.isHasNic()) {
            form.setNicProofNo(eligibility.getNicNumber());
        }
        return form;
    }

    @Transactional(readOnly = true)
    // Loads an NIC application that an officer returned for correction.
    public NewNicApplicationForm prepareNewNicCorrection(Integer citizenId, Integer applicationId) {
        CitizenApplication application = getCorrectionApplication(citizenId, applicationId, NIC);
        if (!NEW.equals(application.getApplicationKind())) {
            throw new IllegalArgumentException("This correction is not a new NIC application.");
        }

        NewNicApplicationForm form = new NewNicApplicationForm();
        CitizenProfileMapper.copyToForm(findCitizen(citizenId), form);
        return form;
    }

    @Transactional(readOnly = true)
    // Loads a saved NIC draft for editing.
    public NewNicApplicationForm prepareNewNicDraftEdit(Integer citizenId, Integer applicationId) {
        CitizenApplication application = getEditableNewApplication(citizenId, applicationId);
        if (!NIC.equals(application.getServiceType()) || !"DRAFT".equals(application.getCurrentStatus())) {
            throw new IllegalArgumentException("Only an unsubmitted new NIC draft can be edited here.");
        }

        NewNicApplicationForm form = new NewNicApplicationForm();
        CitizenProfileMapper.copyToForm(findCitizen(citizenId), form);
        return form;
    }

    @Transactional(readOnly = true)
    // Loads a passport application that an officer returned for correction.
    public NewPassportApplicationForm prepareNewPassportCorrection(Integer citizenId, Integer applicationId) {
        CitizenApplication application = getCorrectionApplication(citizenId, applicationId, PASSPORT);
        if (!NEW.equals(application.getApplicationKind())) {
            throw new IllegalArgumentException("This correction is not a new passport application.");
        }

        NewPassportApplicationForm form = new NewPassportApplicationForm();
        CitizenProfileMapper.copyToForm(findCitizen(citizenId), form);
        PassportApplication passport = passportRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Passport application details were not found."));
        form.setServiceLevel(passport.getServiceLevel());
        form.setTravelDocumentType(passport.getTravelDocumentType());
        form.setNicProofNo(passport.getNicProofNo());
        form.setParentNicNo(passport.getParentNicNo());
        return form;
    }

    @Transactional(readOnly = true)
    // Loads a saved passport draft for editing.
    public NewPassportApplicationForm prepareNewPassportDraftEdit(Integer citizenId, Integer applicationId) {
        CitizenApplication application = getEditableNewApplication(citizenId, applicationId);
        if (!PASSPORT.equals(application.getServiceType()) || !"DRAFT".equals(application.getCurrentStatus())) {
            throw new IllegalArgumentException("Only an unsubmitted new passport draft can be edited here.");
        }

        NewPassportApplicationForm form = new NewPassportApplicationForm();
        CitizenProfileMapper.copyToForm(findCitizen(citizenId), form);
        PassportApplication passport = passportRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Passport application details were not found."));
        form.setServiceLevel(passport.getServiceLevel());
        form.setTravelDocumentType(passport.getTravelDocumentType());
        form.setNicProofNo(passport.getNicProofNo());
        form.setParentNicNo(passport.getParentNicNo());
        return form;
    }

    @Transactional
    // Validates the form and creates a new NIC application draft.
    public CitizenApplication createNewNicApplication(Integer citizenId, NewNicApplicationForm form) {
        checkNewEligibility(getEligibility(citizenId), NIC);
        validateNicAge(form.getDateOfBirth());
        CitizenProfileMapper.validateConditionalFields(form);
        checkForActiveApplication(citizenId, NIC);

        CitizenAccount citizen = findCitizen(citizenId);
        CitizenProfileMapper.updateCitizen(citizen, form);
        citizenRepository.save(citizen);

        CitizenApplication application = createBaseApplication(citizenId, NIC);
        NicApplication nicApplication = new NicApplication();
        nicApplication.setApplicationId(application.getApplicationId());
        nicRepository.save(nicApplication);
        return application;
    }

    @Transactional
    // Validates the form and creates a new passport application draft.
    public CitizenApplication createNewPassportApplication(
            Integer citizenId,
            NewPassportApplicationForm form) {
        CitizenEligibilityDto eligibility = getEligibility(citizenId);
        checkNewEligibility(eligibility, PASSPORT);
        CitizenProfileMapper.validateConditionalFields(form);
        validatePassportProof(eligibility, form.getNicProofNo(), form.getParentNicNo());
        checkForActiveApplication(citizenId, PASSPORT);

        CitizenAccount citizen = findCitizen(citizenId);
        CitizenProfileMapper.updateCitizen(citizen, form);
        citizenRepository.save(citizen);

        CitizenApplication application = createBaseApplication(citizenId, PASSPORT);
        PassportApplication passportApplication = new PassportApplication();
        passportApplication.setApplicationId(application.getApplicationId());
        passportApplication.setServiceLevel("NORMAL");
        passportApplication.setTravelDocumentType("ALL_COUNTRIES");
        passportApplication.setNicProofNo(CitizenProfileMapper.blankToNull(form.getNicProofNo()));
        passportApplication.setParentNicNo(CitizenProfileMapper.blankToNull(form.getParentNicNo()));
        passportRepository.save(passportApplication);
        return application;
    }

    @Transactional
    // Applies citizen changes to an editable NIC draft or correction.
    public CitizenApplication updateNewNicApplication(
            Integer citizenId,
            Integer applicationId,
            NewNicApplicationForm form
    ) {
        CitizenApplication application = getEditableNewApplication(citizenId, applicationId);
        if (!NIC.equals(application.getServiceType())) {
            throw new IllegalArgumentException("This is not a new NIC application.");
        }
        validateNicAge(form.getDateOfBirth());
        CitizenProfileMapper.validateConditionalFields(form);
        CitizenAccount citizen = findCitizen(citizenId);
        CitizenProfileMapper.updateCitizen(citizen, form);
        citizenRepository.save(citizen);
        return application;
    }

    @Transactional
    // Applies citizen and passport changes to an editable passport application.
    public CitizenApplication updateNewPassportApplication(
            Integer citizenId,
            Integer applicationId,
            NewPassportApplicationForm form
    ) {
        CitizenApplication application = getEditableNewApplication(citizenId, applicationId);
        if (!PASSPORT.equals(application.getServiceType())) {
            throw new IllegalArgumentException("This is not a new passport application.");
        }
        CitizenProfileMapper.validateConditionalFields(form);
        validatePassportProof(getEligibility(citizenId), form.getNicProofNo(), form.getParentNicNo());

        CitizenAccount citizen = findCitizen(citizenId);
        CitizenProfileMapper.updateCitizen(citizen, form);
        citizenRepository.save(citizen);

        PassportApplication passport = passportRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Passport application details were not found."));
        passport.setServiceLevel(CitizenProfileMapper.blankToNull(form.getServiceLevel()));
        passport.setTravelDocumentType(CitizenProfileMapper.blankToNull(form.getTravelDocumentType()));
        passport.setNicProofNo(CitizenProfileMapper.blankToNull(form.getNicProofNo()));
        passport.setParentNicNo(CitizenProfileMapper.blankToNull(form.getParentNicNo()));
        passportRepository.save(passport);
        return application;
    }

    @Transactional(readOnly = true)
    // Returns all applications owned by the citizen, newest first.
    public List<CitizenApplication> getCitizenApplications(Integer citizenId) {
        return applicationRepository.findAllByCitizenIdOrderByCreatedAtDesc(citizenId);
    }

    @Transactional(readOnly = true)
    // Checks whether the citizen already has an active application for the service.
    public boolean hasActiveApplication(Integer citizenId, String serviceType) {
        return applicationRepository.existsByCitizenIdAndServiceTypeAndCurrentStatusIn(
                citizenId,
                serviceType,
                ACTIVE_STATUSES
        );
    }

    @Transactional(readOnly = true)
    // Returns a citizen-owned new application, including submitted applications.
    public CitizenApplication getNewApplication(Integer citizenId, Integer applicationId) {
        CitizenApplication application = findOwnedApplication(citizenId, applicationId);
        if (!NEW.equals(application.getApplicationKind())) {
            throw new IllegalArgumentException("This page only handles new applications.");
        }
        return application;
    }

    @Transactional(readOnly = true)
    // Returns a citizen-owned new application only when it can still be edited.
    public CitizenApplication getEditableNewApplication(Integer citizenId, Integer applicationId) {
        CitizenApplication application = getNewApplication(citizenId, applicationId);
        requireEditable(application);
        return application;
    }

    @Transactional(readOnly = true)
    // Returns the NIC-specific details when they exist.
    public Optional<NicApplication> getNicApplication(Integer applicationId) {
        return nicRepository.findById(applicationId);
    }

    @Transactional(readOnly = true)
    // Returns the passport-specific details when they exist.
    public Optional<PassportApplication> getPassportApplication(Integer applicationId) {
        return passportRepository.findById(applicationId);
    }

    @Transactional(readOnly = true)
    // Returns the documents belonging to a citizen-owned new application.
    public List<ApplicationDocument> getNewDocuments(Integer citizenId, Integer applicationId) {
        CitizenApplication application = getNewApplication(citizenId, applicationId);
        requireEditable(application);
        return documentRepository.findAllByApplicationIdOrderByUploadedAtAsc(applicationId);
    }

    @Transactional
    // Deletes a citizen-owned draft and its stored document files.
    public CitizenApplication deleteDraftApplication(Integer citizenId, Integer applicationId) {
        CitizenApplication application = findOwnedApplication(citizenId, applicationId);
        if (!"DRAFT".equals(application.getCurrentStatus())) {
            throw new IllegalArgumentException(
                    "Only an unsubmitted draft application can be deleted."
            );
        }

        List<ApplicationDocument> documents = documentRepository
                .findAllByApplicationIdOrderByUploadedAtAsc(applicationId);
        documentRepository.deleteAll(documents);

        if (NIC.equals(application.getServiceType())) {
            nicRepository.deleteById(applicationId);
        } else if (PASSPORT.equals(application.getServiceType())) {
            passportRepository.deleteById(applicationId);
        }
        applicationRepository.delete(application);
        deleteDraftFiles(application, documents);
        return application;
    }

    @Transactional
    // Adds or replaces a valid document in an editable application.
    public void uploadNewDocument(
            Integer citizenId,
            Integer applicationId,
            String documentType,
            MultipartFile file) {
        CitizenApplication application = getNewApplication(citizenId, applicationId);
        saveDocument(application, documentType, file);
    }

    @Transactional
    // Checks the application and documents before moving it into the verification queue.
    public CitizenApplication submitNewApplication(Integer citizenId, Integer applicationId) {
        applicationRepository.findEditableForUpdate(applicationId, citizenId)
                .orElseThrow(() -> new IllegalArgumentException("Only drafts or corrected applications can be submitted."));
        CitizenApplication application = getNewApplication(citizenId, applicationId);
        CitizenEligibilityDto eligibility = getEligibility(citizenId);
        checkNewEligibility(eligibility, application.getServiceType());
        if (NIC.equals(application.getServiceType())) {
            CitizenAccount citizen = findCitizen(citizenId);
            LocalDate dateOfBirth = citizen.getDateOfBirth() == null
                    ? null : citizen.getDateOfBirth().toLocalDate();
            validateNicAge(dateOfBirth);
        } else {
            PassportApplication passport = passportRepository.findById(applicationId)
                    .orElseThrow(() -> new IllegalArgumentException("Passport application details were not found."));
            validatePassportProof(eligibility, passport.getNicProofNo(), passport.getParentNicNo());
        }
        submitWhenDocumentsComplete(application, requiredNewDocumentTypes(application));
        application = applicationRepository.save(application);
        sendSubmissionNotification(application);
        return application;
    }

    /** Shared by new applications, renewals and replacements. Submitted evidence is retained. */
    @Transactional
    // Deletes one unreviewed document from an unsubmitted draft.
    public CitizenApplication deleteDraftDocument(Integer citizenId, Integer applicationId, Integer documentId) {
        CitizenApplication application = applicationRepository.findEditableForUpdate(applicationId, citizenId)
                .orElseThrow(() -> new IllegalArgumentException("Only documents in your unsubmitted draft can be deleted."));
        if (!"DRAFT".equals(application.getCurrentStatus())) {
            throw new IllegalArgumentException("Only documents in an unsubmitted draft can be deleted.");
        }
        ApplicationDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document was not found."));
        if (!applicationId.equals(document.getApplicationId()) || !"PENDING".equals(document.getVerificationStatus())) {
            throw new IllegalArgumentException("Only an unreviewed document belonging to this draft can be deleted.");
        }
        Path file = uploadRoot.resolve(document.getFilePath()).normalize();
        if (!file.startsWith(uploadRoot)) throw new IllegalArgumentException("Invalid document path.");
        documentRepository.delete(document);
        documentRepository.flush();
        // Remove the file only after the database delete commits, so rollback keeps the upload usable.
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override public void afterCommit() {
                        try { Files.deleteIfExists(file); }
                        catch (IOException exception) {
                            System.getLogger(CitizenApplicationService.class.getName()).log(
                                    System.Logger.Level.WARNING, "Draft document deleted; file cleanup failed.", exception);
                        }
                    }
                });
        return application;
    }

    // Returns the required evidence list for the application's service type.
    public List<String> requiredNewDocumentTypes(CitizenApplication application) {
        List<String> required = NIC.equals(application.getServiceType())
                ? List.of("PHOTOGRAPH", "BIRTH_CERTIFICATE")
                : List.of("PHOTOGRAPH", "BIRTH_CERTIFICATE", "NIC_PROOF");
        return addConditionalCitizenshipCertificate(required, application.getCitizenId());
    }

    /** Returns missing evidence for both the upload page and the protected review route. */
    public List<String> missingNewDocumentTypes(CitizenApplication application) {
        Set<String> uploaded = documentRepository
                .findAllByApplicationIdOrderByUploadedAtAsc(application.getApplicationId())
                .stream().map(ApplicationDocument::getDocumentType)
                .collect(java.util.stream.Collectors.toSet());
        return requiredNewDocumentTypes(application).stream()
                .filter(type -> !uploaded.contains(type))
                .toList();
    }

    @Transactional(readOnly = true)
    public CitizenAccount getCitizenProfile(Integer citizenId) {
        return findCitizen(citizenId);
    }

    /** Resolves a preview only after the document's application owner has been checked. */
    @Transactional(readOnly = true)
    public CitizenDocumentFile getDocumentForCitizen(Integer documentId, Integer citizenId) {
        ApplicationDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document was not found."));
        CitizenApplication application = applicationRepository.findById(document.getApplicationId())
                .orElseThrow(() -> new IllegalArgumentException("Application was not found."));
        if (!citizenId.equals(application.getCitizenId())) {
            throw new IllegalArgumentException("Document was not found.");
        }

        Path file = uploadRoot.resolve(document.getFilePath()).normalize();
        if (!file.startsWith(uploadRoot) || !Files.isRegularFile(file)) {
            throw new IllegalArgumentException("The uploaded file is not available.");
        }
        String extension = fileExtension(file.getFileName().toString());
        String contentType = switch (extension) {
            case "pdf" -> "application/pdf";
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            default -> "application/octet-stream";
        };
        try {
            return new CitizenDocumentFile(
                    new UrlResource(file.toUri()),
                    contentType,
                    document.getDocumentType().toLowerCase(Locale.ROOT) + "." + extension
            );
        } catch (java.net.MalformedURLException exception) {
            throw new IllegalArgumentException("The uploaded file is not available.");
        }
    }

    // Creates and saves the common application record in DRAFT status.
    private CitizenApplication createBaseApplication(Integer citizenId, String serviceType) {
        CitizenApplication application = new CitizenApplication();
        application.setApplicationNo(generateApplicationNo(serviceType));
        application.setCitizenId(citizenId);
        application.setServiceType(serviceType);
        application.setApplicationKind(NEW);
        application.setCurrentStatus("DRAFT");
        application.setDeclarationAccepted("Y");
        return applicationRepository.save(application);
    }

    // Rejects a new application when an identity conflict or existing document is found.
    private void checkNewEligibility(CitizenEligibilityDto eligibility, String serviceType) {
        if (eligibility.isIdentityConflict()) {
            throw new IllegalArgumentException(
                    "Verify your existing documents through Link existing NIC or passport, or request officer verification."
            );
        }
        boolean hasDocument = NIC.equals(serviceType) ? eligibility.isHasNic() : eligibility.isHasPassport();
        if (hasDocument) {
            throw new IllegalArgumentException(
                    "You already have this identity document. Choose renewal or lost replacement."
            );
        }
    }

    // Prevents the citizen from starting a duplicate active application.
    private void checkForActiveApplication(Integer citizenId, String serviceType) {
        if (hasActiveApplication(citizenId, serviceType)) {
            throw new IllegalArgumentException(
                    "An " + serviceType + " application is already in progress. "
                            + "Use Continue on your dashboard."
            );
        }
    }

    // Enforces the minimum age for an NIC application.
    private void validateNicAge(LocalDate dateOfBirth) {
        if (dateOfBirth == null || dateOfBirth.isAfter(LocalDate.now().minusYears(15))) {
            throw new IllegalArgumentException("You must be at least 15 years old to apply for an NIC.");
        }
    }

    // Checks whether the correct citizen or parent NIC proof was supplied.
    private void validatePassportProof(
            CitizenEligibilityDto eligibility,
            String nicProofNo,
            String parentNicNo
    ) {
        String nic = CitizenProfileMapper.blankToNull(nicProofNo);
        String parentNic = CitizenProfileMapper.blankToNull(parentNicNo);

        if (eligibility.isHasNic()) {
            if (parentNic != null) {
                throw new IllegalArgumentException(
                        "A parent NIC cannot be used because your account already has an NIC record."
                );
            }
            if (nic == null || !nic.equalsIgnoreCase(eligibility.getNicNumber())) {
                throw new IllegalArgumentException("Use the NIC number registered to your account.");
            }
            return;
        }

        if (nic == null && parentNic == null) {
            throw new IllegalArgumentException("Enter the citizen's NIC number or a parent's NIC number.");
        }
        if (nic != null && parentNic != null) {
            throw new IllegalArgumentException("Enter either the citizen's NIC number or a parent's NIC number, not both.");
        }
    }

    // Validates, stores, and records an uploaded application document.
    private void saveDocument(
            CitizenApplication application,
            String documentType,
            MultipartFile file) {
        String type = documentType == null ? "" : documentType.trim().toUpperCase(Locale.ROOT);
        validateDocument(type, file);
        // Only documents required by this exact application and citizenship state are accepted.
        if (!requiredNewDocumentTypes(application).contains(type)) {
            throw new IllegalArgumentException("That document type is not required for this application.");
        }
        boolean correctionMode = "CORRECTION_REQUIRED".equals(application.getCurrentStatus());
        if (!"DRAFT".equals(application.getCurrentStatus()) && !correctionMode) {
            throw new IllegalArgumentException("Documents can only be uploaded for a draft or correction request.");
        }
        Optional<ApplicationDocument> existingDocument = documentRepository
                .findByApplicationIdAndDocumentType(application.getApplicationId(), type);
        if (existingDocument.isPresent() && !correctionMode) {
            throw new IllegalArgumentException("That document type has already been uploaded.");
        }

        String storedName = UUID.randomUUID() + "." + fileExtension(file.getOriginalFilename());
        Path applicationFolder = uploadRoot.resolve("application-documents")
                .resolve(application.getApplicationNo()).normalize();
        Path destination = applicationFolder.resolve(storedName).normalize();
        if (!destination.startsWith(uploadRoot)) {
            throw new IllegalArgumentException("Invalid upload path.");
        }

        try {
            Files.createDirectories(applicationFolder);
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new IllegalArgumentException("The document could not be saved.");
        }

        ApplicationDocument document = existingDocument.orElseGet(ApplicationDocument::new);
        document.setApplicationId(application.getApplicationId());
        document.setDocumentType(type);
        document.setFilePath(uploadRoot.relativize(destination).toString().replace('\\', '/'));
        document.setUploadedAt(LocalDateTime.now());
        document.setVerificationStatus("PENDING");
        document.setVerifiedByStaffId(null);
        document.setVerifiedAt(null);
        documentRepository.save(document);
    }

    // Rejects changes after submission unless an officer requested a correction.
    private void requireEditable(CitizenApplication application) {
        if (!"DRAFT".equals(application.getCurrentStatus())
                && !"CORRECTION_REQUIRED".equals(application.getCurrentStatus())) {
            throw new IllegalArgumentException(
                    "This application is locked after submission. It can only be edited when an officer requests a correction."
            );
        }
    }

    // Removes uploaded files that belonged to a deleted draft.
    private void deleteDraftFiles(
            CitizenApplication application,
            List<ApplicationDocument> documents
    ) {
        for (ApplicationDocument document : documents) {
            Path file = uploadRoot.resolve(document.getFilePath()).normalize();
            if (file.startsWith(uploadRoot)) {
                try {
                    Files.deleteIfExists(file);
                } catch (IOException ignored) {
                    // The database draft is deleted even if an orphaned upload cannot be removed.
                }
            }
        }

        Path applicationFolder = uploadRoot.resolve("application-documents")
                .resolve(application.getApplicationNo()).normalize();
        if (applicationFolder.startsWith(uploadRoot)) {
            try {
                Files.deleteIfExists(applicationFolder);
            } catch (IOException ignored) {
                // A non-empty or locked folder can be cleaned up later without restoring the draft.
            }
        }
    }

    // Verifies required documents and changes the application to submitted or resubmitted.
    private void submitWhenDocumentsComplete(
            CitizenApplication application,
            List<String> requiredTypes) {
        boolean resubmission = "CORRECTION_REQUIRED".equals(application.getCurrentStatus());
        if (!"DRAFT".equals(application.getCurrentStatus()) && !resubmission) {
            throw new IllegalArgumentException("Only drafts or corrected applications can be submitted.");
        }
        Set<String> uploadedTypes = documentRepository
                .findAllByApplicationIdOrderByUploadedAtAsc(application.getApplicationId())
                .stream()
                .map(ApplicationDocument::getDocumentType)
                .collect(java.util.stream.Collectors.toSet());
        List<String> missingTypes = requiredTypes.stream()
                .filter(type -> !uploadedTypes.contains(type))
                .toList();
        if (!missingTypes.isEmpty()) {
            throw new IllegalArgumentException(
                    "Upload the required documents: " + String.join(", ", missingTypes)
            );
        }
        if (resubmission) {
            List<ApplicationDocument> documents = documentRepository
                    .findAllByApplicationIdOrderByUploadedAtAsc(application.getApplicationId());
            for (ApplicationDocument document : documents) {
                document.setVerificationStatus("PENDING");
                document.setVerifiedByStaffId(null);
                document.setVerifiedAt(null);
            }
            documentRepository.saveAll(documents);
        }
        application.setCurrentStatus(resubmission ? "RESUBMITTED" : "SUBMITTED");
        application.setSubmittedAt(LocalDateTime.now());
        application.setDeclarationAccepted("Y");
    }

    private List<String> addConditionalCitizenshipCertificate(List<String> required, Integer citizenId) {
        boolean dualCitizen = citizenRepository.findById(citizenId)
                .map(citizen -> "Y".equals(citizen.getDualCitizen()))
                .orElse(false);
        if (!dualCitizen) return required;
        return java.util.stream.Stream.concat(required.stream(), java.util.stream.Stream.of("CITIZENSHIP_CERTIFICATE"))
                .toList();
    }

    // Publishes the correct citizen notification after submission.
    private void sendSubmissionNotification(CitizenApplication application) {
        boolean resubmitted = "RESUBMITTED".equals(application.getCurrentStatus());
        notificationPublisher.sendToCitizen(
                application.getCitizenId(),
                null,
                resubmitted ? "APPLICATION_RESUBMITTED" : "APPLICATION_SUBMITTED",
                application.getServiceType() + " application " + application.getApplicationNo()
                        + (resubmitted
                        ? " was resubmitted successfully and returned to the verification queue."
                        : " was submitted successfully and is waiting for officer verification.")
        );
    }

    // Returns an owned application only when it matches the service and needs correction.
    private CitizenApplication getCorrectionApplication(
            Integer citizenId,
            Integer applicationId,
            String serviceType
    ) {
        CitizenApplication application = findOwnedApplication(citizenId, applicationId);
        if (!serviceType.equals(application.getServiceType())) {
            throw new IllegalArgumentException("Application service type does not match.");
        }
        if (!"CORRECTION_REQUIRED".equals(application.getCurrentStatus())) {
            throw new IllegalArgumentException("This application is not open for corrections.");
        }
        return application;
    }

    // Checks document type, presence, size, and file extension.
    private void validateDocument(String type, MultipartFile file) {
        if (!ALLOWED_DOCUMENT_TYPES.contains(type)) {
            throw new IllegalArgumentException("Select a valid document type.");
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Choose a document to upload.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("The document must not exceed 5 MB.");
        }
        if (!ALLOWED_EXTENSIONS.contains(fileExtension(file.getOriginalFilename()))) {
            throw new IllegalArgumentException("Only PDF, JPG and PNG files are allowed.");
        }
    }

    // Generates a unique public application number for the selected service.
    private String generateApplicationNo(String serviceType) {
        String applicationNo;
        do {
            int number = ThreadLocalRandom.current().nextInt(100000, 1000000);
            applicationNo = "LK-" + serviceType + "-" + Year.now().getValue() + "-" + number;
        } while (applicationRepository.existsByApplicationNo(applicationNo));
        return applicationNo;
    }

    // Finds a safe government-record match and reports ambiguous or invalid matches.
    private IdentityMatch findGovernmentIdentity(UserAccount user, CitizenAccount citizen) {
        // Keep the established link when a citizen later changes their contact details.
        if (citizen.getGovernmentRecordId() != null) {
            GovernmentIdentityRecord record = governmentRepository.findById(citizen.getGovernmentRecordId()).orElse(null);
            boolean conflict = record == null || "CANCELLED".equalsIgnoreCase(record.getIdentityStatus())
                    || citizenRepository.existsByGovernmentRecordIdAndUserIdNot(
                    citizen.getGovernmentRecordId(), citizen.getUserId());
            return new IdentityMatch(conflict ? null : record, conflict);
        }
        // Project assumption: existing holders register with the unique email and phone in the registry.
        List<GovernmentIdentityRecord> matches = governmentRepository
                .findAllByRegisteredEmailIgnoreCaseOrRegisteredPhone(user.getEmail(), user.getPhone());
        if (matches.isEmpty()) {
            return new IdentityMatch(null, citizenRepository.countIdentityRequests(citizen.getUserId()) > 0);
        }
        if (matches.size() != 1 || !"Y".equals(user.getEmailVerified())) {
            return new IdentityMatch(null, true);
        }
        CitizenAccount lockedCitizen = citizenRepository.findByIdForIdentityLink(citizen.getUserId()).orElseThrow();
        GovernmentIdentityRecord record = governmentRepository
                .findByIdForIdentityLink(matches.get(0).getGovernmentRecordId()).orElse(null);
        if (record == null || !user.getEmail().equalsIgnoreCase(record.getRegisteredEmail())
                || !user.getPhone().equals(record.getRegisteredPhone())
                || "CANCELLED".equalsIgnoreCase(record.getIdentityStatus())
                || citizenRepository.existsByGovernmentRecordIdAndUserIdNot(record.getGovernmentRecordId(), citizen.getUserId())
                || (lockedCitizen.getGovernmentRecordId() != null
                    && !lockedCitizen.getGovernmentRecordId().equals(record.getGovernmentRecordId()))) {
            return new IdentityMatch(null, true);
        }
        lockedCitizen.setGovernmentRecordId(record.getGovernmentRecordId());
        citizenRepository.save(lockedCitizen);
        return new IdentityMatch(record, false);
    }

    // Finds an application only when it belongs to the given citizen.
    private CitizenApplication findOwnedApplication(Integer citizenId, Integer applicationId) {
        return applicationRepository.findByApplicationIdAndCitizenId(applicationId, citizenId)
                .orElseThrow(() -> new IllegalArgumentException("Application was not found."));
    }

    // Finds the user account or reports that it is missing.
    private UserAccount findUser(Integer userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Citizen user account was not found."));
    }

    // Finds the citizen profile or reports that it is missing.
    private CitizenAccount findCitizen(Integer userId) {
        return citizenRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Citizen account was not found."));
    }

    // Extracts a lower-case extension from an uploaded filename.
    private String fileExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    // Keeps the registry match and conflict result together.
    private record IdentityMatch(GovernmentIdentityRecord record, boolean conflict) {
    }
}
