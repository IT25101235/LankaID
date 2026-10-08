package lk.sliit.identityissuanceservice;

import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Model.Appointment;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Service.BookingChangePolicy;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class BookingChangePolicyTests {
    @Test void biometricDeadlineIncludesExactlySeventyTwoHours() {
        var now = LocalDateTime.of(2026, 9, 15, 10, 0);
        var appointment = new Appointment(); appointment.setAppointmentType("BIOMETRIC");
        appointment.setAppointmentStatus("CONFIRMED"); appointment.setScheduledAt(now.plusHours(72));
        assertTrue(BookingChangePolicy.canChange(appointment, now));
        assertFalse(BookingChangePolicy.canChange(appointment, now.plusSeconds(1)));
        appointment.setAppointmentStatus("CHECKED_IN");
        assertFalse(BookingChangePolicy.canChange(appointment, now));
    }
    @Test void walkInChangesStopAtStartTime() {
        var now = LocalDateTime.of(2026, 9, 15, 10, 0);
        var appointment = new Appointment(); appointment.setAppointmentType("WALK_IN");
        appointment.setAppointmentStatus("CONFIRMED"); appointment.setScheduledAt(now.plusSeconds(1));
        assertTrue(BookingChangePolicy.canChange(appointment, now));
        assertFalse(BookingChangePolicy.canChange(appointment, now.plusSeconds(1)));
    }

    @Test void missedConfirmedBookingCanBeRescheduledButNotCancelled() {
        var now = LocalDateTime.of(2026, 9, 15, 10, 0);
        var appointment = new Appointment(); appointment.setAppointmentType("WALK_IN");
        appointment.setAppointmentStatus("CONFIRMED"); appointment.setScheduledAt(now.minusDays(1));
        assertTrue(BookingChangePolicy.canReschedule(appointment, now));
        assertFalse(BookingChangePolicy.canCancel(appointment, now));
        appointment.setAppointmentStatus("CHECKED_IN");
        assertFalse(BookingChangePolicy.canReschedule(appointment, now));
    }

    @Test void checkInIsAllowedOnlyOnBookedCalendarDate() {
        var appointment = new Appointment(); appointment.setAppointmentStatus("CONFIRMED");
        appointment.setScheduledAt(LocalDate.now().atTime(8, 0));
        assertDoesNotThrow(() -> BookingChangePolicy.requireCheckInToday(appointment));
        appointment.setScheduledAt(LocalDate.now().minusDays(1).atTime(8, 0));
        assertThrows(IllegalArgumentException.class,
                () -> BookingChangePolicy.requireCheckInToday(appointment));
        appointment.setScheduledAt(LocalDate.now().plusDays(1).atTime(8, 0));
        assertThrows(IllegalArgumentException.class,
                () -> BookingChangePolicy.requireCheckInToday(appointment));
    }
}
