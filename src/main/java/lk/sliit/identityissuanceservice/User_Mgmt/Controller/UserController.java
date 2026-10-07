package lk.sliit.identityissuanceservice.User_Mgmt.Controller;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpSession;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Dto.OfficerDetailsDto;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Service.AdminAccountService;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Service.OfficerAccountService;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Service.SystemMaintenanceService;
import lk.sliit.identityissuanceservice.User_Mgmt.Dto.CitizenRegistrationDto;
import lk.sliit.identityissuanceservice.User_Mgmt.Dto.LoginRequestDto;
import lk.sliit.identityissuanceservice.User_Mgmt.Dto.OfficerLoginRequestDto;
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

@Controller
public class UserController {

    private final UserAccountService userAccountService;
    private final AdminAccountService adminAccountService;
    private final OfficerAccountService officerAccountService;
    private final SystemMaintenanceService maintenanceService;

    public UserController(
            UserAccountService userAccountService,
            AdminAccountService adminAccountService,
            OfficerAccountService officerAccountService,
            SystemMaintenanceService maintenanceService) {
        this.userAccountService = userAccountService;
        this.adminAccountService = adminAccountService;
        this.officerAccountService = officerAccountService;
        this.maintenanceService = maintenanceService;
    }

    @GetMapping("/")
    public String index(Model model) {
        addMaintenanceStatus(model);
        return "index";
    }

    @GetMapping("/login")
    public String loginPage(Model model) {
        addLoginForms(model);
        addMaintenanceStatus(model);
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

        if (maintenanceService.isMaintenanceActive()) {
            bindingResult.reject(
                    "maintenance.active",
                    "The system is under maintenance. Only the system administrator can sign in."
            );
            model.addAttribute("officerLoginRequest", new OfficerLoginRequestDto());
            model.addAttribute("activeTab", "citizen");
            addMaintenanceStatus(model);
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

        if (maintenanceService.isMaintenanceActive()
                && !"admin".equals(officerLoginRequest.getRole())) {
            bindingResult.reject(
                    "maintenance.active",
                    "The system is under maintenance. Only the system administrator can sign in."
            );
            model.addAttribute("loginRequest", new LoginRequestDto());
            model.addAttribute("activeTab", "officer");
            addMaintenanceStatus(model);
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
            RedirectAttributes redirectAttributes) {
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
            userAccountService.registerCitizen(
                    registrationRequest.getEmail(),
                    registrationRequest.getPhone(),
                    registrationRequest.getPassword()
            );
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("registration.failed", exception.getMessage());
            return "citizen_frontends/citizen_register";
        }

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Registration successful. You can now sign in."
        );
        return "redirect:/login";
    }

    @GetMapping("/citizen/dashboard")
    public String citizenDashboard(
            @RequestParam(defaultValue = "new") String profile,
            Model model,
            HttpSession session) {
        if (session.getAttribute("loggedInUserId") == null) {
            return "redirect:/login";
        }

        model.addAttribute("loggedInUserEmail", session.getAttribute("loggedInUserEmail"));

        /*
         * Temporary UI data only. Later these two values will come from the
         * citizen's database record. The query parameter lets us demonstrate
         * every dashboard state before the database is connected.
         */
        boolean hasNic = profile.equals("existing") || profile.equals("nic-only");
        boolean hasPassport = profile.equals("existing");
        model.addAttribute("hasNic", hasNic);
        model.addAttribute("hasPassport", hasPassport);
        model.addAttribute("demoProfile", profile);
        return "citizen_frontends/citizen_dashboard";
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

    private void addMaintenanceStatus(Model model) {
        var settings = maintenanceService.getSettings();
        model.addAttribute("maintenanceActive", settings.isMaintenanceActive());
        model.addAttribute("maintenanceWarning", settings.getMaintenanceMessage());
    }

}
