package lk.sliit.identityissuanceservice.Verification_And_Approval_Management.Controller;

import jakarta.servlet.http.HttpSession;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Service.OfficerAccountService;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model.CitizenApplication;
import lk.sliit.identityissuanceservice.Verification_And_Approval_Management.Dto.VerificationReviewDetails;
import lk.sliit.identityissuanceservice.Verification_And_Approval_Management.Model.DirectorReviewReferral;
import lk.sliit.identityissuanceservice.Verification_And_Approval_Management.Service.DirectorReviewReferralService;
import lk.sliit.identityissuanceservice.Verification_And_Approval_Management.Service.VerificationReviewService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Locale;

/** HTTP endpoints for the complete Director-referral lifecycle. */
@Controller
public class DirectorReviewReferralController {

    private final DirectorReviewReferralService referralService;
    private final VerificationReviewService reviewService;

    public DirectorReviewReferralController(
            DirectorReviewReferralService referralService,
            VerificationReviewService reviewService
    ) {
        this.referralService = referralService;
        this.reviewService = reviewService;
    }

    @PostMapping("/officer/{service}/applications/{reference}/director-referral")
    public String referToDirector(
            @PathVariable String service,
            @PathVariable String reference,
            @RequestParam String category,
            @RequestParam String reason,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        String redirect = reviewRedirect(service, reference);
        try {
            referralService.refer(
                    reference,
                    normalizeService(service),
                    officerId(session),
                    (String) session.getAttribute("loggedInOfficerRole"),
                    category,
                    reason
            );
            redirectAttributes.addFlashAttribute(
                    "verificationMessage", "Application sent to the Director for an additional recommendation."
            );
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("decisionError", exception.getMessage());
        }
        return redirect;
    }

    @PostMapping("/officer/{service}/applications/{reference}/director-referral/{referralId}/delete")
    public String withdrawReferral(
            @PathVariable String service,
            @PathVariable String reference,
            @PathVariable Integer referralId,
            @RequestParam String withdrawalReason,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        String redirect = reviewRedirect(service, reference);
        try {
            referralService.withdraw(
                    referralId,
                    reference,
                    normalizeService(service),
                    officerId(session),
                    (String) session.getAttribute("loggedInOfficerRole"),
                    withdrawalReason
            );
            redirectAttributes.addFlashAttribute(
                    "verificationMessage", "Director referral withdrawn. The audit record was preserved."
            );
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("decisionError", exception.getMessage());
        }
        return redirect;
    }

    @GetMapping("/officer/director/referrals")
    public String directorQueue(HttpSession session, Model model) {
        if (!isDirector(session)) return "redirect:/login";
        model.addAttribute("referrals", referralService.getPendingQueue());
        addLoggedInOfficer(model, session);
        return "officers_frontends/director_referrals";
    }

    @GetMapping("/officer/director/referrals/{referralId}")
    public String reviewReferral(
            @PathVariable Integer referralId,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (!isDirector(session)) return "redirect:/login";
        try {
            DirectorReviewReferral referral = referralService.getPendingReferral(referralId);
            CitizenApplication application = referralService.getApplication(referral);
            VerificationReviewDetails details = reviewService.getReviewDetails(
                    application.getApplicationNo(), application.getServiceType());
            addReviewData(model, details);
            model.addAttribute("directorReferral", referral);
            model.addAttribute("directorMode", true);
            model.addAttribute("canReferToDirector", false);
            model.addAttribute("decisionAllowed", false);
            model.addAttribute("referralPending", true);
            model.addAttribute("documentType", application.getServiceType());
            model.addAttribute("queueRoute", "/officer/director/referrals");
            addLoggedInOfficer(model, session);
            return "officers_frontends/application_review";
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("referralError", exception.getMessage());
            return "redirect:/officer/director/referrals";
        }
    }

    @PostMapping("/officer/director/referrals/{referralId}/respond")
    public String respond(
            @PathVariable Integer referralId,
            @RequestParam String recommendation,
            @RequestParam String comment,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        if (!isDirector(session)) return "redirect:/login";
        try {
            referralService.respond(referralId, officerId(session), recommendation, comment);
            redirectAttributes.addFlashAttribute(
                    "referralMessage", "Recommendation returned to the application officer."
            );
            return "redirect:/officer/director/referrals";
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("decisionError", exception.getMessage());
            return "redirect:/officer/director/referrals/" + referralId;
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
    }

    private String normalizeService(String service) {
        String normalized = service == null ? "" : service.trim().toUpperCase(Locale.ROOT);
        if (!VerificationReviewService.NIC.equals(normalized)
                && !VerificationReviewService.PASSPORT.equals(normalized)) {
            throw new IllegalArgumentException("Invalid verification service.");
        }
        return normalized;
    }

    private String reviewRedirect(String service, String reference) {
        String safeService = service == null ? "" : service.toLowerCase(Locale.ROOT);
        if (!"nic".equals(safeService) && !"passport".equals(safeService)) return "redirect:/login";
        return "redirect:/officer/" + safeService + "/applications/" + reference;
    }

    private Integer officerId(HttpSession session) {
        return session.getAttribute("loggedInOfficerId") instanceof Integer id ? id : null;
    }

    private boolean isDirector(HttpSession session) {
        return session.getAttribute("loggedInAdminId") == null
                && OfficerAccountService.DIRECTOR.equals(session.getAttribute("loggedInOfficerRole"));
    }

    private void addLoggedInOfficer(Model model, HttpSession session) {
        model.addAttribute("loggedInOfficerRoleLabel", session.getAttribute("loggedInOfficerRoleLabel"));
        model.addAttribute("loggedInOfficerEmployeeNo", session.getAttribute("loggedInOfficerEmployeeNo"));
        model.addAttribute("loggedInOfficerEmail", session.getAttribute("loggedInOfficerEmail"));
    }
}
