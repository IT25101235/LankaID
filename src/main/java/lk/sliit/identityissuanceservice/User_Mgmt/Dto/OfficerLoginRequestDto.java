package lk.sliit.identityissuanceservice.User_Mgmt.Dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OfficerLoginRequestDto {

    @NotBlank(message = "Please select your role")
    @Pattern(regexp = "^(nic-officer|passport-officer|csr|director|admin)$",
            message = "Invalid officer role selected")
    private String role;

    @NotBlank(message = "Staff email is required")
    @Email(message = "Enter a valid email address")
    @Pattern(regexp = "^[A-Za-z0-9._%+-]+@lankaid\\.gov\\.lk$",
            message = "Use your official @lankaid.gov.lk email address")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be at least 8 characters")
    private String password;

    private boolean rememberMe;
}
