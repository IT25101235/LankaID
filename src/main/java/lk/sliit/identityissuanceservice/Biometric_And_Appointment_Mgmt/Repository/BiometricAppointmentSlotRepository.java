package lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Repository;

import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Model.BiometricAppointmentSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/** Repository pattern: CRUD and calendar queries for slots. */
// OOP - Inheritance, Abstraction and Polymorphism: this interface extends JpaRepository, so it inherits
// save, find and delete. We only declare it; Spring supplies the real implementation at runtime.
public interface BiometricAppointmentSlotRepository extends JpaRepository<BiometricAppointmentSlot, Integer> {

    /** Locks the slot row so two citizens cannot take the last place at the same time. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select slot from BiometricAppointmentSlot slot where slot.slotId = :slotId")
    Optional<BiometricAppointmentSlot> findByIdForBooking(@Param("slotId") Integer slotId);

    /** Locks every slot of one day, so publishing or closing it cannot race with a booking. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select slot from BiometricAppointmentSlot slot where slot.serviceType = :serviceType "
            + "and slot.centreId = :centreId and slot.slotDate = :slotDate order by slot.slotTime")
    List<BiometricAppointmentSlot> findDayForUpdate(
            @Param("serviceType") String serviceType, @Param("centreId") Integer centreId,
            @Param("slotDate") LocalDate slotDate);

    Optional<BiometricAppointmentSlot> findByCentreIdAndServiceTypeAndSlotDateAndSlotTime(
            Integer centreId, String serviceType, LocalDate slotDate, LocalTime slotTime);

    List<BiometricAppointmentSlot> findAllByServiceTypeAndCentreIdAndSlotDateBetweenOrderBySlotDateAscSlotTimeAsc(
            String serviceType, Integer centreId, LocalDate startDate, LocalDate endDate);
}
