package lk.sliit.identityissuanceservice.Notification_Mgmt.Service;

import lk.sliit.identityissuanceservice.Admin_Mgmt.Model.StaffAccount;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Repository.StaffAccountRepository;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Repository.CitizenApplicationRepository;
import lk.sliit.identityissuanceservice.Notification_Mgmt.Dto.AdminNotificationRequest;
import lk.sliit.identityissuanceservice.Notification_Mgmt.Dto.AdminRecipient;
import lk.sliit.identityissuanceservice.Notification_Mgmt.Dto.EmailDispatchResult;
import lk.sliit.identityissuanceservice.Notification_Mgmt.Observer.NotificationPublisher;
import lk.sliit.identityissuanceservice.User_Mgmt.Model.CitizenAccount;
import lk.sliit.identityissuanceservice.User_Mgmt.Model.UserAccount;
import lk.sliit.identityissuanceservice.User_Mgmt.Repository.CitizenAccountRepository;
import lk.sliit.identityissuanceservice.User_Mgmt.Repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AdminNotificationService {

    private static final Set<String> RECIPIENT_TYPES = Set.of("CITIZEN", "OFFICER");
    private static final Set<String> AUDIENCES = Set.of("ALL", "FILTERED", "SELECTED");
    private static final Set<String> CATEGORIES = Set.of("GENERAL", "SERVICE_INTERRUPTION", "APPOINTMENT", "APPLICATION", "EMERGENCY");

    private final CitizenAccountRepository citizenAccountRepository;
    private final StaffAccountRepository staffAccountRepository;
    private final UserAccountRepository userAccountRepository;
    private final CitizenApplicationRepository citizenApplicationRepository;
    private final NotificationPublisher notificationPublisher;
    private final OfficialNotificationEmailService emailService;

    public AdminNotificationService(
            CitizenAccountRepository citizenAccountRepository,
            StaffAccountRepository staffAccountRepository,
            UserAccountRepository userAccountRepository,
            CitizenApplicationRepository citizenApplicationRepository,
            NotificationPublisher notificationPublisher,
            OfficialNotificationEmailService emailService
    ) {
        this.citizenAccountRepository = citizenAccountRepository;
        this.staffAccountRepository = staffAccountRepository;
        this.userAccountRepository = userAccountRepository;
        this.citizenApplicationRepository = citizenApplicationRepository;
        this.notificationPublisher = notificationPublisher;
        this.emailService = emailService;
    }

    @Transactional
    public int sendInApp(AdminNotificationRequest request, Integer administratorId) {
        List<AdminRecipient> recipients = resolveRecipients(request, false);
        String type = "ADMIN_" + normalizedCategory(request.getCategory());
        String message = request.getTitle().trim() + "\n\n" + request.getMessage().trim();
        return notificationPublisher.sendToUsers(
                recipients.stream().map(AdminRecipient::userId).toList(),
                administratorId,
                type,
                message,
                !"SELECTED".equalsIgnoreCase(request.getAudience())
        );
    }

    @Transactional(readOnly = true)
    public EmailDispatchResult sendEmail(AdminNotificationRequest request) {
        List<AdminRecipient> recipients = resolveRecipients(request, true);
        return emailService.sendOfficialEmail(recipients, request.getTitle(), request.getMessage());
    }

    private List<AdminRecipient> resolveRecipients(AdminNotificationRequest request, boolean emailOnly) {
        String recipientType = normalizedRequired(request.getRecipientType(), RECIPIENT_TYPES, "recipient type");
        String audience = normalizedRequired(request.getAudience(), AUDIENCES, "audience");
        Map<Integer, UserAccount> users = userAccountRepository.findAll().stream()
                .collect(Collectors.toMap(UserAccount::getUserId, Function.identity()));

        List<AdminRecipient> recipients = "CITIZEN".equals(recipientType)
                ? resolveCitizens(request, audience, users)
                : resolveOfficers(request, audience, users);

        if (emailOnly) {
            recipients = recipients.stream()
                    .filter(recipient -> {
                        UserAccount user = users.get(recipient.userId());
                        return user != null && "Y".equalsIgnoreCase(user.getEmailVerified());
                    })
                    .toList();
        }
        if (recipients.isEmpty()) {
            throw new IllegalArgumentException(emailOnly
                    ? "No eligible recipients with verified email addresses matched your selection."
                    : "No eligible recipients matched your selection.");
        }
        return recipients;
    }

    private List<AdminRecipient> resolveCitizens(
            AdminNotificationRequest request,
            String audience,
            Map<Integer, UserAccount> users
    ) {
        Map<Integer, CitizenAccount> citizens = citizenAccountRepository.findAll().stream()
                .collect(Collectors.toMap(CitizenAccount::getUserId, Function.identity()));
        if ("SELECTED".equals(audience)) {
            return selectedRecipients(request.getSelectedRecipients(), users, citizens.keySet(), Map.of());
        }

        String requiredStatus = "ALL".equals(audience) ? "ACTIVE" : normalizedStatus(request.getAccountStatus());
        String service = normalize(request.getCitizenService());
        return citizens.keySet().stream()
                .filter(users::containsKey)
                .filter(id -> accountMatches(users.get(id), requiredStatus))
                .filter(id -> !"FILTERED".equals(audience) || "ALL".equals(service)
                        || citizenApplicationRepository.existsByCitizenIdAndServiceType(id, service))
                .sorted()
                .map(id -> new AdminRecipient(id, users.get(id).getEmail()))
                .toList();
    }

    private List<AdminRecipient> resolveOfficers(
            AdminNotificationRequest request,
            String audience,
            Map<Integer, UserAccount> users
    ) {
        Map<Integer, StaffAccount> officers = staffAccountRepository.findAll().stream()
                .filter(staff -> !"SYSTEM_ADMIN".equals(staff.getStaffRole()))
                .filter(staff -> !"REMOVED".equals(staff.getStaffStatus()))
                .collect(Collectors.toMap(StaffAccount::getUserId, Function.identity()));
        Map<String, Integer> employeeNumbers = officers.values().stream()
                .collect(Collectors.toMap(staff -> normalize(staff.getEmployeeNo()), StaffAccount::getUserId));
        if ("SELECTED".equals(audience)) {
            return selectedRecipients(request.getSelectedRecipients(), users, officers.keySet(), employeeNumbers);
        }

        String requiredStatus = "ALL".equals(audience) ? "ACTIVE" : normalizedStatus(request.getAccountStatus());
        String role = normalize(request.getOfficerRole());
        return officers.values().stream()
                .filter(staff -> "ALL".equals(requiredStatus) || requiredStatus.equals(normalize(staff.getStaffStatus())))
                .filter(staff -> accountMatches(users.get(staff.getUserId()), requiredStatus))
                .filter(staff -> !"FILTERED".equals(audience) || "ALL".equals(role) || role.equals(normalize(staff.getStaffRole())))
                .sorted(java.util.Comparator.comparing(StaffAccount::getEmployeeNo))
                .map(staff -> new AdminRecipient(staff.getUserId(), users.get(staff.getUserId()).getEmail()))
                .toList();
    }

    private List<AdminRecipient> selectedRecipients(
            String rawIdentifiers,
            Map<Integer, UserAccount> users,
            Set<Integer> allowedUserIds,
            Map<String, Integer> extraIdentifiers
    ) {
        List<String> identifiers = Arrays.stream(rawIdentifiers == null ? new String[0] : rawIdentifiers.split("[,;\\n]"))
                .map(String::trim).filter(value -> !value.isBlank()).distinct().toList();
        if (identifiers.isEmpty()) {
            throw new IllegalArgumentException("Enter at least one account ID, employee number, or email address.");
        }

        Map<String, Integer> lookups = new LinkedHashMap<>(extraIdentifiers);
        allowedUserIds.forEach(id -> {
            UserAccount user = users.get(id);
            lookups.put(String.valueOf(id), id);
            lookups.put("CTZ-%06d".formatted(id), id);
            if (user != null) lookups.put(normalize(user.getEmail()), id);
        });

        List<String> unknown = identifiers.stream()
                .filter(identifier -> !lookups.containsKey(normalize(identifier)))
                .toList();
        if (!unknown.isEmpty()) {
            throw new IllegalArgumentException("Unknown or ineligible recipient: " + unknown.get(0));
        }
        return identifiers.stream()
                .map(identifier -> lookups.get(normalize(identifier)))
                .distinct()
                .filter(id -> accountMatches(users.get(id), "ALL"))
                .map(id -> new AdminRecipient(id, users.get(id).getEmail()))
                .toList();
    }

    private boolean accountMatches(UserAccount user, String requiredStatus) {
        if (user == null || "REMOVED".equalsIgnoreCase(user.getAccountStatus())) return false;
        return "ALL".equals(requiredStatus) || requiredStatus.equalsIgnoreCase(user.getAccountStatus());
    }

    private String normalizedStatus(String status) {
        String normalized = normalize(status);
        return Set.of("ACTIVE", "SUSPENDED", "ALL").contains(normalized) ? normalized : "ACTIVE";
    }

    private String normalizedCategory(String category) {
        String normalized = normalize(category);
        return CATEGORIES.contains(normalized) ? normalized : "GENERAL";
    }

    private String normalizedRequired(String value, Set<String> allowed, String field) {
        String normalized = normalize(value);
        if (!allowed.contains(normalized)) throw new IllegalArgumentException("Please select a valid " + field + ".");
        return normalized;
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? "ALL" : value.trim().toUpperCase(Locale.ROOT);
    }
}
