package lk.sliit.identityissuanceservice.Document_Renewal_And_Replacement_Mgmt.Controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class Passport_Controller {

    @GetMapping("/citizen/applications/passport/renewal")
    public String renewPassport(Model model, HttpSession session) {
        if (session.getAttribute("loggedInUserId") == null) {
            return "redirect:/login";
        }

        model.addAttribute("applicationType", "renewal");
        return "citizen_frontends/passport_application_form";
    }

    @GetMapping("/citizen/applications/passport/replacement")
    public String replacePassport(Model model, HttpSession session) {
        if (session.getAttribute("loggedInUserId") == null) {
            return "redirect:/login";
        }

        model.addAttribute("applicationType", "lost");
        return "citizen_frontends/passport_application_form";
    }
}
