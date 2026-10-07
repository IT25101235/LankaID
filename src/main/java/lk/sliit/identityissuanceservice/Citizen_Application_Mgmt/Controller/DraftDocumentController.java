package lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Controller;

import lk.sliit.identityissuanceservice.User_Mgmt.Controller.DeletionFormToken;

import jakarta.servlet.http.HttpSession;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Service.CitizenApplicationService;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.Locale;

@Controller
public class DraftDocumentController {
    private final CitizenApplicationService applications;
    public DraftDocumentController(CitizenApplicationService applications) { this.applications = applications; }

    @PostMapping("/citizen/applications/{applicationId}/documents/{documentId}/delete")
    public String delete(@PathVariable Integer applicationId, @PathVariable Integer documentId,
            @RequestParam(required = false) String deleteToken, HttpSession session, RedirectAttributes redirect) {
        if (!(session.getAttribute("loggedInUserId") instanceof Integer citizenId)
                || !"CITIZEN".equals(session.getAttribute("loggedInUserRole"))) return "redirect:/login";
        try {
            DeletionFormToken.check(deleteToken, session);
            var application = applications.deleteDraftDocument(citizenId, applicationId, documentId);
            redirect.addFlashAttribute("documentMessage", "Document deleted. Upload a replacement before submitting if it is required.");
            String workflow = "NEW".equals(application.getApplicationKind()) ? "applications" : "renewals";
            return "redirect:/citizen/" + workflow + "/" + application.getServiceType().toLowerCase(Locale.ROOT)
                    + "/documents?applicationId=" + applicationId;
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("applicationError", exception.getMessage());
        } catch (DataAccessException exception) {
            redirect.addFlashAttribute("applicationError", "The document could not be deleted. Reload the application and try again.");
        }
        return "redirect:/citizen/applications";
    }
}
