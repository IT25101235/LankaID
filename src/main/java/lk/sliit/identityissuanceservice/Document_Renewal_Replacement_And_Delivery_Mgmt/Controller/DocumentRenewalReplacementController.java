package lk.sliit.identityissuanceservice.Document_Renewal_Replacement_And_Delivery_Mgmt.Controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Dto.CitizenEligibilityDto;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model.CitizenApplication;
import lk.sliit.identityissuanceservice.Document_Renewal_Replacement_And_Delivery_Mgmt.Dto.NicRenewalReplacementForm;
import lk.sliit.identityissuanceservice.Document_Renewal_Replacement_And_Delivery_Mgmt.Dto.PassportRenewalReplacementForm;
import lk.sliit.identityissuanceservice.Document_Renewal_Replacement_And_Delivery_Mgmt.Service.DocumentRenewalReplacementService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class DocumentRenewalReplacementController {

    private final DocumentRenewalReplacementService renewalService;

    public DocumentRenewalReplacementController(DocumentRenewalReplacementService renewalService) {
        this.renewalService = renewalService;
    }

    @GetMapping("/citizen/applications/nic/{formKind:renewal|replacement}")
    public String nicForm(
            @PathVariable String formKind,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {
        Integer citizenId = loggedInCitizenId(session);
        if (citizenId == null) return "redirect:/login";

        try {
            model.addAttribute("applicationForm", renewalService.prepareNicForm(citizenId, formKind));
            addFormData(model, citizenId, "NIC", formKind);
            return "citizen_frontends/nic_application_form";
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("applicationError", exception.getMessage());
            return "redirect:/citizen/dashboard";
        }
    }

    @PostMapping("/citizen/applications/nic/{formKind:renewal|replacement}")
    public String saveNicRequest(
            @PathVariable String formKind,
            @Valid @ModelAttribute("applicationForm") NicRenewalReplacementForm form,
            BindingResult bindingResult,
            @RequestParam(required = false) Integer applicationId,
            HttpSession session,
            Model model) {
        Integer citizenId = loggedInCitizenId(session);
        if (citizenId == null) return "redirect:/login";
        if (bindingResult.hasErrors()) {
            addFormData(model, citizenId, "NIC", formKind);
            addCorrectionFormData(model, applicationId);
            return "citizen_frontends/nic_application_form";
        }

        try {
            CitizenApplication application = applicationId == null
                    ? renewalService.createNicRequest(citizenId, formKind, form)
                    : renewalService.updateNicCorrection(citizenId, applicationId, form);
            return renewalDocumentRedirect("nic", application.getApplicationId());
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("renewal.failed", exception.getMessage());
            addFormData(model, citizenId, "NIC", formKind);
            addCorrectionFormData(model, applicationId);
            return "citizen_frontends/nic_application_form";
        }
    }

    @GetMapping("/citizen/applications/passport/{formKind:renewal|replacement}")
    public String passportForm(
            @PathVariable String formKind,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {
        Integer citizenId = loggedInCitizenId(session);
        if (citizenId == null) return "redirect:/login";

        try {
            model.addAttribute("applicationForm", renewalService.preparePassportForm(citizenId, formKind));
            addFormData(model, citizenId, "Passport", formKind);
            return "citizen_frontends/passport_application_form";
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("applicationError", exception.getMessage());
            return "redirect:/citizen/dashboard";
        }
    }

    @PostMapping("/citizen/applications/passport/{formKind:renewal|replacement}")
    public String savePassportRequest(
            @PathVariable String formKind,
            @Valid @ModelAttribute("applicationForm") PassportRenewalReplacementForm form,
            BindingResult bindingResult,
            @RequestParam(required = false) Integer applicationId,
            HttpSession session,
            Model model) {
        Integer citizenId = loggedInCitizenId(session);
        if (citizenId == null) return "redirect:/login";
        if (bindingResult.hasErrors()) {
            addFormData(model, citizenId, "Passport", formKind);
            addCorrectionFormData(model, applicationId);
            return "citizen_frontends/passport_application_form";
        }

        try {
            CitizenApplication application = applicationId == null
                    ? renewalService.createPassportRequest(citizenId, formKind, form)
                    : renewalService.updatePassportCorrection(citizenId, applicationId, form);
            return renewalDocumentRedirect("passport", application.getApplicationId());
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("renewal.failed", exception.getMessage());
            addFormData(model, citizenId, "Passport", formKind);
            addCorrectionFormData(model, applicationId);
            return "citizen_frontends/passport_application_form";
        }
    }

    @GetMapping("/citizen/renewals/{service:nic|passport}/correction")
    public String correctionForm(
            @PathVariable String service,
            @RequestParam Integer applicationId,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        Integer citizenId = loggedInCitizenId(session);
        if (citizenId == null) return "redirect:/login";

        try {
            CitizenApplication application = renewalService.getRequest(citizenId, applicationId);
            checkService(application, service);
            String formKind = "RENEWAL".equals(application.getApplicationKind()) ? "renewal" : "replacement";

            if ("nic".equals(service)) {
                model.addAttribute("applicationForm", renewalService.prepareNicCorrection(citizenId, applicationId));
                addFormData(model, citizenId, "NIC", formKind);
            } else {
                model.addAttribute("applicationForm", renewalService.preparePassportCorrection(citizenId, applicationId));
                addFormData(model, citizenId, "Passport", formKind);
            }
            addCorrectionFormData(model, applicationId);
            return "citizen_frontends/" + service + "_application_form";
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("applicationError", exception.getMessage());
            return "redirect:/citizen/dashboard";
        }
    }

    @GetMapping("/citizen/renewals/{service:nic|passport}/documents")
    public String documentPage(
            @PathVariable String service,
            @RequestParam Integer applicationId,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {
        Integer citizenId = loggedInCitizenId(session);
        if (citizenId == null) return "redirect:/login";

        try {
            addDocumentData(model, citizenId, applicationId, service);
            return "citizen_frontends/application_document_upload";
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("applicationError", exception.getMessage());
            return "redirect:/citizen/dashboard";
        }
    }

    @PostMapping("/citizen/renewals/{service:nic|passport}/documents")
    public String uploadDocument(
            @PathVariable String service,
            @RequestParam Integer applicationId,
            @RequestParam String documentType,
            @RequestParam("file") MultipartFile file,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        Integer citizenId = loggedInCitizenId(session);
        if (citizenId == null) return "redirect:/login";

        try {
            checkService(renewalService.getRequest(citizenId, applicationId), service);
            renewalService.uploadDocument(citizenId, applicationId, documentType, file);
            redirectAttributes.addFlashAttribute("documentMessage", "Document uploaded successfully.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("documentError", exception.getMessage());
        }
        return renewalDocumentRedirect(service, applicationId);
    }

    @GetMapping("/citizen/renewals/{service:nic|passport}/review")
    public String reviewRequest(
            @PathVariable String service,
            @RequestParam Integer applicationId,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {
        Integer citizenId = loggedInCitizenId(session);
        if (citizenId == null) return "redirect:/login";

        try {
            CitizenApplication application = renewalService.getRequest(citizenId, applicationId);
            checkService(application, service);
            model.addAttribute("currentApplication", application);
            model.addAttribute("documents", renewalService.getDocuments(citizenId, applicationId));
            model.addAttribute("requiredTypes", renewalService.requiredDocumentTypes(application));
            model.addAttribute("servicePath", service);
            model.addAttribute("workflowPath", "renewals");
            model.addAttribute("correctionMode", "CORRECTION_REQUIRED".equals(application.getCurrentStatus()));
            renewalService.getNicRequest(applicationId)
                    .ifPresent(value -> model.addAttribute("nicApplication", value));
            renewalService.getPassportRequest(applicationId)
                    .ifPresent(value -> model.addAttribute("passportApplication", value));
            return "citizen_frontends/citizen_application_review";
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("applicationError", exception.getMessage());
            return "redirect:/citizen/dashboard";
        }
    }

    @PostMapping("/citizen/renewals/{service:nic|passport}/submit")
    public String submitRequest(
            @PathVariable String service,
            @RequestParam Integer applicationId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        Integer citizenId = loggedInCitizenId(session);
        if (citizenId == null) return "redirect:/login";

        try {
            checkService(renewalService.getRequest(citizenId, applicationId), service);
            CitizenApplication application = renewalService.submitRequest(citizenId, applicationId);
            redirectAttributes.addFlashAttribute(
                    "applicationMessage",
                    "Request " + application.getApplicationNo() + " submitted successfully."
            );
            return "redirect:/citizen/dashboard";
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("documentError", exception.getMessage());
            return renewalDocumentRedirect(service, applicationId);
        }
    }

    private void addFormData(Model model, Integer citizenId, String service, String formKind) {
        CitizenEligibilityDto eligibility = renewalService.getEligibility(citizenId);
        model.addAttribute("formKind", formKind);
        model.addAttribute("applicationKind", applicationKind(formKind));
        model.addAttribute("pageTitle", title(service, formKind));
        model.addAttribute(
                "existingDocumentNo",
                "NIC".equals(service) ? eligibility.getNicNumber() : eligibility.getPassportNumber()
        );
        model.addAttribute("existingNicNo", eligibility.getNicNumber());
    }

    private void addDocumentData(
            Model model,
            Integer citizenId,
            Integer applicationId,
            String service) {
        CitizenApplication application = renewalService.getRequest(citizenId, applicationId);
        checkService(application, service);
        var documents = renewalService.getDocuments(citizenId, applicationId);
        model.addAttribute("currentApplication", application);
        model.addAttribute("documents", documents);
        model.addAttribute(
                "uploadedTypes",
                documents.stream().map(document -> document.getDocumentType()).toList()
        );
        model.addAttribute("requiredTypes", renewalService.requiredDocumentTypes(application));
        model.addAttribute("servicePath", service);
        model.addAttribute("workflowPath", "renewals");
        model.addAttribute("correctionMode", "CORRECTION_REQUIRED".equals(application.getCurrentStatus()));
    }

    private void addCorrectionFormData(Model model, Integer applicationId) {
        if (applicationId != null) {
            model.addAttribute("correctionApplicationId", applicationId);
            model.addAttribute("pageTitle", "Correct Application Details");
        }
    }

    private String applicationKind(String formKind) {
        return "renewal".equals(formKind) ? "RENEWAL" : "LOST";
    }

    private String title(String service, String formKind) {
        return "renewal".equals(formKind)
                ? service + " Renewal Application"
                : "Lost " + service + " Replacement";
    }

    private void checkService(CitizenApplication application, String servicePath) {
        if (!application.getServiceType().equalsIgnoreCase(servicePath)) {
            throw new IllegalArgumentException("Application service type does not match the requested page.");
        }
    }

    private String renewalDocumentRedirect(String service, Integer applicationId) {
        return "redirect:/citizen/renewals/" + service
                + "/documents?applicationId=" + applicationId;
    }

    private Integer loggedInCitizenId(HttpSession session) {
        Object citizenId = session.getAttribute("loggedInUserId");
        Object role = session.getAttribute("loggedInUserRole");
        return citizenId instanceof Integer && "CITIZEN".equals(role) ? (Integer) citizenId : null;
    }
}
