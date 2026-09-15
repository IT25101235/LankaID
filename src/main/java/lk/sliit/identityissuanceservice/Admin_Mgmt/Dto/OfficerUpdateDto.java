package lk.sliit.identityissuanceservice.Admin_Mgmt.Dto;

import jakarta.validation.constraints.Email;
import lk.sliit.identityissuanceservice.User_Mgmt.Service.PasswordRules;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class OfficerUpdateDto {

    @NotBlank(message = "Email is required.")
    @Email(message = "Enter a valid email address.")
    @Size(max = 100, message = "Email must not exceed 100 characters.")
    private String email;

    @NotBlank(message = "Phone number is required.")
    @Pattern(regexp = "^(?:\\+94|0)?7[0-9]{8}$", message = "Enter nine mobile digits starting with 7 after +94 (for example, 771234567).")
    private String phone;

    @NotBlank(message = "Officer role is required.")
    private String role;

    @Pattern(regexp = "^$|" + PasswordRules.PATTERN, message = PasswordRules.MESSAGE)
    private String newPassword;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
