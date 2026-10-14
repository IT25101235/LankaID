package lk.sliit.identityissuanceservice.Notification_Mgmt.Observer;

/** The lecture's Observer contract: react to a notification from the subject. */
public interface NotificationObserver {
    void update(InAppNotificationEvent event);
}
