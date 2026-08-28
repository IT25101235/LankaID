package lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Service;

import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Dto.CitizenEligibilityDto;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Dto.CitizenProfileForm;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Dto.NicApplicationForm;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Dto.PassportApplicationForm;
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
import lk.sliit.identityissuanceservice.User_Mgmt.Model.CitizenAccount;
import lk.sliit.identityissuanceservice.User_Mgmt.Model.UserAccount;
import lk.sliit.identityissuanceservice.User_Mgmt.Repository.CitizenAccountRepository;
import lk.sliit.identityissuanceservice.User_Mgmt.Repository.UserAccountRepository;
import org.springframework.beans.factory.annotation.Value;
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
public class CitizenApplicationService {

    public static final String NIC = "NIC";
    public static final String PASSPORT = "PASSPORT";
    public static final String NEW = "NEW";
    public static final String RENEWAL = "RENEWAL";
    public static final String LOST = "LOST";

    private static final Set<String> ACTIVE_APPLICATION_STATUSES = Set.of(
            "DRAFT", "SUBMITTED", "UNDER_REVIEW", "CORRECTION_REQUIRED", "RESUBMITTED",
            "APPROVED", "PAYMENT_PENDING", "APPOINTMENT_BOOKED", "BIOMETRIC_COLLECTED", "PROCESSING"
    );
    private static final Set<String> ALLOWED_DOCUMENT_TYPES = Set.of(
            "PHOTOGRAPH", "BIRTH_CERTIFICATE", "POLICE_REPORT",
            "NIC_PROOF", "CITIZENSHIP_CERTIFICATE", "OTHER"
    );
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "jpg", "jpeg", "png");
    private static final long MAX_FILE_SIZE = 5L * 1024L * 1024L;

    private final UserAccountRepository userAccountRepository;
    private final CitizenAccountRepository citizenAccountRepository;
    private final GovernmentIdentityRecordRepository governmentRecordRepository;
    private final CitizenApplicationRepository applicationRepository;
    private final NicApplicationRepository nicApplicationRepository;
    private final PassportApplicationRepository passportApplicationRepository;
    private final ApplicationDocumentRepository documentRepository;
    private final Path uploadRoot;

    public CitizenApplicationService(
            UserAccountRepository userAccountRepository,
            CitizenAccountRepository citizenAccountRepository,
            GovernmentIdentityRecordRepository governmentRecordRepository,
            CitizenApplicationRepository applicationRepository,
            NicApplicationRepository nicApplicationRepository,
            PassportApplicationRepository passportApplicationRepository,
            ApplicationDocumentRepository documentRepository,
            @Value("${app.upload.directory:uploads}") String uploadDirectory) {
        this.userAccountRepository = userAccountRepository;
        this.citizenAccountRepository = citizenAccountRepository;
        this.governmentRecordRepository = governmentRecordRepository;
        this.applicationRepository = applicationRepository;
        this.nicApplicationRepository = nicApplicationRepository;
        this.passportApplicationRepository = passportApplicationRepository;
        this.documentRepository = documentRepository;
        this.uploadRoot = Path.of(uploadDirectory).toAbsolutePath().normalize();
    }

    @Transactional
    public CitizenEligibilityDto getEligibility(Integer citizenId) {
        UserAccount user = findUser(citizenId);
        CitizenAccount citizen = findCitizen(citizenId);
        IdentityMatch match = findGovernmentIdentity(user, citizen);

        if (match.conflict()) {
            return new CitizenEligibilityDto(false, false, true, null, null);
        }

        GovernmentIdentityRecord record = match.record();
        boolean usableRecord = record != null && !"CANCELLED".equals(record.getIdentityStatus());
        String nicNumber = usableRecord ? blankToNull(record.getNicNo()) : null;
        String passportNumber = usableRecord ? blankToNull(record.getPassportNo()) : null;

        return new CitizenEligibilityDto(
                nicNumber != null,
                passportNumber != null,
                false,
                nicNumber,
                passportNumber
        );
    }

    @Transactional
    public NicApplicationForm prepareNicForm(Integer citizenId, String formKind) {
        String applicationKind = applicationKind(formKind);
        CitizenEligibilityDto eligibility = getEligibility(citizenId);
        checkEligibility(eligibility, NIC, applicationKind);

        NicApplicationForm form = new NicApplicationForm();
        copyCitizenToForm(findCitizen(citizenId), form);
        if (!NEW.equals(applicationKind)) {
            form.setLastNicNo(eligibility.getNicNumber());
        }
        return form;
    }

    @Transactional
    public PassportApplicationForm preparePassportForm(Integer citizenId, String formKind) {
        String applicationKind = applicationKind(formKind);
        CitizenEligibilityDto eligibility = getEligibility(citizenId);
        checkEligibility(eligibility, PASSPORT, applicationKind);

        PassportApplicationForm form = new PassportApplicationForm();
        copyCitizenToForm(findCitizen(citizenId), form);
        if (eligibility.isHasNic()) {
            form.setNicProofNo(eligibility.getNicNumber());
        }
        return form;
    }

    @Transactional
    public CitizenApplication createNicApplication(
            Integer citizenId,
            String formKind,
            NicApplicationForm form) {
        String applicationKind = applicationKind(formKind);
        CitizenEligibilityDto eligibility = getEligibility(citizenId);
        checkEligibility(eligibility, NIC, applicationKind);
        validateNicDetails(applicationKind, form);
        checkForActiveApplication(citizenId, NIC);

        updateCitizenProfile(findCitizen(citizenId), form);
        CitizenApplication application = createBaseApplication(citizenId, NIC, applicationKind);

        NicApplication nicApplication = new NicApplication();
        nicApplication.setApplicationId(application.getApplicationId());
        nicApplication.setLastNicNo(blankToNull(form.getLastNicNo()));
        nicApplication.setLastIssuedDate(atStartOfDay(form.getLastIssuedDate()));
        nicApplication.setPoliceStation(blankToNull(form.getPoliceStation()));
        nicApplication.setPoliceReportDate(atStartOfDay(form.getPoliceReportDate()));
        nicApplicationRepository.save(nicApplication);
        return application;
    }

    @Transactional
    public CitizenApplication createPassportApplication(
            Integer citizenId,
            String formKind,
            PassportApplicationForm form) {
        String applicationKind = applicationKind(formKind);
        CitizenEligibilityDto eligibility = getEligibility(citizenId);
        checkEligibility(eligibility, PASSPORT, applicationKind);
        validatePassportDetails(form);
        checkForActiveApplication(citizenId, PASSPORT);

        updateCitizenProfile(findCitizen(citizenId), form);
        CitizenApplication application = createBaseApplication(citizenId, PASSPORT, applicationKind);

        PassportApplication passportApplication = new PassportApplication();
        passportApplication.setApplicationId(application.getApplicationId());
        passportApplication.setServiceLevel("NORMAL");
        passportApplication.setTravelDocumentType("ALL_COUNTRIES");
        passportApplication.setNicProofNo(blankToNull(form.getNicProofNo()));
        passportApplication.setParentNicNo(blankToNull(form.getParentNicNo()));
        passportApplicationRepository.save(passportApplication);
        return application;
    }

    @Transactional(readOnly = true)
    public List<CitizenApplication> getCitizenApplications(Integer citizenId) {
        return applicationRepository.findAllByCitizenIdOrderByCreatedAtDesc(citizenId);
    }

    @Transactional(readOnly = true)
    public CitizenApplication getApplication(Integer citizenId, Integer applicationId) {
        return applicationRepository.findByApplicationIdAndCitizenId(applicationId, citizenId)
                .orElseThrow(() -> new IllegalArgumentException("Application was not found."));
    }

    @Transactional(readOnly = true)
    public Optional<NicApplication> getNicApplication(Integer applicationId) {
        return nicApplicationRepository.findById(applicationId);
    }

    @Transactional(readOnly = true)
    public Optional<PassportApplication> getPassportApplication(Integer applicationId) {
        return passportApplicationRepository.findById(applicationId);
    }

    @Transactional(readOnly = true)
    public List<ApplicationDocument> getDocuments(Integer citizenId, Integer applicationId) {
        getApplication(citizenId, applicationId);
        return documentRepository.findAllByApplicationIdOrderByUploadedAtAsc(applicationId);
    }

    @Transactional
    public void uploadDocument(
            Integer citizenId,
            Integer applicationId,
            String documentType,
            MultipartFile file) {
        CitizenApplication application = getApplication(citizenId, applicationId);
        String type = documentType == null ? "" : documentType.trim().toUpperCase(Locale.ROOT);
        validateDocument(type, file);

        if (!"DRAFT".equals(application.getCurrentStatus())) {
            throw new IllegalArgumentException("Documents can only be uploaded while the application is a draft.");
        }
        if (documentRepository.existsByApplicationIdAndDocumentType(applicationId, type)) {
            throw new IllegalArgumentException("That document type has already been uploaded.");
        }

        String extension = fileExtension(file.getOriginalFilename());
        String storedName = UUID.randomUUID() + "." + extension;
        Path applicationFolder = uploadRoot.resolve("application-documents")
                .resolve(application.getApplicationNo())
                .normalize();
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

        ApplicationDocument document = new ApplicationDocument();
        document.setApplicationId(applicationId);
        document.setDocumentType(type);
        document.setFilePath(uploadRoot.relativize(destination).toString().replace('\\', '/'));
        documentRepository.save(document);
    }

    @Transactional
    public CitizenApplication submitApplication(Integer citizenId, Integer applicationId) {
        CitizenApplication application = getApplication(citizenId, applicationId);
        if (!"DRAFT".equals(application.getCurrentStatus())) {
            throw new IllegalArgumentException("Only draft applications can be submitted.");
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
            throw new IllegalArgumentException("Upload the required documents: " + String.join(", ", missingTypes));
        }

        application.setCurrentStatus("SUBMITTED");
        application.setSubmittedAt(LocalDateTime.now());
        application.setDeclarationAccepted("Y");
        return applicationRepository.save(application);
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

    private void checkForActiveApplication(Integer citizenId, String serviceType) {
        if (applicationRepository.existsByCitizenIdAndServiceTypeAndCurrentStatusIn(
                citizenId,
                serviceType,
                ACTIVE_APPLICATION_STATUSES)) {
            throw new IllegalArgumentException(
                    "You already have an active " + serviceType + " application. Continue it from your dashboard."
            );
        }
    }

    private void checkEligibility(
            CitizenEligibilityDto eligibility,
            String serviceType,
            String applicationKind) {
        if (eligibility.isIdentityConflict()) {
            throw new IllegalArgumentException(
                    "Your email and phone match different government records. Please contact support."
            );
        }

        boolean hasDocument = NIC.equals(serviceType) ? eligibility.isHasNic() : eligibility.isHasPassport();
        if (NEW.equals(applicationKind) && hasDocument) {
            throw new IllegalArgumentException("You already have this identity document. Choose renewal or lost replacement.");
        }
        if (!NEW.equals(applicationKind) && !hasDocument) {
            throw new IllegalArgumentException("No existing identity document was found for this request.");
        }
    }

    private void validateNicDetails(String applicationKind, NicApplicationForm form) {
        validateConditionalProfileFields(form);
        if (!NEW.equals(applicationKind) && blankToNull(form.getLastNicNo()) == null) {
            throw new IllegalArgumentException("Existing NIC number is required.");
        }
        if (LOST.equals(applicationKind)
                && (blankToNull(form.getPoliceStation()) == null || form.getPoliceReportDate() == null)) {
            throw new IllegalArgumentException("Police station and police report date are required for a lost NIC.");
        }
    }

    private void validatePassportDetails(PassportApplicationForm form) {
        validateConditionalProfileFields(form);
        if (blankToNull(form.getNicProofNo()) == null && blankToNull(form.getParentNicNo()) == null) {
            throw new IllegalArgumentException("Enter the citizen's NIC number or a parent's NIC number.");
        }
    }

    private void validateConditionalProfileFields(CitizenProfileForm form) {
        if (!form.isPostalSameAsPermanent()
                && (blankToNull(form.getPostalHouseNo()) == null
                || blankToNull(form.getPostalStreet()) == null
                || blankToNull(form.getPostalCity()) == null
                || blankToNull(form.getPostalPostalCode()) == null)) {
            throw new IllegalArgumentException("Complete the postal address or select same as permanent address.");
        }
        if (form.isDualCitizen()
                && (blankToNull(form.getCitizenshipCertificateNo()) == null
                || form.getCitizenshipIssuedDate() == null)) {
            throw new IllegalArgumentException("Citizenship certificate details are required for dual citizens.");
        }
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
        String extension = fileExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Only PDF, JPG and PNG files are allowed.");
        }
    }

    private String applicationKind(String formKind) {
        return switch (formKind == null ? "" : formKind.toLowerCase(Locale.ROOT)) {
            case "new" -> NEW;
            case "renewal" -> RENEWAL;
            case "replacement", "lost" -> LOST;
            default -> throw new IllegalArgumentException("Invalid application type.");
        };
    }

    private String generateApplicationNo(String serviceType) {
        String applicationNo;
        do {
            int number = ThreadLocalRandom.current().nextInt(100000, 1000000);
            applicationNo = "LK-" + serviceType + "-" + Year.now().getValue() + "-" + number;
        } while (applicationRepository.existsByApplicationNo(applicationNo));
        return applicationNo;
    }

    private IdentityMatch findGovernmentIdentity(UserAccount user, CitizenAccount citizen) {
        Optional<GovernmentIdentityRecord> exactMatch = governmentRecordRepository
                .findByRegisteredEmailIgnoreCaseAndRegisteredPhone(user.getEmail(), user.getPhone());
        Optional<GovernmentIdentityRecord> emailMatch = governmentRecordRepository
                .findByRegisteredEmailIgnoreCase(user.getEmail());
        Optional<GovernmentIdentityRecord> phoneMatch = governmentRecordRepository
                .findByRegisteredPhone(user.getPhone());

        boolean partialOrDifferentMatch = exactMatch.isEmpty()
                && (emailMatch.isPresent() || phoneMatch.isPresent());
        if (partialOrDifferentMatch) {
            return new IdentityMatch(null, true);
        }

        GovernmentIdentityRecord matchedRecord = exactMatch.orElse(null);
        if (citizen.getGovernmentRecordId() != null) {
            GovernmentIdentityRecord linkedRecord = governmentRecordRepository
                    .findById(citizen.getGovernmentRecordId())
                    .orElse(null);
            if (linkedRecord == null || matchedRecord == null
                    || !matchedRecord.getGovernmentRecordId().equals(linkedRecord.getGovernmentRecordId())) {
                return new IdentityMatch(null, true);
            }
            return new IdentityMatch(linkedRecord, false);
        }

        if (matchedRecord != null) {
            citizen.setGovernmentRecordId(matchedRecord.getGovernmentRecordId());
            citizenAccountRepository.save(citizen);
        }
        return new IdentityMatch(matchedRecord, false);
    }

    private void updateCitizenProfile(CitizenAccount citizen, CitizenProfileForm form) {
        citizen.setFamilyName(clean(form.getFamilyName()));
        citizen.setGivenName(clean(form.getGivenName()));
        citizen.setSurname(clean(form.getSurname()));
        citizen.setGender(form.getGender().toUpperCase(Locale.ROOT));
        citizen.setCivilStatus(form.getCivilStatus().toUpperCase(Locale.ROOT));
        citizen.setOccupation(blankToNull(form.getOccupation()));
        citizen.setDateOfBirth(atStartOfDay(form.getDateOfBirth()));
        citizen.setBirthCertificateNo(clean(form.getBirthCertificateNo()));
        citizen.setBirthPlace(clean(form.getBirthPlace()));
        citizen.setBirthDivision(clean(form.getBirthDivision()));
        citizen.setBirthDistrict(clean(form.getBirthDistrict()));
        citizen.setCountryOfBirth(clean(form.getCountryOfBirth()));
        citizen.setBirthCity(clean(form.getBirthCity()));
        citizen.setPermanentHouseNo(clean(form.getPermanentHouseNo()));
        citizen.setPermanentStreet(clean(form.getPermanentStreet()));
        citizen.setPermanentCity(clean(form.getPermanentCity()));
        citizen.setPermanentPostalCode(clean(form.getPermanentPostalCode()));
        citizen.setPostalSameAsPermanent(form.isPostalSameAsPermanent() ? "Y" : "N");

        if (form.isPostalSameAsPermanent()) {
            citizen.setPostalHouseNo(citizen.getPermanentHouseNo());
            citizen.setPostalStreet(citizen.getPermanentStreet());
            citizen.setPostalCity(citizen.getPermanentCity());
            citizen.setPostalPostalCode(citizen.getPermanentPostalCode());
        } else {
            citizen.setPostalHouseNo(clean(form.getPostalHouseNo()));
            citizen.setPostalStreet(clean(form.getPostalStreet()));
            citizen.setPostalCity(clean(form.getPostalCity()));
            citizen.setPostalPostalCode(clean(form.getPostalPostalCode()));
        }

        citizen.setDualCitizen(form.isDualCitizen() ? "Y" : "N");
        citizen.setCitizenshipCertificateNo(
                form.isDualCitizen() ? blankToNull(form.getCitizenshipCertificateNo()) : null
        );
        citizen.setCitizenshipIssuedDate(
                form.isDualCitizen() ? atStartOfDay(form.getCitizenshipIssuedDate()) : null
        );
        citizenAccountRepository.save(citizen);
    }

    private void copyCitizenToForm(CitizenAccount citizen, CitizenProfileForm form) {
        form.setFamilyName(citizen.getFamilyName());
        form.setGivenName(citizen.getGivenName());
        form.setSurname(citizen.getSurname());
        form.setGender(citizen.getGender());
        form.setCivilStatus(citizen.getCivilStatus());
        form.setOccupation(citizen.getOccupation());
        form.setDateOfBirth(toLocalDate(citizen.getDateOfBirth()));
        form.setBirthCertificateNo(citizen.getBirthCertificateNo());
        form.setBirthPlace(citizen.getBirthPlace());
        form.setBirthDivision(citizen.getBirthDivision());
        form.setBirthDistrict(citizen.getBirthDistrict());
        form.setCountryOfBirth(citizen.getCountryOfBirth());
        form.setBirthCity(citizen.getBirthCity());
        form.setPermanentHouseNo(citizen.getPermanentHouseNo());
        form.setPermanentStreet(citizen.getPermanentStreet());
        form.setPermanentCity(citizen.getPermanentCity());
        form.setPermanentPostalCode(citizen.getPermanentPostalCode());
        form.setPostalSameAsPermanent(!"N".equals(citizen.getPostalSameAsPermanent()));
        form.setPostalHouseNo(citizen.getPostalHouseNo());
        form.setPostalStreet(citizen.getPostalStreet());
        form.setPostalCity(citizen.getPostalCity());
        form.setPostalPostalCode(citizen.getPostalPostalCode());
        form.setDualCitizen("Y".equals(citizen.getDualCitizen()));
        form.setCitizenshipCertificateNo(citizen.getCitizenshipCertificateNo());
        form.setCitizenshipIssuedDate(toLocalDate(citizen.getCitizenshipIssuedDate()));
    }

    private UserAccount findUser(Integer userId) {
        return userAccountRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Citizen user account was not found."));
    }

    private CitizenAccount findCitizen(Integer userId) {
        return citizenAccountRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Citizen account was not found."));
    }

    private String fileExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    private String clean(String value) {
        return value == null ? null : value.trim();
    }

    private String blankToNull(String value) {
        String cleaned = clean(value);
        return cleaned == null || cleaned.isEmpty() ? null : cleaned;
    }

    private LocalDateTime atStartOfDay(LocalDate date) {
        return date == null ? null : date.atStartOfDay();
    }

    private LocalDate toLocalDate(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.toLocalDate();
    }

    private record IdentityMatch(GovernmentIdentityRecord record, boolean conflict) {
    }
}
