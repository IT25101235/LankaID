package lk.sliit.identityissuanceservice.Verification_And_Approval_Management.Dto;

import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model.CitizenApplication;
import lk.sliit.identityissuanceservice.User_Mgmt.Model.CitizenAccount;
import lk.sliit.identityissuanceservice.Verification_And_Approval_Management.Model.DirectorReviewReferral;

import java.time.LocalDateTime;

/** A compact Director-queue view composed from referral, application and citizen data. */
public record DirectorReferralQueueItem(
        Integer referralId,
        String applicationNo,
        String serviceType,
        String applicationKind,
        String citizenName,
        String referralCategory,
        String referralReason,
        LocalDateTime createdAt
) {
    public DirectorReferralQueueItem(
            DirectorReviewReferral referral,
            CitizenApplication application,
            CitizenAccount citizen
    ) {
        this(
                referral.getReferralId(),
                application.getApplicationNo(),
                application.getServiceType(),
                application.getApplicationKind(),
                citizenName(citizen, application.getCitizenId()),
                referral.getReferralCategory(),
                referral.getReferralReason(),
                referral.getCreatedAt()
        );
    }

    private static String citizenName(CitizenAccount citizen, Integer citizenId) {
        if (citizen == null) return "Citizen #" + citizenId;
        String given = citizen.getGivenName() == null ? "" : citizen.getGivenName().trim();
        String family = citizen.getFamilyName() == null ? "" : citizen.getFamilyName().trim();
        String fullName = (given + " " + family).trim();
        if (!fullName.isBlank()) return fullName;
        return citizen.getSurname() == null || citizen.getSurname().isBlank()
                ? "Citizen #" + citizenId : citizen.getSurname();
    }
}
