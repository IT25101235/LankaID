package lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Dto;

import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Model.Appointment;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Model.Centre;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model.CitizenApplication;
import lk.sliit.identityissuanceservice.User_Mgmt.Model.CitizenAccount;

/** DTO: everything the officer screens and the pass need about one appointment. */
// OOP - Composition: built from several objects (appointment, application, citizen, centre) instead of
// repeating their fields.
public record OfficerBiometricDetails(
        Appointment appointment,
        CitizenApplication application,
        CitizenAccount citizen,
        Centre centre,
        String applicantName,
        boolean checkedIn,
        boolean biometricCollected
) {
}
