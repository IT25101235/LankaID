package lk.sliit.identityissuanceservice.Payment_Mgmt.Repository;

import lk.sliit.identityissuanceservice.Payment_Mgmt.Model.AppointmentChangePayment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppointmentChangePaymentRepository extends JpaRepository<AppointmentChangePayment, Integer> {
    boolean existsByRequestToken(String requestToken);
    long countByAppointmentId(Integer appointmentId);
}
