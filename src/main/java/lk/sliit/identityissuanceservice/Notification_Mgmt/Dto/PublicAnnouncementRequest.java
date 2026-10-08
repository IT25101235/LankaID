package lk.sliit.identityissuanceservice.Notification_Mgmt.Dto;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

public class PublicAnnouncementRequest {

    private String title;
    private String message;
    private String announcementType = "IMPORTANT";
    private String sourceLabel;
    private String sourceUrl;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime startsAt;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime expiresAt;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getAnnouncementType() { return announcementType; }
    public void setAnnouncementType(String announcementType) { this.announcementType = announcementType; }
    public String getSourceLabel() { return sourceLabel; }
    public void setSourceLabel(String sourceLabel) { this.sourceLabel = sourceLabel; }
    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }
    public LocalDateTime getStartsAt() { return startsAt; }
    public void setStartsAt(LocalDateTime startsAt) { this.startsAt = startsAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
}
