package lk.sliit.identityissuanceservice.Payment_Mgmt.Service;

import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Model.Appointment;
import lk.sliit.identityissuanceservice.Payment_Mgmt.Model.AppointmentChangePayment;
import lk.sliit.identityissuanceservice.Payment_Mgmt.Repository.AppointmentChangePaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class AppointmentChangePaymentService {
    public static final BigDecimal CHANGE_FEE = new BigDecimal("300.00");
    private final AppointmentChangePaymentRepository repository;

    public AppointmentChangePaymentService(AppointmentChangePaymentRepository repository) {
        this.repository = repository;
    }

    // The booking and this simulated payment must commit or roll back together.
    @Transactional(propagation = Propagation.MANDATORY)
    public void record(Appointment appointment, LocalDateTime newTime, String changeType, String token) {
        if (token == null || !token.matches("[a-fA-F0-9-]{36}")) {
            throw new IllegalArgumentException("Reload the booking page before paying.");
        }
        if (repository.existsByRequestToken(token)) {
            throw new IllegalArgumentException("This payment was already processed. Reload your booking.");
        }
        AppointmentChangePayment payment = new AppointmentChangePayment();
        payment.setAppointmentId(appointment.getAppointmentId());
        payment.setRequestToken(token);
        payment.setChangeType(changeType);
        payment.setAmount(CHANGE_FEE);
        payment.setPreviousScheduledAt(appointment.getScheduledAt());
        payment.setNewScheduledAt(newTime);
        payment.setPaidAt(LocalDateTime.now());
        repository.saveAndFlush(payment);
    }
}
