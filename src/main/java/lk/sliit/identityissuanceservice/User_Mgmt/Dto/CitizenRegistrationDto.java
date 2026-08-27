package lk.sliit.identityissuanceservice.User_Mgmt.Dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CitizenRegistrationDto {

    @NotBlank(message = "Email is required.")
    @Email(message = "Enter a valid email address.")
    @Size(max = 100, message = "Email cannot be longer than 100 characters.")
    private String email;

    @NotBlank(message = "Phone number is required.")
    @Pattern(
            regexp = "^(?:\\+94|0)7\\d{8}$",
            message = "Use a Sri Lankan mobile number such as 0771234567 or +94771234567."
    )
    private String phone;

    @NotBlank(message = "Password is required.")
    @Size(min = 8, max = 72, message = "Password must contain 8 to 72 characters.")
    private String password;

    @NotBlank(message = "Please repeat your password.")
    private String confirmPassword;

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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}
