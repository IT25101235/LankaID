package lk.sliit.identityissuanceservice.Admin_Mgmt.Dto;

import java.util.List;

public record CitizenAccountDashboardDto(
        List<CitizenAccountRowDto> citizens,
        long citizenAccountCount,
        long registeredThisMonthCount,
        long suspendedCitizenCount
) {
}
