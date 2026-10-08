package lk.sliit.identityissuanceservice.Document_Renewal_Replacement_And_Delivery_Mgmt.Dto;

import lk.sliit.identityissuanceservice.Document_Renewal_Replacement_And_Delivery_Mgmt.Model.DocumentDelivery;

// View data for citizen tracking: delivery record with readable collection centre details.
public record CitizenDeliveryDetails(
        DocumentDelivery delivery,
        String centreName,
        String centreAddress
) {
}
