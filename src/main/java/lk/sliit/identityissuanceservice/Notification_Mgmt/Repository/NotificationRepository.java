package lk.sliit.identityissuanceservice.Notification_Mgmt.Repository;

import lk.sliit.identityissuanceservice.Notification_Mgmt.Model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {
}
