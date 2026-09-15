package lk.sliit.identityissuanceservice.Admin_Mgmt.Controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Dto.OfficerDetailsDto;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Dto.OfficerRegistrationDto;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Dto.OfficerUpdateDto;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Service.OfficerAccountService;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Service.ServiceCentreService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class AdminController {

    private final OfficerAccountService officerAccountService;
    private final ServiceCentreService serviceCentreService;

    public AdminController(
            OfficerAccountService officerAccountService,
            ServiceCentreService serviceCentreService
    ) {
        this.officerAccountService = officerAccountService;
        this.serviceCentreService = serviceCentreService;
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
            @Valid @ModelAttribute("officerUpdate") OfficerUpdateDto officerUpdate,
            BindingResult bindingResult,
            Model model,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (session.getAttribute("loggedInAdminId") == null) {
            return "redirect:/login";
        }

        if (bindingResult.hasErrors()) {
            return officerEditError(userId, bindingResult, session, model);
        }

        try {
            officerAccountService.updateOfficer(userId, officerUpdate);
            redirectAttributes.addFlashAttribute("officerActionMessage", "Officer account updated successfully.");
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("officer.update.failed", exception.getMessage());
            return officerEditError(userId, bindingResult, session, model);
        }
        return "redirect:/officer/admin";
    }

    private String officerEditError(Integer userId, BindingResult errors, HttpSession session, Model model) {
        model.addAttribute("editOfficerId", userId);
        model.addAttribute("officerEditError", firstValidationError(errors));
        return officerManagement(session, model);
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

    @PostMapping("/officer/admin/centres")
    public String addServiceCentre(
            @RequestParam String centreName,
            @RequestParam(required = false) String street,
            @RequestParam String city,
            @RequestParam String district,
            @RequestParam Integer dailyCapacity,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        if (session.getAttribute("loggedInAdminId") == null) {
            return "redirect:/login";
        }

        try {
            serviceCentreService.addCentre(centreName, street, city, district, dailyCapacity);
            redirectAttributes.addFlashAttribute("centreMessage", "Service centre added successfully.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("centreError", exception.getMessage());
        }
        return "redirect:/officer/admin?section=centres";
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
        model.addAttribute("centres", serviceCentreService.getAllCentres());
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
