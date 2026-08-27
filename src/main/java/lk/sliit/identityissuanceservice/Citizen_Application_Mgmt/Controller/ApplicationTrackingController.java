package lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class ApplicationTrackingController {

    @GetMapping("/citizen/applications")
    public String applicationTracking(HttpSession session) {
        if (session.getAttribute("loggedInUserId") == null) {
            return "redirect:/login";
        }

        return "citizen_frontends/application_tracking";
    }

    @GetMapping("/citizen/applications/nic/{reference}/correct")
    public String correctNicApplication(
            @PathVariable String reference,
            Model model,
            HttpSession session) {
        if (session.getAttribute("loggedInUserId") == null) {
            return "redirect:/login";
        }

        /*
         * Correction mode reuses the original NIC form. Later the reference
         * will be used to load the correct application and unlock only the
         * fields requested by the officer.
         */
        model.addAttribute("applicationType", "lost");
        model.addAttribute("correctionMode", true);
        model.addAttribute("reference", reference);
        return "citizen_frontends/nic_application_form";
    }

    @GetMapping("/citizen/applications/passport/{reference}/correct")
    public String correctPassportApplication(
            @PathVariable String reference,
            Model model,
            HttpSession session) {
        if (session.getAttribute("loggedInUserId") == null) {
            return "redirect:/login";
        }

        model.addAttribute("applicationType", "renewal");
        model.addAttribute("correctionMode", true);
        model.addAttribute("reference", reference);
        return "citizen_frontends/passport_application_form";
    }
}
