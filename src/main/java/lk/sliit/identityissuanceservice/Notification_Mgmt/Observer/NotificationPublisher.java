package lk.sliit.identityissuanceservice.Notification_Mgmt.Observer;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** The Subject in the Observer pattern: announces a notification request to its listeners. */
@Service
public class NotificationPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public NotificationPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void sendToCitizen(Integer citizenId, Integer staffId, String type, String message) {
        sendToUsers(List.of(citizenId), staffId, type, message, false);
    }

    @Transactional
    public int sendToUsers(List<Integer> userIds, Integer staffId, String type,
                           String message, boolean broadcast) {
        InAppNotificationEvent event = new InAppNotificationEvent(userIds, staffId, type, message, broadcast);

        // Spring calls the registered observer immediately, in this thread and transaction.
        // The publisher announces the request; it does not save notifications itself.
        eventPublisher.publishEvent(event);

        // The observer records the real count so the admin success message stays the same.
        return event.getDeliveredCount();
    }
}
