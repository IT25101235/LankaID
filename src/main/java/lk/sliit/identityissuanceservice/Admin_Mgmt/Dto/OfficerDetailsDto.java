package lk.sliit.identityissuanceservice.Admin_Mgmt.Dto;

public class OfficerDetailsDto {

    private final Integer userId;
    private final String employeeNo;
    private final String email;
    private final String roleCode;
    private final String roleLabel;
    private final String roleFilter;
    private final String status;

    public OfficerDetailsDto(
            Integer userId,
            String employeeNo,
            String email,
            String roleCode,
            String roleLabel,
            String roleFilter,
            String status) {
        this.userId = userId;
        this.employeeNo = employeeNo;
        this.email = email;
        this.roleCode = roleCode;
        this.roleLabel = roleLabel;
        this.roleFilter = roleFilter;
        this.status = status;
    }

    public Integer getUserId() {
        return userId;
    }

    public String getEmployeeNo() {
        return employeeNo;
    }

    public String getEmail() {
        return email;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public String getRoleLabel() {
        return roleLabel;
    }

    public String getRoleFilter() {
        return roleFilter;
    }

    public String getStatus() {
        return status;
    }
}
