package lk.sliit.identityissuanceservice.Notification_Mgmt.Model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class UserNotificationId implements Serializable {

    @Column(name = "notification_id")
    private Integer notificationId;

    @Column(name = "user_id")
    private Integer userId;

    public UserNotificationId() {
    }

    public UserNotificationId(Integer notificationId, Integer userId) {
        this.notificationId = notificationId;
        this.userId = userId;
    }

    public Integer getNotificationId() { return notificationId; }
    public void setNotificationId(Integer notificationId) { this.notificationId = notificationId; }
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof UserNotificationId that)) return false;
        return Objects.equals(notificationId, that.notificationId)
                && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(notificationId, userId);
    }
}
