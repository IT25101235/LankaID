package lk.sliit.identityissuanceservice.Payment_Mgmt.Dto;

import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Model.Centre;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model.CitizenApplication;
import lk.sliit.identityissuanceservice.Document_Delivery_Mgmt.Model.DocumentDelivery;
import lk.sliit.identityissuanceservice.Payment_Mgmt.Model.Payment;

import java.math.BigDecimal;
import java.util.List;

public record PaymentSummary(
        CitizenApplication application,
        BigDecimal amount,
        BigDecimal homeDeliveryFee,
        Payment payment,
        DocumentDelivery delivery,
        List<Centre> centres,
        String homeAddress
) {
    public boolean paid() {
        return payment != null && "PAID".equals(payment.getPaymentStatus());
    }

    public boolean deliverySelected() {
        return delivery != null;
    }
}
