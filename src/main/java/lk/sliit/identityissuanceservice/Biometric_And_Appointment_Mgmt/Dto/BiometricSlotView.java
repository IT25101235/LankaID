package lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Dto;

import java.time.LocalDate;
import java.time.LocalTime;

/** DTO: a slot with its centre name and how many places are booked and left. */
// OOP - Immutability and Encapsulation: a record is read-only after creation, so the screen cannot change
// slot data by mistake.
public record BiometricSlotView(
        Integer slotId,
        Integer centreId,
        String centreName,
        String serviceType,
        LocalDate slotDate,
        LocalTime slotTime,
        int capacity,
        long bookedCount,
        int remainingPlaces,
        boolean available
) { }
