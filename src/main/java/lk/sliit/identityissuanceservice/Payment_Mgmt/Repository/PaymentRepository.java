package lk.sliit.identityissuanceservice.Payment_Mgmt.Repository;

import lk.sliit.identityissuanceservice.Payment_Mgmt.Model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Integer> {

    Optional<Payment> findFirstByAppointmentIdAndPaymentStatusOrderByCreatedAtDesc(
            Integer appointmentId,
            String paymentStatus
    );

    boolean existsByTransactionReference(String transactionReference);
}
