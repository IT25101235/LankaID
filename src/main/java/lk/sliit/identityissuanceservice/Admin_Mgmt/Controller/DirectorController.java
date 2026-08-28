package lk.sliit.identityissuanceservice.Admin_Mgmt.Controller;

import jakarta.servlet.http.HttpSession;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Service.OfficerAccountService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DirectorController {

    @GetMapping("/officer/director")
    public String directorDashboard(HttpSession session, Model model) {
        if (session.getAttribute("loggedInAdminId") != null) {
            return "redirect:/officer/admin";
        }
        if (!OfficerAccountService.DIRECTOR.equals(session.getAttribute("loggedInOfficerRole"))) {
            return "redirect:/login";
        }

        addLoggedInOfficer(model, session);
        return "officers_frontends/director_dashboard";
    }

    @GetMapping("/officer/director/printing")
    public String printingExport(HttpSession session) {
        if (session.getAttribute("loggedInAdminId") != null) {
            return "redirect:/officer/admin";
        }
        if (!OfficerAccountService.DIRECTOR.equals(session.getAttribute("loggedInOfficerRole"))) {
            return "redirect:/login";
        }

        return "officers_frontends/printing_export";
    }

    private void addLoggedInOfficer(Model model, HttpSession session) {
        model.addAttribute("loggedInOfficerRoleLabel", session.getAttribute("loggedInOfficerRoleLabel"));
        model.addAttribute("loggedInOfficerEmployeeNo", session.getAttribute("loggedInOfficerEmployeeNo"));
        model.addAttribute("loggedInOfficerEmail", session.getAttribute("loggedInOfficerEmail"));
    }
}
