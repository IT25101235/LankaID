package lk.sliit.identityissuanceservice.Admin_Mgmt.Dto;

import java.util.List;

public record DirectorDashboardStats(
        long totalApplications,
        long approvedApplications,
        long readyForPrinting,
        long printedDocuments,
        long nicApplications,
        long passportApplications,
        long pendingReview,
        long biometricPipeline,
        long correctionRequired,
        long rejectedApplications,
        List<String> monthLabels,
        List<Long> monthlyNicApplications,
        List<Long> monthlyPassportApplications,
        List<DirectorRecentApplicationRow> recentApplications,
        List<OfficerPerformanceRow> officerPerformance
) {
}
