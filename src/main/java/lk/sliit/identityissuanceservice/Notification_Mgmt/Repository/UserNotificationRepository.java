package lk.sliit.identityissuanceservice.Notification_Mgmt.Repository;

import lk.sliit.identityissuanceservice.Notification_Mgmt.Model.UserNotification;
import lk.sliit.identityissuanceservice.Notification_Mgmt.Model.UserNotificationId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserNotificationRepository extends JpaRepository<UserNotification, UserNotificationId> {

    List<UserNotification> findAllById_UserIdOrderByDeliveredAtDesc(Integer userId);

    Optional<UserNotification> findById_NotificationIdAndId_UserId(Integer notificationId, Integer userId);

    long countById_UserIdAndReadAtIsNull(Integer userId);
}
