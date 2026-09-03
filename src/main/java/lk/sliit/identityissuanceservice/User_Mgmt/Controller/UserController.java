package lk.sliit.identityissuanceservice.User_Mgmt.Controller;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpSession;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Dto.OfficerDetailsDto;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Service.AdminAccountService;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Service.OfficerAccountService;
import lk.sliit.identityissuanceservice.User_Mgmt.Dto.CitizenRegistrationDto;
import lk.sliit.identityissuanceservice.User_Mgmt.Dto.LoginRequestDto;
import lk.sliit.identityissuanceservice.User_Mgmt.Dto.OfficerLoginRequestDto;
import lk.sliit.identityissuanceservice.User_Mgmt.Dto.EmailVerificationChallenge;
import lk.sliit.identityissuanceservice.User_Mgmt.Dto.PreparedCitizenRegistration;
import lk.sliit.identityissuanceservice.User_Mgmt.Service.EmailVerificationService;
import lk.sliit.identityissuanceservice.User_Mgmt.Service.UserAccountService;
import lk.sliit.identityissuanceservice.User_Mgmt.Model.UserAccount;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Objects;
import java.util.Optional;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Controller
public class UserController {

    private static final String PENDING_REGISTRATION = "pendingCitizenRegistration";
    private static final String EMAIL_CHALLENGE = "registrationEmailChallenge";

    private static final DateTimeFormatter OFFICER_LOGIN_TIME_FORMAT =
            DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    private final UserAccountService userAccountService;
    private final EmailVerificationService emailVerificationService;
    private final AdminAccountService adminAccountService;
    private final OfficerAccountService officerAccountService;

    public UserController(
            UserAccountService userAccountService,
            EmailVerificationService emailVerificationService,
            AdminAccountService adminAccountService,
            OfficerAccountService officerAccountService) {
        this.userAccountService = userAccountService;
        this.emailVerificationService = emailVerificationService;
        this.adminAccountService = adminAccountService;
        this.officerAccountService = officerAccountService;
    }

    @GetMapping("/")
    public String index(HttpSession session, Model model) {
        String dashboardPath = landingDashboardPath(session);
        model.addAttribute("landingSignedIn", dashboardPath != null);
        model.addAttribute("landingDashboardPath", dashboardPath == null ? "" : dashboardPath);
        return "index";
    }

    @GetMapping("/login")
    public String loginPage(Model model) {
        addLoginForms(model);
        return "login_portal";
    }

    @PostMapping("/login")
    public String citizenLogin(
            @Valid @ModelAttribute("loginRequest") LoginRequestDto loginRequest,
            BindingResult bindingResult,
            Model model,
            HttpSession session) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("officerLoginRequest", new OfficerLoginRequestDto());
            model.addAttribute("activeTab", "citizen");
            return "login_portal";
        }

        Optional<UserAccount> authenticatedAccount = userAccountService.authenticateCitizen(
                loginRequest.getEmail(),
                loginRequest.getPassword()
        );

        if (authenticatedAccount.isEmpty()) {
            bindingResult.reject("login.failed", "Invalid email or password.");
            model.addAttribute("officerLoginRequest", new OfficerLoginRequestDto());
            model.addAttribute("activeTab", "citizen");
            return "login_portal";
        }

        UserAccount account = authenticatedAccount.get();
        session.setAttribute("loggedInUserId", account.getUserId());
        session.setAttribute("loggedInUserEmail", account.getEmail());
        session.setAttribute("loggedInUserRole", "CITIZEN");
        session.setAttribute("loggedInDashboardPath", "/citizen/dashboard");

        int sessionTimeout = loginRequest.isRememberMe()
                ? 7 * 24 * 60 * 60
                : 30 * 60;
        session.setMaxInactiveInterval(sessionTimeout);

        return "redirect:/citizen/dashboard";
    }

    @PostMapping("/logout")
    public String logout(HttpSession session, RedirectAttributes redirectAttributes) {
        session.invalidate();
        redirectAttributes.addFlashAttribute("successMessage", "You have signed out successfully.");
        return "redirect:/login";
    }

    @PostMapping("/login/officer")
    public String officerLogin(
            @Valid @ModelAttribute("officerLoginRequest") OfficerLoginRequestDto officerLoginRequest,
            BindingResult bindingResult,
            Model model,
            HttpSession session) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("loginRequest", new LoginRequestDto());
            model.addAttribute("activeTab", "officer");
            return "login_portal";
        }

        if (officerLoginRequest.getRole().equals("admin")) {
            Optional<UserAccount> authenticatedAdmin = adminAccountService.authenticateAdmin(
                    officerLoginRequest.getEmail(),
                    officerLoginRequest.getPassword()
            );

            if (authenticatedAdmin.isEmpty()) {
                bindingResult.reject("admin.login.failed", "Invalid administrator email or password.");
                model.addAttribute("loginRequest", new LoginRequestDto());
                model.addAttribute("activeTab", "officer");
                return "login_portal";
            }

            UserAccount admin = authenticatedAdmin.get();
            session.setAttribute("loggedInAdminId", admin.getUserId());
            session.setAttribute("loggedInAdminEmail", admin.getEmail());
            session.setAttribute("loggedInAdminRole", "SYSTEM_ADMIN");
            session.setAttribute("loggedInOfficerEmail", admin.getEmail());
            session.setAttribute("loggedInOfficerRole", "SYSTEM_ADMIN");
            session.setAttribute("loggedInOfficerRoleLabel", "System Administrator");
            session.setAttribute("loggedInOfficerEmployeeNo", "ADMIN");
            session.setAttribute("loggedInOfficerLoginTime", currentOfficerLoginTime());
            session.setAttribute("loggedInDashboardPath", "/officer/admin");

            int sessionTimeout = officerLoginRequest.isRememberMe()
                    ? 7 * 24 * 60 * 60
                    : 30 * 60;
            session.setMaxInactiveInterval(sessionTimeout);
            return "redirect:/officer/admin";
        }

        if (session.getAttribute("loggedInAdminId") != null
                || adminAccountService.isSystemAdminEmail(officerLoginRequest.getEmail())) {
            bindingResult.reject(
                    "admin.role.not.allowed",
                    "The system administrator account can only use the System Administrator role."
            );
            model.addAttribute("loginRequest", new LoginRequestDto());
            model.addAttribute("activeTab", "officer");
            return "login_portal";
        }

        String dashboardPath = officerAccountService.dashboardForRoleSelection(officerLoginRequest.getRole());

        if (dashboardPath == null) {
            bindingResult.rejectValue("role", "role.invalid", "Please select a valid officer role.");
            model.addAttribute("loginRequest", new LoginRequestDto());
            model.addAttribute("activeTab", "officer");
            return "login_portal";
        }

        Optional<OfficerDetailsDto> authenticatedOfficer = officerAccountService.authenticateOfficer(
                officerLoginRequest.getEmail(),
                officerLoginRequest.getPassword(),
                officerLoginRequest.getRole()
        );

        if (authenticatedOfficer.isEmpty()) {
            bindingResult.reject("officer.login.failed", "Invalid officer email, password or selected role.");
            model.addAttribute("loginRequest", new LoginRequestDto());
            model.addAttribute("activeTab", "officer");
            return "login_portal";
        }

        OfficerDetailsDto officer = authenticatedOfficer.get();
        session.setAttribute("loggedInOfficerId", officer.getUserId());
        session.setAttribute("loggedInOfficerEmail", officer.getEmail());
        session.setAttribute("loggedInOfficerRole", officer.getRoleCode());
        session.setAttribute("loggedInOfficerRoleLabel", officer.getRoleLabel());
        session.setAttribute("loggedInOfficerEmployeeNo", officer.getEmployeeNo());
        session.setAttribute("loggedInOfficerLoginTime", currentOfficerLoginTime());
        session.setAttribute("loggedInDashboardPath", dashboardPath);

        int sessionTimeout = officerLoginRequest.isRememberMe()
                ? 7 * 24 * 60 * 60
                : 30 * 60;
        session.setMaxInactiveInterval(sessionTimeout);

        return "redirect:" + dashboardPath;
    }

    @GetMapping("/register")
    public String registrationPage(Model model) {
        if (!model.containsAttribute("registrationRequest")) {
            model.addAttribute("registrationRequest", new CitizenRegistrationDto());
        }
        return "citizen_frontends/citizen_register";
    }

    @PostMapping("/register")
    public String registerCitizen(
            @Valid @ModelAttribute("registrationRequest") CitizenRegistrationDto registrationRequest,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            HttpSession session) {
        if (!Objects.equals(registrationRequest.getPassword(), registrationRequest.getConfirmPassword())) {
            bindingResult.rejectValue(
                    "confirmPassword",
                    "password.mismatch",
                    "Passwords do not match."
            );
        }

        if (bindingResult.hasErrors()) {
            return "citizen_frontends/citizen_register";
        }

        try {
            PreparedCitizenRegistration pendingRegistration = userAccountService.prepareCitizenRegistration(
                    registrationRequest.getEmail(),
                    registrationRequest.getPhone(),
                    registrationRequest.getPassword()
            );
            EmailVerificationChallenge challenge = emailVerificationService.sendVerificationCode(
                    pendingRegistration.email()
            );
            session.setAttribute(PENDING_REGISTRATION, pendingRegistration);
            session.setAttribute(EMAIL_CHALLENGE, challenge);
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("registration.failed", exception.getMessage());
            return "citizen_frontends/citizen_register";
        } catch (RuntimeException exception) {
            bindingResult.reject(
                    "registration.email.failed",
                    "We could not send the verification email. Check the mail configuration and try again."
            );
            return "citizen_frontends/citizen_register";
        }

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "We sent a six-digit verification code to your email address."
        );
        return "redirect:/register/verify";
    }

    @GetMapping("/register/verify")
    public String emailVerificationPage(HttpSession session, Model model) {
        PreparedCitizenRegistration registration = pendingRegistration(session);
        if (registration == null) {
            return "redirect:/register";
        }
        addEmailVerificationModel(model, registration, emailChallenge(session));
        return "citizen_frontends/citizen_email_verification";
    }

    @PostMapping("/register/verify")
    public String verifyRegistrationEmail(
            @RequestParam("code") String code,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {
        PreparedCitizenRegistration registration = pendingRegistration(session);
        EmailVerificationChallenge challenge = emailChallenge(session);
        if (registration == null || challenge == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Start registration again to request a new code.");
            return "redirect:/register";
        }

        if (challenge.isExpired(Instant.now())) {
            addEmailVerificationModel(model, registration, challenge);
            model.addAttribute("errorMessage", "That code has expired. Request a new code.");
            return "citizen_frontends/citizen_email_verification";
        }

        if (!emailVerificationService.isValid(challenge, code == null ? "" : code.trim())) {
            challenge.recordFailedAttempt();
            addEmailVerificationModel(model, registration, challenge);
            int remaining = emailVerificationService.attemptsRemaining(challenge);
            model.addAttribute("errorMessage", remaining == 0
                    ? "Too many incorrect attempts. Request a new code."
                    : "Incorrect code. " + remaining + " attempt(s) remaining.");
            return "citizen_frontends/citizen_email_verification";
        }

        try {
            userAccountService.registerVerifiedCitizen(registration);
        } catch (IllegalArgumentException exception) {
            clearPendingRegistration(session);
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/register";
        }

        clearPendingRegistration(session);
        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Email verified and registration completed. You can now sign in."
        );
        return "redirect:/login";
    }

    @PostMapping("/register/verify/resend")
    public String resendRegistrationEmail(
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        PreparedCitizenRegistration registration = pendingRegistration(session);
        EmailVerificationChallenge challenge = emailChallenge(session);
        if (registration == null || challenge == null) {
            return "redirect:/register";
        }

        Instant now = Instant.now();
        if (!challenge.canResend(now)) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Please wait " + challenge.resendWaitSeconds(now) + " second(s) before requesting another code."
            );
            return "redirect:/register/verify";
        }

        try {
            session.setAttribute(
                    EMAIL_CHALLENGE,
                    emailVerificationService.sendVerificationCode(registration.email())
            );
            redirectAttributes.addFlashAttribute("successMessage", "A new verification code was sent.");
        } catch (RuntimeException exception) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "We could not resend the verification email. Check the mail configuration and try again."
            );
        }
        return "redirect:/register/verify";
    }

    @GetMapping("/dashboard/citizen")
    public String oldCitizenDashboardRoute() {
        // Keeps old bookmarks working while the project moves to clean routes.
        return "redirect:/citizen/dashboard";
    }

    private void addLoginForms(Model model) {
        if (!model.containsAttribute("loginRequest")) {
            model.addAttribute("loginRequest", new LoginRequestDto());
        }
        if (!model.containsAttribute("officerLoginRequest")) {
            model.addAttribute("officerLoginRequest", new OfficerLoginRequestDto());
        }
        if (!model.containsAttribute("activeTab")) {
            model.addAttribute("activeTab", "citizen");
        }
    }

    private PreparedCitizenRegistration pendingRegistration(HttpSession session) {
        Object value = session.getAttribute(PENDING_REGISTRATION);
        return value instanceof PreparedCitizenRegistration registration ? registration : null;
    }

    private EmailVerificationChallenge emailChallenge(HttpSession session) {
        Object value = session.getAttribute(EMAIL_CHALLENGE);
        return value instanceof EmailVerificationChallenge challenge ? challenge : null;
    }

    private void addEmailVerificationModel(
            Model model,
            PreparedCitizenRegistration registration,
            EmailVerificationChallenge challenge) {
        model.addAttribute("maskedEmail", maskEmail(registration.email()));
        model.addAttribute("canResend", challenge == null || challenge.canResend(Instant.now()));
    }

    private String maskEmail(String email) {
        int separator = email.indexOf('@');
        if (separator <= 1) {
            return "***" + email.substring(Math.max(separator, 0));
        }
        return email.charAt(0) + "***" + email.substring(separator);
    }

    private void clearPendingRegistration(HttpSession session) {
        session.removeAttribute(PENDING_REGISTRATION);
        session.removeAttribute(EMAIL_CHALLENGE);
    }

    private String currentOfficerLoginTime() {
        return LocalDateTime.now().format(OFFICER_LOGIN_TIME_FORMAT);
    }

    private String landingDashboardPath(HttpSession session) {
        if (session.getAttribute("loggedInUserId") != null) {
            return "/citizen/dashboard";
        }

        Object savedDashboard = session.getAttribute("loggedInDashboardPath");
        if (savedDashboard instanceof String path && !path.isBlank()) {
            return path;
        }
        if (session.getAttribute("loggedInAdminId") != null) {
            return "/officer/admin";
        }

        Object officerRole = session.getAttribute("loggedInOfficerRole");
        if (officerRole == null) return null;
        return switch (officerRole.toString()) {
            case "SYSTEM_ADMIN" -> "/officer/admin";
            case OfficerAccountService.DIRECTOR -> "/officer/director";
            case OfficerAccountService.CSR -> "/officer/csr";
            case OfficerAccountService.PASSPORT_OFFICER -> "/officer/passport/applications";
            case OfficerAccountService.NIC_OFFICER -> "/officer/nic/applications";
            default -> null;
        };
    }
}
