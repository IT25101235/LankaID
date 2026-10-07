package lk.sliit.identityissuanceservice.Admin_Mgmt.Dto;

public record CitizenAccountRowDto(
        Integer userId,
        String accountId,
        String displayName,
        String email,
        long nicApplicationCount,
        long passportApplicationCount,
        String status,
        String statusLabel,
        boolean active,
        boolean registeredThisMonth
) {
}
