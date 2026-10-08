package lk.sliit.identityissuanceservice.Document_Renewal_Replacement_And_Delivery_Mgmt.Controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Dto.CitizenEligibilityDto;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model.CitizenApplication;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Service.CitizenCorrectionRequestService;
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

// Handles citizen pages for renewing a document or replacing a lost one.
@Controller
public class DocumentRenewalReplacementController {

    private final DocumentRenewalReplacementService renewalService;
    private final CitizenCorrectionRequestService correctionRequestService;

    public DocumentRenewalReplacementController(
            DocumentRenewalReplacementService renewalService,
            CitizenCorrectionRequestService correctionRequestService
    ) {
        this.renewalService = renewalService;
        this.correctionRequestService = correctionRequestService;
    }

    // Show a prefilled NIC form only when an existing verified NIC is linked.
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

    // Validate the form, save a draft or edit the same request, then open uploads.
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
            addEditFormData(model, citizenId, applicationId);
            return "citizen_frontends/nic_application_form";
        }

        try {
            CitizenApplication application = applicationId == null
                    ? renewalService.createNicRequest(citizenId, formKind, form)
                    : renewalService.updateNicRequest(citizenId, applicationId, form);
            return renewalDocumentRedirect("nic", application.getApplicationId());
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("renewal.failed", exception.getMessage());
            addFormData(model, citizenId, "NIC", formKind);
            addEditFormData(model, citizenId, applicationId);
            return "citizen_frontends/nic_application_form";
        }
    }

    // Load the passport form with the citizen profile and linked document details.
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

    // Keep validation errors on this page; a saved request continues to uploads.
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
            addEditFormData(model, citizenId, applicationId);
            return "citizen_frontends/passport_application_form";
        }

        try {
            CitizenApplication application = applicationId == null
                    ? renewalService.createPassportRequest(citizenId, formKind, form)
                    : renewalService.updatePassportRequest(citizenId, applicationId, form);
            return renewalDocumentRedirect("passport", application.getApplicationId());
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("renewal.failed", exception.getMessage());
            addFormData(model, citizenId, "Passport", formKind);
            addEditFormData(model, citizenId, applicationId);
            return "citizen_frontends/passport_application_form";
        }
    }

    // Reopen an owned, unsubmitted draft without creating another application.
    @GetMapping("/citizen/renewals/{service:nic|passport}/edit")
    public String editDraft(
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
                model.addAttribute("applicationForm", renewalService.prepareNicDraftEdit(citizenId, applicationId));
                addFormData(model, citizenId, "NIC", formKind);
            } else {
                model.addAttribute("applicationForm", renewalService.preparePassportDraftEdit(citizenId, applicationId));
                addFormData(model, citizenId, "Passport", formKind);
            }
            addDraftEditFormData(model, applicationId);
            return "citizen_frontends/" + service + "_application_form";
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("applicationError", exception.getMessage());
            return "redirect:/citizen/dashboard";
        }
    }

    // An officer must request a correction before a submitted form can be edited.
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
            addCorrectionFormData(model, citizenId, applicationId);
            return "citizen_frontends/" + service + "_application_form";
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("applicationError", exception.getMessage());
            return "redirect:/citizen/dashboard";
        }
    }

    // Reuse the shared upload page with renewal routes and required document types.
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

    // Check the owner and service before handing the uploaded file to the service.
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

    // Show saved details and documents so the citizen can review before submission.
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
            addCorrectionMessage(model, citizenId, application);
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

    // Submit the existing draft or correction request; saving a form alone does not submit it.
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
        model.addAttribute("verifiedNicIssuedDate", eligibility.getNicIssuedDate() != null);
        model.addAttribute("workflowPath", "renewals");
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
        var requiredTypes = renewalService.requiredDocumentTypes(application);
        var uploadedTypes = documents.stream().map(document -> document.getDocumentType()).toList();
        model.addAttribute("requiredTypes", requiredTypes);
        model.addAttribute(
                "missingRequiredTypes",
                requiredTypes.stream().filter(type -> !uploadedTypes.contains(type)).toList()
        );
        model.addAttribute("servicePath", service);
        model.addAttribute("workflowPath", "renewals");
        model.addAttribute("correctionMode", "CORRECTION_REQUIRED".equals(application.getCurrentStatus()));
        addCorrectionMessage(model, citizenId, application);
    }

    private void addCorrectionFormData(Model model, Integer citizenId, Integer applicationId) {
        if (applicationId != null) {
            model.addAttribute("correctionApplicationId", applicationId);
            model.addAttribute("pageTitle", "Correct Application Details");
            model.addAttribute(
                    "correctionMessage",
                    correctionRequestService.getCurrentCorrectionMessage(citizenId, applicationId)
            );
        }
    }

    private void addDraftEditFormData(Model model, Integer applicationId) {
        model.addAttribute("draftEditApplicationId", applicationId);
        model.addAttribute("pageTitle", "Edit Saved Draft");
    }

    private void addEditFormData(Model model, Integer citizenId, Integer applicationId) {
        if (applicationId == null) return;
        CitizenApplication application = renewalService.getRequest(citizenId, applicationId);
        if ("CORRECTION_REQUIRED".equals(application.getCurrentStatus())) {
            addCorrectionFormData(model, citizenId, applicationId);
        } else if ("DRAFT".equals(application.getCurrentStatus())) {
            addDraftEditFormData(model, applicationId);
        }
    }

    private void addCorrectionMessage(Model model, Integer citizenId, CitizenApplication application) {
        if ("CORRECTION_REQUIRED".equals(application.getCurrentStatus())) {
            model.addAttribute(
                    "correctionMessage",
                    correctionRequestService.getCurrentCorrectionMessage(citizenId, application.getApplicationId())
            );
        }
    }

    // The replacement page represents a lost document and uses the database value LOST.
    private String applicationKind(String formKind) {
        return "renewal".equals(formKind) ? "RENEWAL" : "LOST";
    }

    private String title(String service, String formKind) {
        return "renewal".equals(formKind)
                ? service + " Renewal Application"
                : "Lost " + service + " Replacement";
    }

    // Prevent a NIC request from being opened through a passport route, or the reverse.
    private void checkService(CitizenApplication application, String servicePath) {
        if (!application.getServiceType().equalsIgnoreCase(servicePath)) {
            throw new IllegalArgumentException("Application service type does not match the requested page.");
        }
    }

    private String renewalDocumentRedirect(String service, Integer applicationId) {
        return "redirect:/citizen/renewals/" + service
                + "/documents?applicationId=" + applicationId;
    }

    // Use the signed-in citizen ID from the session rather than a submitted user ID.
    private Integer loggedInCitizenId(HttpSession session) {
        Object citizenId = session.getAttribute("loggedInUserId");
        Object role = session.getAttribute("loggedInUserRole");
        return citizenId instanceof Integer && "CITIZEN".equals(role) ? (Integer) citizenId : null;
    }
}
