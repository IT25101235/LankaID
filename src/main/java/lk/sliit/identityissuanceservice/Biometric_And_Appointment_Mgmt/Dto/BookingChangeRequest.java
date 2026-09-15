package lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Dto;

import jakarta.servlet.http.HttpSession;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Model.Appointment;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/** Session-bound confirmation protects against stale forms and repeated payments. */
public record BookingChangeRequest(Integer appointmentId, String status, LocalDateTime scheduledAt, Long version,
                                   String token) implements Serializable {
    private static final String KEY = "bookingChangeRequest";

    public static BookingChangeRequest prepare(Appointment appointment, HttpSession session) {
        BookingChangeRequest request = new BookingChangeRequest(appointment.getAppointmentId(),
                appointment.getAppointmentStatus(), appointment.getScheduledAt(), appointment.getBookingVersion(), UUID.randomUUID().toString());
        session.setAttribute(KEY, request);
        return request;
    }

    public static BookingChangeRequest require(String token, HttpSession session) {
        Object value = session.getAttribute(KEY);
        if (!(value instanceof BookingChangeRequest request) || !request.token().equals(token)) {
            throw new IllegalArgumentException("This confirmation expired. Reload the booking and try again.");
        }
        return request;
    }

    public static void clear(HttpSession session) { session.removeAttribute(KEY); }
}
