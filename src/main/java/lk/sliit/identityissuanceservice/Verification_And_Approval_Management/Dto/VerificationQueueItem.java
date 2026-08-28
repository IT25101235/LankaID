package lk.sliit.identityissuanceservice.Verification_And_Approval_Management.Dto;

import java.time.LocalDateTime;

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
