package lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Service;

import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Model.Appointment;
import java.time.LocalDateTime;

/** The same rules are used for showing actions and accepting submitted changes. */
public final class BookingChangePolicy {
    private BookingChangePolicy() { }

    public static boolean canChange(Appointment appointment) {
        return canChange(appointment, LocalDateTime.now());
    }

    public static boolean canChange(Appointment appointment, LocalDateTime now) {
        if (!"CONFIRMED".equals(appointment.getAppointmentStatus())) return false;
        if ("BIOMETRIC".equals(appointment.getAppointmentType())) {
            return !appointment.getScheduledAt().isBefore(now.plusDays(3));
        }
        return "WALK_IN".equals(appointment.getAppointmentType())
                && appointment.getScheduledAt().isAfter(now);
    }

    public static void requireChangeAllowed(Appointment appointment) {
        if (!canChange(appointment)) {
            throw new IllegalArgumentException("BIOMETRIC".equals(appointment.getAppointmentType())
                    ? "Only confirmed bookings at least 72 hours away can be cancelled or rescheduled."
                    : "Only confirmed walk-in bookings before their start time can be cancelled or rescheduled.");
        }
    }

    public static void requireUnchanged(Appointment appointment,
            lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Dto.BookingChangeRequest request) {
        if (!appointment.getAppointmentStatus().equals(request.status())
                || !appointment.getScheduledAt().equals(request.scheduledAt())
                || !java.util.Objects.equals(appointment.getBookingVersion(), request.version())) {
            throw new IllegalArgumentException("This booking has changed. Reload it before continuing.");
        }
    }
}
