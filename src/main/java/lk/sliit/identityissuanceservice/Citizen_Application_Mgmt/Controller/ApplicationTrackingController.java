package lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Controller;

import jakarta.servlet.http.HttpSession;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Service.CitizenApplicationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ApplicationTrackingController {

    private final CitizenApplicationService applicationService;

    public ApplicationTrackingController(CitizenApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @GetMapping("/citizen/applications")
    public String applicationTracking(HttpSession session, Model model) {
        Integer citizenId = loggedInCitizenId(session);
        if (citizenId == null) {
            return "redirect:/login";
        }

        model.addAttribute("applications", applicationService.getCitizenApplications(citizenId));
        return "citizen_frontends/application_tracking";
    }

    private Integer loggedInCitizenId(HttpSession session) {
        Object userId = session.getAttribute("loggedInUserId");
        Object role = session.getAttribute("loggedInUserRole");
        if (!(userId instanceof Integer) || !"CITIZEN".equals(role)) {
            return null;
        }
        return (Integer) userId;
    }
}
