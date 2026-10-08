package lk.sliit.identityissuanceservice.Verification_And_Approval_Management.Controller;

import jakarta.servlet.http.HttpSession;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Service.OfficerAccountService;
import lk.sliit.identityissuanceservice.Verification_And_Approval_Management.Dto.VerificationQueueItem;
import lk.sliit.identityissuanceservice.Verification_And_Approval_Management.Dto.VerificationReviewDetails;
import lk.sliit.identityissuanceservice.Verification_And_Approval_Management.Service.VerificationQueueService;
import lk.sliit.identityissuanceservice.Verification_And_Approval_Management.Service.VerificationReviewService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * MVC controller for the NIC verification workflow.
 *
 * <p>The controller is responsible for HTTP/session concerns: it checks the officer role,
 * prepares data for Thymeleaf, and converts service errors into flash messages. The actual
 * verification rules and database updates remain encapsulated in the service layer.</p>
 *
 * <p>OOP concepts: composition is used through constructor-injected services, while the
 * controller depends on service abstractions instead of creating repository objects itself.</p>
 */
@Controller
public class NicVerificationController {

    private final VerificationQueueService verificationQueueService;
    private final VerificationReviewService verificationReviewService;

    public NicVerificationController(
            VerificationQueueService verificationQueueService,
            VerificationReviewService verificationReviewService
    ) {
        this.verificationQueueService = verificationQueueService;
        this.verificationReviewService = verificationReviewService;
    }

    @GetMapping("/officer/nic/applications")
    public String nicApplicationQueue(HttpSession session, Model model) {
        String accessResult = checkAccess(session);
        if (accessResult != null) {
            return accessResult;
        }

        List<VerificationQueueItem> queueItems = verificationQueueService.getNicQueue();
        addLoggedInOfficer(model, session);
        model.addAttribute("queueItems", queueItems);
        model.addAttribute("queueCount", queueItems.size());
        return "officers_frontends/nic_verification_queue";
    }

    @GetMapping("/officer/nic/applications/{reference}")
    public String reviewNicApplication(
            @PathVariable String reference,
            Model model,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        // Every decision passes through the same authorization and error-handling path.
        String accessResult = checkAccess(session);
        if (accessResult != null) {
            return accessResult;
        }

        try {
            addReviewData(model, verificationReviewService.getReviewDetails(reference, "NIC"));
            addOfficerPageData(model, "NIC", "/officer/nic/applications", "/officer/nic/biometrics");
            return "officers_frontends/application_review";
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("verificationError", exception.getMessage());
            return "redirect:/officer/nic/applications";
        }
    }

    @PostMapping("/officer/nic/applications/{reference}/review")
    public String startNicReview(
            @PathVariable String reference,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        String accessResult = checkAccess(session);
        if (accessResult != null) return accessResult;

        try {
            verificationReviewService.startReview(reference, "NIC");
            return "redirect:/officer/nic/applications/" + reference;
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("verificationError", exception.getMessage());
            return "redirect:/officer/nic/applications";
        }
    }

    @PostMapping("/officer/nic/applications/{reference}/approve")
    public String approveNic(
            @PathVariable String reference,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        return processDecision(reference, session, redirectAttributes, "approve", false, List.of());
    }

    @PostMapping("/officer/nic/applications/{reference}/reject")
    public String rejectNic(
            @PathVariable String reference,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        return processDecision(reference, session, redirectAttributes, "reject", false, List.of());
    }

    @PostMapping("/officer/nic/applications/{reference}/correction")
    public String requestNicCorrection(
            @PathVariable String reference,
            @RequestParam(defaultValue = "false") boolean correctForm,
            @RequestParam(required = false) List<Integer> documentIds,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        return processDecision(reference, session, redirectAttributes, "correction", correctForm, documentIds);
    }

    private String processDecision(
            String reference,
            HttpSession session,
            RedirectAttributes redirectAttributes,
            String decision,
            boolean correctForm,
            List<Integer> documentIds
    ) {
        String accessResult = checkAccess(session);
        if (accessResult != null) return accessResult;

        try {
            Integer staffId = session.getAttribute("loggedInOfficerId") instanceof Integer id ? id : null;
            // Delegate the business state transition to the transactional service.
            if ("approve".equals(decision)) {
                verificationReviewService.approve(reference, "NIC", staffId);
                redirectAttributes.addFlashAttribute("verificationMessage", "Application approved successfully.");
            } else if ("reject".equals(decision)) {
                verificationReviewService.reject(reference, "NIC", staffId);
                redirectAttributes.addFlashAttribute("verificationMessage", "Application rejected.");
            } else {
                verificationReviewService.requestCorrection(reference, "NIC", correctForm, documentIds, staffId);
                redirectAttributes.addFlashAttribute("verificationMessage", "Correction requested from the citizen.");
            }
            return "redirect:/officer/nic/applications";
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("decisionError", exception.getMessage());
            return "redirect:/officer/nic/applications/" + reference;
        }
    }

    private void addReviewData(Model model, VerificationReviewDetails details) {
        model.addAttribute("reviewApplication", details.reviewApplication());
        model.addAttribute("citizen", details.citizen());
        model.addAttribute("userAccount", details.userAccount());
        model.addAttribute("nicApplication", details.nicApplication());
        model.addAttribute("passportApplication", details.passportApplication());
        model.addAttribute("documents", details.documents());
        model.addAttribute("applicantName", details.applicantName());
        model.addAttribute("permanentAddress", details.permanentAddress());
        model.addAttribute("decisionAllowed", "UNDER_REVIEW".equals(details.reviewApplication().getCurrentStatus()));
    }

    private String checkAccess(HttpSession session) {
        // System administrators use their own dashboard and cannot act as verification officers.
        if (session.getAttribute("loggedInAdminId") != null) {
            return "redirect:/officer/admin";
        }

        Object role = session.getAttribute("loggedInOfficerRole");
        // A director may supervise both queues; a normal officer is restricted to the NIC queue.
        boolean allowed = OfficerAccountService.NIC_OFFICER.equals(role)
                || OfficerAccountService.DIRECTOR.equals(role);
        return allowed ? null : "redirect:/login";
    }

    private void addOfficerPageData(Model model, String documentType, String queueRoute, String biometricRoute) {
        model.addAttribute("documentType", documentType);
        model.addAttribute("queueRoute", queueRoute);
        model.addAttribute("biometricRoute", biometricRoute);
    }

    private void addLoggedInOfficer(Model model, HttpSession session) {
        model.addAttribute("loggedInOfficerRoleLabel", session.getAttribute("loggedInOfficerRoleLabel"));
        model.addAttribute("loggedInOfficerEmployeeNo", session.getAttribute("loggedInOfficerEmployeeNo"));
        model.addAttribute("loggedInOfficerEmail", session.getAttribute("loggedInOfficerEmail"));
    }
}
