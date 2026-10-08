package lk.sliit.identityissuanceservice;

import lk.sliit.identityissuanceservice.Notification_Mgmt.Dto.PublicAnnouncementRequest;
import lk.sliit.identityissuanceservice.Notification_Mgmt.Model.PublicAnnouncement;
import lk.sliit.identityissuanceservice.Notification_Mgmt.Repository.PublicAnnouncementRepository;
import lk.sliit.identityissuanceservice.Notification_Mgmt.Service.PublicAnnouncementService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PublicAnnouncementServiceTests {

    @Test
    void publishingReplacesThePreviousPublicAnnouncement() {
        PublicAnnouncementRepository repository = mock(PublicAnnouncementRepository.class);
        PublicAnnouncement previous = new PublicAnnouncement();
        previous.setActive("Y");
        when(repository.findByActive("Y")).thenReturn(List.of(previous));
        when(repository.save(any(PublicAnnouncement.class))).thenAnswer(invocation -> invocation.getArgument(0));
        PublicAnnouncementService service = new PublicAnnouncementService(repository);

        PublicAnnouncementRequest request = new PublicAnnouncementRequest();
        request.setTitle("Address to the nation");
        request.setMessage("Citizens can read the verified announcement through the official source.");
        request.setAnnouncementType("PRESIDENTIAL");
        request.setSourceUrl("https://www.presidentsoffice.gov.lk/news/example");

        PublicAnnouncement saved = service.publish(request, 50);

        assertEquals("N", previous.getActive());
        assertEquals("Y", saved.getActive());
        assertEquals("Read the official announcement", saved.getSourceLabel());
        assertEquals(50, saved.getCreatedByStaffId());
        verify(repository).saveAll(List.of(previous));
        verify(repository).save(saved);
    }

    @Test
    void unsafeSourceLinksAndInvalidSchedulesAreRejected() {
        PublicAnnouncementRepository repository = mock(PublicAnnouncementRepository.class);
        PublicAnnouncementService service = new PublicAnnouncementService(repository);
        PublicAnnouncementRequest request = new PublicAnnouncementRequest();
        request.setTitle("Official notice");
        request.setMessage("Verified message");
        request.setAnnouncementType("IMPORTANT");
        request.setSourceUrl("javascript:alert(1)");

        assertThrows(IllegalArgumentException.class, () -> service.publish(request, 50));

        request.setSourceUrl("https://www.gov.lk/notice");
        request.setStartsAt(LocalDateTime.of(2026, 10, 8, 10, 0));
        request.setExpiresAt(LocalDateTime.of(2026, 10, 8, 9, 0));
        assertThrows(IllegalArgumentException.class, () -> service.publish(request, 50));
        verify(repository, never()).save(any());
    }

    @Test
    void futureAnnouncementKeepsCurrentNoticeUntilItsScheduledStart() {
        PublicAnnouncementRepository repository = mock(PublicAnnouncementRepository.class);
        PublicAnnouncement current = new PublicAnnouncement();
        current.setActive("Y");
        when(repository.findByActive("Y")).thenReturn(List.of(current));
        when(repository.save(any(PublicAnnouncement.class))).thenAnswer(invocation -> invocation.getArgument(0));
        PublicAnnouncementService service = new PublicAnnouncementService(repository);
        LocalDateTime scheduledStart = LocalDateTime.now().plusDays(1);

        PublicAnnouncementRequest request = new PublicAnnouncementRequest();
        request.setTitle("Tomorrow's service update");
        request.setMessage("The updated process begins tomorrow.");
        request.setAnnouncementType("SERVICE_UPDATE");
        request.setStartsAt(scheduledStart);

        service.publish(request, 50);

        assertEquals("Y", current.getActive());
        assertEquals(scheduledStart, current.getExpiresAt());
    }
}
