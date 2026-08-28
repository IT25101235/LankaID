package lk.sliit.identityissuanceservice.Verification_And_Approval_Management.Service;

import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model.CitizenApplication;
import lk.sliit.identityissuanceservice.User_Mgmt.Model.CitizenAccount;
import lk.sliit.identityissuanceservice.User_Mgmt.Repository.CitizenAccountRepository;
import lk.sliit.identityissuanceservice.Verification_And_Approval_Management.Dto.VerificationQueueItem;
import lk.sliit.identityissuanceservice.Verification_And_Approval_Management.Repository.VerificationApplicationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class VerificationQueueService {

    private static final List<String> QUEUE_STATUSES = List.of(
            "SUBMITTED",
            "UNDER_REVIEW",
            "CORRECTION_REQUIRED",
            "RESUBMITTED"
    );

    private final VerificationApplicationRepository applicationRepository;
    private final CitizenAccountRepository citizenAccountRepository;

    public VerificationQueueService(
            VerificationApplicationRepository applicationRepository,
            CitizenAccountRepository citizenAccountRepository
    ) {
        this.applicationRepository = applicationRepository;
        this.citizenAccountRepository = citizenAccountRepository;
    }

    @Transactional(readOnly = true)
    public List<VerificationQueueItem> getNicQueue() {
        return getQueue("NIC");
    }

    @Transactional(readOnly = true)
    public List<VerificationQueueItem> getPassportQueue() {
        return getQueue("PASSPORT");
    }

    private List<VerificationQueueItem> getQueue(String serviceType) {
        List<CitizenApplication> applications = applicationRepository
                .findByServiceTypeAndCurrentStatusInOrderBySubmittedAtAscApplicationIdAsc(
                        serviceType,
                        QUEUE_STATUSES
                );

        List<Integer> citizenIds = applications.stream()
                .map(CitizenApplication::getCitizenId)
                .distinct()
                .toList();

        Map<Integer, CitizenAccount> citizens = citizenAccountRepository.findAllById(citizenIds)
                .stream()
                .collect(Collectors.toMap(CitizenAccount::getUserId, Function.identity()));

        return applications.stream()
                .map(application -> toQueueItem(application, citizens.get(application.getCitizenId()), serviceType))
                .toList();
    }

    private VerificationQueueItem toQueueItem(
            CitizenApplication application,
            CitizenAccount citizen,
            String serviceType
    ) {
        String applicantName = buildApplicantName(citizen, application.getCitizenId());

        return new VerificationQueueItem(
                application.getApplicationId(),
                application.getApplicationNo(),
                applicantName,
                buildInitials(applicantName),
                citizen == null ? null : citizen.getDateOfBirth(),
                buildApplicationKindLabel(application.getApplicationKind(), serviceType),
                application.getSubmittedAt(),
                application.getCurrentStatus(),
                buildStatusLabel(application.getCurrentStatus())
        );
    }

    private String buildApplicantName(CitizenAccount citizen, Integer citizenId) {
        if (citizen == null) {
            return "Citizen #" + citizenId;
        }

        String givenName = clean(citizen.getGivenName());
        String familyName = clean(citizen.getFamilyName());

        String fullName = (givenName + " " + familyName).trim();
        if (!fullName.isBlank()) {
            return fullName;
        }

        String surname = clean(citizen.getSurname());
        return surname.isBlank() ? "Citizen #" + citizenId : surname;
    }

    private String buildInitials(String name) {
        StringBuilder initials = new StringBuilder();
        for (String word : name.split("\\s+")) {
            if (!word.isBlank() && Character.isLetter(word.charAt(0)) && initials.length() < 2) {
                initials.append(Character.toUpperCase(word.charAt(0)));
            }
        }
        return initials.isEmpty() ? "C" : initials.toString();
    }

    private String buildApplicationKindLabel(String applicationKind, String serviceType) {
        if ("NEW".equals(applicationKind)) {
            return "New " + ("NIC".equals(serviceType) ? "NIC" : "Passport");
        }
        if ("RENEWAL".equals(applicationKind)) {
            return "Renewal";
        }
        if ("LOST".equals(applicationKind)) {
            return "Lost replacement";
        }
        return buildStatusLabel(applicationKind);
    }

    private String buildStatusLabel(String status) {
        if (status == null || status.isBlank()) {
            return "Unknown";
        }

        String words = status.toLowerCase().replace('_', ' ');
        return Character.toUpperCase(words.charAt(0)) + words.substring(1);
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
