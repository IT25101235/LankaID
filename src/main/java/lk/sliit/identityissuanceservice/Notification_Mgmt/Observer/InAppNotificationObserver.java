package lk.sliit.identityissuanceservice.Notification_Mgmt.Observer;

import lk.sliit.identityissuanceservice.Notification_Mgmt.Service.NotificationService;
import org.springframework.stereotype.Component;

/** The ConcreteObserver: reacts to a notification request by using the existing service. */
@Component
public class InAppNotificationObserver implements NotificationObserver {

    private final NotificationService notificationService;

    public InAppNotificationObserver(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    public void update(InAppNotificationEvent event) {
        // The subject calls this directly, so storage joins the business transaction.
        int delivered = notificationService.sendToUsers(
                event.getUserIds(), event.getStaffId(), event.getType(),
                event.getMessage(), event.isBroadcast()
        );
        event.setDeliveredCount(delivered);
    }
}
