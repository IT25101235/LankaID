package lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Dto.CitizenEligibilityDto;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Dto.NicApplicationForm;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Dto.PassportApplicationForm;
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
        if (citizenId == null) {
            return "redirect:/login";
        }

        CitizenEligibilityDto eligibility = applicationService.getEligibility(citizenId);
        model.addAttribute("loggedInUserEmail", session.getAttribute("loggedInUserEmail"));
        model.addAttribute("hasNic", eligibility.isHasNic());
        model.addAttribute("hasPassport", eligibility.isHasPassport());
        model.addAttribute("identityConflict", eligibility.isIdentityConflict());
        model.addAttribute("nicNumber", eligibility.getNicNumber());
        model.addAttribute("passportNumber", eligibility.getPassportNumber());
        model.addAttribute("applications", applicationService.getCitizenApplications(citizenId));
        return "citizen_frontends/citizen_dashboard";
    }

    @GetMapping("/citizen/applications/nic/{formKind:new|renewal|replacement}")
    public String nicApplicationForm(
            @PathVariable String formKind,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {
        Integer citizenId = loggedInCitizenId(session);
        if (citizenId == null) {
            return "redirect:/login";
        }

        try {
            model.addAttribute("applicationForm", applicationService.prepareNicForm(citizenId, formKind));
            addNicFormData(model, citizenId, formKind);
            return "citizen_frontends/nic_application_form";
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("applicationError", exception.getMessage());
            return "redirect:/citizen/dashboard";
        }
    }

    @PostMapping("/citizen/applications/nic/{formKind:new|renewal|replacement}")
    public String saveNicApplication(
            @PathVariable String formKind,
            @Valid @ModelAttribute("applicationForm") NicApplicationForm applicationForm,
            BindingResult bindingResult,
            HttpSession session,
            Model model) {
        Integer citizenId = loggedInCitizenId(session);
        if (citizenId == null) {
            return "redirect:/login";
        }

        if (bindingResult.hasErrors()) {
            addNicFormData(model, citizenId, formKind);
            return "citizen_frontends/nic_application_form";
        }

        try {
            CitizenApplication application = applicationService.createNicApplication(
                    citizenId,
                    formKind,
                    applicationForm
            );
            return "redirect:/citizen/applications/nic/documents?applicationId="
                    + application.getApplicationId();
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("application.failed", exception.getMessage());
            addNicFormData(model, citizenId, formKind);
            return "citizen_frontends/nic_application_form";
        }
    }

    @GetMapping("/citizen/applications/passport/{formKind:new|renewal|replacement}")
    public String passportApplicationForm(
            @PathVariable String formKind,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {
        Integer citizenId = loggedInCitizenId(session);
        if (citizenId == null) {
            return "redirect:/login";
        }

        try {
            model.addAttribute("applicationForm", applicationService.preparePassportForm(citizenId, formKind));
            addPassportFormData(model, citizenId, formKind);
            return "citizen_frontends/passport_application_form";
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("applicationError", exception.getMessage());
            return "redirect:/citizen/dashboard";
        }
    }

    @PostMapping("/citizen/applications/passport/{formKind:new|renewal|replacement}")
    public String savePassportApplication(
            @PathVariable String formKind,
            @Valid @ModelAttribute("applicationForm") PassportApplicationForm applicationForm,
            BindingResult bindingResult,
            HttpSession session,
            Model model) {
        Integer citizenId = loggedInCitizenId(session);
        if (citizenId == null) {
            return "redirect:/login";
        }

        if (bindingResult.hasErrors()) {
            addPassportFormData(model, citizenId, formKind);
            return "citizen_frontends/passport_application_form";
        }

        try {
            CitizenApplication application = applicationService.createPassportApplication(
                    citizenId,
                    formKind,
                    applicationForm
            );
            return "redirect:/citizen/applications/passport/documents?applicationId="
                    + application.getApplicationId();
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("application.failed", exception.getMessage());
            addPassportFormData(model, citizenId, formKind);
            return "citizen_frontends/passport_application_form";
        }
    }

    @GetMapping("/citizen/applications/{service:nic|passport}/documents")
    public String documentUploadPage(
            @PathVariable String service,
            @RequestParam Integer applicationId,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {
        Integer citizenId = loggedInCitizenId(session);
        if (citizenId == null) {
            return "redirect:/login";
        }

        try {
            addDocumentPageData(model, citizenId, applicationId, service);
            return "citizen_frontends/application_document_upload";
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("applicationError", exception.getMessage());
            return "redirect:/citizen/dashboard";
        }
    }

    @PostMapping("/citizen/applications/{service:nic|passport}/documents")
    public String uploadDocument(
            @PathVariable String service,
            @RequestParam Integer applicationId,
            @RequestParam String documentType,
            @RequestParam("file") MultipartFile file,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        Integer citizenId = loggedInCitizenId(session);
        if (citizenId == null) {
            return "redirect:/login";
        }

        try {
            checkService(applicationService.getApplication(citizenId, applicationId), service);
            applicationService.uploadDocument(citizenId, applicationId, documentType, file);
            redirectAttributes.addFlashAttribute("documentMessage", "Document uploaded successfully.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("documentError", exception.getMessage());
        }
        return "redirect:/citizen/applications/" + service + "/documents?applicationId=" + applicationId;
    }

    @GetMapping("/citizen/applications/{service:nic|passport}/review")
    public String reviewApplication(
            @PathVariable String service,
            @RequestParam Integer applicationId,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {
        Integer citizenId = loggedInCitizenId(session);
        if (citizenId == null) {
            return "redirect:/login";
        }

        try {
            CitizenApplication application = applicationService.getApplication(citizenId, applicationId);
            checkService(application, service);
            model.addAttribute("application", application);
            model.addAttribute("documents", applicationService.getDocuments(citizenId, applicationId));
            model.addAttribute("requiredTypes", applicationService.requiredDocumentTypes(application));
            model.addAttribute("servicePath", service);
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
    public String submitApplication(
            @PathVariable String service,
            @RequestParam Integer applicationId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        Integer citizenId = loggedInCitizenId(session);
        if (citizenId == null) {
            return "redirect:/login";
        }

        try {
            checkService(applicationService.getApplication(citizenId, applicationId), service);
            CitizenApplication application = applicationService.submitApplication(citizenId, applicationId);
            redirectAttributes.addFlashAttribute(
                    "applicationMessage",
                    "Application " + application.getApplicationNo() + " submitted successfully."
            );
            return "redirect:/citizen/dashboard";
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("documentError", exception.getMessage());
            return "redirect:/citizen/applications/" + service + "/documents?applicationId=" + applicationId;
        }
    }

    private void addNicFormData(Model model, Integer citizenId, String formKind) {
        CitizenEligibilityDto eligibility = applicationService.getEligibility(citizenId);
        model.addAttribute("formKind", formKind);
        model.addAttribute("applicationKind", applicationKind(formKind));
        model.addAttribute("pageTitle", title("NIC", formKind));
        model.addAttribute("existingDocumentNo", eligibility.getNicNumber());
    }

    private void addPassportFormData(Model model, Integer citizenId, String formKind) {
        CitizenEligibilityDto eligibility = applicationService.getEligibility(citizenId);
        model.addAttribute("formKind", formKind);
        model.addAttribute("applicationKind", applicationKind(formKind));
        model.addAttribute("pageTitle", title("Passport", formKind));
        model.addAttribute("existingDocumentNo", eligibility.getPassportNumber());
    }

    private void addDocumentPageData(Model model, Integer citizenId, Integer applicationId, String service) {
        CitizenApplication application = applicationService.getApplication(citizenId, applicationId);
        checkService(application, service);
        model.addAttribute("application", application);
        model.addAttribute("documents", applicationService.getDocuments(citizenId, applicationId));
        model.addAttribute("requiredTypes", applicationService.requiredDocumentTypes(application));
        model.addAttribute("servicePath", service);
    }

    private void checkService(CitizenApplication application, String servicePath) {
        if (!application.getServiceType().equalsIgnoreCase(servicePath)) {
            throw new IllegalArgumentException("Application service type does not match the requested page.");
        }
    }

    private String applicationKind(String formKind) {
        return switch (formKind) {
            case "new" -> "NEW";
            case "renewal" -> "RENEWAL";
            case "replacement" -> "LOST";
            default -> throw new IllegalArgumentException("Invalid application type.");
        };
    }

    private String title(String service, String formKind) {
        return switch (formKind) {
            case "new" -> "New " + service + " Application";
            case "renewal" -> service + " Renewal Application";
            case "replacement" -> "Lost " + service + " Replacement";
            default -> service + " Application";
        };
    }

    private Integer loggedInCitizenId(HttpSession session) {
        Object citizenId = session.getAttribute("loggedInUserId");
        Object role = session.getAttribute("loggedInUserRole");
        return citizenId instanceof Integer && "CITIZEN".equals(role) ? (Integer) citizenId : null;
    }
}
