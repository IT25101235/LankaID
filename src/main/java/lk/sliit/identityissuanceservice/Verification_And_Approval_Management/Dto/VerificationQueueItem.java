package lk.sliit.identityissuanceservice.Verification_And_Approval_Management.Dto;

import java.time.LocalDateTime;

/**
 * Immutable projection used by NIC and passport queue templates.
 * It exposes only the fields needed by the UI instead of passing persistence entities directly.
 */
public record VerificationQueueItem(
        Integer applicationId,
        String applicationNo,
        String applicantName,
        String applicantInitials,
        LocalDateTime dateOfBirth,
        String applicationKindLabel,
        LocalDateTime submittedAt,
        String currentStatus,
        String statusLabel
) {
}
