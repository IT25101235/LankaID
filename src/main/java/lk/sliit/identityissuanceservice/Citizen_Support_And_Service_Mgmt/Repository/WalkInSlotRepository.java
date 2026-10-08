package lk.sliit.identityissuanceservice.Citizen_Support_And_Service_Mgmt.Repository;

import jakarta.persistence.LockModeType;
import lk.sliit.identityissuanceservice.Citizen_Support_And_Service_Mgmt.Model.WalkInSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/** Reads the published schedule and locks slots while capacity-changing work is performed. */
public interface WalkInSlotRepository extends JpaRepository<WalkInSlot, Integer> {

    // Booking, rescheduling, editing and deletion share this lock to protect slot capacity.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT slot FROM WalkInSlot slot WHERE slot.slotId = :slotId")
    Optional<WalkInSlot> findByIdForBooking(@Param("slotId") Integer slotId);

    Optional<WalkInSlot> findByCentreIdAndSlotDateAndSlotTime(
            Integer centreId,
            LocalDate slotDate,
            LocalTime slotTime
    );

    List<WalkInSlot> findAllByCentreIdAndSlotDateOrderBySlotTimeAsc(
            Integer centreId,
            LocalDate slotDate
    );

    List<WalkInSlot> findAllBySlotDateGreaterThanEqualOrderBySlotDateAscSlotTimeAsc(LocalDate date);

    List<WalkInSlot> findAllBySlotDateBetweenOrderBySlotDateAscSlotTimeAsc(
            LocalDate startDate,
            LocalDate endDate
    );

    List<WalkInSlot> findAllBySlotDateGreaterThanEqualAndIsAvailableOrderBySlotDateAscSlotTimeAsc(
            LocalDate date,
            String isAvailable
    );
}
