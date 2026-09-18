package lk.sliit.identityissuanceservice.Admin_Mgmt.Service;

import lk.sliit.identityissuanceservice.Admin_Mgmt.Dto.CitizenAccountDashboardDto;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Dto.CitizenAccountRowDto;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Repository.CitizenApplicationRepository;
import lk.sliit.identityissuanceservice.User_Mgmt.Model.CitizenAccount;
import lk.sliit.identityissuanceservice.User_Mgmt.Model.UserAccount;
import lk.sliit.identityissuanceservice.User_Mgmt.Repository.CitizenAccountRepository;
import lk.sliit.identityissuanceservice.User_Mgmt.Repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CitizenAccountDashboardService {

    private static final String ACTIVE = "ACTIVE";
    private static final String SUSPENDED = "SUSPENDED";
    private static final String NIC = "NIC";
    private static final String PASSPORT = "PASSPORT";

    private final CitizenAccountRepository citizenAccountRepository;
    private final UserAccountRepository userAccountRepository;
    private final CitizenApplicationRepository applicationRepository;

    public CitizenAccountDashboardService(
            CitizenAccountRepository citizenAccountRepository,
            UserAccountRepository userAccountRepository,
            CitizenApplicationRepository applicationRepository
    ) {
        this.citizenAccountRepository = citizenAccountRepository;
        this.userAccountRepository = userAccountRepository;
        this.applicationRepository = applicationRepository;
    }

    @Transactional(readOnly = true)
    public CitizenAccountDashboardDto getDashboard() {
        List<CitizenAccount> citizenAccounts = citizenAccountRepository.findAll().stream()
                .sorted((left, right) -> right.getUserId().compareTo(left.getUserId()))
                .toList();
        List<Integer> citizenIds = citizenAccounts.stream().map(CitizenAccount::getUserId).toList();

        Map<Integer, UserAccount> users = userAccountRepository.findAllById(citizenIds).stream()
                .collect(Collectors.toMap(UserAccount::getUserId, Function.identity()));
        Map<Integer, Map<String, Long>> applicationCounts = applicationCounts(citizenIds);
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();

        List<CitizenAccountRowDto> rows = citizenAccounts.stream()
                .map(citizen -> toRow(
                        citizen, users.get(citizen.getUserId()), applicationCounts, monthStart))
                .filter(java.util.Objects::nonNull)
                .toList();

        long registeredThisMonth = rows.stream().filter(CitizenAccountRowDto::registeredThisMonth).count();
        long suspended = rows.stream().filter(row -> SUSPENDED.equals(row.status())).count();

        return new CitizenAccountDashboardDto(rows, rows.size(), registeredThisMonth, suspended);
    }

    private Map<Integer, Map<String, Long>> applicationCounts(List<Integer> citizenIds) {
        Map<Integer, Map<String, Long>> counts = new HashMap<>();
        if (citizenIds.isEmpty()) {
            return counts;
        }
        for (CitizenApplicationRepository.ApplicationCount projection
                : applicationRepository.countApplicationsByCitizenAndServiceType(citizenIds)) {
            counts.computeIfAbsent(projection.getCitizenId(), ignored -> new HashMap<>())
                    .put(normalize(projection.getServiceType()), projection.getApplicationCount());
        }
        return counts;
    }

    private CitizenAccountRowDto toRow(
            CitizenAccount citizen,
            UserAccount user,
            Map<Integer, Map<String, Long>> applicationCounts,
            LocalDateTime monthStart
    ) {
        if (user == null) {
            return null;
        }
        Map<String, Long> counts = applicationCounts.getOrDefault(citizen.getUserId(), Map.of());
        long nicCount = counts.getOrDefault(NIC, 0L);
        long passportCount = counts.getOrDefault(PASSPORT, 0L);
        String status = normalize(user.getAccountStatus());
        return new CitizenAccountRowDto(
                citizen.getUserId(),
                "CTZ-%06d".formatted(citizen.getUserId()),
                displayName(citizen),
                user.getEmail(),
                nicCount,
                passportCount,
                status,
                statusLabel(status),
                ACTIVE.equals(status),
                user.getRegisteredAt() != null && !user.getRegisteredAt().isBefore(monthStart)
        );
    }

    private String displayName(CitizenAccount citizen) {
        String name = (clean(citizen.getGivenName()) + " " + clean(citizen.getFamilyName())).trim();
        return name.isBlank() ? "Profile not completed" : name;
    }

    private String statusLabel(String status) {
        if (status.isBlank()) {
            return "Unknown";
        }
        String lowerCase = status.toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(lowerCase.charAt(0)) + lowerCase.substring(1);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
