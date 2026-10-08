package lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Service;

import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Dto.BookingChangeRequest;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Model.Appointment;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Repository.*;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Repository.CitizenApplicationRepository;
import lk.sliit.identityissuanceservice.Notification_Mgmt.Observer.NotificationPublisher;
import lk.sliit.identityissuanceservice.Payment_Mgmt.Service.PaymentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service: cancel or reschedule a confirmed biometric booking. The original payment is kept, so no new
 * payment.
 */
@Service
public class BiometricBookingChangeService {
    // OOP - Composition and Encapsulation: the repositories and services is a private final field handed
    // in through the constructor (dependency injection), so this class is loosely coupled and the object
    // is never created here.
    private final AppointmentRepository appointments;
    private final AppointmentCheckInRepository checkIns;
    private final CitizenApplicationRepository applications;
    private final BiometricScheduleService schedules;
    private final PaymentService payments;
    private final NotificationPublisher notificationPublisher;

    public BiometricBookingChangeService(AppointmentRepository appointments,
            AppointmentCheckInRepository checkIns, CitizenApplicationRepository applications,
            BiometricScheduleService schedules,
            PaymentService payments, NotificationPublisher notificationPublisher) {
        this.appointments = appointments;
        this.checkIns = checkIns;
        this.applications = applications;
        this.schedules = schedules;
        this.payments = payments;
        this.notificationPublisher = notificationPublisher;
    }

    /** UPDATE: status becomes CANCELLED, the QR is removed and the application goes back to APPROVED. */
    @Transactional
    public Appointment cancel(Integer citizenId, BookingChangeRequest request) {
        Appointment appointment = ownedBooking(citizenId, request);
        BookingChangePolicy.requireCancellationAllowed(appointment);
        appointment.setAppointmentStatus("CANCELLED");
        appointment.setQrCode(null);
        appointments.save(appointment);
        var application = applications.findById(appointment.getApplicationId()).orElseThrow();
        application.setCurrentStatus("APPROVED");
        applications.save(application);
        notificationPublisher.sendToCitizen(citizenId, null, "APPOINTMENT_CANCELLED",
                "Booking " + appointment.getReferenceNo() + " was cancelled and its place released. "
                + "Your original payment is retained. Book another available slot without paying again to continue your application.");
        return appointment;
    }

    /** UPDATE: lock the new slot and move the booking to it (also used to rebook after a cancel). */
    @Transactional
    public Appointment changeBooking(Integer citizenId, BookingChangeRequest request, Integer slotId) {
        Appointment appointment = ownedBooking(citizenId, request);
        var application = applications.findById(appointment.getApplicationId()).orElseThrow();
        boolean rebooking = "CANCELLED".equals(appointment.getAppointmentStatus());
        if (!rebooking) BookingChangePolicy.requireRescheduleAllowed(appointment);
        var slot = schedules.lockBookableSlot(slotId, application.getServiceType(), appointment.getAppointmentId());
        var newTime = slot.getSlotDate().atTime(slot.getSlotTime());
        if (!rebooking && slot.getSlotId().equals(appointment.getBiometricSlotId())) {
            throw new IllegalArgumentException("Choose a different appointment slot.");
        }
        appointment.setCentreId(slot.getCentreId());
        appointment.setBiometricSlotId(slot.getSlotId());
        appointment.setScheduledAt(newTime);
        appointment.setAppointmentStatus("CONFIRMED");
        appointment.setQrCode(appointment.getReferenceNo());
        appointments.save(appointment);
        application.setCurrentStatus("APPOINTMENT_BOOKED");
        applications.save(application);
        notificationPublisher.sendToCitizen(citizenId, null, "APPOINTMENT_CONFIRMED",
                "Booking " + appointment.getReferenceNo() + " is confirmed for " + newTime
                        + ". Your original payment was retained; no additional payment was required.");
        return appointment;
    }

    /** Safety checks: owner, unchanged since the page opened, not checked in, application paid. */
    private Appointment ownedBooking(Integer citizenId, BookingChangeRequest request) {
        Appointment appointment = appointments.findByIdForChange(request.appointmentId())
                .orElseThrow(() -> new IllegalArgumentException("Booking was not found."));
        if (!"BIOMETRIC".equals(appointment.getAppointmentType()) || !citizenId.equals(appointment.getCitizenId())) {
            throw new IllegalArgumentException("Booking was not found.");
        }
        BookingChangePolicy.requireUnchanged(appointment, request);
        if (checkIns.existsById(appointment.getAppointmentId())) {
            throw new IllegalArgumentException("A checked-in booking cannot be changed.");
        }
        var application = applications.findById(appointment.getApplicationId()).orElseThrow();
        if (!java.util.List.of("APPROVED", "APPOINTMENT_BOOKED").contains(application.getCurrentStatus())
                || !payments.hasPaid(appointment.getApplicationId())) {
            throw new IllegalArgumentException("This application is not available for booking changes.");
        }
        return appointment;
    }
}
