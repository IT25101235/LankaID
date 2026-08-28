package lk.sliit.identityissuanceservice.Admin_Mgmt.Dto;

import java.time.LocalDateTime;

public record DirectorRecentApplicationRow(
        String applicationNo,
        String serviceType,
        String applicationKind,
        String citizenName,
        String currentStatus,
        LocalDateTime createdAt
) {
}
