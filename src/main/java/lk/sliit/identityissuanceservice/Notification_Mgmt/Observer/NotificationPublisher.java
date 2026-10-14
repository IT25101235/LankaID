package lk.sliit.identityissuanceservice.Notification_Mgmt.Observer;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/** ConcreteSubject: owns its observer list and calls each observer directly. */
@Service
public class NotificationPublisher implements NotificationSubject {

    private final CopyOnWriteArrayList<NotificationObserver> observers = new CopyOnWriteArrayList<>();

    public NotificationPublisher(List<NotificationObserver> initialObservers) {
        initialObservers.forEach(this::addObserver);
    }

    @Override
    public void addObserver(NotificationObserver observer) {
        observers.addIfAbsent(Objects.requireNonNull(observer, "Observer must not be null."));
    }

    @Override
    public void removeObserver(NotificationObserver observer) {
        observers.remove(observer);
    }

    @Override
    public void notifyObservers(InAppNotificationEvent event) {
        Objects.requireNonNull(event, "Notification event must not be null.");
        // A snapshot permits concurrent publication and registration without duplicate delivery.
        // Direct calls preserve the caller's thread, transaction, and exception handling.
        for (NotificationObserver observer : observers) {
            observer.update(event);
        }
    }

    @Transactional
    public void sendToCitizen(Integer citizenId, Integer staffId, String type, String message) {
        sendToUsers(List.of(citizenId), staffId, type, message, false);
    }

    @Transactional
    public int sendToUsers(List<Integer> userIds, Integer staffId, String type,
                           String message, boolean broadcast) {
        InAppNotificationEvent event = new InAppNotificationEvent(userIds, staffId, type, message, broadcast);

        // Keep event data local to this call: simultaneous requests must not share message state.
        notifyObservers(event);

        // The observer records the real count so the admin success message stays the same.
        return event.getDeliveredCount();
    }
}
