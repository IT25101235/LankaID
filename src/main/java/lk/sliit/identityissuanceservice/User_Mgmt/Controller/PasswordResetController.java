package lk.sliit.identityissuanceservice.User_Mgmt.Controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lk.sliit.identityissuanceservice.OTP_Mgmt.Email.EmailOtpService;
import lk.sliit.identityissuanceservice.User_Mgmt.Dto.PasswordResetDto;
import lk.sliit.identityissuanceservice.User_Mgmt.Service.UserAccountService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Objects;

@Controller
public class PasswordResetController {
    static final String PURPOSE = "citizen-password-reset";
    private static final String RESET_EMAIL = "passwordResetEmail";
    private final UserAccountService accounts;
    private final EmailOtpService emailOtp;

    public PasswordResetController(UserAccountService accounts, EmailOtpService emailOtp) {
        this.accounts = accounts;
        this.emailOtp = emailOtp;
    }

    @GetMapping("/forgot-password")
    public String page(Model model, HttpSession session) {
        if (!model.containsAttribute("passwordReset")) model.addAttribute("passwordReset", new PasswordResetDto());
        addState(model, session);
        return "citizen_frontends/forgot_password";
    }

    @PostMapping("/forgot-password/email/send")
    public String send(@RequestParam String email, HttpSession session, RedirectAttributes redirect) {
        try {
            String normalized = accounts.requirePasswordResetCitizen(email);
            if (!normalized.equals(value(session))) emailOtp.clear(PURPOSE, session);
            session.setAttribute(RESET_EMAIL, normalized);
            emailOtp.send(normalized, PURPOSE, session);
            redirect.addFlashAttribute("successMessage", "A six-digit password reset code was sent to your email.");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("errorMessage", exception.getMessage());
        } catch (RuntimeException exception) {
            redirect.addFlashAttribute("errorMessage", "We could not send the reset email. Please try again.");
        }
        return "redirect:/forgot-password";
    }

    @PostMapping("/forgot-password/email/verify")
    public String verify(@RequestParam String code, HttpSession session, RedirectAttributes redirect) {
        try {
            emailOtp.verify(code == null ? "" : code.trim(), PURPOSE, session);
            redirect.addFlashAttribute("successMessage", "Email verified. You can now choose a new password.");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/forgot-password";
    }

    @PostMapping("/forgot-password/email/resend")
    public String resend(HttpSession session, RedirectAttributes redirect) { return send(value(session), session, redirect); }

    @PostMapping("/forgot-password/reset")
    public String reset(@Valid @ModelAttribute("passwordReset") PasswordResetDto request,
                        BindingResult binding, Model model, HttpSession session, RedirectAttributes redirect) {
        String email = value(session);
        if (!Objects.equals(request.getPassword(), request.getConfirmPassword())) {
            binding.rejectValue("confirmPassword", "password.mismatch", "Passwords do not match.");
        }
        if (!emailOtp.isVerified(email, PURPOSE, session)) {
            binding.reject("email.unverified", "Verify your email before resetting the password.");
        }
        if (binding.hasErrors()) {
            addState(model, session);
            return "citizen_frontends/forgot_password";
        }
        try {
            accounts.resetCitizenPassword(email, request.getPassword());
            emailOtp.clear(PURPOSE, session);
            session.removeAttribute(RESET_EMAIL);
            redirect.addFlashAttribute("successMessage", "Password reset successfully. Sign in with your new password.");
            return "redirect:/login";
        } catch (IllegalArgumentException exception) {
            binding.reject("reset.failed", exception.getMessage());
            addState(model, session);
            return "citizen_frontends/forgot_password";
        }
    }

    private void addState(Model model, HttpSession session) {
        String email = value(session);
        model.addAttribute("resetEmail", email);
        model.addAttribute("codeSent", !email.isBlank());
        model.addAttribute("emailVerified", !email.isBlank() && emailOtp.isVerified(email, PURPOSE, session));
        model.addAttribute("emailRetryAfterSeconds", emailOtp.retryAfterSeconds(PURPOSE, session));
    }

    private String value(HttpSession session) {
        Object value = session.getAttribute(RESET_EMAIL);
        return value instanceof String text ? text : "";
    }
}
