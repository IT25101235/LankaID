package lk.sliit.identityissuanceservice.Notification_Mgmt.Model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "Notification", schema = "dbo")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Integer notificationId;

    @Column(name = "sent_by_staff_id")
    private Integer sentByStaffId;

    @Column(name = "generated_by_status_update_id")
    private Integer generatedByStatusUpdateId;

    @Column(name = "notification_type", nullable = false, length = 40)
    private String notificationType;

    @Column(name = "message", nullable = false, length = 2000)
    private String message;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "is_broadcast", columnDefinition = "CHAR(1)")
    private String broadcast;

    @PrePersist
    private void applyDefaults() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (broadcast == null) broadcast = "N";
    }

    public Integer getNotificationId() { return notificationId; }
    public void setNotificationId(Integer notificationId) { this.notificationId = notificationId; }
    public Integer getSentByStaffId() { return sentByStaffId; }
    public void setSentByStaffId(Integer sentByStaffId) { this.sentByStaffId = sentByStaffId; }
    public Integer getGeneratedByStatusUpdateId() { return generatedByStatusUpdateId; }
    public void setGeneratedByStatusUpdateId(Integer generatedByStatusUpdateId) { this.generatedByStatusUpdateId = generatedByStatusUpdateId; }
    public String getNotificationType() { return notificationType; }
    public void setNotificationType(String notificationType) { this.notificationType = notificationType; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getBroadcast() { return broadcast; }
    public void setBroadcast(String broadcast) { this.broadcast = broadcast; }
}
