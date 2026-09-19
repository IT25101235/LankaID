package lk.sliit.identityissuanceservice.User_Mgmt.Controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lk.sliit.identityissuanceservice.OTP_Mgmt.Email.EmailOtpService;
import lk.sliit.identityissuanceservice.OTP_Mgmt.Otp.SmsOtpService;
import lk.sliit.identityissuanceservice.User_Mgmt.Dto.CitizenRegistrationDto;
import lk.sliit.identityissuanceservice.User_Mgmt.Dto.PreparedCitizenRegistration;
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
public class CitizenRegistrationController {
    static final String EMAIL_PURPOSE = "citizen-registration-email";
    static final String PHONE_PURPOSE = "citizen-registration-phone";
    private static final String REGISTRATION_EMAIL = "registrationEmail";
    private static final String REGISTRATION_PHONE = "registrationPhone";

    private final UserAccountService accounts;
    private final EmailOtpService emailOtp;
    private final SmsOtpService smsOtp;

    public CitizenRegistrationController(UserAccountService accounts, EmailOtpService emailOtp, SmsOtpService smsOtp) {
        this.accounts = accounts;
        this.emailOtp = emailOtp;
        this.smsOtp = smsOtp;
    }

    @GetMapping("/register")
    public String page(Model model, HttpSession session) {
        if (!model.containsAttribute("registrationRequest")) {
            CitizenRegistrationDto request = new CitizenRegistrationDto();
            request.setEmail(value(session, REGISTRATION_EMAIL));
            request.setPhone(value(session, REGISTRATION_PHONE));
            model.addAttribute("registrationRequest", request);
        }
        addState(model, session);
        return "citizen_frontends/citizen_register";
    }

    @PostMapping("/register/email/send")
    public String sendEmail(@RequestParam String email, HttpSession session, RedirectAttributes redirect) {
        try {
            String normalized = accounts.validateRegistrationEmail(email);
            if (!normalized.equals(value(session, REGISTRATION_EMAIL))) {
                emailOtp.clear(EMAIL_PURPOSE, session);
                smsOtp.clear(PHONE_PURPOSE, session);
                session.removeAttribute(REGISTRATION_PHONE);
            }
            session.setAttribute(REGISTRATION_EMAIL, normalized);
            emailOtp.send(normalized, EMAIL_PURPOSE, session);
            redirect.addFlashAttribute("successMessage", "A six-digit code was sent to your email address.");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("errorMessage", exception.getMessage());
        } catch (RuntimeException exception) {
            redirect.addFlashAttribute("errorMessage", "We could not send the email code. Please try again.");
        }
        return "redirect:/register";
    }

    @PostMapping("/register/email/verify")
    public String verifyEmail(@RequestParam String code, HttpSession session, RedirectAttributes redirect) {
        try {
            emailOtp.verify(code == null ? "" : code.trim(), EMAIL_PURPOSE, session);
            redirect.addFlashAttribute("successMessage", "Email address verified. Now verify your mobile number.");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/register";
    }

    @PostMapping("/register/email/resend")
    public String resendEmail(HttpSession session, RedirectAttributes redirect) {
        return sendEmail(value(session, REGISTRATION_EMAIL), session, redirect);
    }

    @PostMapping("/register/phone/send")
    public String sendPhone(@RequestParam String phone, HttpSession session, RedirectAttributes redirect) {
        String email = value(session, REGISTRATION_EMAIL);
        if (!emailOtp.isVerified(email, EMAIL_PURPOSE, session)) {
            redirect.addFlashAttribute("errorMessage", "Verify your email address first.");
            return "redirect:/register";
        }
        try {
            String normalized = accounts.validateRegistrationPhone(phone);
            if (!normalized.equals(value(session, REGISTRATION_PHONE))) smsOtp.clear(PHONE_PURPOSE, session);
            session.setAttribute(REGISTRATION_PHONE, normalized);
            smsOtp.send(normalized, PHONE_PURPOSE, session);
            redirect.addFlashAttribute("successMessage", "A six-digit SMS code was sent to your mobile number.");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/register";
    }

    @PostMapping("/register/phone/verify")
    public String verifyPhone(@RequestParam String code, HttpSession session, RedirectAttributes redirect) {
        try {
            smsOtp.verify(code == null ? "" : code.trim(), PHONE_PURPOSE, session);
            redirect.addFlashAttribute("successMessage", "Mobile number verified. Create your password to finish.");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/register";
    }

    @PostMapping("/register/phone/resend")
    public String resendPhone(HttpSession session, RedirectAttributes redirect) {
        return sendPhone(value(session, REGISTRATION_PHONE), session, redirect);
    }

    @PostMapping("/register")
    public String complete(@Valid @ModelAttribute("registrationRequest") CitizenRegistrationDto request,
                           BindingResult binding, Model model, HttpSession session,
                           RedirectAttributes redirect) {
        String email = value(session, REGISTRATION_EMAIL);
        String phone = value(session, REGISTRATION_PHONE);
        if (!Objects.equals(request.getPassword(), request.getConfirmPassword())) {
            binding.rejectValue("confirmPassword", "password.mismatch", "Passwords do not match.");
        }
        // The verified session values are canonical. Form values are display-only and
        // may use the local phone format (77...) instead of the stored +947... form.
        if (!emailOtp.isVerified(email, EMAIL_PURPOSE, session)) {
            binding.reject("email.unverified", "Verify your email address before creating the account.");
        }
        if (!smsOtp.isVerified(phone, PHONE_PURPOSE, session)) {
            binding.reject("phone.unverified", "Verify your mobile number before creating the account.");
        }
        if (binding.hasErrors()) {
            addState(model, session);
            return "citizen_frontends/citizen_register";
        }
        try {
            PreparedCitizenRegistration prepared = accounts.prepareCitizenRegistration(email, phone, request.getPassword());
            accounts.registerVerifiedCitizen(prepared);
            clear(session);
            redirect.addFlashAttribute("successMessage", "Registration completed. You can now sign in.");
            return "redirect:/login";
        } catch (IllegalArgumentException exception) {
            binding.reject("registration.failed", exception.getMessage());
            addState(model, session);
            return "citizen_frontends/citizen_register";
        }
    }

    @GetMapping("/register/verify")
    public String oldVerificationPage() { return "redirect:/register"; }

    private void addState(Model model, HttpSession session) {
        String email = value(session, REGISTRATION_EMAIL);
        String phone = value(session, REGISTRATION_PHONE);
        boolean emailVerified = !email.isBlank() && emailOtp.isVerified(email, EMAIL_PURPOSE, session);
        boolean phoneVerified = !phone.isBlank() && smsOtp.isVerified(phone, PHONE_PURPOSE, session);
        model.addAttribute("registrationEmail", email);
        model.addAttribute("registrationPhone", localPhone(phone));
        model.addAttribute("emailCodeSent", !email.isBlank());
        model.addAttribute("phoneCodeSent", !phone.isBlank());
        model.addAttribute("emailVerified", emailVerified);
        model.addAttribute("phoneVerified", phoneVerified);
        model.addAttribute("identityVerified", emailVerified && phoneVerified);
        model.addAttribute("emailRetryAfterSeconds", emailOtp.retryAfterSeconds(EMAIL_PURPOSE, session));
        model.addAttribute("phoneRetryAfterSeconds", phone.isBlank() ? 0 : smsOtp.retryAfterSeconds(phone, PHONE_PURPOSE, session));
    }

    private void clear(HttpSession session) {
        emailOtp.clear(EMAIL_PURPOSE, session);
        smsOtp.clear(PHONE_PURPOSE, session);
        session.removeAttribute(REGISTRATION_EMAIL);
        session.removeAttribute(REGISTRATION_PHONE);
    }

    private String value(HttpSession session, String key) {
        Object value = session.getAttribute(key);
        return value instanceof String text ? text : "";
    }

    private String localPhone(String phone) { return phone.startsWith("+94") ? phone.substring(3) : phone; }
}
