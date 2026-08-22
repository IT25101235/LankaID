package lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class PassportApplicationController {

    @GetMapping("/citizen/applications/passport/new")
    public String newPassportApplication(Model model) {
        model.addAttribute("applicationType", "new");
        return "citizen_frontends/passport_application_form";
    }

    @GetMapping("/citizen/applications/passport/documents")
    public String passportDocumentUpload() {
        return "citizen_frontends/passport_document_upload";
    }

    @GetMapping("/citizen/applications/passport/review")
    public String citizenPassportReview() {
        return "citizen_frontends/passport_application_review";
    }

    @GetMapping("/officer/passport/applications")
    public String passportApplicationQueue() {
        return "officers_frontends/passport_verification_queue";
    }

    @GetMapping("/officer/passport/applications/processed")
    public String processedPassportApplications(Model model) {
        addOfficerPageData(model, "Passport", "/officer/passport/applications", "/officer/passport/biometrics");
        return "officers_frontends/processed_applications";
    }

    @GetMapping("/officer/passport/applications/{reference}")
    public String reviewPassportApplication(@PathVariable String reference, Model model) {
        addOfficerPageData(model, "Passport", "/officer/passport/applications", "/officer/passport/biometrics");
        model.addAttribute("reference", reference);
        return "officers_frontends/application_review";
    }

    private void addOfficerPageData(Model model, String documentType, String queueRoute, String biometricRoute) {
        // The NIC and Passport review layouts are the same, so one template is reused.
        model.addAttribute("documentType", documentType);
        model.addAttribute("queueRoute", queueRoute);
        model.addAttribute("biometricRoute", biometricRoute);
    }
}
