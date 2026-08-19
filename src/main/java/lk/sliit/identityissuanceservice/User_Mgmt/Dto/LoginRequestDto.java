package lk.sliit.identityissuanceservice.User_Mgmt.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class LoginRequestDto {

    @NotBlank(message = "NIC number or email is required")
    @Size(min = 3, max = 100, message = "NIC or email must be between 3 and 100 characters")
    @Pattern(regexp = "^(\\d{10,12}|[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,})$",
            message = "Enter a valid NIC number or email address")
    private String identifier;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be at least 8 characters")
    @ToString.Exclude
    private String password;

    private boolean rememberMe;
}
