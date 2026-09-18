package lk.sliit.identityissuanceservice.User_Mgmt.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lk.sliit.identityissuanceservice.User_Mgmt.Service.PasswordRules;

public class PasswordResetDto {
    @NotBlank(message = "Password is required.")
    @Size(min = 8, max = 72, message = "Password must contain 8 to 72 characters.")
    @Pattern(regexp = PasswordRules.PATTERN, message = PasswordRules.MESSAGE)
    private String password;

    @NotBlank(message = "Please repeat your password.")
    private String confirmPassword;

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
}
