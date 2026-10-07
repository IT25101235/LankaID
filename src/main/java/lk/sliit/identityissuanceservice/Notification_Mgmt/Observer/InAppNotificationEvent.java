package lk.sliit.identityissuanceservice.Notification_Mgmt.Observer;

import java.util.List;

/** Carries the same notification details that the workflow service already supplies. */
public class InAppNotificationEvent {

    private final List<Integer> userIds;
    private final Integer staffId;
    private final String type;
    private final String message;
    private final boolean broadcast;
    private Integer deliveredCount;

    public InAppNotificationEvent(List<Integer> userIds, Integer staffId, String type,
                                  String message, boolean broadcast) {
        this.userIds = userIds;
        this.staffId = staffId;
        this.type = type;
        this.message = message;
        this.broadcast = broadcast;
    }

    public List<Integer> getUserIds() {
        return userIds;
    }

    public Integer getStaffId() {
        return staffId;
    }

    public String getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public boolean isBroadcast() {
        return broadcast;
    }

    public void setDeliveredCount(int deliveredCount) {
        this.deliveredCount = deliveredCount;
    }

    public int getDeliveredCount() {
        // A missing observer must not look like a successful notification delivery.
        if (deliveredCount == null) {
            throw new IllegalStateException("The in-app notification observer did not handle the event.");
        }
        return deliveredCount;
    }
}
