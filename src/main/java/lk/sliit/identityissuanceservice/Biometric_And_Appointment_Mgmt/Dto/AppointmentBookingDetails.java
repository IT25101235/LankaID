package lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Dto;

import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Model.Appointment;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Model.Centre;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model.CitizenApplication;

import java.time.LocalDate;
import java.util.List;

public record AppointmentBookingDetails(
        CitizenApplication application,
        Appointment appointment,
        List<Centre> centres,
        LocalDate earliestDate,
        LocalDate latestDate
) {
}
