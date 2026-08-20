package lk.sliit.identityissuanceservice.User_Mgmt.Controller;

import jakarta.validation.Valid;
import lk.sliit.identityissuanceservice.User_Mgmt.Dto.LoginRequestDto;
import lk.sliit.identityissuanceservice.User_Mgmt.Dto.OfficerLoginRequestDto;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class UserController {

    @GetMapping("/")
    public String index() {
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
            Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("officerLoginRequest", new OfficerLoginRequestDto());
            model.addAttribute("activeTab", "citizen");
            return "login_portal";
        }

        // Temporary flow: authenticate the credentials here after the database is connected.
        return "redirect:/dashboard/citizen";
    }

    @PostMapping("/login/officer")
    public String officerLogin(
            @Valid @ModelAttribute("officerLoginRequest") OfficerLoginRequestDto officerLoginRequest,
            BindingResult bindingResult,
            Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("loginRequest", new LoginRequestDto());
            model.addAttribute("activeTab", "officer");
            return "login_portal";
        }

        String dashboardPath = switch (officerLoginRequest.getRole()) {
            case "nic-officer" -> "/dashboard/officer/nic";
            case "passport-officer" -> "/dashboard/officer/passport";
            case "csr" -> "/dashboard/officer/csr";
            case "director" -> "/dashboard/officer/director";
            case "admin" -> "/dashboard/officer/admin";
            default -> null;
        };

        if (dashboardPath == null) {
            bindingResult.rejectValue("role", "role.invalid", "Please select a valid officer role.");
            model.addAttribute("loginRequest", new LoginRequestDto());
            model.addAttribute("activeTab", "officer");
            return "login_portal";
        }

        // Temporary flow: authenticate the officer credentials here after the database is connected.
        return "redirect:" + dashboardPath;
    }

    @GetMapping("/register")
    public String registrationPage() {
        return "citizen_frontends/citizen_register";
    }

    @GetMapping("/dashboard/citizen")
    public String citizenDashboard() {
        return "citizen_frontends/citizen_dashboard";
    }

    @GetMapping("/dashboard/officer/nic")
    public String nicOfficerDashboard() {
        return "officers_frontends/nic_verification_queue";
    }

    @GetMapping("/dashboard/officer/passport")
    public String passportOfficerDashboard() {
        return "officers_frontends/passport_verification_queue";
    }

    @GetMapping("/dashboard/officer/csr")
    public String csrDashboard() {
        return "officers_frontends/csr_dashboard";
    }

    @GetMapping("/dashboard/officer/director")
    public String directorDashboard() {
        return "officers_frontends/director_dashboard";
    }

    @GetMapping("/dashboard/officer/admin")
    public String adminDashboard() {
        return "officers_frontends/system_admin_dashboard";
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

}
