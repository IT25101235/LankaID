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

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select a from Appointment a where a.appointmentId = :id")
    Optional<Appointment> findByIdForChange(@org.springframework.data.repository.query.Param("id") Integer id);

    @org.springframework.data.jpa.repository.Query("""
            select count(a) from Appointment a where a.biometricSlotId = :slotId
            and a.appointmentStatus in ('CONFIRMED', 'CHECKED_IN', 'COMPLETED')
            and a.appointmentId <> :excludedId
            """)
    long countActiveBiometricSlotBookings(Integer slotId, Integer excludedId);

    List<Appointment> findAllByBiometricSlotIdAndAppointmentStatusIn(
            Integer biometricSlotId, Collection<String> statuses);

    boolean existsByBiometricSlotId(Integer biometricSlotId);

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

    boolean existsByCentreIdAndScheduledAtAfterAndAppointmentStatusIn(
            Integer centreId,
            LocalDateTime scheduledAt,
            Collection<String> statuses
    );
}
