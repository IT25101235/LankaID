package lk.sliit.identityissuanceservice.Citizen_Support_And_Service_Mgmt.Dto;

import lk.sliit.identityissuanceservice.Citizen_Support_And_Service_Mgmt.Model.SupportRequest;

/** Combines the request with citizen and handling-officer details needed by the UI. */
public record SupportRequestView(
        SupportRequest request,
        String reference,
        String citizenName,
        String citizenEmail,
        String citizenPhone,
        String handledByEmployeeNo
) {
}
