package lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Service.IdentityLinkService;
import lk.sliit.identityissuanceservice.Citizen_Support_And_Service_Mgmt.Service.CitizenSupportService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
// Handles citizen OTP linking and officer review of existing identity records.
public class IdentityLinkController {
    // Services used for identity records and related support requests.
    private final IdentityLinkService identityService;
    private final CitizenSupportService supportService;

    /** Receives the services used for identity linking and CSR support-request access. */
    public IdentityLinkController(IdentityLinkService identityService, CitizenSupportService supportService) {
        this.identityService = identityService;
        this.supportService = supportService;
    }

    /** Displays the citizen identity-linking page and its latest verification state. */
    @GetMapping("/citizen/identity")
    public String page(HttpSession session, Model model, HttpServletResponse response) {
        Integer citizenId = citizenId(session);
        if (citizenId == null) return "redirect:/login";
        response.setHeader("Cache-Control", "no-store");
        model.addAttribute("linked", identityService.isLinked(citizenId));
        model.addAttribute("identityRequest", identityService.latestRequest(citizenId));
        model.addAttribute("maskedPhone", identityService.maskedPhone(citizenId, session));
        model.addAttribute("identityToken", token(session));
        return "citizen_frontends/citizen_identity_link";
    }

    /** Starts an identity-link request using either OTP verification or officer review. */
    @PostMapping("/citizen/identity/request")
    public String request(@RequestParam String type, @RequestParam(defaultValue = "") String number,
            @RequestParam String explanation, @RequestParam String action, @RequestParam String identityToken,
            HttpSession session, RedirectAttributes flash) {
        Integer citizenId = citizenId(session);
        if (citizenId == null) return "redirect:/login";
        checkToken(identityToken, session);
        try {
            if (!"OTP".equals(action) && !"OFFICER".equals(action)) throw new IllegalArgumentException("Choose a verification method.");
            identityService.cancelCode(session);
            identityService.requestVerification(citizenId, type, number, explanation);
            if ("OTP".equals(action)) {
                identityService.sendCode(citizenId, type, number, session);
                flash.addFlashAttribute("identityMessage", "Verification code sent to the phone held in the government record.");
            } else {
                flash.addFlashAttribute("identityMessage", "Request saved. A CSR officer will contact you through Citizen Support to arrange verification.");
            }
        } catch (IllegalArgumentException exception) {
            flash.addFlashAttribute("identityError", exception.getMessage());
        }
        return "redirect:/citizen/identity";
    }

    /** Verifies the submitted OTP and links the matching government identity record. */
    @PostMapping("/citizen/identity/verify")
    public String verify(@RequestParam String code, @RequestParam String identityToken,
            HttpSession session, RedirectAttributes flash) {
        Integer citizenId = citizenId(session);
        if (citizenId == null) return "redirect:/login";
        checkToken(identityToken, session);
        try {
            identityService.verifyCode(citizenId, code, session);
            flash.addFlashAttribute("applicationMessage", "Existing documents linked successfully. You can now select the appropriate renewal or replacement service.");
            return "redirect:/citizen/dashboard";
        } catch (IllegalArgumentException exception) {
            flash.addFlashAttribute("identityError", exception.getMessage());
            return "redirect:/citizen/identity";
        }
    }

    /** Displays the CSR identity-review page for an authorized officer. */
    @GetMapping("/officer/csr/identity/{requestId}")
    public String reviewPage(@PathVariable Integer requestId, @RequestParam(required = false) Integer recordId, HttpSession session, Model model,
            HttpServletResponse response) {
        if (officerId(session) == null) return "redirect:/login";
        response.setHeader("Cache-Control", "no-store");
        model.addAttribute("supportRequest", supportService.getCsrRequest(requestId.toString()));
        model.addAttribute("identityToken", token(session));
        if (recordId != null) {
            try {
                model.addAttribute("governmentRecord", identityService.reviewRecord(officerId(session), recordId));
            } catch (IllegalArgumentException exception) {
                model.addAttribute("identityError", exception.getMessage());
            }
        }
        return "officers_frontends/csr_identity_verification";
    }

    /** Validates and saves a CSR officer's approval or rejection of an identity-link request. */
    @PostMapping("/officer/csr/identity/{requestId}")
    public String review(@PathVariable Integer requestId, @RequestParam String decision,
            @RequestParam(required = false) Integer recordId, @RequestParam String notes,
            @RequestParam(defaultValue = "false") boolean evidenceChecked, @RequestParam String identityToken,
            HttpSession session, RedirectAttributes flash) {
        Integer officerId = officerId(session);
        if (officerId == null) return "redirect:/login";
        checkToken(identityToken, session);
        try {
            if (!"APPROVE".equals(decision) && !"REJECT".equals(decision)) throw new IllegalArgumentException("Select a decision.");
            if ("APPROVE".equals(decision) && !evidenceChecked) {
                throw new IllegalArgumentException("Confirm that the citizen's identity and government record were checked.");
            }
            identityService.review(requestId, officerId, "APPROVE".equals(decision), recordId, notes);
            flash.addFlashAttribute("supportMessage", "Identity decision saved.");
            return "redirect:/officer/csr/support/" + requestId;
        } catch (IllegalArgumentException exception) {
            flash.addFlashAttribute("identityError", exception.getMessage());
            return "redirect:/officer/csr/identity/" + requestId;
        }
    }

    /** Returns the citizen ID only when the current session has the CITIZEN role. */
    private Integer citizenId(HttpSession session) {
        return "CITIZEN".equals(session.getAttribute("loggedInUserRole"))
                && session.getAttribute("loggedInUserId") instanceof Integer id ? id : null;
    }

    /** Returns the officer ID only for CSR or DIRECTOR sessions. */
    private Integer officerId(HttpSession session) {
        Object role = session.getAttribute("loggedInOfficerRole");
        return ("CSR".equals(role) || "DIRECTOR".equals(role))
                && session.getAttribute("loggedInOfficerId") instanceof Integer id ? id : null;
    }

    /** Creates or returns the session token that prevents cross-site identity-change submissions. */
    private String token(HttpSession session) {
        if (session.getAttribute("identityFormToken") == null) session.setAttribute("identityFormToken", UUID.randomUUID().toString());
        return (String) session.getAttribute("identityFormToken");
    }

    /** Rejects an identity-changing request when its submitted form token does not match the session. */
    private void checkToken(String value, HttpSession session) {
        if (value == null || !value.equals(session.getAttribute("identityFormToken"))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Reload the identity page and try again.");
        }
    }
}
