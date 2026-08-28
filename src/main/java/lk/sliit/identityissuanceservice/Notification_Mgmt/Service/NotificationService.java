package lk.sliit.identityissuanceservice.Notification_Mgmt.Service;

import lk.sliit.identityissuanceservice.Notification_Mgmt.Dto.CitizenNotificationDto;
import lk.sliit.identityissuanceservice.Notification_Mgmt.Model.Notification;
import lk.sliit.identityissuanceservice.Notification_Mgmt.Model.UserNotification;
import lk.sliit.identityissuanceservice.Notification_Mgmt.Model.UserNotificationId;
import lk.sliit.identityissuanceservice.Notification_Mgmt.Repository.NotificationRepository;
import lk.sliit.identityissuanceservice.Notification_Mgmt.Repository.UserNotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserNotificationRepository userNotificationRepository;

    public NotificationService(
            NotificationRepository notificationRepository,
            UserNotificationRepository userNotificationRepository
    ) {
        this.notificationRepository = notificationRepository;
        this.userNotificationRepository = userNotificationRepository;
    }

    @Transactional
    public void sendToCitizen(Integer citizenId, Integer staffId, String type, String message) {
        Notification notification = new Notification();
        notification.setSentByStaffId(staffId);
        notification.setNotificationType(type);
        notification.setMessage(message);
        notification.setBroadcast("N");
        notification = notificationRepository.save(notification);

        UserNotification recipient = new UserNotification();
        recipient.setId(new UserNotificationId(notification.getNotificationId(), citizenId));
        userNotificationRepository.save(recipient);
    }

    @Transactional(readOnly = true)
    public List<CitizenNotificationDto> getCitizenNotifications(Integer citizenId) {
        List<UserNotification> recipients = userNotificationRepository
                .findAllById_UserIdOrderByDeliveredAtDesc(citizenId);
        Map<Integer, Notification> notifications = notificationRepository.findAllById(
                        recipients.stream().map(item -> item.getId().getNotificationId()).toList()
                ).stream()
                .collect(Collectors.toMap(Notification::getNotificationId, Function.identity()));

        return recipients.stream()
                .map(recipient -> notifications.get(recipient.getId().getNotificationId()))
                .filter(java.util.Objects::nonNull)
                .map(notification -> {
                    UserNotification recipient = recipients.stream()
                            .filter(item -> item.getId().getNotificationId().equals(notification.getNotificationId()))
                            .findFirst().orElseThrow();
                    return new CitizenNotificationDto(
                            notification.getNotificationId(),
                            notification.getNotificationType(),
                            notification.getMessage(),
                            notification.getCreatedAt(),
                            recipient.getReadAt() != null
                    );
                })
                .sorted((left, right) -> right.createdAt().compareTo(left.createdAt()))
                .toList();
    }

    @Transactional
    public void markAsRead(Integer citizenId, Integer notificationId) {
        UserNotification recipient = userNotificationRepository
                .findById_NotificationIdAndId_UserId(notificationId, citizenId)
                .orElseThrow(() -> new IllegalArgumentException("Notification was not found."));
        recipient.setReadAt(LocalDateTime.now());
        userNotificationRepository.save(recipient);
    }

    @Transactional
    public void markAllAsRead(Integer citizenId) {
        List<UserNotification> recipients = userNotificationRepository
                .findAllById_UserIdOrderByDeliveredAtDesc(citizenId);
        LocalDateTime now = LocalDateTime.now();
        recipients.stream()
                .filter(recipient -> recipient.getReadAt() == null)
                .forEach(recipient -> recipient.setReadAt(now));
        userNotificationRepository.saveAll(recipients);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Integer citizenId) {
        return userNotificationRepository.countById_UserIdAndReadAtIsNull(citizenId);
    }
}
