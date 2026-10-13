package lk.sliit.identityissuanceservice.Payment_Mgmt.Service;

import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Model.Appointment;
import lk.sliit.identityissuanceservice.Payment_Mgmt.Dto.CardPaymentForm;
import lk.sliit.identityissuanceservice.Payment_Mgmt.Model.Payment;
import lk.sliit.identityissuanceservice.Payment_Mgmt.Repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class WalkInPaymentService {
    public static final BigDecimal WALK_IN_FEE = new BigDecimal("1500.00");

    private final PaymentRepository paymentRepository;

    public WalkInPaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    // Uses the same simulated payment approach as identity application payments.
    // The enclosing booking transaction saves the appointment and payment together.
    @Transactional
    public Payment pay(Appointment appointment, CardPaymentForm cardForm) {
        if (appointment.getAppointmentId() == null || !"WALK_IN".equals(appointment.getAppointmentType())
                || !"PENDING_PAYMENT".equals(appointment.getAppointmentStatus())) {
            throw new IllegalArgumentException("A pending walk-in appointment is required for payment.");
        }
        CardPaymentValidation.requireValid(cardForm);
        Payment payment = new Payment();
        payment.setAppointmentId(appointment.getAppointmentId());
        payment.setTransactionReference("WALKPAY-" + UUID.randomUUID());
        payment.setAmount(WALK_IN_FEE);
        payment.setPaymentStatus("PAID");
        payment.setPaymentMethod("CARD");
        payment.setCardBrand(CardPaymentValidation.cardBrand(cardForm));
        payment.setCardLastFour(CardPaymentValidation.lastFour(cardForm));
        payment.setCardholderName(cardForm.getCardholderName().trim());
        payment.setCardExpiry(cardForm.getExpiry());
        payment.setPaidAt(LocalDateTime.now());
        return paymentRepository.saveAndFlush(payment);
    }

    @Transactional(readOnly = true)
    public Optional<Payment> findPaidPayment(Integer appointmentId) {
        return paymentRepository.findFirstByAppointmentIdAndPaymentStatusOrderByCreatedAtDesc(
                appointmentId, "PAID");
    }
}
