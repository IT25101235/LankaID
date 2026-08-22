package lk.sliit.identityissuanceservice.Notification_Mgmt.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class NotificationController {

    @GetMapping("/citizen/notifications")
    public String citizenNotifications() {
        return "citizen_frontends/citizen_notifications";
    }
}
