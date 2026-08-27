package lk.sliit.identityissuanceservice.Admin_Mgmt.Controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework
        .web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminController {

    @GetMapping({"/officer/admin", "/officer/admin/officers"})
    public String officerManagement(HttpSession session, Model model) {
        if (session.getAttribute("loggedInAdminId") == null) {
            return "redirect:/login";
        }

        model.addAttribute("loggedInAdminEmail", session.getAttribute("loggedInAdminEmail"));
        return "officers_frontends/admin_dashboard";
    }

    @GetMapping("/officer/admin/citizens")
    public String citizenAccounts(HttpSession session) {
        if (session.getAttribute("loggedInAdminId") == null) {
            return "redirect:/login";
        }

        return "redirect:/officer/admin?section=citizens";
    }

    @GetMapping("/officer/admin/notifications")
    public String notificationComposer(HttpSession session) {
        if (session.getAttribute("loggedInAdminId") == null) {
            return "redirect:/login";
        }

        return "redirect:/officer/admin?section=notifications";
    }

    @PostMapping("/officer/admin/logout")
    public String adminLogout(HttpSession session, RedirectAttributes redirectAttributes) {
        session.invalidate();
        redirectAttributes.addFlashAttribute("officerSuccessMessage", "You have signed out successfully.");
        redirectAttributes.addFlashAttribute("activeTab", "officer");
        return "redirect:/login";
    }
}
