package lk.sliit.identityissuanceservice.Verification_And_Approval_Management.Dto;

import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model.CitizenApplication;
import lk.sliit.identityissuanceservice.User_Mgmt.Model.CitizenAccount;
import lk.sliit.identityissuanceservice.Verification_And_Approval_Management.Model.ApplicationReview;

public record ReviewedApplicationRow(ApplicationReview review, CitizenApplication application, CitizenAccount citizen) {
    public String citizenName() {
        if (citizen == null) return "Citizen #" + application.getCitizenId();
        String name = (text(citizen.getGivenName()) + " " + text(citizen.getFamilyName())).trim();
        if (!name.isBlank()) return name;
        return text(citizen.getSurname()).isBlank() ? "Citizen #" + application.getCitizenId() : citizen.getSurname();
    }

    public String kindLabel() {
        return switch (application.getApplicationKind()) {
            case "NEW" -> "New";
            case "RENEWAL" -> "Renewal";
            case "LOST" -> "Lost replacement";
            default -> application.getApplicationKind();
        };
    }

    public String decisionLabel() {
        return switch (review.getDecision()) {
            case "APPROVED" -> "Approved";
            case "REJECTED" -> "Rejected";
            case "CORRECTION_REQUIRED" -> "Correction requested";
            default -> review.getDecision();
        };
    }

    private static String text(String value) { return value == null ? "" : value.trim(); }
}
