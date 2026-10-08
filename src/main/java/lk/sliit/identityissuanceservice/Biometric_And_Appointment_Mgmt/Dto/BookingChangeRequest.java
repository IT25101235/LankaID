package lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Dto;

import jakarta.servlet.http.HttpSession;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Model.Appointment;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.UUID;

/** Session-bound confirmation protects against stale forms and repeated payments. */
public record BookingChangeRequest(Integer appointmentId, String status, LocalDateTime scheduledAt, Long version,
                                   String token) implements Serializable {
    private static final String KEY = "bookingChangeRequests";
    // Several tabs can be open at once; keep the most recent confirmations and drop the oldest.
    private static final int MAX_OPEN_REQUESTS = 10;

    public static BookingChangeRequest prepare(Appointment appointment, HttpSession session) {
        BookingChangeRequest request = new BookingChangeRequest(appointment.getAppointmentId(),
                appointment.getAppointmentStatus(), appointment.getScheduledAt(), appointment.getBookingVersion(), UUID.randomUUID().toString());
        LinkedHashMap<String, BookingChangeRequest> open = open(session);
        open.put(request.token(), request);
        while (open.size() > MAX_OPEN_REQUESTS) open.remove(open.keySet().iterator().next());
        session.setAttribute(KEY, open);
        return request;
    }

    public static BookingChangeRequest require(String token, HttpSession session) {
        BookingChangeRequest request = token == null ? null : open(session).get(token);
        if (request == null) {
            throw new IllegalArgumentException("This confirmation expired. Reload the booking and try again.");
        }
        return request;
    }

    public static void clear(HttpSession session) { session.removeAttribute(KEY); }

    @SuppressWarnings("unchecked")
    private static LinkedHashMap<String, BookingChangeRequest> open(HttpSession session) {
        return session.getAttribute(KEY) instanceof LinkedHashMap<?, ?> existing
                ? (LinkedHashMap<String, BookingChangeRequest>) existing : new LinkedHashMap<>();
    }
}
