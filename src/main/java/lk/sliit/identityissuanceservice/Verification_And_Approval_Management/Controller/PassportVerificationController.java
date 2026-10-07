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

@Controller
public class PassportVerificationController {

    private final VerificationQueueService verificationQueueService;
    private final VerificationReviewService verificationReviewService;

    public PassportVerificationController(
            VerificationQueueService verificationQueueService,
            VerificationReviewService verificationReviewService
    ) {
        this.verificationQueueService = verificationQueueService;
        this.verificationReviewService = verificationReviewService;
    }

    @GetMapping("/officer/passport/applications")
    public String passportApplicationQueue(HttpSession session, Model model) {
        String accessResult = checkAccess(session);
        if (accessResult != null) {
            return accessResult;
        }

        List<VerificationQueueItem> queueItems = verificationQueueService.getPassportQueue();
        addLoggedInOfficer(model, session);
        model.addAttribute("queueItems", queueItems);
        model.addAttribute("queueCount", queueItems.size());
        return "officers_frontends/passport_verification_queue";
    }

    @GetMapping("/officer/passport/applications/{reference}")
    public String reviewPassportApplication(
            @PathVariable String reference,
            Model model,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        String accessResult = checkAccess(session);
        if (accessResult != null) {
            return accessResult;
        }

        try {
            addReviewData(model, verificationReviewService.getReviewDetails(reference, "PASSPORT"));
            addOfficerPageData(model, "Passport", "/officer/passport/applications", "/officer/passport/biometrics");
            return "officers_frontends/application_review";
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("verificationError", exception.getMessage());
            return "redirect:/officer/passport/applications";
        }
    }

    @PostMapping("/officer/passport/applications/{reference}/review")
    public String startPassportReview(
            @PathVariable String reference,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        String accessResult = checkAccess(session);
        if (accessResult != null) return accessResult;

        try {
            verificationReviewService.startReview(reference, "PASSPORT");
            return "redirect:/officer/passport/applications/" + reference;
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("verificationError", exception.getMessage());
            return "redirect:/officer/passport/applications";
        }
    }

    @PostMapping("/officer/passport/applications/{reference}/approve")
    public String approvePassport(
            @PathVariable String reference,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        return processDecision(reference, session, redirectAttributes, "approve", false, List.of());
    }

    @PostMapping("/officer/passport/applications/{reference}/reject")
    public String rejectPassport(
            @PathVariable String reference,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        return processDecision(reference, session, redirectAttributes, "reject", false, List.of());
    }

    @PostMapping("/officer/passport/applications/{reference}/correction")
    public String requestPassportCorrection(
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
            if ("approve".equals(decision)) {
                verificationReviewService.approve(reference, "PASSPORT", staffId);
                redirectAttributes.addFlashAttribute("verificationMessage", "Application approved successfully.");
            } else if ("reject".equals(decision)) {
                verificationReviewService.reject(reference, "PASSPORT", staffId);
                redirectAttributes.addFlashAttribute("verificationMessage", "Application rejected.");
            } else {
                verificationReviewService.requestCorrection(reference, "PASSPORT", correctForm, documentIds, staffId);
                redirectAttributes.addFlashAttribute("verificationMessage", "Correction requested from the citizen.");
            }
            return "redirect:/officer/passport/applications";
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("decisionError", exception.getMessage());
            return "redirect:/officer/passport/applications/" + reference;
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
        if (session.getAttribute("loggedInAdminId") != null) {
            return "redirect:/officer/admin";
        }

        Object role = session.getAttribute("loggedInOfficerRole");
        boolean allowed = OfficerAccountService.PASSPORT_OFFICER.equals(role)
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
