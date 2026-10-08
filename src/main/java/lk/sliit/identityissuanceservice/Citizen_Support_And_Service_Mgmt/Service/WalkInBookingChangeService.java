package lk.sliit.identityissuanceservice.Citizen_Support_And_Service_Mgmt.Service;

import jakarta.servlet.http.HttpSession;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Dto.BookingChangeRequest;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Model.Appointment;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Repository.AppointmentCheckInRepository;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Service.BookingChangePolicy;
import lk.sliit.identityissuanceservice.Citizen_Support_And_Service_Mgmt.Repository.WalkInAppointmentRepository;
import lk.sliit.identityissuanceservice.Citizen_Support_And_Service_Mgmt.Repository.WalkInSlotRepository;
import lk.sliit.identityissuanceservice.OTP_Mgmt.Otp.SriLankanMobileNumber;
import lk.sliit.identityissuanceservice.Payment_Mgmt.Service.AppointmentChangePaymentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Applies cancellation and paid rescheduling rules to recovered walk-in bookings. */
@Service
public class WalkInBookingChangeService {
    private final WalkInAppointmentService bookings;
    private final WalkInAppointmentRepository appointments;
    private final WalkInSlotRepository slots;
    private final AppointmentCheckInRepository checkIns;
    private final AppointmentChangePaymentService payments;

    public WalkInBookingChangeService(WalkInAppointmentService bookings, WalkInAppointmentRepository appointments,
            WalkInSlotRepository slots, AppointmentCheckInRepository checkIns, AppointmentChangePaymentService payments) {
        this.bookings = bookings;
        this.appointments = appointments;
        this.slots = slots;
        this.checkIns = checkIns;
        this.payments = payments;
    }

    @Transactional
    public void cancel(String reference, BookingChangeRequest request, HttpSession session) {
        Appointment appointment = ownedBooking(reference, request, session);
        // The shared policy checks the time window and current status before cancellation.
        BookingChangePolicy.requireCancellationAllowed(appointment);
        appointment.setAppointmentStatus("CANCELLED");
        appointment.setQrCode(null);
        appointments.save(appointment);
    }

    @Transactional
    public void payAndReschedule(String reference, BookingChangeRequest request, Integer slotId, HttpSession session) {
        if (slotId == null) throw new IllegalArgumentException("Select a new appointment slot.");
        // Lock the destination just as a new visitor booking does, then recheck capacity.
        var slot = slots.findByIdForBooking(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Select an available slot."));
        Appointment appointment = ownedBooking(reference, request, session);
        BookingChangePolicy.requireRescheduleAllowed(appointment);
        var newTime = slot.getSlotDate().atTime(slot.getSlotTime());
        if (slot.getCentreId().equals(appointment.getCentreId()) && newTime.equals(appointment.getScheduledAt())) {
            throw new IllegalArgumentException("Choose a different slot. Your existing booking does not need another payment.");
        }
        bookings.requireAvailableSlot(slot);
        if (bookings.getPaidPayment(appointment.getAppointmentId()) == null) {
            throw new IllegalArgumentException("The original booking payment was not found. Contact the service centre.");
        }
        // Record the change charge before moving the booking; the transaction rolls both back on failure.
        payments.record(appointment, newTime, "RESCHEDULE", request.token());
        appointment.setCentreId(slot.getCentreId());
        appointment.setScheduledAt(newTime);
        appointments.save(appointment);
        // Availability is calculated from confirmed bookings; moving this row releases the old slot.
    }

    private Appointment ownedBooking(String reference, BookingChangeRequest request, HttpSession session) {
        // Recovery verification proves control of the phone number linked to the booking.
        if (!bookings.isRecoveryVerified(session)) {
            throw new IllegalArgumentException("Verify your mobile number again before managing bookings.");
        }
        Appointment appointment = appointments.findForChange(reference)
                .orElseThrow(() -> new IllegalArgumentException("Booking was not found."));
        if (!appointment.getAppointmentId().equals(request.appointmentId())
                || !SriLankanMobileNumber.normalize(appointment.getVisitorPhone()).equals(bookings.recoveryPhone(session))) {
            throw new IllegalArgumentException("Booking was not found for your verified number.");
        }
        // The signed request snapshot detects changes made from another tab or officer action.
        BookingChangePolicy.requireUnchanged(appointment, request);
        if (checkIns.existsById(appointment.getAppointmentId())) {
            throw new IllegalArgumentException("A checked-in booking cannot be changed.");
        }
        return appointment;
    }
}
