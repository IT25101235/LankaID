package lk.sliit.identityissuanceservice.Notification_Mgmt.Service;

import lk.sliit.identityissuanceservice.Notification_Mgmt.Dto.PublicAnnouncementRequest;
import lk.sliit.identityissuanceservice.Notification_Mgmt.Model.PublicAnnouncement;
import lk.sliit.identityissuanceservice.Notification_Mgmt.Repository.PublicAnnouncementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
public class PublicAnnouncementService {

    private static final Set<String> TYPES = Set.of("IMPORTANT", "PRESIDENTIAL", "EMERGENCY", "SERVICE_UPDATE");
    private final PublicAnnouncementRepository repository;

    public PublicAnnouncementService(PublicAnnouncementRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Optional<PublicAnnouncement> getCurrentAnnouncement() {
        return repository.findCurrent(LocalDateTime.now()).stream().findFirst();
    }

    @Transactional(readOnly = true)
    public List<PublicAnnouncement> getRecentAnnouncements() {
        return repository.findTop10ByOrderByCreatedAtDesc();
    }

    @Transactional
    public PublicAnnouncement publish(PublicAnnouncementRequest request, Integer administratorId) {
        String title = clean(request.getTitle());
        String message = clean(request.getMessage());
        String type = clean(request.getAnnouncementType()).toUpperCase(Locale.ROOT);
        String sourceLabel = clean(request.getSourceLabel());
        String sourceUrl = clean(request.getSourceUrl());
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startsAt = request.getStartsAt() == null ? now : request.getStartsAt();
        LocalDateTime expiresAt = request.getExpiresAt();

        if (title.isBlank()) throw new IllegalArgumentException("Enter an announcement title.");
        if (title.length() > 140) throw new IllegalArgumentException("The title must be 140 characters or fewer.");
        if (message.isBlank()) throw new IllegalArgumentException("Enter an announcement message.");
        if (message.length() > 1200) throw new IllegalArgumentException("The message must be 1,200 characters or fewer.");
        if (!TYPES.contains(type)) throw new IllegalArgumentException("Select a valid announcement type.");
        if (sourceLabel.length() > 80) throw new IllegalArgumentException("The source label must be 80 characters or fewer.");
        if (sourceUrl.length() > 500) throw new IllegalArgumentException("The source link is too long.");
        if (!sourceUrl.isBlank() && !isSafeWebUrl(sourceUrl)) {
            throw new IllegalArgumentException("The source link must be a complete http:// or https:// address.");
        }
        if (sourceUrl.isBlank() && !sourceLabel.isBlank()) {
            throw new IllegalArgumentException("Add a source link or remove the source label.");
        }
        if (!sourceUrl.isBlank() && sourceLabel.isBlank()) sourceLabel = "Read the official announcement";
        if (expiresAt != null && !expiresAt.isAfter(startsAt)) {
            throw new IllegalArgumentException("The expiry date must be after the publication date.");
        }

        List<PublicAnnouncement> activeAnnouncements = repository.findByActive("Y");
        activeAnnouncements.forEach(announcement -> {
            if (startsAt.isAfter(now)) {
                if (announcement.getExpiresAt() == null || announcement.getExpiresAt().isAfter(startsAt)) {
                    announcement.setExpiresAt(startsAt);
                }
            } else {
                announcement.setActive("N");
            }
        });
        repository.saveAll(activeAnnouncements);

        PublicAnnouncement announcement = new PublicAnnouncement();
        announcement.setTitle(title);
        announcement.setMessage(message);
        announcement.setAnnouncementType(type);
        announcement.setSourceLabel(sourceLabel.isBlank() ? null : sourceLabel);
        announcement.setSourceUrl(sourceUrl.isBlank() ? null : sourceUrl);
        announcement.setStartsAt(startsAt);
        announcement.setExpiresAt(expiresAt);
        announcement.setActive("Y");
        announcement.setCreatedByStaffId(administratorId);
        return repository.save(announcement);
    }

    @Transactional
    public void unpublish(Integer announcementId) {
        PublicAnnouncement announcement = repository.findById(announcementId)
                .orElseThrow(() -> new IllegalArgumentException("Announcement not found."));
        announcement.setActive("N");
        repository.save(announcement);
    }

    private boolean isSafeWebUrl(String value) {
        try {
            URI uri = URI.create(value);
            return uri.getHost() != null && Set.of("http", "https").contains(uri.getScheme().toLowerCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
