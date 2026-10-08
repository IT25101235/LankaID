package lk.sliit.identityissuanceservice.Notification_Mgmt.Model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDateTime;

@Entity
@Table(name = "PublicAnnouncement", schema = "dbo")
public class PublicAnnouncement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "announcement_id")
    private Integer announcementId;

    @Column(name = "title", nullable = false, length = 140)
    @Nationalized
    private String title;

    @Column(name = "message", nullable = false, length = 1200)
    @Nationalized
    private String message;

    @Column(name = "announcement_type", nullable = false, length = 30)
    private String announcementType;

    @Column(name = "source_label", length = 80)
    @Nationalized
    private String sourceLabel;

    @Column(name = "source_url", length = 500)
    private String sourceUrl;

    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "is_active", nullable = false, columnDefinition = "CHAR(1)")
    private String active;

    @Column(name = "created_by_staff_id")
    private Integer createdByStaffId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    private void applyDefaults() {
        if (startsAt == null) startsAt = LocalDateTime.now();
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (active == null) active = "Y";
    }

    public Integer getAnnouncementId() { return announcementId; }
    public void setAnnouncementId(Integer announcementId) { this.announcementId = announcementId; }
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
    public String getActive() { return active; }
    public void setActive(String active) { this.active = active; }
    public Integer getCreatedByStaffId() { return createdByStaffId; }
    public void setCreatedByStaffId(Integer createdByStaffId) { this.createdByStaffId = createdByStaffId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
