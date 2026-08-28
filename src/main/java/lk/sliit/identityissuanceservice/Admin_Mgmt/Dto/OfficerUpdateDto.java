package lk.sliit.identityissuanceservice.Admin_Mgmt.Dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class OfficerUpdateDto {

    @NotBlank(message = "Email is required.")
    @Email(message = "Enter a valid email address.")
    @Size(max = 100, message = "Email must not exceed 100 characters.")
    private String email;

    @NotBlank(message = "Phone number is required.")
    @Pattern(regexp = "^\\+?[0-9]{10,14}$", message = "Enter a valid phone number.")
    private String phone;

    @NotBlank(message = "Officer role is required.")
    private String role;

    @Pattern(regexp = "^$|^.{8,72}$", message = "New password must contain 8 to 72 characters.")
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
