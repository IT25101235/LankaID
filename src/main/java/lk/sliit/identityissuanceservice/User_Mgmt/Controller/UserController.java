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
        model.addAttribute("loginRequest", new LoginRequestDto());
        model.addAttribute("officerLoginRequest", new OfficerLoginRequestDto());
        model.addAttribute("activeTab", "citizen");
        return "login_portal";
    }

    @PostMapping("/login")
    public String submitLogin(@Valid @ModelAttribute("loginRequest") LoginRequestDto loginRequest,
                              BindingResult bindingResult,
                              Model model) {

        model.addAttribute("officerLoginRequest", new OfficerLoginRequestDto());
        model.addAttribute("activeTab", "citizen");
        if (bindingResult.hasErrors()) {
            return "login_portal";
        }

        /*No Authentication Just for Testing*/

        return "citizen_frontends/citizen_dashboard";
    }

    @PostMapping("/login/officer")
    public String submitOfficerLogin(@Valid @ModelAttribute("officerLoginRequest") OfficerLoginRequestDto officerLoginRequest,
                                     BindingResult bindingResult,
                                     Model model) {

        model.addAttribute("loginRequest", new LoginRequestDto());
        model.addAttribute("activeTab", "officer");
        if (bindingResult.hasErrors()) {
            return "login_portal";
        }

        if (officerLoginRequest.getRole().equals("nic-officer")) {
            return "officers_frontends/nic_verification_queue";
        } else if (officerLoginRequest.getRole().equals("passport-officer")) {
            return "officers_frontends/passport_verification_queue";
        } else if (officerLoginRequest.getRole().equals("csr")) {
            return "officers_frontends/csr_dashboard";
        } else if (officerLoginRequest.getRole().equals("director")) {
            return "officers_frontends/director_dashboard";
        } else if (officerLoginRequest.getRole().equals("admin")) {
            return "officers_frontends/admin_dashboard";
        }
        return "login_portal";
    }
}
