package lk.sliit.identityissuanceservice.Admin_Mgmt.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DirectorController {

    @GetMapping("/officer/director")
    public String directorDashboard() {
        return "officers_frontends/director_dashboard";
    }

    @GetMapping("/officer/director/printing")
    public String printingExport() {
        return "officers_frontends/printing_export";
    }
}
