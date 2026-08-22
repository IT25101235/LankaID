package lk.sliit.identityissuanceservice.Document_Renewal_And_Replacement_Mgmt.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class NIC_Controller {

    @GetMapping("/citizen/applications/nic/renewal")
    public String renewNic(Model model) {
        model.addAttribute("applicationType", "renewal");
        return "citizen_frontends/nic_application_form";
    }

    @GetMapping("/citizen/applications/nic/replacement")
    public String replaceNic(Model model) {
        model.addAttribute("applicationType", "lost");
        return "citizen_frontends/nic_application_form";
    }
}
