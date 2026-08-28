package lk.sliit.identityissuanceservice.Citizen_Support_And_Service_Mgmt.Controller;

import jakarta.servlet.http.HttpSession;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Service.OfficerAccountService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class CitizenSupportController {

    @GetMapping("/citizen/support/new")
    public String newSupportRequest(HttpSession session) {
        if (session.getAttribute("loggedInUserId") == null) {
            return "redirect:/login";
        }

        return "citizen_frontends/citizen_complaints";
    }

    @GetMapping("/citizen/support")
    public String citizenSupportRequests(HttpSession session) {
        if (session.getAttribute("loggedInUserId") == null) {
            return "redirect:/login";
        }

        return "citizen_frontends/citizen_support_requests";
    }

    @GetMapping("/citizen/support/{reference}")
    public String citizenSupportDetail(
            @PathVariable String reference,
            Model model,
            HttpSession session) {
        if (session.getAttribute("loggedInUserId") == null) {
            return "redirect:/login";
        }

        model.addAttribute("reference", reference);
        return "citizen_frontends/citizen_support_detail";
    }

    @GetMapping("/officer/csr")
    public String csrDashboard(HttpSession session, Model model) {
        if (session.getAttribute("loggedInAdminId") != null) {
            return "redirect:/officer/admin";
        }
        if (!canAccessCsrPages(session)) {
            return "redirect:/login";
        }

        addLoggedInOfficer(model, session);
        return "officers_frontends/csr_dashboard";
    }

    @GetMapping("/officer/csr/complaints/{reference}")
    public String complaintDetail(@PathVariable String reference, Model model, HttpSession session) {
        if (session.getAttribute("loggedInAdminId") != null) {
            return "redirect:/officer/admin";
        }
        if (!canAccessCsrPages(session)) {
            return "redirect:/login";
        }

        model.addAttribute("reference", reference);
        return "officers_frontends/csr_complaints_detail";
    }

    private boolean canAccessCsrPages(HttpSession session) {
        Object role = session.getAttribute("loggedInOfficerRole");
        return OfficerAccountService.CSR.equals(role)
                || OfficerAccountService.DIRECTOR.equals(role);
    }

    private void addLoggedInOfficer(Model model, HttpSession session) {
        model.addAttribute("loggedInOfficerRoleLabel", session.getAttribute("loggedInOfficerRoleLabel"));
        model.addAttribute("loggedInOfficerEmployeeNo", session.getAttribute("loggedInOfficerEmployeeNo"));
        model.addAttribute("loggedInOfficerEmail", session.getAttribute("loggedInOfficerEmail"));
    }
}
