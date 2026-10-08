package lk.sliit.identityissuanceservice.Admin_Mgmt.Controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Dto.OfficerDetailsDto;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Dto.OfficerRegistrationDto;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Dto.OfficerUpdateDto;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Dto.CitizenAccountDashboardDto;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Service.AdminAccountService;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Service.ApplicationFeeSettingsService;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Service.CitizenAccountDashboardService;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Service.DirectorDashboardService;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Service.OfficerAccountService;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Service.ServiceCentreService;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Service.SystemMaintenanceService;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Model.Centre;
import lk.sliit.identityissuanceservice.Notification_Mgmt.Service.PublicAnnouncementService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class AdminController {

    private final OfficerAccountService officerAccountService;
    private final ServiceCentreService serviceCentreService;
    private final CitizenAccountDashboardService citizenAccountDashboardService;
    private final DirectorDashboardService directorDashboardService;
    private final AdminAccountService adminAccountService;
    private final SystemMaintenanceService maintenanceService;
    private final ApplicationFeeSettingsService feeSettingsService;
    private final PublicAnnouncementService publicAnnouncementService;

    public AdminController(
            OfficerAccountService officerAccountService,
            ServiceCentreService serviceCentreService,
            CitizenAccountDashboardService citizenAccountDashboardService,
            DirectorDashboardService directorDashboardService,
            AdminAccountService adminAccountService,
            SystemMaintenanceService maintenanceService,
            ApplicationFeeSettingsService feeSettingsService,
            PublicAnnouncementService publicAnnouncementService
    ) {
        this.officerAccountService = officerAccountService;
        this.serviceCentreService = serviceCentreService;
        this.citizenAccountDashboardService = citizenAccountDashboardService;
        this.directorDashboardService = directorDashboardService;
        this.adminAccountService = adminAccountService;
        this.maintenanceService = maintenanceService;
        this.feeSettingsService = feeSettingsService;
        this.publicAnnouncementService = publicAnnouncementService;
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
            model.addAttribute("activeAdminSection", "users");
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
        return "redirect:/officer/admin?section=users";
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
        return "redirect:/officer/admin?section=users";
    }

    private String officerEditError(Integer userId, BindingResult errors, HttpSession session, Model model) {
        model.addAttribute("editOfficerId", userId);
        model.addAttribute("officerEditError", firstValidationError(errors));
        model.addAttribute("activeAdminSection", "users");
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
        return "redirect:/officer/admin?section=users";
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
        return "redirect:/officer/admin?section=users";
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

    @PostMapping("/officer/admin/centres/{centreId}/edit")
    public String updateServiceCentre(
            @PathVariable Integer centreId,
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
            serviceCentreService.updateCentre(
                    centreId, centreName, street, city, district, dailyCapacity);
            redirectAttributes.addFlashAttribute("centreMessage", "Service centre updated successfully.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("centreError", exception.getMessage());
        }
        return "redirect:/officer/admin?section=centres";
    }

    @PostMapping("/officer/admin/centres/{centreId}/status")
    public String changeServiceCentreStatus(
            @PathVariable Integer centreId,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        if (session.getAttribute("loggedInAdminId") == null) {
            return "redirect:/login";
        }

        try {
            String status = serviceCentreService.toggleCentreStatus(centreId);
            redirectAttributes.addFlashAttribute("centreMessage", "ACTIVE".equals(status)
                    ? "Service centre activated successfully."
                    : "Service centre deactivated successfully.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("centreError", exception.getMessage());
        }
        return "redirect:/officer/admin?section=centres";
    }

    @PostMapping("/officer/admin/centres/{centreId}/delete")
    public String deleteServiceCentre(
            @PathVariable Integer centreId,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        if (session.getAttribute("loggedInAdminId") == null) {
            return "redirect:/login";
        }

        try {
            serviceCentreService.removeCentre(centreId);
            redirectAttributes.addFlashAttribute("centreMessage", "Service centre deleted successfully.");
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

    @PostMapping("/officer/admin/maintenance")
    public String updateMaintenanceMode(
            @RequestParam boolean enable,
            @RequestParam(required = false) String message,
            @RequestParam String password,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        if (session.getAttribute("loggedInAdminId") == null) {
            return "redirect:/login";
        }

        String adminEmail = String.valueOf(session.getAttribute("loggedInAdminEmail"));
        if (password.isBlank()
                || adminAccountService.authenticateAdmin(adminEmail, password).isEmpty()) {
            redirectAttributes.addFlashAttribute(
                    "maintenanceError", "The administrator password is incorrect.");
            return "redirect:/officer/admin?section=maintenance";
        }

        try {
            maintenanceService.updateMaintenance(enable, message);
            redirectAttributes.addFlashAttribute(
                    "maintenanceMessage",
                    enable
                            ? "Maintenance mode activated. All non-admin operations are paused."
                            : "Maintenance mode ended. All system operations are available again."
            );
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("maintenanceError", exception.getMessage());
        }
        return "redirect:/officer/admin?section=maintenance";
    }

    @PostMapping("/officer/admin/settings")
    public String updateApplicationFees(
            @RequestParam String nicFee,
            @RequestParam String passportFee,
            @RequestParam String password,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        if (session.getAttribute("loggedInAdminId") == null) {
            return "redirect:/login";
        }

        String adminEmail = String.valueOf(session.getAttribute("loggedInAdminEmail"));
        if (password.isBlank()
                || adminAccountService.authenticateAdmin(adminEmail, password).isEmpty()) {
            redirectAttributes.addFlashAttribute(
                    "settingsError", "The administrator password is incorrect.");
            return "redirect:/officer/admin?section=settings";
        }

        try {
            feeSettingsService.updateFees(nicFee, passportFee);
            redirectAttributes.addFlashAttribute(
                    "settingsMessage", "Application service fees updated successfully.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("settingsError", exception.getMessage());
        }
        return "redirect:/officer/admin?section=settings";
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
        List<Centre> centres = serviceCentreService.getAllCentres();
        Map<Integer, Long> centreBookingCounts = new HashMap<>();
        for (Centre centre : centres) {
            centreBookingCounts.put(
                    centre.getCentreId(),
                    serviceCentreService.getTodayBookingCount(centre.getCentreId()));
        }
        model.addAttribute("officers", officers);
        model.addAttribute("nicOfficerCount", countRole(officers, OfficerAccountService.NIC_OFFICER));
        model.addAttribute("passportOfficerCount", countRole(officers, OfficerAccountService.PASSPORT_OFFICER));
        model.addAttribute("csrCount", countRole(officers, OfficerAccountService.CSR));
        model.addAttribute("directorCount", countRole(officers, OfficerAccountService.DIRECTOR));
        model.addAttribute("maintenanceSettings", maintenanceService.getSettings());
        model.addAttribute("feeSettings", feeSettingsService.getSettings());
        model.addAttribute("publicAnnouncements", publicAnnouncementService.getRecentAnnouncements());
        model.addAttribute("centres", centres);
        model.addAttribute("centreBookingCounts", centreBookingCounts);
        model.addAttribute("activeOfficerCount", officers.stream()
                .filter(officer -> "ACTIVE".equals(officer.getStatus()))
                .count());
        model.addAttribute("serviceCentreCount", centres.size());
        model.addAttribute("adminOverview", directorDashboardService.getStats());
        CitizenAccountDashboardDto citizenDashboard = citizenAccountDashboardService.getDashboard();
        model.addAttribute("citizens", citizenDashboard.citizens());
        model.addAttribute("citizenAccountCount", citizenDashboard.citizenAccountCount());
        model.addAttribute("registeredThisMonthCount", citizenDashboard.registeredThisMonthCount());
        model.addAttribute("suspendedCitizenCount", citizenDashboard.suspendedCitizenCount());
    }

    @PostMapping("/officer/admin/citizens/{userId}/status")
    public String changeCitizenStatus(
            @PathVariable Integer userId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (session.getAttribute("loggedInAdminId") == null) {
            return "redirect:/login";
        }
        try {
            String status = citizenAccountDashboardService.toggleCitizenStatus(userId);
            redirectAttributes.addFlashAttribute("citizenActionMessage", "ACTIVE".equals(status)
                    ? "Citizen account reactivated successfully."
                    : "Citizen account suspended successfully.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("citizenActionError", exception.getMessage());
        }
        return "redirect:/officer/admin?section=citizens";
    }

    @PostMapping("/officer/admin/citizens/{userId}/delete")
    public String deleteCitizen(
            @PathVariable Integer userId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (session.getAttribute("loggedInAdminId") == null) {
            return "redirect:/login";
        }
        try {
            citizenAccountDashboardService.removeCitizenAccount(userId);
            redirectAttributes.addFlashAttribute(
                    "citizenActionMessage",
                    "Citizen login deleted. Government identity records and application history were preserved."
            );
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("citizenActionError", exception.getMessage());
        }
        return "redirect:/officer/admin?section=citizens";
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
