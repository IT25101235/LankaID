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
import org.springframework.web.bind.annotation.RequestParam;

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
        return "redirect:/citizen/dashboard";
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
            case "nic-officer" -> "/officer/nic/applications";
            case "passport-officer" -> "/officer/passport/applications";
            case "csr" -> "/officer/csr";
            case "director" -> "/officer/director";
            case "admin" -> "/officer/admin";
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

    @GetMapping("/citizen/dashboard")
    public String citizenDashboard(
            @RequestParam(defaultValue = "new") String profile,
            Model model) {
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

}
