package lk.sliit.identityissuanceservice.Notification_Mgmt.Observer;

/** The lecture's Subject contract: register, detach, and notify observers. */
public interface NotificationSubject {
    void addObserver(NotificationObserver observer);
    void removeObserver(NotificationObserver observer);
    void notifyObservers(InAppNotificationEvent event);
}
