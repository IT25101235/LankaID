package lk.sliit.identityissuanceservice.Admin_Mgmt.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminController {

    @GetMapping({"/officer/admin", "/officer/admin/officers"})
    public String officerManagement() {
        return "officers_frontends/admin_dashboard";
    }

    @GetMapping("/officer/admin/citizens")
    public String citizenAccounts() {
        return "officers_frontends/admin_citizens";
    }

    @GetMapping("/officer/admin/notifications")
    public String notificationComposer() {
        return "officers_frontends/admin_notifications";
    }
}
