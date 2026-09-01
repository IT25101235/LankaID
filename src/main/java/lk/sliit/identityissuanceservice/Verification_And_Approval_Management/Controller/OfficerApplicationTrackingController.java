package lk.sliit.identityissuanceservice.Verification_And_Approval_Management.Controller;

import jakarta.servlet.http.HttpSession;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Service.OfficerAccountService;
import lk.sliit.identityissuanceservice.Verification_And_Approval_Management.Dto.OfficerApplicationTrackingItem;
import lk.sliit.identityissuanceservice.Verification_And_Approval_Management.Service.OfficerApplicationTrackingService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class OfficerApplicationTrackingController {

    private final OfficerApplicationTrackingService trackingService;

    public OfficerApplicationTrackingController(OfficerApplicationTrackingService trackingService) {
        this.trackingService = trackingService;
    }

    @GetMapping("/officer/application-tracking")
    public String applicationTracking(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            HttpSession session,
            Model model
    ) {
        if (session.getAttribute("loggedInAdminId") != null) return "redirect:/officer/admin";
        String role = session.getAttribute("loggedInOfficerRole") instanceof String value ? value : null;
        if (OfficerAccountService.CSR.equals(role)) return "redirect:/officer/csr";
        if (!canTrack(role)) return "redirect:/login";

        Page<OfficerApplicationTrackingItem> trackingPage =
                trackingService.getTrackingPage(role, search, page);

        model.addAttribute("trackingItems", trackingPage.getContent());
        model.addAttribute("currentPage", trackingPage.getNumber());
        model.addAttribute("totalPages", trackingPage.getTotalPages());
        model.addAttribute("totalApplications", trackingPage.getTotalElements());
        model.addAttribute("hasPrevious", trackingPage.hasPrevious());
        model.addAttribute("hasNext", trackingPage.hasNext());
        model.addAttribute("search", search == null ? "" : search);
        model.addAttribute("scopeLabel", scopeLabel(role));
        model.addAttribute("loggedInOfficerRole", role);
        model.addAttribute("backRoute", backRoute(role));
        model.addAttribute("loggedInOfficerRoleLabel", session.getAttribute("loggedInOfficerRoleLabel"));
        model.addAttribute("loggedInOfficerEmployeeNo", session.getAttribute("loggedInOfficerEmployeeNo"));
        model.addAttribute("loggedInOfficerEmail", session.getAttribute("loggedInOfficerEmail"));
        return "officers_frontends/officer_application_tracking";
    }

    private boolean canTrack(String role) {
        return OfficerAccountService.DIRECTOR.equals(role)
                || OfficerAccountService.NIC_OFFICER.equals(role)
                || OfficerAccountService.PASSPORT_OFFICER.equals(role);
    }

    private String scopeLabel(String role) {
        if (OfficerAccountService.DIRECTOR.equals(role)) return "All NIC and Passport applications";
        return OfficerAccountService.NIC_OFFICER.equals(role) ? "NIC applications" : "Passport applications";
    }

    private String backRoute(String role) {
        if (OfficerAccountService.DIRECTOR.equals(role)) return "/officer/director";
        return OfficerAccountService.NIC_OFFICER.equals(role)
                ? "/officer/nic/applications" : "/officer/passport/applications";
    }
}
