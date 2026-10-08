package lk.sliit.identityissuanceservice.Document_Renewal_Replacement_And_Delivery_Mgmt.Dto;

import lk.sliit.identityissuanceservice.Document_Renewal_Replacement_And_Delivery_Mgmt.Model.DocumentDelivery;

// View data for the collection page, combining delivery, citizen, application, and document details.
public record CsrCollectionDetails(
        DocumentDelivery delivery,
        String applicationNo,
        String serviceType,
        String applicationKind,
        String citizenName,
        String citizenEmail,
        String citizenPhone,
        String documentNumber,
        String centreName,
        String centreAddress
) {
}
