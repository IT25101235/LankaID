package lk.sliit.identityissuanceservice.Payment_Mgmt.Dto;

import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model.CitizenApplication;
import lk.sliit.identityissuanceservice.Payment_Mgmt.Model.Payment;

import java.math.BigDecimal;

public record PaymentSummary(
        CitizenApplication application,
        BigDecimal amount,
        Payment payment
) {
    public boolean paid() {
        return payment != null && "PAID".equals(payment.getPaymentStatus());
    }
}
