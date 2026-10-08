package lk.sliit.identityissuanceservice.Citizen_Support_And_Service_Mgmt.Dto;

import java.time.LocalDate;
import java.time.LocalTime;

/** Presents stored capacity together with the current number of remaining places. */
public record WalkInSlotView(
        Integer slotId,
        Integer centreId,
        String centreName,
        LocalDate slotDate,
        LocalTime slotTime,
        int capacity,
        long bookedCount,
        int remainingPlaces,
        boolean available
) { }
