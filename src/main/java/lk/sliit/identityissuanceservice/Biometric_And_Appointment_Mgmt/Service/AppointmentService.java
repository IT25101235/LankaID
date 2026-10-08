package lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Service;

import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Dto.AppointmentBookingDetails;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Dto.OfficerBiometricDetails;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Dto.BiometricSlotView;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Model.Appointment;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Model.AppointmentCheckIn;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Model.Centre;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Repository.AppointmentCheckInRepository;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Repository.AppointmentRepository;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Repository.BiometricCollectionRepository;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Repository.CentreRepository;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model.CitizenApplication;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Repository.CitizenApplicationRepository;
import lk.sliit.identityissuanceservice.Notification_Mgmt.Observer.NotificationPublisher;
import lk.sliit.identityissuanceservice.Payment_Mgmt.Service.PaymentService;
import lk.sliit.identityissuanceservice.User_Mgmt.Model.CitizenAccount;
import lk.sliit.identityissuanceservice.User_Mgmt.Repository.CitizenAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AppointmentService {

    private static final List<String> OFFICER_QUEUE_STATUSES =
            List.of("CONFIRMED", "CHECKED_IN", "COMPLETED");

    private final AppointmentRepository appointmentRepository;
    private final AppointmentCheckInRepository checkInRepository;
    private final BiometricCollectionRepository biometricRepository;
    private final CentreRepository centreRepository;
    private final CitizenApplicationRepository applicationRepository;
    private final CitizenAccountRepository citizenRepository;
    private final PaymentService paymentService;
    private final NotificationPublisher notificationPublisher;
    private final BiometricScheduleService scheduleService;

    public AppointmentService(
            AppointmentRepository appointmentRepository,
            AppointmentCheckInRepository checkInRepository,
            BiometricCollectionRepository biometricRepository,
            CentreRepository centreRepository,
            CitizenApplicationRepository applicationRepository,
            CitizenAccountRepository citizenRepository,
            PaymentService paymentService,
            NotificationPublisher notificationPublisher,
            BiometricScheduleService scheduleService
    ) {
        this.appointmentRepository = appointmentRepository;
        this.checkInRepository = checkInRepository;
        this.biometricRepository = biometricRepository;
        this.centreRepository = centreRepository;
        this.applicationRepository = applicationRepository;
        this.citizenRepository = citizenRepository;
        this.paymentService = paymentService;
        this.notificationPublisher = notificationPublisher;
        this.scheduleService = scheduleService;
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
        List<Centre> centres = centreRepository.findAllByCentreStatusOrderByCentreNameAsc("ACTIVE");
        if (centres.isEmpty()) {
            throw new IllegalArgumentException("No service centres are available. Please contact the system administrator.");
        }
        return new AppointmentBookingDetails(
                application,
                appointment,
                centres,
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(90)
        );
    }

    @Transactional
    public Appointment bookAppointment(
            Integer citizenId,
            Integer applicationId,
            Integer slotId
    ) {
        CitizenApplication application = findOwnedApplication(citizenId, applicationId);
        if (!paymentService.hasPaid(applicationId)) {
            throw new IllegalArgumentException("Complete the required payment before booking biometrics.");
        }
        Appointment appointment = appointmentRepository
                .findByApplicationIdAndAppointmentType(applicationId, "BIOMETRIC")
                .orElseThrow(() -> new IllegalArgumentException("Appointment record was not found."));
        appointment = appointmentRepository.findByIdForChange(appointment.getAppointmentId()).orElseThrow();
        if (!"PENDING_PAYMENT".equals(appointment.getAppointmentStatus())
                || !List.of("APPROVED", "PAYMENT_PENDING").contains(application.getCurrentStatus())) {
            throw new IllegalArgumentException("Use Manage booking to change your confirmed appointment.");
        }

        var slot = scheduleService.lockBookableSlot(slotId, application.getServiceType(), appointment.getAppointmentId());
        Centre centre = centreRepository.findById(slot.getCentreId())
                .orElseThrow(() -> new IllegalArgumentException("The slot's service centre was not found."));
        LocalDateTime scheduledAt = slot.getSlotDate().atTime(slot.getSlotTime());

        appointment.setCentreId(slot.getCentreId());
        appointment.setBiometricSlotId(slot.getSlotId());
        appointment.setScheduledAt(scheduledAt);
        appointment.setAppointmentStatus("CONFIRMED");
        appointment.setQrCode(appointment.getReferenceNo());
        appointment = appointmentRepository.save(appointment);

        application.setCurrentStatus("APPOINTMENT_BOOKED");
        applicationRepository.save(application);

        notificationPublisher.sendToCitizen(
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
    public List<OfficerBiometricDetails> getOfficerHistory(String serviceType) {
        return appointmentRepository
                .findAllByAppointmentTypeAndAppointmentStatusInOrderByScheduledAtAsc(
                        "BIOMETRIC", OFFICER_QUEUE_STATUSES)
                .stream()
                .map(appointment -> buildOfficerDetails(appointment, serviceType, false))
                .filter(java.util.Objects::nonNull)
                .sorted(java.util.Comparator
                        .comparing(
                                (OfficerBiometricDetails details) -> details.application().getUpdatedAt(),
                                java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())
                        )
                        .thenComparing(
                                details -> details.appointment().getAppointmentId(),
                                java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())
                        ))
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
        if ("COMPLETED".equals(appointment.getAppointmentStatus())) {
            throw new IllegalArgumentException("Biometrics have already been completed.");
        }
        if ("CHECKED_IN".equals(appointment.getAppointmentStatus())) {
            throw new IllegalArgumentException("This citizen has already been checked in.");
        }
        if (!"CONFIRMED".equals(appointment.getAppointmentStatus())) {
            throw new IllegalArgumentException("Only a confirmed appointment can be checked in.");
        }
        if (!appointment.getScheduledAt().toLocalDate().equals(LocalDate.now())) {
            throw new IllegalArgumentException("Check-in is only available on the appointment date ("
                    + appointment.getScheduledAt().toLocalDate() + ").");
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
        notificationPublisher.sendToCitizen(
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

    @Transactional(readOnly = true)
    public List<BiometricSlotView> getAvailableSchedule(
            String serviceType, Integer centreId, LocalDate startDate, LocalDate endDate) {
        return scheduleService.getSchedule(serviceType, centreId, startDate, endDate);
    }
}
