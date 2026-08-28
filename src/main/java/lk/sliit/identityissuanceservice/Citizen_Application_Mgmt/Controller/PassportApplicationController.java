package lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Controller;

import jakarta.servlet.http.HttpSession;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Service.OfficerAccountService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class PassportApplicationController {

    @GetMapping("/officer/passport/applications")
    public String passportApplicationQueue(HttpSession session, Model model) {
        if (session.getAttribute("loggedInAdminId") != null) {
            return "redirect:/officer/admin";
        }
        if (!canAccessPassportOfficerPages(session)) {
            return "redirect:/login";
        }

        addLoggedInOfficer(model, session);
        return "officers_frontends/passport_verification_queue";
    }

    @GetMapping("/officer/passport/applications/processed")
    public String processedPassportApplications(Model model, HttpSession session) {
        if (session.getAttribute("loggedInAdminId") != null) {
            return "redirect:/officer/admin";
        }
        if (!canAccessPassportOfficerPages(session)) {
            return "redirect:/login";
        }

        addOfficerPageData(model, "Passport", "/officer/passport/applications", "/officer/passport/biometrics");
        return "officers_frontends/processed_applications";
    }

    @GetMapping("/officer/passport/applications/{reference}")
    public String reviewPassportApplication(@PathVariable String reference, Model model, HttpSession session) {
        if (session.getAttribute("loggedInAdminId") != null) {
            return "redirect:/officer/admin";
        }
        if (!canAccessPassportOfficerPages(session)) {
            return "redirect:/login";
        }

        addOfficerPageData(model, "Passport", "/officer/passport/applications", "/officer/passport/biometrics");
        model.addAttribute("reference", reference);
        return "officers_frontends/application_review";
    }

    private void addOfficerPageData(Model model, String documentType, String queueRoute, String biometricRoute) {
        // The NIC and Passport review layouts are the same, so one template is reused.
        model.addAttribute("documentType", documentType);
        model.addAttribute("queueRoute", queueRoute);
        model.addAttribute("biometricRoute", biometricRoute);
    }

    private boolean canAccessPassportOfficerPages(HttpSession session) {
        Object role = session.getAttribute("loggedInOfficerRole");
        return OfficerAccountService.PASSPORT_OFFICER.equals(role)
                || OfficerAccountService.DIRECTOR.equals(role);
    }

    private void addLoggedInOfficer(Model model, HttpSession session) {
        model.addAttribute("loggedInOfficerRoleLabel", session.getAttribute("loggedInOfficerRoleLabel"));
        model.addAttribute("loggedInOfficerEmployeeNo", session.getAttribute("loggedInOfficerEmployeeNo"));
        model.addAttribute("loggedInOfficerEmail", session.getAttribute("loggedInOfficerEmail"));
    }
}
