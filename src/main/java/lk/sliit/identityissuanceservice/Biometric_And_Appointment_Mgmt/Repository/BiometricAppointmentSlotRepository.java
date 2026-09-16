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

public interface BiometricAppointmentSlotRepository extends JpaRepository<BiometricAppointmentSlot, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select slot from BiometricAppointmentSlot slot where slot.slotId = :slotId")
    Optional<BiometricAppointmentSlot> findByIdForBooking(@Param("slotId") Integer slotId);

    Optional<BiometricAppointmentSlot> findByCentreIdAndServiceTypeAndSlotDateAndSlotTime(
            Integer centreId, String serviceType, LocalDate slotDate, LocalTime slotTime);

    List<BiometricAppointmentSlot> findAllByServiceTypeAndCentreIdAndSlotDateBetweenOrderBySlotDateAscSlotTimeAsc(
            String serviceType, Integer centreId, LocalDate startDate, LocalDate endDate);

    List<BiometricAppointmentSlot> findAllByServiceTypeAndCentreIdAndSlotDateOrderBySlotTimeAsc(
            String serviceType, Integer centreId, LocalDate slotDate);
}
