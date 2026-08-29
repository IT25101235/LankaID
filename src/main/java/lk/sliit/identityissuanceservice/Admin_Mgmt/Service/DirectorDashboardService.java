package lk.sliit.identityissuanceservice.Admin_Mgmt.Service;

import lk.sliit.identityissuanceservice.Admin_Mgmt.Dto.DirectorDashboardStats;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Dto.DirectorRecentApplicationRow;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Dto.OfficerPerformanceRow;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Model.StaffAccount;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Repository.StaffAccountRepository;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model.CitizenApplication;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Repository.CitizenApplicationRepository;
import lk.sliit.identityissuanceservice.User_Mgmt.Model.CitizenAccount;
import lk.sliit.identityissuanceservice.User_Mgmt.Model.UserAccount;
import lk.sliit.identityissuanceservice.User_Mgmt.Repository.CitizenAccountRepository;
import lk.sliit.identityissuanceservice.User_Mgmt.Repository.UserAccountRepository;
import lk.sliit.identityissuanceservice.Verification_And_Approval_Management.Model.ApplicationReview;
import lk.sliit.identityissuanceservice.Verification_And_Approval_Management.Repository.ApplicationReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DirectorDashboardService {

    private static final Set<String> APPROVED_OR_LATER = Set.of(
            "APPROVED", "APPOINTMENT_BOOKED", "BIOMETRIC_COLLECTED", "ISSUED"
    );
    private static final Set<String> REVIEW_QUEUE = Set.of("SUBMITTED", "RESUBMITTED", "UNDER_REVIEW");
    private static final Set<String> BIOMETRIC_PIPELINE = Set.of("APPROVED", "APPOINTMENT_BOOKED");

    private final CitizenApplicationRepository applicationRepository;
    private final CitizenAccountRepository citizenRepository;
    private final StaffAccountRepository staffRepository;
    private final UserAccountRepository userRepository;
    private final ApplicationReviewRepository reviewRepository;

    public DirectorDashboardService(
            CitizenApplicationRepository applicationRepository,
            CitizenAccountRepository citizenRepository,
            StaffAccountRepository staffRepository,
            UserAccountRepository userRepository,
            ApplicationReviewRepository reviewRepository
    ) {
        this.applicationRepository = applicationRepository;
        this.citizenRepository = citizenRepository;
        this.staffRepository = staffRepository;
        this.userRepository = userRepository;
        this.reviewRepository = reviewRepository;
    }

    @Transactional(readOnly = true)
    public DirectorDashboardStats getStats() {
        List<CitizenApplication> applications = applicationRepository.findAll();
        List<YearMonth> months = lastSixMonths();

        return new DirectorDashboardStats(
                applications.size(),
                countStatuses(applications, APPROVED_OR_LATER),
                countStatus(applications, "BIOMETRIC_COLLECTED"),
                countStatus(applications, "ISSUED"),
                countService(applications, "NIC"),
                countService(applications, "PASSPORT"),
                countStatuses(applications, REVIEW_QUEUE),
                countStatuses(applications, BIOMETRIC_PIPELINE),
                countStatus(applications, "CORRECTION_REQUIRED"),
                countStatus(applications, "REJECTED"),
                months.stream().map(month -> month.format(DateTimeFormatter.ofPattern("MMM yyyy"))).toList(),
                monthlyCounts(applications, months, "NIC"),
                monthlyCounts(applications, months, "PASSPORT"),
                recentApplications(applications),
                officerPerformance()
        );
    }

    private List<DirectorRecentApplicationRow> recentApplications(List<CitizenApplication> applications) {
        Map<Integer, CitizenAccount> citizens = citizenRepository.findAllById(
                        applications.stream().map(CitizenApplication::getCitizenId).distinct().toList())
                .stream().collect(Collectors.toMap(CitizenAccount::getUserId, Function.identity()));

        return applications.stream()
                .sorted(Comparator.comparing(
                        CitizenApplication::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .limit(8)
                .map(application -> {
                    CitizenAccount citizen = citizens.get(application.getCitizenId());
                    return new DirectorRecentApplicationRow(
                            application.getApplicationNo(),
                            application.getServiceType(),
                            application.getApplicationKind(),
                            citizenName(citizen, application.getCitizenId()),
                            application.getCurrentStatus(),
                            application.getCreatedAt()
                    );
                })
                .toList();
    }

    private List<OfficerPerformanceRow> officerPerformance() {
        List<StaffAccount> officers = staffRepository.findAll().stream()
                .filter(staff -> "ACTIVE".equals(staff.getStaffStatus()))
                .filter(staff -> OfficerAccountService.NIC_OFFICER.equals(staff.getStaffRole())
                        || OfficerAccountService.PASSPORT_OFFICER.equals(staff.getStaffRole()))
                .sorted(Comparator.comparing(StaffAccount::getEmployeeNo))
                .toList();
        Map<Integer, UserAccount> users = userRepository.findAllById(
                        officers.stream().map(StaffAccount::getUserId).toList())
                .stream().collect(Collectors.toMap(UserAccount::getUserId, Function.identity()));
        List<ApplicationReview> reviews = reviewRepository.findAllByOrderByReviewedAtDesc();

        return officers.stream().map(officer -> {
            List<ApplicationReview> officerReviews = reviews.stream()
                    .filter(review -> officer.getUserId().equals(review.getStaffId()))
                    .toList();
            long approved = countDecision(officerReviews, "APPROVED");
            long corrections = countDecision(officerReviews, "CORRECTION_REQUIRED");
            long rejected = countDecision(officerReviews, "REJECTED");
            long reviewed = officerReviews.size();
            double approvalRate = reviewed == 0 ? 0 : Math.round(approved * 1000.0 / reviewed) / 10.0;
            UserAccount user = users.get(officer.getUserId());
            return new OfficerPerformanceRow(
                    officer.getEmployeeNo(),
                    user == null ? "Account not found" : user.getEmail(),
                    roleLabel(officer.getStaffRole()),
                    reviewed,
                    approved,
                    corrections,
                    rejected,
                    approvalRate
            );
        }).toList();
    }

    private List<YearMonth> lastSixMonths() {
        List<YearMonth> months = new ArrayList<>();
        YearMonth current = YearMonth.now();
        for (int index = 5; index >= 0; index--) months.add(current.minusMonths(index));
        return months;
    }

    private List<Long> monthlyCounts(
            List<CitizenApplication> applications,
            List<YearMonth> months,
            String serviceType
    ) {
        return months.stream().map(month -> applications.stream()
                .filter(application -> serviceType.equals(application.getServiceType()))
                .filter(application -> application.getCreatedAt() != null)
                .filter(application -> month.equals(YearMonth.from(application.getCreatedAt())))
                .count()).toList();
    }

    private long countService(List<CitizenApplication> applications, String serviceType) {
        return applications.stream().filter(application -> serviceType.equals(application.getServiceType())).count();
    }

    private long countStatus(List<CitizenApplication> applications, String status) {
        return applications.stream().filter(application -> status.equals(application.getCurrentStatus())).count();
    }

    private long countStatuses(List<CitizenApplication> applications, Set<String> statuses) {
        return applications.stream().filter(application -> statuses.contains(application.getCurrentStatus())).count();
    }

    private long countDecision(List<ApplicationReview> reviews, String decision) {
        return reviews.stream().filter(review -> decision.equals(review.getDecision())).count();
    }

    private String citizenName(CitizenAccount citizen, Integer citizenId) {
        if (citizen == null) return "Citizen #" + citizenId;
        String givenName = citizen.getGivenName() == null ? "" : citizen.getGivenName().trim();
        String familyName = citizen.getFamilyName() == null ? "" : citizen.getFamilyName().trim();
        String name = (givenName + " " + familyName).trim();
        if (!name.isBlank()) return name;
        return citizen.getSurname() == null || citizen.getSurname().isBlank()
                ? "Citizen #" + citizenId : citizen.getSurname();
    }

    private String roleLabel(String role) {
        return OfficerAccountService.NIC_OFFICER.equals(role)
                ? "NIC Application Officer" : "Passport Application Officer";
    }
}
