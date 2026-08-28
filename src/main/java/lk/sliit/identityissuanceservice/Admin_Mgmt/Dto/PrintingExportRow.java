package lk.sliit.identityissuanceservice.Admin_Mgmt.Dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PrintingExportRow(
        Integer applicationId,
        String applicationNo,
        String serviceType,
        String applicationKind,
        String citizenName,
        LocalDate dateOfBirth,
        String gender,
        String permanentAddress,
        String email,
        String phone,
        String photographPath,
        String documentNumber,
        LocalDateTime readyAt
) {
}
