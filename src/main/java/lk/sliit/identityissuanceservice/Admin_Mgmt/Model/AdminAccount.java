package lk.sliit.identityissuanceservice.Admin_Mgmt.Model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "StaffAccount", schema = "dbo")
public class AdminAccount {

    @Id
    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "employee_no", nullable = false, unique = true, length = 20)
    private String employeeNo;

    @Column(name = "staff_role", nullable = false, length = 30)
    private String staffRole;

    @Column(name = "staff_status", nullable = false, length = 20)
    private String staffStatus;

    public AdminAccount() {
    }

    public AdminAccount(Integer userId, String employeeNo) {
        this.userId = userId;
        this.employeeNo = employeeNo;
        this.staffRole = "SYSTEM_ADMIN";
        this.staffStatus = "ACTIVE";
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getEmployeeNo() {
        return employeeNo;
    }

    public void setEmployeeNo(String employeeNo) {
        this.employeeNo = employeeNo;
    }

    public String getStaffRole() {
        return staffRole;
    }

    public void setStaffRole(String staffRole) {
        this.staffRole = staffRole;
    }

    public String getStaffStatus() {
        return staffStatus;
    }

    public void setStaffStatus(String staffStatus) {
        this.staffStatus = staffStatus;
    }
}
