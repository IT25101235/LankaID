package lk.sliit.identityissuanceservice.Citizen_Support_And_Service_Mgmt.Dto;

import java.math.BigDecimal;

public record WalkInPaymentDetails(String token, String serviceLabel, WalkInSlotView slot, BigDecimal amount) { }
