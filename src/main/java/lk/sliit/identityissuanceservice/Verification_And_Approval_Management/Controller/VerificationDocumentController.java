package lk.sliit.identityissuanceservice.Verification_And_Approval_Management.Controller;

import jakarta.servlet.http.HttpSession;
import lk.sliit.identityissuanceservice.Verification_And_Approval_Management.Dto.VerificationDocumentFile;
import lk.sliit.identityissuanceservice.Verification_And_Approval_Management.Service.VerificationReviewService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Streams an uploaded verification document only after the officer role has been authorized.
 * The service resolves and validates the physical path; this controller only builds the HTTP
 * response and deliberately returns no document body when validation fails.
 */
@Controller
public class VerificationDocumentController {

    private final VerificationReviewService reviewService;

    public VerificationDocumentController(VerificationReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/officer/verification/documents/{documentId}/view")
    public ResponseEntity<?> viewDocument(@PathVariable Integer documentId, HttpSession session) {
        // Reject anonymous sessions before looking up any document information.
        Object role = session.getAttribute("loggedInOfficerRole");
        if (!(role instanceof String officerRole)) {
            return ResponseEntity.status(403).build();
        }

        try {
            // Service-level authorization also checks that the role matches NIC or passport type.
            VerificationDocumentFile document = reviewService.getDocumentForOfficer(documentId, officerRole);
            ContentDisposition disposition = ContentDisposition.inline()
                    .filename(document.fileName())
                    .build();

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(document.contentType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                    .body(document.resource());
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.notFound().build();
        }
    }
}
