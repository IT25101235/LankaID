package lk.sliit.identityissuanceservice.Notification_Mgmt.Observer;

import lk.sliit.identityissuanceservice.Notification_Mgmt.Service.NotificationService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** The ConcreteObserver: reacts to a notification request by using the existing service. */
@Component
public class InAppNotificationObserver {

    private final NotificationService notificationService;

    public InAppNotificationObserver(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @EventListener
    public void update(InAppNotificationEvent event) {
        // Spring registers this method as an observer. Keep it synchronous (no @Async)
        // so the business update and notification still commit or roll back together.
        int delivered = notificationService.sendToUsers(
                event.getUserIds(), event.getStaffId(), event.getType(),
                event.getMessage(), event.isBroadcast()
        );
        event.setDeliveredCount(delivered);
    }
}
