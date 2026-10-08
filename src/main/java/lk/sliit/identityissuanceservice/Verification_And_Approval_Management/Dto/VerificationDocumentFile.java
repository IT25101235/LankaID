package lk.sliit.identityissuanceservice.Verification_And_Approval_Management.Dto;

import org.springframework.core.io.Resource;

/**
 * Immutable result of an authorized document lookup: file resource plus safe response metadata.
 * The controller can stream this value without knowing filesystem paths or authorization rules.
 */
public record VerificationDocumentFile(
        Resource resource,
        String contentType,
        String fileName
) {
}
