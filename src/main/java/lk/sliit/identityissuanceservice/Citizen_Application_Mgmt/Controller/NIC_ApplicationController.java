package lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Controller;

import jakarta.servlet.http.HttpSession;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Service.OfficerAccountService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class NIC_ApplicationController {

    @GetMapping("/officer/nic/applications")
    public String nicApplicationQueue(HttpSession session, Model model) {
        if (session.getAttribute("loggedInAdminId") != null) {
            return "redirect:/officer/admin";
        }
        if (!canAccessNicOfficerPages(session)) {
            return "redirect:/login";
        }

        addLoggedInOfficer(model, session);
        return "officers_frontends/nic_verification_queue";
    }

    @GetMapping("/officer/nic/applications/processed")
    public String processedNicApplications(Model model, HttpSession session) {
        if (session.getAttribute("loggedInAdminId") != null) {
            return "redirect:/officer/admin";
        }
        if (!canAccessNicOfficerPages(session)) {
            return "redirect:/login";
        }

        addOfficerPageData(model, "NIC", "/officer/nic/applications", "/officer/nic/biometrics");
        return "officers_frontends/processed_applications";
    }

    @GetMapping("/officer/nic/applications/{reference}")
    public String reviewNicApplication(@PathVariable String reference, Model model, HttpSession session) {
        if (session.getAttribute("loggedInAdminId") != null) {
            return "redirect:/officer/admin";
        }
        if (!canAccessNicOfficerPages(session)) {
            return "redirect:/login";
        }

        addOfficerPageData(model, "NIC", "/officer/nic/applications", "/officer/nic/biometrics");
        model.addAttribute("reference", reference);
        return "officers_frontends/application_review";
    }

    private void addOfficerPageData(Model model, String documentType, String queueRoute, String biometricRoute) {
        // These display values will be replaced with application data after the DB is added.
        model.addAttribute("documentType", documentType);
        model.addAttribute("queueRoute", queueRoute);
        model.addAttribute("biometricRoute", biometricRoute);
    }

    private boolean canAccessNicOfficerPages(HttpSession session) {
        Object role = session.getAttribute("loggedInOfficerRole");
        return OfficerAccountService.NIC_OFFICER.equals(role)
                || OfficerAccountService.DIRECTOR.equals(role);
    }

    private void addLoggedInOfficer(Model model, HttpSession session) {
        model.addAttribute("loggedInOfficerRoleLabel", session.getAttribute("loggedInOfficerRoleLabel"));
        model.addAttribute("loggedInOfficerEmployeeNo", session.getAttribute("loggedInOfficerEmployeeNo"));
        model.addAttribute("loggedInOfficerEmail", session.getAttribute("loggedInOfficerEmail"));
    }
}
