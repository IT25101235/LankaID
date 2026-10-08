package lk.sliit.identityissuanceservice.Verification_And_Approval_Management.Dto;

import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model.ApplicationDocument;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model.CitizenApplication;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model.NicApplication;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model.PassportApplication;
import lk.sliit.identityissuanceservice.User_Mgmt.Model.CitizenAccount;
import lk.sliit.identityissuanceservice.User_Mgmt.Model.UserAccount;

import java.util.List;

/**
 * Immutable composite view model for the officer review page.
 * A Java record provides encapsulation without mutable setters and groups the common application,
 * service-specific details, citizen profile and uploaded documents in one value object.
 */
public record VerificationReviewDetails(
        CitizenApplication reviewApplication,
        CitizenAccount citizen,
        UserAccount userAccount,
        NicApplication nicApplication,
        PassportApplication passportApplication,
        List<ApplicationDocument> documents,
        String applicantName,
        String permanentAddress
) {
}
