package lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class NIC_ApplicationController {

    @GetMapping("/citizen/applications/nic/new")
    public String newNicApplication(Model model) {
        model.addAttribute("applicationType", "new");
        return "citizen_frontends/nic_application_form";
    }

    @GetMapping("/citizen/applications/nic/documents")
    public String nicDocumentUpload() {
        return "citizen_frontends/nic_document_upload";
    }

    @GetMapping("/citizen/applications/nic/review")
    public String citizenNicReview() {
        return "citizen_frontends/nic_application_review";
    }

    @GetMapping("/officer/nic/applications")
    public String nicApplicationQueue() {
        return "officers_frontends/nic_verification_queue";
    }

    @GetMapping("/officer/nic/applications/processed")
    public String processedNicApplications(Model model) {
        addOfficerPageData(model, "NIC", "/officer/nic/applications", "/officer/nic/biometrics");
        return "officers_frontends/processed_applications";
    }

    @GetMapping("/officer/nic/applications/{reference}")
    public String reviewNicApplication(@PathVariable String reference, Model model) {
        addOfficerPageData(model, "NIC", "/officer/nic/applications", "/officer/nic/biometrics");
        model.addAttribute("reference", reference);
        return "officers_frontends/application_review";
    }

    private void addOfficerPageData(Model model, String documentType, String queueRoute, String biometricRoute) {
        // These display values will be replaced with application data after the DB is added.
        model.addAttribute("documentType", documentType);
        model.addAttribute("queueRoute", queueRoute);
        model.addAttribute("biometricRoute", biometricRoute);
    }
}
