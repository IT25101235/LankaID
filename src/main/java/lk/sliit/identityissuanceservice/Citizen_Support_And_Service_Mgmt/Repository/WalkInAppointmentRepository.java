package lk.sliit.identityissuanceservice.Citizen_Support_And_Service_Mgmt.Repository;

import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Model.Appointment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Limits appointment queries to the walk-in workflow and its officer queue. */
public interface WalkInAppointmentRepository extends JpaRepository<Appointment, Integer> {

    boolean existsByAppointmentTypeAndCentreIdAndScheduledAt(String appointmentType, Integer centreId, LocalDateTime scheduledAt);

    // Booking changes lock the appointment so stale requests cannot update it concurrently.
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select a from Appointment a where a.referenceNo = :reference and a.appointmentType = 'WALK_IN'")
    Optional<Appointment> findForChange(@org.springframework.data.repository.query.Param("reference") String reference);

    // Include the old local format so existing bookings can still be recovered.
    List<Appointment> findAllByVisitorPhoneInAndAppointmentTypeOrderByScheduledAtDesc(
            Collection<String> visitorPhones, String appointmentType);

    Optional<Appointment> findByReferenceNoIgnoreCaseAndAppointmentType(
            String referenceNo,
            String appointmentType
    );

    List<Appointment> findAllByVisitorPhoneAndAppointmentTypeOrderByScheduledAtDesc(
            String visitorPhone,
            String appointmentType
    );

    Page<Appointment> findAllByAppointmentTypeOrderByScheduledAtDesc(
            String appointmentType,
            Pageable pageable
    );

    Page<Appointment> findAllByAppointmentTypeAndVisitorServiceOrderByScheduledAtDesc(
            String appointmentType,
            String visitorService,
            Pageable pageable
    );

    List<Appointment> findAllByAppointmentTypeAndCentreIdAndScheduledAtBetweenAndAppointmentStatusIn(
            String appointmentType,
            Integer centreId,
            LocalDateTime start,
            LocalDateTime end,
            Collection<String> statuses
    );

    List<Appointment> findAllByAppointmentTypeAndScheduledAtBetweenOrderByScheduledAtAsc(
            String appointmentType,
            LocalDateTime start,
            LocalDateTime end
    );

    long countByAppointmentTypeAndCentreIdAndScheduledAtAndAppointmentStatusIn(
            String appointmentType,
            Integer centreId,
            LocalDateTime scheduledAt,
            Collection<String> statuses
    );

    boolean existsByReferenceNo(String referenceNo);
}
