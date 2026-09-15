package lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Service;

import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Dto.BookingChangeRequest;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Model.Appointment;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Model.Centre;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Repository.*;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Repository.CitizenApplicationRepository;
import lk.sliit.identityissuanceservice.Notification_Mgmt.Service.NotificationService;
import lk.sliit.identityissuanceservice.Payment_Mgmt.Service.AppointmentChangePaymentService;
import lk.sliit.identityissuanceservice.Payment_Mgmt.Service.PaymentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalTime;

@Service
public class BiometricBookingChangeService {
    private final AppointmentRepository appointments;
    private final CentreRepository centres;
    private final AppointmentCheckInRepository checkIns;
    private final CitizenApplicationRepository applications;
    private final AppointmentService bookingService;
    private final PaymentService payments;
    private final AppointmentChangePaymentService changePayments;
    private final NotificationService notifications;

    public BiometricBookingChangeService(AppointmentRepository appointments, CentreRepository centres,
            AppointmentCheckInRepository checkIns, CitizenApplicationRepository applications,
            AppointmentService bookingService, PaymentService payments,
            AppointmentChangePaymentService changePayments, NotificationService notifications) {
        this.appointments = appointments;
        this.centres = centres;
        this.checkIns = checkIns;
        this.applications = applications;
        this.bookingService = bookingService;
        this.payments = payments;
        this.changePayments = changePayments;
        this.notifications = notifications;
    }

    @Transactional
    public Appointment cancel(Integer citizenId, BookingChangeRequest request) {
        Appointment appointment = ownedBooking(citizenId, request);
        BookingChangePolicy.requireChangeAllowed(appointment);
        appointment.setAppointmentStatus("CANCELLED");
        appointment.setQrCode(null);
        appointments.save(appointment);
        var application = applications.findById(appointment.getApplicationId()).orElseThrow();
        application.setCurrentStatus("APPROVED");
        applications.save(application);
        notifications.sendToCitizen(citizenId, null, "APPOINTMENT_CANCELLED",
                "Booking " + appointment.getReferenceNo() + " was cancelled and its place released. "
                + "Your original payment is retained. Pay LKR 300 when rebooking to continue your application.");
        return appointment;
    }

    @Transactional
    public Appointment payAndChange(Integer citizenId, BookingChangeRequest request,
                                    Integer centreId, LocalDate date, LocalTime time) {
        if (centreId == null) throw new IllegalArgumentException("Select a service centre.");
        Centre centre = centres.findByIdForBooking(centreId)
                .orElseThrow(() -> new IllegalArgumentException("Select a valid service centre."));
        Appointment appointment = ownedBooking(citizenId, request);
        boolean rebooking = "CANCELLED".equals(appointment.getAppointmentStatus());
        if (!rebooking) BookingChangePolicy.requireChangeAllowed(appointment);
        bookingService.validateSelection(centre, date, time, appointment.getAppointmentId());
        var newTime = date.atTime(time);
        if (!rebooking && centreId.equals(appointment.getCentreId()) && newTime.equals(appointment.getScheduledAt())) {
            throw new IllegalArgumentException("Choose a different date, time or centre. No payment is needed for an unchanged booking.");
        }
        changePayments.record(appointment, newTime, rebooking ? "REBOOK" : "RESCHEDULE", request.token());
        appointment.setCentreId(centreId);
        appointment.setScheduledAt(newTime);
        appointment.setAppointmentStatus("CONFIRMED");
        appointment.setQrCode(appointment.getReferenceNo());
        appointments.save(appointment);
        var application = applications.findById(appointment.getApplicationId()).orElseThrow();
        application.setCurrentStatus("APPOINTMENT_BOOKED");
        applications.save(application);
        notifications.sendToCitizen(citizenId, null, "APPOINTMENT_CONFIRMED",
                "LKR 300 received. Booking " + appointment.getReferenceNo() + " is confirmed for "
                + newTime + ". Download the updated appointment pass.");
        return appointment;
    }

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
