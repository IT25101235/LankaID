package lk.sliit.identityissuanceservice.Admin_Mgmt.Dto;

public record OfficerPerformanceRow(
        String employeeNo,
        String email,
        String role,
        long reviewed,
        long approved,
        long corrections,
        long rejected,
        double approvalRate
) {
}
