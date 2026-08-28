package lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Repository;

import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Model.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Integer> {

    Optional<Appointment> findByApplicationIdAndAppointmentType(Integer applicationId, String appointmentType);

    Optional<Appointment> findByReferenceNoIgnoreCase(String referenceNo);

    boolean existsByReferenceNo(String referenceNo);

    List<Appointment> findAllByAppointmentTypeAndAppointmentStatusInOrderByScheduledAtAsc(
            String appointmentType,
            Collection<String> statuses
    );

    long countByCentreIdAndScheduledAtBetweenAndAppointmentStatusIn(
            Integer centreId,
            LocalDateTime start,
            LocalDateTime end,
            Collection<String> statuses
    );
}
