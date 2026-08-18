package lk.sliit.identityissuanceservice.User_Mgmt.Controller;

import jakarta.validation.Valid;
import lk.sliit.identityissuanceservice.User_Mgmt.DTO.LoginRequestDto;
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
        return "login_portal";
    }

    @PostMapping("/login")
    public String submitLogin(@Valid @ModelAttribute("loginRequest") LoginRequestDto loginRequest,
                              BindingResult bindingResult,
                              Model model) {

        if (bindingResult.hasErrors()) {
            return "login_portal";
        }


        model.addAttribute("successMessage",
                "Login request received for " + loginRequest.getIdentifier() + ".");
        model.addAttribute("loginRequest", loginRequest);
        return "login_portal";
    }
}
