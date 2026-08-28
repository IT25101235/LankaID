package lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Dto.CitizenEligibilityDto;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Dto.NewNicApplicationForm;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Dto.NewPassportApplicationForm;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model.CitizenApplication;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Service.CitizenApplicationService;
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
public class CitizenApplicationController {

    private final CitizenApplicationService applicationService;

    public CitizenApplicationController(CitizenApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @GetMapping("/citizen/dashboard")
    public String citizenDashboard(HttpSession session, Model model) {
        Integer citizenId = loggedInCitizenId(session);
        if (citizenId == null) return "redirect:/login";

        CitizenEligibilityDto eligibility = applicationService.getEligibility(citizenId);
        model.addAttribute("loggedInUserEmail", session.getAttribute("loggedInUserEmail"));
        model.addAttribute("hasNic", eligibility.isHasNic());
        model.addAttribute("hasPassport", eligibility.isHasPassport());
        model.addAttribute("identityConflict", eligibility.isIdentityConflict());
        model.addAttribute("nicNumber", eligibility.getNicNumber());
        model.addAttribute("passportNumber", eligibility.getPassportNumber());
        model.addAttribute(
                "hasActiveNicApplication",
                applicationService.hasActiveApplication(citizenId, CitizenApplicationService.NIC)
        );
        model.addAttribute(
                "hasActivePassportApplication",
                applicationService.hasActiveApplication(citizenId, CitizenApplicationService.PASSPORT)
        );
        model.addAttribute("applications", applicationService.getCitizenApplications(citizenId));
        return "citizen_frontends/citizen_dashboard";
    }

    @GetMapping("/citizen/applications/nic/new")
    public String newNicForm(
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {
        Integer citizenId = loggedInCitizenId(session);
        if (citizenId == null) return "redirect:/login";

        try {
            model.addAttribute("applicationForm", applicationService.prepareNewNicForm(citizenId));
            addNewFormData(model, "NIC");
            return "citizen_frontends/nic_application_form";
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("applicationError", exception.getMessage());
            return "redirect:/citizen/dashboard";
        }
    }

    @PostMapping("/citizen/applications/nic/new")
    public String saveNewNic(
            @Valid @ModelAttribute("applicationForm") NewNicApplicationForm form,
            BindingResult bindingResult,
            @RequestParam(required = false) Integer applicationId,
            HttpSession session,
            Model model) {
        Integer citizenId = loggedInCitizenId(session);
        if (citizenId == null) return "redirect:/login";
        if (bindingResult.hasErrors()) {
            addNewFormData(model, "NIC");
            addCorrectionFormData(model, applicationId);
            return "citizen_frontends/nic_application_form";
        }

        try {
            CitizenApplication application = applicationId == null
                    ? applicationService.createNewNicApplication(citizenId, form)
                    : applicationService.updateNewNicCorrection(citizenId, applicationId, form);
            return "redirect:/citizen/applications/nic/documents?applicationId="
                    + application.getApplicationId();
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("application.failed", exception.getMessage());
            addNewFormData(model, "NIC");
            addCorrectionFormData(model, applicationId);
            return "citizen_frontends/nic_application_form";
        }
    }

    @GetMapping("/citizen/applications/nic/correction")
    public String correctNewNic(
            @RequestParam Integer applicationId,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        Integer citizenId = loggedInCitizenId(session);
        if (citizenId == null) return "redirect:/login";

        try {
            model.addAttribute("applicationForm", applicationService.prepareNewNicCorrection(citizenId, applicationId));
            addNewFormData(model, "NIC");
            addCorrectionFormData(model, applicationId);
            return "citizen_frontends/nic_application_form";
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("applicationError", exception.getMessage());
            return "redirect:/citizen/dashboard";
        }
    }

    @GetMapping("/citizen/applications/passport/new")
    public String newPassportForm(
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {
        Integer citizenId = loggedInCitizenId(session);
        if (citizenId == null) return "redirect:/login";

        try {
            model.addAttribute("applicationForm", applicationService.prepareNewPassportForm(citizenId));
            addNewFormData(model, "Passport");
            return "citizen_frontends/passport_application_form";
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("applicationError", exception.getMessage());
            return "redirect:/citizen/dashboard";
        }
    }

    @PostMapping("/citizen/applications/passport/new")
    public String saveNewPassport(
            @Valid @ModelAttribute("applicationForm") NewPassportApplicationForm form,
            BindingResult bindingResult,
            @RequestParam(required = false) Integer applicationId,
            HttpSession session,
            Model model) {
        Integer citizenId = loggedInCitizenId(session);
        if (citizenId == null) return "redirect:/login";
        if (bindingResult.hasErrors()) {
            addNewFormData(model, "Passport");
            addCorrectionFormData(model, applicationId);
            return "citizen_frontends/passport_application_form";
        }

        try {
            CitizenApplication application = applicationId == null
                    ? applicationService.createNewPassportApplication(citizenId, form)
                    : applicationService.updateNewPassportCorrection(citizenId, applicationId, form);
            return "redirect:/citizen/applications/passport/documents?applicationId="
                    + application.getApplicationId();
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("application.failed", exception.getMessage());
            addNewFormData(model, "Passport");
            addCorrectionFormData(model, applicationId);
            return "citizen_frontends/passport_application_form";
        }
    }

    @GetMapping("/citizen/applications/passport/correction")
    public String correctNewPassport(
            @RequestParam Integer applicationId,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        Integer citizenId = loggedInCitizenId(session);
        if (citizenId == null) return "redirect:/login";

        try {
            model.addAttribute("applicationForm", applicationService.prepareNewPassportCorrection(citizenId, applicationId));
            addNewFormData(model, "Passport");
            addCorrectionFormData(model, applicationId);
            return "citizen_frontends/passport_application_form";
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("applicationError", exception.getMessage());
            return "redirect:/citizen/dashboard";
        }
    }

    @GetMapping("/citizen/applications/{service:nic|passport}/documents")
    public String newDocumentPage(
            @PathVariable String service,
            @RequestParam Integer applicationId,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {
        Integer citizenId = loggedInCitizenId(session);
        if (citizenId == null) return "redirect:/login";

        try {
            addNewDocumentData(model, citizenId, applicationId, service);
            return "citizen_frontends/application_document_upload";
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("applicationError", exception.getMessage());
            return "redirect:/citizen/dashboard";
        }
    }

    @PostMapping("/citizen/applications/{service:nic|passport}/documents")
    public String uploadNewDocument(
            @PathVariable String service,
            @RequestParam Integer applicationId,
            @RequestParam String documentType,
            @RequestParam("file") MultipartFile file,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        Integer citizenId = loggedInCitizenId(session);
        if (citizenId == null) return "redirect:/login";

        try {
            checkService(applicationService.getNewApplication(citizenId, applicationId), service);
            applicationService.uploadNewDocument(citizenId, applicationId, documentType, file);
            redirectAttributes.addFlashAttribute("documentMessage", "Document uploaded successfully.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("documentError", exception.getMessage());
        }
        return newDocumentRedirect(service, applicationId);
    }

    @GetMapping("/citizen/applications/{service:nic|passport}/review")
    public String reviewNewApplication(
            @PathVariable String service,
            @RequestParam Integer applicationId,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {
        Integer citizenId = loggedInCitizenId(session);
        if (citizenId == null) return "redirect:/login";

        try {
            CitizenApplication application = applicationService.getNewApplication(citizenId, applicationId);
            checkService(application, service);
            model.addAttribute("currentApplication", application);
            model.addAttribute("documents", applicationService.getNewDocuments(citizenId, applicationId));
            model.addAttribute("requiredTypes", applicationService.requiredNewDocumentTypes(application));
            model.addAttribute("servicePath", service);
            model.addAttribute("workflowPath", "applications");
            model.addAttribute("correctionMode", "CORRECTION_REQUIRED".equals(application.getCurrentStatus()));
            applicationService.getNicApplication(applicationId)
                    .ifPresent(value -> model.addAttribute("nicApplication", value));
            applicationService.getPassportApplication(applicationId)
                    .ifPresent(value -> model.addAttribute("passportApplication", value));
            return "citizen_frontends/citizen_application_review";
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("applicationError", exception.getMessage());
            return "redirect:/citizen/dashboard";
        }
    }

    @PostMapping("/citizen/applications/{service:nic|passport}/submit")
    public String submitNewApplication(
            @PathVariable String service,
            @RequestParam Integer applicationId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        Integer citizenId = loggedInCitizenId(session);
        if (citizenId == null) return "redirect:/login";

        try {
            checkService(applicationService.getNewApplication(citizenId, applicationId), service);
            CitizenApplication application = applicationService.submitNewApplication(citizenId, applicationId);
            redirectAttributes.addFlashAttribute(
                    "applicationMessage",
                    "Application " + application.getApplicationNo() + " submitted successfully."
            );
            return "redirect:/citizen/dashboard";
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("documentError", exception.getMessage());
            return newDocumentRedirect(service, applicationId);
        }
    }

    private void addNewFormData(Model model, String service) {
        model.addAttribute("formKind", "new");
        model.addAttribute("applicationKind", "NEW");
        model.addAttribute("pageTitle", "New " + service + " Application");
        model.addAttribute("existingDocumentNo", null);
    }

    private void addNewDocumentData(
            Model model,
            Integer citizenId,
            Integer applicationId,
            String service) {
        CitizenApplication application = applicationService.getNewApplication(citizenId, applicationId);
        checkService(application, service);
        var documents = applicationService.getNewDocuments(citizenId, applicationId);
        model.addAttribute("currentApplication", application);
        model.addAttribute("documents", documents);
        model.addAttribute(
                "uploadedTypes",
                documents.stream().map(document -> document.getDocumentType()).toList()
        );
        model.addAttribute("requiredTypes", applicationService.requiredNewDocumentTypes(application));
        model.addAttribute("servicePath", service);
        model.addAttribute("workflowPath", "applications");
        model.addAttribute("correctionMode", "CORRECTION_REQUIRED".equals(application.getCurrentStatus()));
    }

    private void addCorrectionFormData(Model model, Integer applicationId) {
        if (applicationId != null) {
            model.addAttribute("correctionApplicationId", applicationId);
            model.addAttribute("pageTitle", "Correct Application Details");
        }
    }

    private void checkService(CitizenApplication application, String servicePath) {
        if (!application.getServiceType().equalsIgnoreCase(servicePath)) {
            throw new IllegalArgumentException("Application service type does not match the requested page.");
        }
    }

    private String newDocumentRedirect(String service, Integer applicationId) {
        return "redirect:/citizen/applications/" + service
                + "/documents?applicationId=" + applicationId;
    }

    private Integer loggedInCitizenId(HttpSession session) {
        Object citizenId = session.getAttribute("loggedInUserId");
        Object role = session.getAttribute("loggedInUserRole");
        return citizenId instanceof Integer && "CITIZEN".equals(role) ? (Integer) citizenId : null;
    }
}
