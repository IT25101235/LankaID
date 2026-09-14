package lk.sliit.identityissuanceservice.Document_Renewal_Replacement_And_Delivery_Mgmt.Dto;

import lk.sliit.identityissuanceservice.Document_Renewal_Replacement_And_Delivery_Mgmt.Model.DocumentDelivery;

public record CitizenDeliveryDetails(
        DocumentDelivery delivery,
        String centreName,
        String centreAddress
) {
}
