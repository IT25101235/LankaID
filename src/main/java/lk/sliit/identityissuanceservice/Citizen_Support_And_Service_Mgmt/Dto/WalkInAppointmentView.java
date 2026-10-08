package lk.sliit.identityissuanceservice.Citizen_Support_And_Service_Mgmt.Dto;

import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Model.Appointment;

/** Adds service-centre and check-in information to an appointment for display. */
public record WalkInAppointmentView(
        Appointment appointment,
        String centreName,
        String centreAddress,
        boolean checkedIn
) { }
