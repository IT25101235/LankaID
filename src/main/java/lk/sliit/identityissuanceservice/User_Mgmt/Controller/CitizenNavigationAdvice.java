package lk.sliit.identityissuanceservice.User_Mgmt.Controller;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletRequest;
import lk.sliit.identityissuanceservice.Chatbot_Mgmt.Controller.ChatbotRequestScope;
import lk.sliit.identityissuanceservice.Notification_Mgmt.Service.NotificationService;
import lk.sliit.identityissuanceservice.Citizen_Support_And_Service_Mgmt.Service.CitizenSupportService;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class CitizenNavigationAdvice {

    private final NotificationService notificationService;
    private final CitizenSupportService supportService;

    public CitizenNavigationAdvice(NotificationService notificationService, CitizenSupportService supportService) {
        this.notificationService = notificationService;
        this.supportService = supportService;
    }

    @ModelAttribute
    public void exposePageNavigation(HttpServletRequest request, Model model) {
        // Public chatbot requests must not read account counters or session identity.
        if (ChatbotRequestScope.isChatbotRequest(request)) return;
        HttpSession session = request.getSession(false);
        if (session != null) exposeCitizenNavigation(session, model);
    }

    public void exposeCitizenNavigation(HttpSession session, Model model) {
        Object userId = session.getAttribute("loggedInUserId");
        Object role = session.getAttribute("loggedInUserRole");
        if (userId instanceof Integer citizenId && "CITIZEN".equals(role)) {
            model.addAttribute("unreadNotificationCount", notificationService.getUnreadCount(citizenId));
            long activeSupportRequests = supportService.getCitizenOpenCount(citizenId)
                    + supportService.getCitizenRespondedCount(citizenId);
            model.addAttribute("activeSupportRequestCount", activeSupportRequests);
        }
    }
}
