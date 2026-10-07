package lk.sliit.identityissuanceservice.Notification_Mgmt.Model;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "UserNotification", schema = "dbo")
public class UserNotification {

    @EmbeddedId
    private UserNotificationId id;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @PrePersist
    private void applyDefaults() {
        if (deliveredAt == null) deliveredAt = LocalDateTime.now();
    }

    public UserNotificationId getId() { return id; }
    public void setId(UserNotificationId id) { this.id = id; }
    public LocalDateTime getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(LocalDateTime deliveredAt) { this.deliveredAt = deliveredAt; }
    public LocalDateTime getReadAt() { return readAt; }
    public void setReadAt(LocalDateTime readAt) { this.readAt = readAt; }
}
