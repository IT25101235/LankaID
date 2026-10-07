package lk.sliit.identityissuanceservice.Notification_Mgmt.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AdminNotificationRequest {

    @NotBlank(message = "Please select citizens or officers.")
    private String recipientType = "CITIZEN";

    @NotBlank(message = "Please select an audience.")
    private String audience = "ALL";

    private String accountStatus = "ACTIVE";
    private String citizenService = "ALL";
    private String officerRole = "ALL";
    private String selectedRecipients;

    @NotBlank(message = "Please enter a title.")
    @Size(max = 120, message = "The title cannot exceed 120 characters.")
    private String title;

    @NotBlank(message = "Please enter a message.")
    @Size(max = 1800, message = "The message cannot exceed 1,800 characters.")
    private String message;

    private String category = "GENERAL";

    public String getRecipientType() { return recipientType; }
    public void setRecipientType(String recipientType) { this.recipientType = recipientType; }
    public String getAudience() { return audience; }
    public void setAudience(String audience) { this.audience = audience; }
    public String getAccountStatus() { return accountStatus; }
    public void setAccountStatus(String accountStatus) { this.accountStatus = accountStatus; }
    public String getCitizenService() { return citizenService; }
    public void setCitizenService(String citizenService) { this.citizenService = citizenService; }
    public String getOfficerRole() { return officerRole; }
    public void setOfficerRole(String officerRole) { this.officerRole = officerRole; }
    public String getSelectedRecipients() { return selectedRecipients; }
    public void setSelectedRecipients(String selectedRecipients) { this.selectedRecipients = selectedRecipients; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
}
