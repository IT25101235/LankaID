package lk.sliit.identityissuanceservice.Citizen_Support_And_Service_Mgmt.Dto;

import java.math.BigDecimal;

/** Holds the server-verified booking selection shown on the payment step. */
public record WalkInPaymentDetails(String token, String serviceLabel, WalkInSlotView slot, BigDecimal amount) { }
