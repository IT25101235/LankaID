package lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Controller;

import jakarta.servlet.http.HttpSession;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Dto.CitizenDocumentFile;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Service.CitizenApplicationService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/** Streams a document inline only to the citizen who owns its application. */
@Controller
public class CitizenDocumentPreviewController {

    private final CitizenApplicationService applicationService;

    public CitizenDocumentPreviewController(CitizenApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @GetMapping("/citizen/documents/{documentId}/view")
    public ResponseEntity<?> viewDocument(@PathVariable Integer documentId, HttpSession session) {
        Object userId = session.getAttribute("loggedInUserId");
        if (!(userId instanceof Integer citizenId)) {
            return ResponseEntity.status(403).build();
        }

        try {
            CitizenDocumentFile document = applicationService.getDocumentForCitizen(documentId, citizenId);
            ContentDisposition disposition = ContentDisposition.inline().filename(document.fileName()).build();
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(document.contentType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                    .header("X-Content-Type-Options", "nosniff")
                    .body(document.resource());
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.notFound().build();
        }
    }
}
