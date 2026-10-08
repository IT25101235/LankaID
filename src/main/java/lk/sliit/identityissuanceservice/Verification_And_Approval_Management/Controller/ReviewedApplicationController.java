package lk.sliit.identityissuanceservice.Verification_And_Approval_Management.Controller;

import org.springframework.data.domain.Page;
import java.util.List;

import jakarta.servlet.http.HttpSession;
import lk.sliit.identityissuanceservice.Verification_And_Approval_Management.Service.ReviewedApplicationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.Locale;

/**
 * Displays the paginated audit history of completed verification decisions.
 * Normal officers are restricted to their service, while directors can inspect both queues.
 */
@Controller
public class ReviewedApplicationController {
    private final ReviewedApplicationService reviews;
    public ReviewedApplicationController(ReviewedApplicationService reviews) { this.reviews = reviews; }

    @GetMapping("/officer/{service:nic|passport}/applications/processed")
    public String reviewed(@PathVariable String service, @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "") String decision, @RequestParam(defaultValue = "") String date,
            @RequestParam(defaultValue = "0") int page, HttpSession session, Model model) {
        // Enforce the service boundary before executing the history query.
        if (session.getAttribute("loggedInAdminId") != null) return "redirect:/officer/admin";
        String serviceType = service.toUpperCase(Locale.ROOT);
        Object role = session.getAttribute("loggedInOfficerRole");
        if (!(serviceType + "_OFFICER").equals(role) && !"DIRECTOR".equals(role)) return "redirect:/login";
        model.addAttribute("documentType", serviceType);
        model.addAttribute("queueRoute", "/officer/" + service + "/applications");
        model.addAttribute("reviewedRoute", "/officer/" + service + "/applications/processed");
        model.addAttribute("biometricRoute", "/officer/" + service + "/biometrics");
        model.addAttribute("search", search);
        model.addAttribute("decision", decision);
        model.addAttribute("date", date);
        for (String key : List.of("loggedInOfficerEmail", "loggedInOfficerRoleLabel", "loggedInOfficerLoginTime")) {
            model.addAttribute(key, session.getAttribute(key));
        }
        try {
            // Invalid filters are reported on the page instead of producing an application error.
            model.addAttribute("reviews", reviews.find(serviceType, search, decision, date, page));
        } catch (IllegalArgumentException exception) {
            model.addAttribute("filterError", exception.getMessage());
            model.addAttribute("reviews", Page.empty());
        }
        return "officers_frontends/processed_applications";
    }
}
