package lk.sliit.identityissuanceservice.Admin_Mgmt.Controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Dto.OfficerDetailsDto;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Dto.OfficerRegistrationDto;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Dto.OfficerUpdateDto;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Service.OfficerAccountService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class AdminController {

    private final OfficerAccountService officerAccountService;

    public AdminController(OfficerAccountService officerAccountService) {
        this.officerAccountService = officerAccountService;
    }

    @GetMapping({"/officer/admin", "/officer/admin/officers"})
    public String officerManagement(HttpSession session, Model model) {
        if (session.getAttribute("loggedInAdminId") == null) {
            return "redirect:/login";
        }

        model.addAttribute("loggedInAdminEmail", session.getAttribute("loggedInAdminEmail"));
        if (!model.containsAttribute("officerRegistration")) {
            model.addAttribute("officerRegistration", new OfficerRegistrationDto());
        }
        addOfficerTableData(model);
        return "officers_frontends/admin_dashboard";
    }

    @PostMapping("/officer/admin/officers")
    public String createOfficer(
            @Valid @ModelAttribute("officerRegistration") OfficerRegistrationDto officerRegistration,
            BindingResult bindingResult,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (session.getAttribute("loggedInAdminId") == null) {
            return "redirect:/login";
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("loggedInAdminEmail", session.getAttribute("loggedInAdminEmail"));
            model.addAttribute("openOfficerModal", true);
            addOfficerTableData(model);
            return "officers_frontends/admin_dashboard";
        }

        try {
            officerAccountService.createOfficer(officerRegistration);
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("officer.creation.failed", exception.getMessage());
            model.addAttribute("loggedInAdminEmail", session.getAttribute("loggedInAdminEmail"));
            model.addAttribute("openOfficerModal", true);
            addOfficerTableData(model);
            return "officers_frontends/admin_dashboard";
        }

        redirectAttributes.addFlashAttribute("officerCreatedMessage", "Officer account created successfully.");
        return "redirect:/officer/admin";
    }

    @PostMapping("/officer/admin/officers/{userId}/edit")
    public String updateOfficer(
            @PathVariable Integer userId,
            @Valid @ModelAttribute OfficerUpdateDto officerUpdate,
            BindingResult bindingResult,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (session.getAttribute("loggedInAdminId") == null) {
            return "redirect:/login";
        }

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("officerActionError", firstValidationError(bindingResult));
            return "redirect:/officer/admin";
        }

        try {
            officerAccountService.updateOfficer(userId, officerUpdate);
            redirectAttributes.addFlashAttribute("officerActionMessage", "Officer account updated successfully.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("officerActionError", exception.getMessage());
        }
        return "redirect:/officer/admin";
    }

    @PostMapping("/officer/admin/officers/{userId}/status")
    public String changeOfficerStatus(
            @PathVariable Integer userId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (session.getAttribute("loggedInAdminId") == null) {
            return "redirect:/login";
        }

        try {
            String newStatus = officerAccountService.toggleOfficerStatus(userId);
            String message = "ACTIVE".equals(newStatus)
                    ? "Officer account reactivated successfully."
                    : "Officer account disabled successfully.";
            redirectAttributes.addFlashAttribute("officerActionMessage", message);
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("officerActionError", exception.getMessage());
        }
        return "redirect:/officer/admin";
    }

    @PostMapping("/officer/admin/officers/{userId}/delete")
    public String deleteOfficer(
            @PathVariable Integer userId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (session.getAttribute("loggedInAdminId") == null) {
            return "redirect:/login";
        }

        try {
            officerAccountService.removeOfficer(userId);
            redirectAttributes.addFlashAttribute("officerActionMessage", "Officer account deleted successfully.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("officerActionError", exception.getMessage());
        }
        return "redirect:/officer/admin";
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

    private void addOfficerTableData(Model model) {
        List<OfficerDetailsDto> officers = officerAccountService.getAllOfficers();
        model.addAttribute("officers", officers);
        model.addAttribute("nicOfficerCount", countRole(officers, OfficerAccountService.NIC_OFFICER));
        model.addAttribute("passportOfficerCount", countRole(officers, OfficerAccountService.PASSPORT_OFFICER));
        model.addAttribute("csrCount", countRole(officers, OfficerAccountService.CSR));
        model.addAttribute("directorCount", countRole(officers, OfficerAccountService.DIRECTOR));
    }

    private long countRole(List<OfficerDetailsDto> officers, String roleCode) {
        return officers.stream()
                .filter(officer -> roleCode.equals(officer.getRoleCode()))
                .count();
    }

    private String firstValidationError(BindingResult bindingResult) {
        return bindingResult.getAllErrors().isEmpty()
                ? "Please check the officer details."
                : bindingResult.getAllErrors().get(0).getDefaultMessage();
    }
}
