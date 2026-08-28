package lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Dto;

import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Model.Appointment;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Model.Centre;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model.CitizenApplication;
import lk.sliit.identityissuanceservice.User_Mgmt.Model.CitizenAccount;

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
