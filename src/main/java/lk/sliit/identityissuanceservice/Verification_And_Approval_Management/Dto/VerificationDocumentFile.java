package lk.sliit.identityissuanceservice.Verification_And_Approval_Management.Dto;

import org.springframework.core.io.Resource;

public record VerificationDocumentFile(
        Resource resource,
        String contentType,
        String fileName
) {
}
