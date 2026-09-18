package lk.sliit.identityissuanceservice.User_Mgmt.Controller;

import jakarta.servlet.http.HttpSession;
import lk.sliit.identityissuanceservice.Notification_Mgmt.Service.NotificationService;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class CitizenNavigationAdvice {

    private final NotificationService notificationService;

    public CitizenNavigationAdvice(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @ModelAttribute
    public void exposeCitizenNavigation(HttpSession session, Model model) {
        Object userId = session.getAttribute("loggedInUserId");
        Object role = session.getAttribute("loggedInUserRole");
        if (userId instanceof Integer citizenId && "CITIZEN".equals(role)) {
            model.addAttribute("unreadNotificationCount", notificationService.getUnreadCount(citizenId));
        }
    }
}
