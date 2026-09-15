package lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Service;

import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Dto.AppointmentBookingDetails;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Dto.OfficerBiometricDetails;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Model.Appointment;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Model.AppointmentCheckIn;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Model.Centre;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Repository.AppointmentCheckInRepository;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Repository.AppointmentRepository;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Repository.BiometricCollectionRepository;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Repository.CentreRepository;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model.CitizenApplication;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Repository.CitizenApplicationRepository;
import lk.sliit.identityissuanceservice.Notification_Mgmt.Service.NotificationService;
import lk.sliit.identityissuanceservice.Payment_Mgmt.Service.PaymentService;
import lk.sliit.identityissuanceservice.User_Mgmt.Model.CitizenAccount;
import lk.sliit.identityissuanceservice.User_Mgmt.Repository.CitizenAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class AppointmentService {

    @org.springframework.beans.factory.annotation.Value("${app.biometric.slot-capacity:1}")
    private int slotCapacity = 1;

    private static final List<String> OFFICER_QUEUE_STATUSES =
            List.of("CONFIRMED", "CHECKED_IN", "COMPLETED");

    private final AppointmentRepository appointmentRepository;
    private final AppointmentCheckInRepository checkInRepository;
    private final BiometricCollectionRepository biometricRepository;
    private final CentreRepository centreRepository;
    private final CitizenApplicationRepository applicationRepository;
    private final CitizenAccountRepository citizenRepository;
    private final PaymentService paymentService;
    private final NotificationService notificationService;

    public AppointmentService(
            AppointmentRepository appointmentRepository,
            AppointmentCheckInRepository checkInRepository,
            BiometricCollectionRepository biometricRepository,
            CentreRepository centreRepository,
            CitizenApplicationRepository applicationRepository,
            CitizenAccountRepository citizenRepository,
            PaymentService paymentService,
            NotificationService notificationService
    ) {
        this.appointmentRepository = appointmentRepository;
        this.checkInRepository = checkInRepository;
        this.biometricRepository = biometricRepository;
        this.centreRepository = centreRepository;
        this.applicationRepository = applicationRepository;
        this.citizenRepository = citizenRepository;
        this.paymentService = paymentService;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public AppointmentBookingDetails getBookingDetails(Integer citizenId, Integer applicationId) {
        CitizenApplication application = findOwnedApplication(citizenId, applicationId);
        if (!paymentService.hasPaid(applicationId)) {
            throw new IllegalArgumentException("Complete the required payment before booking biometrics.");
        }
        Appointment appointment = appointmentRepository
                .findByApplicationIdAndAppointmentType(applicationId, "BIOMETRIC")
                .orElseThrow(() -> new IllegalArgumentException("Payment appointment record was not found."));
        List<Centre> centres = centreRepository.findAllByOrderByCentreNameAsc();
        if (centres.isEmpty()) {
            throw new IllegalArgumentException("No service centres are available. Please contact the system administrator.");
        }
        return new AppointmentBookingDetails(
                application,
                appointment,
                centres,
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(14)
        );
    }

    @Transactional
    public Appointment bookAppointment(
            Integer citizenId,
            Integer applicationId,
            Integer centreId,
            LocalDate date,
            LocalTime time
    ) {
        CitizenApplication application = findOwnedApplication(citizenId, applicationId);
        if (!paymentService.hasPaid(applicationId)) {
            throw new IllegalArgumentException("Complete the required payment before booking biometrics.");
        }
        Appointment appointment = appointmentRepository
                .findByApplicationIdAndAppointmentType(applicationId, "BIOMETRIC")
                .orElseThrow(() -> new IllegalArgumentException("Appointment record was not found."));
        Centre centre = centreRepository.findByIdForBooking(centreId)
                .orElseThrow(() -> new IllegalArgumentException("Select a valid service centre."));
        appointment = appointmentRepository.findByIdForChange(appointment.getAppointmentId()).orElseThrow();
        if (!"PENDING_PAYMENT".equals(appointment.getAppointmentStatus())
                || !"APPROVED".equals(application.getCurrentStatus())) {
            throw new IllegalArgumentException("Use Manage booking to reschedule or pay the LKR 300 rebooking fee.");
        }

        LocalDateTime scheduledAt = date.atTime(time);
        validateSelection(centre, date, time, appointment.getAppointmentId());

        appointment.setCentreId(centreId);
        appointment.setScheduledAt(scheduledAt);
        appointment.setAppointmentStatus("CONFIRMED");
        appointment.setQrCode(appointment.getReferenceNo());
        appointment = appointmentRepository.save(appointment);

        application.setCurrentStatus("APPOINTMENT_BOOKED");
        applicationRepository.save(application);

        notificationService.sendToCitizen(
                citizenId,
                null,
                "APPOINTMENT_CONFIRMED",
                "Biometric appointment " + appointment.getReferenceNo() + " is confirmed for "
                        + appointment.getScheduledAt() + " at " + centre.getCentreName()
                        + ". Bring the QR appointment pass."
        );
        return appointment;
    }

    @Transactional(readOnly = true)
    public Appointment getCitizenAppointment(Integer citizenId, String reference) {
        Appointment appointment = appointmentRepository.findByReferenceNoIgnoreCase(reference)
                .orElseThrow(() -> new IllegalArgumentException("Appointment was not found."));
        if (!citizenId.equals(appointment.getCitizenId())) {
            throw new IllegalArgumentException("Appointment was not found.");
        }
        return appointment;
    }

    @Transactional(readOnly = true)
    public OfficerBiometricDetails getCitizenPassDetails(Integer citizenId, String reference) {
        Appointment appointment = getCitizenAppointment(citizenId, reference);
        CitizenApplication application = applicationRepository.findById(appointment.getApplicationId())
                .orElseThrow(() -> new IllegalArgumentException("Application was not found."));
        if (!OFFICER_QUEUE_STATUSES.contains(appointment.getAppointmentStatus())) {
            throw new IllegalArgumentException("Confirm an appointment before opening its pass.");
        }
        return buildOfficerDetails(appointment, application.getServiceType(), true);
    }

    @Transactional(readOnly = true)
    public List<OfficerBiometricDetails> getOfficerQueue(String serviceType) {
        return appointmentRepository
                .findAllByAppointmentTypeAndAppointmentStatusInOrderByScheduledAtAsc(
                        "BIOMETRIC", OFFICER_QUEUE_STATUSES)
                .stream()
                .map(appointment -> buildOfficerDetails(appointment, serviceType, false))
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    @Transactional(readOnly = true)
    public OfficerBiometricDetails findForOfficer(String reference, String serviceType) {
        String cleanReference = reference == null ? "" : reference.trim();
        if (cleanReference.isBlank()) {
            throw new IllegalArgumentException("Enter or scan an appointment reference.");
        }

        Appointment appointment = appointmentRepository.findByReferenceNoIgnoreCase(cleanReference)
                .orElseGet(() -> applicationRepository.findByApplicationNoIgnoreCase(cleanReference)
                        .flatMap(application -> appointmentRepository
                                .findByApplicationIdAndAppointmentType(
                                        application.getApplicationId(), "BIOMETRIC"))
                        .orElseThrow(() -> new IllegalArgumentException("Appointment was not found.")));
        OfficerBiometricDetails details = buildOfficerDetails(appointment, serviceType, true);
        if (details == null) {
            throw new IllegalArgumentException("Appointment does not belong to this officer service.");
        }
        return details;
    }

    @Transactional
    public OfficerBiometricDetails checkIn(
            String reference,
            String serviceType,
            Integer staffId,
            String method
    ) {
        if (staffId == null) throw new IllegalArgumentException("Officer session was not found.");
        OfficerBiometricDetails details = findForOfficer(reference, serviceType);
        Appointment appointment = appointmentRepository.findByIdForChange(details.appointment().getAppointmentId()).orElseThrow();
        if (!List.of("CONFIRMED", "CHECKED_IN").contains(appointment.getAppointmentStatus())) {
            throw new IllegalArgumentException("Only a confirmed appointment can be checked in.");
        }
        if ("COMPLETED".equals(appointment.getAppointmentStatus())) {
            throw new IllegalArgumentException("Biometrics have already been completed.");
        }

        AppointmentCheckIn checkIn = checkInRepository.findById(appointment.getAppointmentId())
                .orElseGet(AppointmentCheckIn::new);
        checkIn.setAppointmentId(appointment.getAppointmentId());
        checkIn.setStaffId(staffId);
        checkIn.setCheckedInAt(LocalDateTime.now());
        checkIn.setCheckInMethod("QR".equals(method) ? "QR" : "REFERENCE_NUMBER");
        checkInRepository.save(checkIn);

        appointment.setAppointmentStatus("CHECKED_IN");
        appointmentRepository.save(appointment);
        notificationService.sendToCitizen(
                appointment.getCitizenId(),
                staffId,
                "APPOINTMENT_CHECKED_IN",
                "You have checked in for biometric appointment " + appointment.getReferenceNo() + "."
        );
        return findForOfficer(reference, serviceType);
    }

    private OfficerBiometricDetails buildOfficerDetails(
            Appointment appointment,
            String serviceType,
            boolean rejectPendingPayment
    ) {
        CitizenApplication application = applicationRepository.findById(appointment.getApplicationId())
                .orElse(null);
        if (application == null || !serviceType.equals(application.getServiceType())) return null;
        if (rejectPendingPayment && !OFFICER_QUEUE_STATUSES.contains(appointment.getAppointmentStatus())) {
            throw new IllegalArgumentException("This appointment has not been paid and confirmed.");
        }
        CitizenAccount citizen = citizenRepository.findById(application.getCitizenId())
                .orElseThrow(() -> new IllegalArgumentException("Citizen details were not found."));
        Centre centre = centreRepository.findById(appointment.getCentreId())
                .orElseThrow(() -> new IllegalArgumentException("Service centre was not found."));
        String name = ((citizen.getGivenName() == null ? "" : citizen.getGivenName()) + " "
                + (citizen.getFamilyName() == null ? "" : citizen.getFamilyName())).trim();
        if (name.isBlank()) name = "Citizen #" + citizen.getUserId();

        return new OfficerBiometricDetails(
                appointment,
                application,
                citizen,
                centre,
                name,
                checkInRepository.existsById(appointment.getAppointmentId()),
                biometricRepository.existsById(application.getApplicationId())
        );
    }

    private CitizenApplication findOwnedApplication(Integer citizenId, Integer applicationId) {
        return applicationRepository.findByApplicationIdAndCitizenId(applicationId, citizenId)
                .orElseThrow(() -> new IllegalArgumentException("Application was not found."));
    }

    public void validateSelection(Centre centre, LocalDate date, LocalTime time, Integer excludedId) {
        if (date == null || date.isBefore(LocalDate.now().plusDays(1)) || date.isAfter(LocalDate.now().plusDays(14))) {
            throw new IllegalArgumentException("Choose a date from tomorrow through the next 14 days.");
        }
        if (time == null) throw new IllegalArgumentException("Choose an appointment time.");
        validateWorkingTime(time);
        if (remainingCapacity(centre, date, excludedId) < 1) {
            throw new IllegalArgumentException("That centre is fully booked for the selected date. Choose another date or centre.");
        }
        if (remainingSlotCapacity(centre, date.atTime(time), excludedId) < 1) {
            throw new IllegalArgumentException("That time slot is fully booked. Choose another time; your current booking has not changed.");
        }
    }

    private void validateWorkingTime(LocalTime time) {
        if (time.isBefore(LocalTime.of(9, 0)) || time.isAfter(LocalTime.of(16, 0))
                || time.getHour() == 12 || time.getSecond() != 0 || time.getNano() != 0
                || (time.getMinute() != 0 && time.getMinute() != 30)) {
            throw new IllegalArgumentException("Choose a 30-minute slot between 9:00 AM and 4:00 PM.");
        }
    }

    public int remainingCapacity(Centre centre, LocalDate date, Integer excludedId) {
        long bookings = appointmentRepository.countBiometricBookings(centre.getCentreId(),
                date.atStartOfDay(), date.plusDays(1).atStartOfDay(), excludedId == null ? -1 : excludedId);
        return Math.max(0, centre.getDailyCapacity() - (int) bookings);
    }

    public int remainingSlotCapacity(Centre centre, LocalDateTime time, Integer excludedId) {
        long booked = appointmentRepository.countBiometricSlotBookings(centre.getCentreId(), time,
                excludedId == null ? -1 : excludedId);
        return Math.max(0, slotCapacity - (int) booked);
    }
}
