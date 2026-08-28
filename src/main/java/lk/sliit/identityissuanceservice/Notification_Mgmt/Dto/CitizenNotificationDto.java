package lk.sliit.identityissuanceservice.Notification_Mgmt.Dto;

import java.time.LocalDateTime;

public record CitizenNotificationDto(
        Integer notificationId,
        String notificationType,
        String message,
        LocalDateTime createdAt,
        boolean read
) {
}
