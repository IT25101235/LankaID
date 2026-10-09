package lk.sliit.identityissuanceservice.Notification_Mgmt.Controller;

import jakarta.servlet.http.HttpSession;
import lk.sliit.identityissuanceservice.Notification_Mgmt.Dto.CitizenNotificationDto;
import lk.sliit.identityissuanceservice.Notification_Mgmt.Service.NotificationService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Controller
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/citizen/notifications")
    public String citizenNotifications(HttpSession session, Model model) {
        Integer citizenId = loggedInCitizenId(session);
        if (citizenId == null) return "redirect:/login";

        List<CitizenNotificationDto> notifications = notificationService.getCitizenNotifications(citizenId);
        model.addAttribute("notifications", notifications);
        model.addAttribute("unreadCount", notifications.stream().filter(item -> !item.read()).count());
        return "citizen_frontends/citizen_notifications";
    }

    @PostMapping("/citizen/notifications/{notificationId}/read")
    public String markNotificationRead(@PathVariable Integer notificationId, HttpSession session) {
        Integer citizenId = loggedInCitizenId(session);
        if (citizenId == null) return "redirect:/login";
        notificationService.markAsRead(citizenId, notificationId);
        return "redirect:/citizen/notifications";
    }

    @PostMapping("/citizen/notifications/read-all")
    public String markAllNotificationsRead(HttpSession session) {
        Integer citizenId = loggedInCitizenId(session);
        if (citizenId == null) return "redirect:/login";
        notificationService.markAllAsRead(citizenId);
        return "redirect:/citizen/notifications";
    }

    /** READ (JSON): the five newest notifications for the navigation dropdown. */
    @ResponseBody
    @GetMapping("/citizen/notifications/recent")
    public ResponseEntity<Map<String, Object>> citizenRecentNotifications(HttpSession session) {
        Integer citizenId = loggedInCitizenId(session);
        if (citizenId == null) return ResponseEntity.status(401).build();

        List<CitizenNotificationDto> all = notificationService.getCitizenNotifications(citizenId);
        List<Map<String, Object>> latest = all.stream()
                .limit(5)
                .map(item -> Map.<String, Object>of(
                        "notificationId", item.notificationId(),
                        "type", Objects.toString(item.notificationType(), ""),
                        "message", Objects.toString(item.message(), ""),
                        "createdAt", item.createdAt() == null ? "" : item.createdAt().toString(),
                        "read", item.read()))
                .toList();
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(Map.of(
                        "notifications", latest,
                        "unreadCount", all.stream().filter(item -> !item.read()).count()));
    }

    @GetMapping("/officer/notifications")
    public String officerNotifications(HttpSession session, Model model) {
        Integer officerId = loggedInOfficerId(session);
        if (officerId == null) return "redirect:/login";

        List<CitizenNotificationDto> notifications = notificationService.getCitizenNotifications(officerId);
        model.addAttribute("notifications", notifications);
        model.addAttribute("unreadCount", notifications.stream().filter(item -> !item.read()).count());
        model.addAttribute("loggedInOfficerRole", session.getAttribute("loggedInOfficerRole"));
        model.addAttribute("loggedInOfficerEmail", session.getAttribute("loggedInOfficerEmail"));
        model.addAttribute("loggedInOfficerRoleLabel", session.getAttribute("loggedInOfficerRoleLabel"));
        model.addAttribute("loggedInOfficerLoginTime", session.getAttribute("loggedInOfficerLoginTime"));
        return "officers_frontends/officer_notifications";
    }

    @PostMapping("/officer/notifications/{notificationId}/read")
    public String markOfficerNotificationRead(@PathVariable Integer notificationId, HttpSession session) {
        Integer officerId = loggedInOfficerId(session);
        if (officerId == null) return "redirect:/login";
        notificationService.markAsRead(officerId, notificationId);
        return "redirect:/officer/notifications";
    }

    @PostMapping("/officer/notifications/read-all")
    public String markAllOfficerNotificationsRead(HttpSession session) {
        Integer officerId = loggedInOfficerId(session);
        if (officerId == null) return "redirect:/login";
        notificationService.markAllAsRead(officerId);
        return "redirect:/officer/notifications";
    }

    @ResponseBody
    @GetMapping("/officer/notifications/unread-count")
    public Map<String, Long> officerUnreadCount(HttpSession session) {
        Integer officerId = loggedInOfficerId(session);
        return Map.of("unreadCount", officerId == null ? 0L : notificationService.getUnreadCount(officerId));
    }

    private Integer loggedInCitizenId(HttpSession session) {
        Object userId = session.getAttribute("loggedInUserId");
        Object role = session.getAttribute("loggedInUserRole");
        return userId instanceof Integer id && "CITIZEN".equals(role) ? id : null;
    }

    private Integer loggedInOfficerId(HttpSession session) {
        Object userId = session.getAttribute("loggedInOfficerId");
        return userId instanceof Integer id ? id : null;
    }
}
