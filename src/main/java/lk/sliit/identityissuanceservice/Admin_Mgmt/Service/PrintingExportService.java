package lk.sliit.identityissuanceservice.Admin_Mgmt.Service;

import lk.sliit.identityissuanceservice.Admin_Mgmt.Dto.PrintingExportRow;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model.CitizenApplication;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model.GovernmentIdentityRecord;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Repository.ApplicationDocumentRepository;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Repository.CitizenApplicationRepository;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Repository.GovernmentIdentityRecordRepository;
import lk.sliit.identityissuanceservice.Document_Renewal_Replacement_And_Delivery_Mgmt.Service.DocumentDeliveryService;
import lk.sliit.identityissuanceservice.Notification_Mgmt.Service.NotificationService;
import lk.sliit.identityissuanceservice.User_Mgmt.Model.CitizenAccount;
import lk.sliit.identityissuanceservice.User_Mgmt.Model.UserAccount;
import lk.sliit.identityissuanceservice.User_Mgmt.Repository.CitizenAccountRepository;
import lk.sliit.identityissuanceservice.User_Mgmt.Repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class PrintingExportService {

    public static final String READY_FOR_PRINTING = "BIOMETRIC_COLLECTED";
    // ISSUED is the existing database status used after the physical document is printed.
    public static final String ISSUED = "ISSUED";

    private final CitizenApplicationRepository applicationRepository;
    private final CitizenAccountRepository citizenRepository;
    private final UserAccountRepository userRepository;
    private final GovernmentIdentityRecordRepository governmentRepository;
    private final ApplicationDocumentRepository documentRepository;
    private final NotificationService notificationService;
    private final DocumentDeliveryService deliveryService;

    public PrintingExportService(
            CitizenApplicationRepository applicationRepository,
            CitizenAccountRepository citizenRepository,
            UserAccountRepository userRepository,
            GovernmentIdentityRecordRepository governmentRepository,
            ApplicationDocumentRepository documentRepository,
            NotificationService notificationService,
            DocumentDeliveryService deliveryService
    ) {
        this.applicationRepository = applicationRepository;
        this.citizenRepository = citizenRepository;
        this.userRepository = userRepository;
        this.governmentRepository = governmentRepository;
        this.documentRepository = documentRepository;
        this.notificationService = notificationService;
        this.deliveryService = deliveryService;
    }

    @Transactional(readOnly = true)
    public List<PrintingExportRow> getReadyApplications() {
        return applicationRepository
                .findAllByCurrentStatusOrderByUpdatedAtAsc(READY_FOR_PRINTING)
                .stream()
                .map(this::buildRow)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PrintingExportRow> getPrintedApplications() {
        return applicationRepository
                .findAllByCurrentStatusOrderByUpdatedAtDesc(ISSUED)
                .stream()
                .map(this::buildRow)
                .toList();
    }

    @Transactional
    public byte[] printAndCreateCsv(List<Integer> applicationIds, Integer directorId) {
        if (applicationIds == null || applicationIds.isEmpty()) {
            throw new IllegalArgumentException("Select at least one application to print.");
        }

        List<CitizenApplication> applications = new ArrayList<>(applicationRepository.findAllById(applicationIds));
        if (applications.size() != applicationIds.stream().distinct().count()) {
            throw new IllegalArgumentException("One or more selected applications were not found.");
        }
        applications.sort(Comparator.comparing(CitizenApplication::getApplicationId));

        List<PrintingExportRow> printedRows = new ArrayList<>();
        for (CitizenApplication application : applications) {
            if (!READY_FOR_PRINTING.equals(application.getCurrentStatus())) {
                throw new IllegalArgumentException(
                        "Application " + application.getApplicationNo() + " is not ready for printing."
                );
            }

            CitizenAccount citizen = findCitizen(application.getCitizenId());
            UserAccount user = findUser(application.getCitizenId());
            GovernmentIdentityRecord record = findOrCreateGovernmentRecord(citizen, user);
            String documentNumber = issueDocumentNumber(application, citizen, record);

            record.setIdentityStatus("ACTIVE");
            governmentRepository.save(record);

            application.setCurrentStatus(ISSUED);
            applicationRepository.save(application);
            deliveryService.updateAfterPrinting(application, directorId);

            notificationService.sendToCitizen(
                    application.getCitizenId(),
                    directorId,
                    "DOCUMENT_PRINTED",
                    application.getServiceType() + " application " + application.getApplicationNo()
                            + " has been printed. Document number: " + documentNumber + "."
            );
            printedRows.add(buildRow(application));
        }
        return createCsv(printedRows);
    }

    @Transactional(readOnly = true)
    public byte[] createPrintedRegisterCsv() {
        return createCsv(getPrintedApplications());
    }

    private GovernmentIdentityRecord findOrCreateGovernmentRecord(CitizenAccount citizen, UserAccount user) {
        // Existing documents must have been linked through verification, never just by matching contacts.
        if (citizen.getGovernmentRecordId() != null) {
            return governmentRepository.findById(citizen.getGovernmentRecordId())
                    .orElseThrow(() -> new IllegalArgumentException("The linked government record is missing. Contact support."));
        }
        if (citizenRepository.countIdentityRequests(citizen.getUserId()) > 0
                || governmentRepository.findByRegisteredEmailIgnoreCase(user.getEmail()).isPresent()
                || governmentRepository.findByRegisteredPhone(user.getPhone()).isPresent()) {
            throw new IllegalArgumentException("Verify the citizen's existing government record before printing.");
        }
        GovernmentIdentityRecord record = new GovernmentIdentityRecord();
        record.setRegisteredEmail(user.getEmail());
        record.setRegisteredPhone(user.getPhone());
        record.setIdentityStatus("ACTIVE");
        record = governmentRepository.save(record);
        citizen.setGovernmentRecordId(record.getGovernmentRecordId());
        citizenRepository.save(citizen);
        return record;
    }

    private String issueDocumentNumber(
            CitizenApplication application,
            CitizenAccount citizen,
            GovernmentIdentityRecord record
    ) {
        if ("NIC".equals(application.getServiceType())) {
            if (hasText(record.getNicNo())) return record.getNicNo();
            String number = generateNicNumber(citizen, application.getApplicationId());
            record.setNicNo(number);
            return number;
        }

        String number = generatePassportNumber(application.getApplicationId());
        record.setPassportNo(number);
        return number;
    }

    private String generateNicNumber(CitizenAccount citizen, Integer applicationId) {
        if (citizen.getDateOfBirth() == null) {
            throw new IllegalArgumentException("The citizen date of birth is required to generate the NIC number.");
        }

        LocalDate birthDate = citizen.getDateOfBirth().toLocalDate();
        int encodedDay = birthDate.getDayOfYear();
        if (citizen.getGender() != null && citizen.getGender().toUpperCase().startsWith("F")) {
            encodedDay += 500;
        }

        int firstSerial = Math.floorMod(applicationId, 10_000);
        for (int attempt = 0; attempt < 10_000; attempt++) {
            int serial = (firstSerial + attempt) % 10_000;
            String firstEleven = "%04d%03d%04d".formatted(birthDate.getYear(), encodedDay, serial);
            String candidate = firstEleven + checksumDigit(firstEleven);
            if (!governmentRepository.existsByNicNo(candidate)) return candidate;
        }
        throw new IllegalStateException("A unique simulated NIC number could not be generated.");
    }

    private String generatePassportNumber(Integer applicationId) {
        int firstSerial = Math.floorMod(applicationId, 10_000_000);
        for (int attempt = 0; attempt < 10_000_000; attempt++) {
            int serial = (firstSerial + attempt) % 10_000_000;
            String candidate = "N%07d".formatted(serial);
            if (!governmentRepository.existsByPassportNo(candidate)) return candidate;
        }
        throw new IllegalStateException("A unique simulated passport number could not be generated.");
    }

    private int checksumDigit(String digits) {
        return digits.chars().map(character -> character - '0').sum() % 10;
    }

    private PrintingExportRow buildRow(CitizenApplication application) {
        CitizenAccount citizen = findCitizen(application.getCitizenId());
        UserAccount user = findUser(application.getCitizenId());
        GovernmentIdentityRecord record = citizen.getGovernmentRecordId() == null
                ? null
                : governmentRepository.findById(citizen.getGovernmentRecordId()).orElse(null);
        String documentNumber = record == null ? null
                : "NIC".equals(application.getServiceType()) ? record.getNicNo() : record.getPassportNo();
        String photographPath = documentRepository
                .findByApplicationIdAndDocumentType(application.getApplicationId(), "PHOTOGRAPH")
                .map(document -> document.getFilePath())
                .orElse("");

        return new PrintingExportRow(
                application.getApplicationId(),
                application.getApplicationNo(),
                application.getServiceType(),
                application.getApplicationKind(),
                citizenName(citizen),
                citizen.getDateOfBirth() == null ? null : citizen.getDateOfBirth().toLocalDate(),
                clean(citizen.getGender()),
                permanentAddress(citizen),
                user.getEmail(),
                user.getPhone(),
                photographPath,
                documentNumber,
                application.getUpdatedAt()
        );
    }

    private byte[] createCsv(List<PrintingExportRow> rows) {
        StringBuilder csv = new StringBuilder();
        csv.append("Application Reference,Service,Application Type,Document Number,Citizen Name,")
                .append("Date of Birth,Gender,Permanent Address,Email,Phone,Photograph Path\r\n");
        for (PrintingExportRow row : rows) {
            csv.append(csvValue(row.applicationNo())).append(',')
                    .append(csvValue(row.serviceType())).append(',')
                    .append(csvValue(row.applicationKind())).append(',')
                    .append(csvValue(row.documentNumber())).append(',')
                    .append(csvValue(row.citizenName())).append(',')
                    .append(csvValue(row.dateOfBirth())).append(',')
                    .append(csvValue(row.gender())).append(',')
                    .append(csvValue(row.permanentAddress())).append(',')
                    .append(csvValue(row.email())).append(',')
                    .append(csvValue(row.phone())).append(',')
                    .append(csvValue(row.photographPath())).append("\r\n");
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String csvValue(Object value) {
        String text = value == null ? "" : value.toString();
        return '"' + text.replace("\"", "\"\"") + '"';
    }

    private CitizenAccount findCitizen(Integer citizenId) {
        return citizenRepository.findById(citizenId)
                .orElseThrow(() -> new IllegalArgumentException("Citizen details were not found."));
    }

    private UserAccount findUser(Integer citizenId) {
        return userRepository.findById(citizenId)
                .orElseThrow(() -> new IllegalArgumentException("Citizen account was not found."));
    }

    private String citizenName(CitizenAccount citizen) {
        String name = (clean(citizen.getGivenName()) + " " + clean(citizen.getFamilyName())).trim();
        if (!name.isBlank()) return name;
        return clean(citizen.getSurname()).isBlank() ? "Citizen #" + citizen.getUserId() : citizen.getSurname();
    }

    private String permanentAddress(CitizenAccount citizen) {
        return String.join(", ", List.of(
                clean(citizen.getPermanentHouseNo()),
                clean(citizen.getPermanentStreet()),
                clean(citizen.getPermanentCity()),
                clean(citizen.getPermanentPostalCode())
        ).stream().filter(this::hasText).toList());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
