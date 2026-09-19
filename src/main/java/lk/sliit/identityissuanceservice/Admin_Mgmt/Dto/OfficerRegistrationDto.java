package lk.sliit.identityissuanceservice.Admin_Mgmt.Dto;

import jakarta.validation.constraints.Email;
import lk.sliit.identityissuanceservice.User_Mgmt.Service.PasswordRules;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class OfficerRegistrationDto {

    @NotBlank(message = "Email is required.")
    @Email(message = "Enter a valid email address.")
    @Size(max = 100, message = "Email cannot exceed 100 characters.")
    @Pattern(regexp = "(?i)^[A-Z0-9._%+-]+@gov\\.lk$", message = "Officer email must end with @gov.lk.")
    private String email;

    @NotBlank(message = "Please select an officer role.")
    private String role;

    @NotBlank(message = "Temporary password is required.")
    @Size(min = 8, max = 72, message = "Password must contain 8 to 72 characters.")
    @Pattern(regexp = PasswordRules.PATTERN, message = PasswordRules.MESSAGE)
    private String password;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
