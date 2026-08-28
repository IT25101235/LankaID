package lk.sliit.identityissuanceservice.Verification_And_Approval_Management.Dto;

import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model.ApplicationDocument;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model.CitizenApplication;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model.NicApplication;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model.PassportApplication;
import lk.sliit.identityissuanceservice.User_Mgmt.Model.CitizenAccount;
import lk.sliit.identityissuanceservice.User_Mgmt.Model.UserAccount;

import java.util.List;

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
