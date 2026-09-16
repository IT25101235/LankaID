package lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Dto;

import java.time.LocalDate;
import java.time.LocalTime;

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
