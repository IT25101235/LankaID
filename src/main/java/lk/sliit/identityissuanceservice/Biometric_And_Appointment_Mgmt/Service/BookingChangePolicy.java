package lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Service;

import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Model.Appointment;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** The same rules are used for showing actions and accepting submitted changes. */
// OOP - Encapsulation: a final class with a private constructor cannot be extended or instantiated. It
// only offers static rule methods.
public final class BookingChangePolicy {
    private BookingChangePolicy() { }

    public static boolean canChange(Appointment appointment) {
        return canCancel(appointment);
    }

    public static boolean canChange(Appointment appointment, LocalDateTime now) {
        return canCancel(appointment, now);
    }

    /** Policy rules: a biometric booking can be cancelled up to 72 hours before it. */
    // OOP - Polymorphism (method overloading): canCancel(a) uses the current time, canCancel(a, now) takes
    // a given time.
    public static boolean canCancel(Appointment appointment) {
        return canCancel(appointment, LocalDateTime.now());
    }

    public static boolean canCancel(Appointment appointment, LocalDateTime now) {
        if (!"CONFIRMED".equals(appointment.getAppointmentStatus())) return false;
        if ("BIOMETRIC".equals(appointment.getAppointmentType())) {
            return !appointment.getScheduledAt().isBefore(now.plusDays(3));
        }
        return "WALK_IN".equals(appointment.getAppointmentType())
                && appointment.getScheduledAt().isAfter(now);
    }

    /** Can be rescheduled if cancellable, or if the booked date has already passed (missed). */
    public static boolean canReschedule(Appointment appointment) {
        return canReschedule(appointment, LocalDateTime.now());
    }

    public static boolean canReschedule(Appointment appointment, LocalDateTime now) {
        if (!"CONFIRMED".equals(appointment.getAppointmentStatus())) return false;
        return isMissed(appointment, now.toLocalDate()) || canCancel(appointment, now);
    }

    public static boolean isToday(Appointment appointment) {
        return isToday(appointment, LocalDate.now());
    }

    static boolean isToday(Appointment appointment, LocalDate today) {
        return appointment.getScheduledAt().toLocalDate().equals(today);
    }

    public static boolean isMissed(Appointment appointment) {
        return isMissed(appointment, LocalDate.now());
    }

    static boolean isMissed(Appointment appointment, LocalDate today) {
        return "CONFIRMED".equals(appointment.getAppointmentStatus())
                && appointment.getScheduledAt().toLocalDate().isBefore(today);
    }

    /** Check-in is only allowed on the booked date. */
    public static void requireCheckInToday(Appointment appointment) {
        LocalDate bookedDate = appointment.getScheduledAt().toLocalDate();
        LocalDate today = LocalDate.now();
        if (bookedDate.isBefore(today)) {
            throw new IllegalArgumentException("This appointment date has passed. Reschedule it before check-in.");
        }
        if (bookedDate.isAfter(today)) {
            throw new IllegalArgumentException("Check-in is available only on the booked date: " + bookedDate + ".");
        }
    }

    public static void requireCancellationAllowed(Appointment appointment) {
        if (!canCancel(appointment)) {
            throw new IllegalArgumentException("BIOMETRIC".equals(appointment.getAppointmentType())
                    ? "Only confirmed bookings at least 72 hours away can be cancelled."
                    : "Only confirmed walk-in bookings before their start time can be cancelled.");
        }
    }

    public static void requireRescheduleAllowed(Appointment appointment) {
        if (!canReschedule(appointment)) {
            throw new IllegalArgumentException("BIOMETRIC".equals(appointment.getAppointmentType())
                    ? "Reschedule at least 72 hours before the appointment, or after its date has passed."
                    : "Reschedule before the appointment starts, or after its date has passed.");
        }
    }

    public static void requireMissedAppointment(Appointment appointment) {
        if (!isMissed(appointment)) {
            throw new IllegalArgumentException("Only a confirmed appointment from an earlier date can be rescheduled here.");
        }
    }

    /** Rejects a stale form: the booking changed after the page was opened. */
    public static void requireUnchanged(Appointment appointment,
            lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Dto.BookingChangeRequest request) {
        if (!appointment.getAppointmentStatus().equals(request.status())
                || !appointment.getScheduledAt().equals(request.scheduledAt())
                || !java.util.Objects.equals(appointment.getBookingVersion(), request.version())) {
            throw new IllegalArgumentException("This booking has changed. Reload it before continuing.");
        }
    }
}
