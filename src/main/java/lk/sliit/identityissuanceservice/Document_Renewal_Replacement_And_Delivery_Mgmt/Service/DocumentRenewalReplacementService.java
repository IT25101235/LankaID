package lk.sliit.identityissuanceservice.Document_Renewal_Replacement_And_Delivery_Mgmt.Service;

import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Dto.CitizenEligibilityDto;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Dto.CitizenProfileForm;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Dto.CitizenProfileMapper;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model.ApplicationDocument;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model.CitizenApplication;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model.NicApplication;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model.PassportApplication;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Repository.ApplicationDocumentRepository;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Repository.CitizenApplicationRepository;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Repository.NicApplicationRepository;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Repository.PassportApplicationRepository;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Service.CitizenApplicationService;
import lk.sliit.identityissuanceservice.Document_Renewal_Replacement_And_Delivery_Mgmt.Dto.NicRenewalReplacementForm;
import lk.sliit.identityissuanceservice.Document_Renewal_Replacement_And_Delivery_Mgmt.Dto.PassportRenewalReplacementForm;
import lk.sliit.identityissuanceservice.Notification_Mgmt.Observer.NotificationPublisher;
import lk.sliit.identityissuanceservice.User_Mgmt.Model.CitizenAccount;
import lk.sliit.identityissuanceservice.User_Mgmt.Repository.CitizenAccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class DocumentRenewalReplacementService {

    public static final String RENEWAL = "RENEWAL";
    public static final String LOST = "LOST";

    private static final String NIC = CitizenApplicationService.NIC;
    private static final String PASSPORT = CitizenApplicationService.PASSPORT;
    private static final Set<String> ACTIVE_STATUSES = Set.of(
            "DRAFT", "SUBMITTED", "UNDER_REVIEW", "CORRECTION_REQUIRED", "RESUBMITTED",
            "APPROVED", "PAYMENT_PENDING", "APPOINTMENT_BOOKED", "BIOMETRIC_COLLECTED", "PROCESSING"
    );
    private static final Set<String> ALLOWED_DOCUMENT_TYPES = Set.of(
            "PHOTOGRAPH", "BIRTH_CERTIFICATE", "POLICE_REPORT",
            "NIC_PROOF", "CITIZENSHIP_CERTIFICATE", "OTHER"
    );
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "jpg", "jpeg", "png");
    private static final long MAX_FILE_SIZE = 5L * 1024L * 1024L;

    private final CitizenApplicationService eligibilityService;
    private final CitizenAccountRepository citizenRepository;
    private final CitizenApplicationRepository applicationRepository;
    private final NicApplicationRepository nicRepository;
    private final PassportApplicationRepository passportRepository;
    private final ApplicationDocumentRepository documentRepository;
    private final NotificationPublisher notificationPublisher;
    private final Path uploadRoot;

    public DocumentRenewalReplacementService(
            CitizenApplicationService eligibilityService,
            CitizenAccountRepository citizenRepository,
            CitizenApplicationRepository applicationRepository,
            NicApplicationRepository nicRepository,
            PassportApplicationRepository passportRepository,
            ApplicationDocumentRepository documentRepository,
            NotificationPublisher notificationPublisher,
            @Value("${app.upload.directory:uploads}") String uploadDirectory) {
        this.eligibilityService = eligibilityService;
        this.citizenRepository = citizenRepository;
        this.applicationRepository = applicationRepository;
        this.nicRepository = nicRepository;
        this.passportRepository = passportRepository;
        this.documentRepository = documentRepository;
        this.notificationPublisher = notificationPublisher;
        this.uploadRoot = Path.of(uploadDirectory).toAbsolutePath().normalize();
    }

    @Transactional
    public CitizenEligibilityDto getEligibility(Integer citizenId) {
        return eligibilityService.getEligibility(citizenId);
    }

    @Transactional
    public NicRenewalReplacementForm prepareNicForm(Integer citizenId, String formKind) {
        String applicationKind = applicationKind(formKind);
        CitizenEligibilityDto eligibility = eligibilityService.getEligibility(citizenId);
        checkExistingDocument(eligibility, NIC);
        checkForActiveApplication(citizenId, NIC);

        NicRenewalReplacementForm form = new NicRenewalReplacementForm();
        CitizenProfileMapper.copyToForm(findCitizen(citizenId), form);
        form.setLastNicNo(eligibility.getNicNumber());
        return form;
    }

    @Transactional
    public PassportRenewalReplacementForm preparePassportForm(Integer citizenId, String formKind) {
        applicationKind(formKind);
        CitizenEligibilityDto eligibility = eligibilityService.getEligibility(citizenId);
        checkExistingDocument(eligibility, PASSPORT);
        checkForActiveApplication(citizenId, PASSPORT);

        PassportRenewalReplacementForm form = new PassportRenewalReplacementForm();
        CitizenProfileMapper.copyToForm(findCitizen(citizenId), form);
        if (eligibility.isHasNic()) {
            form.setNicProofNo(eligibility.getNicNumber());
        }
        return form;
    }

    @Transactional(readOnly = true)
    public NicRenewalReplacementForm prepareNicCorrection(Integer citizenId, Integer applicationId) {
        CitizenApplication application = getCorrectionRequest(citizenId, applicationId, NIC);
        NicApplication details = nicRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("NIC application details were not found."));

        NicRenewalReplacementForm form = new NicRenewalReplacementForm();
        CitizenProfileMapper.copyToForm(findCitizen(citizenId), form);
        form.setLastNicNo(details.getLastNicNo());
        form.setLastIssuedDate(details.getLastIssuedDate() == null ? null : details.getLastIssuedDate().toLocalDate());
        form.setPoliceStation(details.getPoliceStation());
        form.setPoliceReportDate(details.getPoliceReportDate() == null ? null : details.getPoliceReportDate().toLocalDate());
        return form;
    }

    @Transactional(readOnly = true)
    public PassportRenewalReplacementForm preparePassportCorrection(Integer citizenId, Integer applicationId) {
        getCorrectionRequest(citizenId, applicationId, PASSPORT);
        PassportApplication details = passportRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Passport application details were not found."));

        PassportRenewalReplacementForm form = new PassportRenewalReplacementForm();
        CitizenProfileMapper.copyToForm(findCitizen(citizenId), form);
        form.setServiceLevel(details.getServiceLevel());
        form.setTravelDocumentType(details.getTravelDocumentType());
        form.setNicProofNo(details.getNicProofNo());
        form.setParentNicNo(details.getParentNicNo());
        return form;
    }

    @Transactional
    public CitizenApplication createNicRequest(
            Integer citizenId,
            String formKind,
            NicRenewalReplacementForm form) {
        String applicationKind = applicationKind(formKind);
        CitizenEligibilityDto eligibility = eligibilityService.getEligibility(citizenId);
        checkExistingDocument(eligibility, NIC);
        form.setLastNicNo(eligibility.getNicNumber());
        CitizenProfileMapper.validateConditionalFields(form);
        validateNicDetails(applicationKind, form);
        checkForActiveApplication(citizenId, NIC);

        saveCitizenProfile(citizenId, form);
        CitizenApplication application = createBaseApplication(citizenId, NIC, applicationKind);

        NicApplication nicApplication = new NicApplication();
        nicApplication.setApplicationId(application.getApplicationId());
        nicApplication.setLastNicNo(CitizenProfileMapper.blankToNull(form.getLastNicNo()));
        nicApplication.setLastIssuedDate(CitizenProfileMapper.atStartOfDay(form.getLastIssuedDate()));
        nicApplication.setPoliceStation(CitizenProfileMapper.blankToNull(form.getPoliceStation()));
        nicApplication.setPoliceReportDate(CitizenProfileMapper.atStartOfDay(form.getPoliceReportDate()));
        nicRepository.save(nicApplication);
        return application;
    }

    @Transactional
    public CitizenApplication createPassportRequest(
            Integer citizenId,
            String formKind,
            PassportRenewalReplacementForm form) {
        String applicationKind = applicationKind(formKind);
        CitizenEligibilityDto eligibility = eligibilityService.getEligibility(citizenId);
        checkExistingDocument(eligibility, PASSPORT);
        if (eligibility.isHasNic()) {
            form.setNicProofNo(eligibility.getNicNumber());
            form.setParentNicNo(null);
        }
        CitizenProfileMapper.validateConditionalFields(form);
        validatePassportProof(form.getNicProofNo(), form.getParentNicNo());
        checkForActiveApplication(citizenId, PASSPORT);

        saveCitizenProfile(citizenId, form);
        CitizenApplication application = createBaseApplication(citizenId, PASSPORT, applicationKind);

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
    public CitizenApplication updateNicCorrection(
            Integer citizenId,
            Integer applicationId,
            NicRenewalReplacementForm form
    ) {
        CitizenApplication application = getCorrectionRequest(citizenId, applicationId, NIC);
        CitizenEligibilityDto eligibility = eligibilityService.getEligibility(citizenId);
        form.setLastNicNo(eligibility.getNicNumber());
        NicApplication details = nicRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("NIC application details were not found."));
        if (details.getLastIssuedDate() != null) {
            form.setLastIssuedDate(details.getLastIssuedDate().toLocalDate());
        }
        CitizenProfileMapper.validateConditionalFields(form);
        validateNicDetails(application.getApplicationKind(), form);
        saveCitizenProfile(citizenId, form);

        details.setLastNicNo(CitizenProfileMapper.blankToNull(form.getLastNicNo()));
        details.setLastIssuedDate(CitizenProfileMapper.atStartOfDay(form.getLastIssuedDate()));
        details.setPoliceStation(CitizenProfileMapper.blankToNull(form.getPoliceStation()));
        details.setPoliceReportDate(CitizenProfileMapper.atStartOfDay(form.getPoliceReportDate()));
        nicRepository.save(details);
        return application;
    }

    @Transactional
    public CitizenApplication updatePassportCorrection(
            Integer citizenId,
            Integer applicationId,
            PassportRenewalReplacementForm form
    ) {
        CitizenApplication application = getCorrectionRequest(citizenId, applicationId, PASSPORT);
        CitizenEligibilityDto eligibility = eligibilityService.getEligibility(citizenId);
        if (eligibility.isHasNic()) {
            form.setNicProofNo(eligibility.getNicNumber());
            form.setParentNicNo(null);
        }
        CitizenProfileMapper.validateConditionalFields(form);
        validatePassportProof(form.getNicProofNo(), form.getParentNicNo());
        saveCitizenProfile(citizenId, form);

        PassportApplication details = passportRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Passport application details were not found."));
        details.setServiceLevel(CitizenProfileMapper.blankToNull(form.getServiceLevel()));
        details.setTravelDocumentType(CitizenProfileMapper.blankToNull(form.getTravelDocumentType()));
        details.setNicProofNo(CitizenProfileMapper.blankToNull(form.getNicProofNo()));
        details.setParentNicNo(CitizenProfileMapper.blankToNull(form.getParentNicNo()));
        passportRepository.save(details);
        return application;
    }

    @Transactional(readOnly = true)
    public CitizenApplication getRequest(Integer citizenId, Integer applicationId) {
        CitizenApplication application = applicationRepository
                .findByApplicationIdAndCitizenId(applicationId, citizenId)
                .orElseThrow(() -> new IllegalArgumentException("Application was not found."));
        if (!RENEWAL.equals(application.getApplicationKind())
                && !LOST.equals(application.getApplicationKind())) {
            throw new IllegalArgumentException("This page only handles renewal and lost replacement requests.");
        }
        return application;
    }

    @Transactional(readOnly = true)
    public Optional<NicApplication> getNicRequest(Integer applicationId) {
        return nicRepository.findById(applicationId);
    }

    @Transactional(readOnly = true)
    public Optional<PassportApplication> getPassportRequest(Integer applicationId) {
        return passportRepository.findById(applicationId);
    }

    @Transactional(readOnly = true)
    public List<ApplicationDocument> getDocuments(Integer citizenId, Integer applicationId) {
        CitizenApplication application = getRequest(citizenId, applicationId);
        if (!"DRAFT".equals(application.getCurrentStatus())
                && !"CORRECTION_REQUIRED".equals(application.getCurrentStatus())) {
            throw new IllegalArgumentException(
                    "This application is locked after submission. It can only be edited when an officer requests a correction."
            );
        }
        return documentRepository.findAllByApplicationIdOrderByUploadedAtAsc(applicationId);
    }

    @Transactional
    public void uploadDocument(
            Integer citizenId,
            Integer applicationId,
            String documentType,
            MultipartFile file) {
        CitizenApplication application = getRequest(citizenId, applicationId);
        String type = documentType == null ? "" : documentType.trim().toUpperCase(Locale.ROOT);
        validateDocument(type, file);
        boolean correctionMode = "CORRECTION_REQUIRED".equals(application.getCurrentStatus());
        if (!"DRAFT".equals(application.getCurrentStatus()) && !correctionMode) {
            throw new IllegalArgumentException("Documents can only be uploaded for a draft or correction request.");
        }
        Optional<ApplicationDocument> existingDocument = documentRepository
                .findByApplicationIdAndDocumentType(applicationId, type);
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
        document.setApplicationId(applicationId);
        document.setDocumentType(type);
        document.setFilePath(uploadRoot.relativize(destination).toString().replace('\\', '/'));
        document.setUploadedAt(LocalDateTime.now());
        document.setVerificationStatus("PENDING");
        document.setVerifiedByStaffId(null);
        document.setVerifiedAt(null);
        documentRepository.save(document);
    }

    @Transactional
    public CitizenApplication submitRequest(Integer citizenId, Integer applicationId) {
        applicationRepository.findEditableForUpdate(applicationId, citizenId)
                .orElseThrow(() -> new IllegalArgumentException("Only drafts or corrected requests can be submitted."));
        CitizenApplication application = getRequest(citizenId, applicationId);
        boolean resubmission = "CORRECTION_REQUIRED".equals(application.getCurrentStatus());
        if (!"DRAFT".equals(application.getCurrentStatus()) && !resubmission) {
            throw new IllegalArgumentException("Only drafts or corrected requests can be submitted.");
        }

        Set<String> uploadedTypes = documentRepository
                .findAllByApplicationIdOrderByUploadedAtAsc(applicationId)
                .stream()
                .map(ApplicationDocument::getDocumentType)
                .collect(java.util.stream.Collectors.toSet());
        List<String> missingTypes = requiredDocumentTypes(application).stream()
                .filter(type -> !uploadedTypes.contains(type))
                .toList();
        if (!missingTypes.isEmpty()) {
            throw new IllegalArgumentException(
                    "Upload the required documents: " + String.join(", ", missingTypes)
            );
        }

        if (resubmission) {
            List<ApplicationDocument> documents = documentRepository
                    .findAllByApplicationIdOrderByUploadedAtAsc(applicationId);
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
        application = applicationRepository.save(application);
        notificationPublisher.sendToCitizen(
                application.getCitizenId(),
                null,
                resubmission ? "APPLICATION_RESUBMITTED" : "APPLICATION_SUBMITTED",
                application.getServiceType() + " " + application.getApplicationKind().toLowerCase(Locale.ROOT)
                        + " application " + application.getApplicationNo()
                        + (resubmission
                        ? " was resubmitted and returned to the verification queue."
                        : " was submitted and is waiting for officer verification.")
        );
        return application;
    }

    private CitizenApplication getCorrectionRequest(
            Integer citizenId,
            Integer applicationId,
            String serviceType
    ) {
        CitizenApplication application = getRequest(citizenId, applicationId);
        if (!serviceType.equals(application.getServiceType())) {
            throw new IllegalArgumentException("Application service type does not match.");
        }
        if (!"CORRECTION_REQUIRED".equals(application.getCurrentStatus())) {
            throw new IllegalArgumentException("This application is not open for corrections.");
        }
        return application;
    }

    public List<String> requiredDocumentTypes(CitizenApplication application) {
        if (NIC.equals(application.getServiceType())) {
            return LOST.equals(application.getApplicationKind())
                    ? List.of("PHOTOGRAPH", "BIRTH_CERTIFICATE", "POLICE_REPORT")
                    : List.of("PHOTOGRAPH", "BIRTH_CERTIFICATE");
        }
        return LOST.equals(application.getApplicationKind())
                ? List.of("PHOTOGRAPH", "BIRTH_CERTIFICATE", "NIC_PROOF", "POLICE_REPORT")
                : List.of("PHOTOGRAPH", "BIRTH_CERTIFICATE", "NIC_PROOF");
    }

    private void checkExistingDocument(CitizenEligibilityDto eligibility, String serviceType) {
        if (eligibility.isIdentityConflict()) {
            throw new IllegalArgumentException(
                    "Verify your existing documents through Link existing NIC or passport, or request officer verification."
            );
        }
        boolean hasDocument = NIC.equals(serviceType) ? eligibility.isHasNic() : eligibility.isHasPassport();
        if (!hasDocument) {
            throw new IllegalArgumentException(
                    "No verified document is linked. If you already own this document, link it or request officer verification."
            );
        }
    }

    private String applicationKind(String formKind) {
        return switch (formKind == null ? "" : formKind.toLowerCase(Locale.ROOT)) {
            case "renewal" -> RENEWAL;
            case "replacement", "lost" -> LOST;
            default -> throw new IllegalArgumentException("Invalid renewal or replacement type.");
        };
    }

    private void validateNicDetails(String applicationKind, NicRenewalReplacementForm form) {
        if (CitizenProfileMapper.blankToNull(form.getLastNicNo()) == null) {
            throw new IllegalArgumentException("Existing NIC number is required.");
        }
        if (LOST.equals(applicationKind)
                && (CitizenProfileMapper.blankToNull(form.getPoliceStation()) == null
                || form.getPoliceReportDate() == null)) {
            throw new IllegalArgumentException(
                    "Police station and police report date are required for a lost NIC."
            );
        }
    }

    private void validatePassportProof(String nicProofNo, String parentNicNo) {
        if (CitizenProfileMapper.blankToNull(nicProofNo) == null
                && CitizenProfileMapper.blankToNull(parentNicNo) == null) {
            throw new IllegalArgumentException("Enter the citizen's NIC number or a parent's NIC number.");
        }
    }

    private void checkForActiveApplication(Integer citizenId, String serviceType) {
        if (applicationRepository.existsByCitizenIdAndServiceTypeAndCurrentStatusIn(
                citizenId,
                serviceType,
                ACTIVE_STATUSES)) {
            throw new IllegalArgumentException(
                    "You already have an active " + serviceType + " request. Continue it from your dashboard."
            );
        }
    }

    private void saveCitizenProfile(Integer citizenId, CitizenProfileForm form) {
        CitizenAccount citizen = findCitizen(citizenId);
        CitizenProfileMapper.updateCitizen(citizen, form);
        citizenRepository.save(citizen);
    }

    private CitizenApplication createBaseApplication(
            Integer citizenId,
            String serviceType,
            String applicationKind) {
        CitizenApplication application = new CitizenApplication();
        application.setApplicationNo(generateApplicationNo(serviceType));
        application.setCitizenId(citizenId);
        application.setServiceType(serviceType);
        application.setApplicationKind(applicationKind);
        application.setCurrentStatus("DRAFT");
        application.setDeclarationAccepted("Y");
        return applicationRepository.save(application);
    }

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

    private String generateApplicationNo(String serviceType) {
        String applicationNo;
        do {
            int number = ThreadLocalRandom.current().nextInt(100000, 1000000);
            applicationNo = "LK-" + serviceType + "-" + Year.now().getValue() + "-" + number;
        } while (applicationRepository.existsByApplicationNo(applicationNo));
        return applicationNo;
    }

    private CitizenAccount findCitizen(Integer citizenId) {
        return citizenRepository.findById(citizenId)
                .orElseThrow(() -> new IllegalArgumentException("Citizen account was not found."));
    }

    private String fileExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }
}
