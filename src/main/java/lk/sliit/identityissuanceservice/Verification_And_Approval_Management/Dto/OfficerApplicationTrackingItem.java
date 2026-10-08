package lk.sliit.identityissuanceservice.Verification_And_Approval_Management.Dto;

import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Model.Appointment;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Model.BiometricCollection;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model.CitizenApplication;
import lk.sliit.identityissuanceservice.Document_Renewal_Replacement_And_Delivery_Mgmt.Model.DocumentDelivery;
import lk.sliit.identityissuanceservice.Payment_Mgmt.Model.Payment;
import lk.sliit.identityissuanceservice.Verification_And_Approval_Management.Model.ApplicationReview;

/**
 * Immutable cross-module snapshot of one application's progress.
 * Composition is visible here because the DTO contains related verification, payment,
 * appointment, biometric and delivery objects rather than duplicating their fields.
 */
public record OfficerApplicationTrackingItem(
        CitizenApplication application,
        String citizenName,
        String citizenEmail,
        ApplicationReview latestReview,
        Appointment appointment,
        Payment payment,
        BiometricCollection biometric,
        DocumentDelivery delivery
) {
}
