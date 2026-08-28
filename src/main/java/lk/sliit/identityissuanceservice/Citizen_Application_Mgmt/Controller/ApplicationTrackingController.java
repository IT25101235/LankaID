package lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Controller;

import jakarta.servlet.http.HttpSession;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Service.CitizenApplicationService;
import lk.sliit.identityissuanceservice.Notification_Mgmt.Service.NotificationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ApplicationTrackingController {

    private final CitizenApplicationService applicationService;
    private final NotificationService notificationService;

    public ApplicationTrackingController(
            CitizenApplicationService applicationService,
            NotificationService notificationService
    ) {
        this.applicationService = applicationService;
        this.notificationService = notificationService;
    }

    @GetMapping("/citizen/applications")
    public String applicationTracking(HttpSession session, Model model) {
        Integer citizenId = loggedInCitizenId(session);
        if (citizenId == null) {
            return "redirect:/login";
        }

        model.addAttribute("applications", applicationService.getCitizenApplications(citizenId));
        model.addAttribute("unreadNotificationCount", notificationService.getUnreadCount(citizenId));
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
