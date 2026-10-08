package lk.sliit.identityissuanceservice.Notification_Mgmt.Repository;

import lk.sliit.identityissuanceservice.Notification_Mgmt.Model.PublicAnnouncement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PublicAnnouncementRepository extends JpaRepository<PublicAnnouncement, Integer> {

    @Query("""
            select announcement from PublicAnnouncement announcement
            where announcement.active = 'Y'
              and announcement.startsAt <= :now
              and (announcement.expiresAt is null or announcement.expiresAt > :now)
            order by announcement.startsAt desc, announcement.createdAt desc
            """)
    List<PublicAnnouncement> findCurrent(@Param("now") LocalDateTime now);

    List<PublicAnnouncement> findTop10ByOrderByCreatedAtDesc();

    List<PublicAnnouncement> findByActive(String active);
}
