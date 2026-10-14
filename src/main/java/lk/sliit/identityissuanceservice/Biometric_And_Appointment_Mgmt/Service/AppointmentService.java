package lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Service;

import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Dto.AppointmentBookingDetails;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Dto.OfficerBiometricDetails;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Dto.BiometricSlotView;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Model.Appointment;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Model.AppointmentCheckIn;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Model.BiometricCollection;
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
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Service: core booking logic (book a slot, passes, officer queue, check-in, missed bookings). Sends
 * notifications through NotificationPublisher (Observer pattern).
 * <p>
 * Design Pattern - Service Layer: all booking rules live here, so the controllers stay thin.<br>
 * Design Pattern - Observer: notificationPublisher announces each change and the notification module reacts.<br>
 * Design Pattern - Repository and Dependency Injection: data access and the other services are handed in.<br>
 * Exception handling: a broken rule throws IllegalArgumentException with a friendly message, and
 * @Transactional rolls the whole change back, so a half-finished booking is never saved.<br>
 * Concurrency: the slot row and then the appointment row are locked while they change.
 */
@Service
public class AppointmentService {

    private static final List<String> OFFICER_ACTIVE_QUEUE_STATUSES =
            List.of("CONFIRMED", "CHECKED_IN");
    private static final List<String> OFFICER_HISTORY_STATUSES =
            List.of("CONFIRMED", "CHECKED_IN", "COMPLETED");
    private static final int LOOKUP_CHUNK_SIZE = 1000;

    // OOP - Composition and Encapsulation: the repositories and other services are private final fields
    // handed in through the constructor (Design Pattern - Dependency Injection), so this class is loosely
    // coupled and the objects are never created here.
    private final AppointmentRepository appointmentRepository;
    private final AppointmentCheckInRepository checkInRepository;
    private final BiometricCollectionRepository biometricRepository;
    private final CentreRepository centreRepository;
    private final CitizenApplicationRepository applicationRepository;
    private final CitizenAccountRepository citizenRepository;
    private final PaymentService paymentService;
    private final NotificationPublisher notificationPublisher;
    private final BiometricScheduleService scheduleService;

    @PersistenceContext
    private EntityManager entityManager;

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

    /** READ: booking page data. Payment must be completed first. */
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
                LocalDate.now(),
                LocalDate.now().plusDays(90)
        );
    }

    /**
     * CREATE: confirm a slot for a paid application. Appointment and slot are locked so the last place
     * cannot be double booked.
     */
    // OOP - Abstraction: controllers just call this method; locking, payment check and notification stay
    // hidden inside.
    @Transactional
    public Appointment bookAppointment(
            Integer citizenId,
            Integer applicationId,
            Integer slotId
    ) {
        scheduleService.lockSlotBeforeAppointment(slotId);
        CitizenApplication application = findOwnedApplication(citizenId, applicationId);
        if (!paymentService.hasPaid(applicationId)) {
            throw new IllegalArgumentException("Complete the required payment before booking biometrics.");
        }
        Appointment appointment = appointmentRepository
                .findByApplicationIdAndAppointmentType(applicationId, "BIOMETRIC")
                .orElseThrow(() -> new IllegalArgumentException("Appointment record was not found."));
        appointment = appointmentRepository.findByIdForChange(appointment.getAppointmentId()).orElseThrow();
        // Input validation: only a paid booking that is still waiting for a slot can be booked.
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

    /** READ: ownership check, so citizens can only open their own bookings. */
    @Transactional(readOnly = true)
    public Appointment getCitizenAppointment(Integer citizenId, String reference) {
        Appointment appointment = appointmentRepository.findByReferenceNoIgnoreCase(reference)
                .orElseThrow(() -> new IllegalArgumentException("Appointment was not found."));
        if (!citizenId.equals(appointment.getCitizenId())) {
            throw new IllegalArgumentException("Appointment was not found.");
        }
        return appointment;
    }

    /** READ: pass data, only once the appointment is confirmed. */
    @Transactional(readOnly = true)
    public OfficerBiometricDetails getCitizenPassDetails(Integer citizenId, String reference) {
        Appointment appointment = getCitizenAppointment(citizenId, reference);
        CitizenApplication application = applicationRepository.findById(appointment.getApplicationId())
                .orElseThrow(() -> new IllegalArgumentException("Application was not found."));
        if (!OFFICER_HISTORY_STATUSES.contains(appointment.getAppointmentStatus())) {
            throw new IllegalArgumentException("Confirm an appointment before opening its pass.");
        }
        return buildOfficerDetails(appointment, application.getServiceType(), true);
    }

    /** READ: appointments waiting for the officer. */
    @Transactional(readOnly = true)
    public List<OfficerBiometricDetails> getOfficerQueue(String serviceType) {
        return buildOfficerDetails(appointmentRepository.findAllForServiceType(
                "BIOMETRIC", OFFICER_ACTIVE_QUEUE_STATUSES, serviceType), serviceType);
    }

    /** READ: processed appointments, newest first. */
    @Transactional(readOnly = true)
    public List<OfficerBiometricDetails> getOfficerHistory(String serviceType) {
        return buildOfficerDetails(appointmentRepository.findAllForServiceType(
                        "BIOMETRIC", OFFICER_HISTORY_STATUSES, serviceType), serviceType)
                .stream()
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

    /** READ: find by appointment reference or application number, for this officer's service only. */
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

    /** UPDATE: CONFIRMED to CHECKED_IN. Rejects repeats and any day other than the booked date. */
    @Transactional
    public OfficerBiometricDetails checkIn(
            String reference,
            String serviceType,
            Integer staffId,
            String method
    ) {
        if (staffId == null) throw new IllegalArgumentException("Officer session was not found.");
        OfficerBiometricDetails details = findForOfficer(reference, serviceType);
        Appointment appointment = lockAppointment(details.appointment().getAppointmentId());
        if ("COMPLETED".equals(appointment.getAppointmentStatus())) {
            throw new IllegalArgumentException("Biometrics have already been completed.");
        }
        if ("CHECKED_IN".equals(appointment.getAppointmentStatus())) {
            throw new IllegalArgumentException("This citizen has already been checked in.");
        }
        if (!"CONFIRMED".equals(appointment.getAppointmentStatus())) {
            throw new IllegalArgumentException("Only a confirmed appointment can be checked in.");
        }
        BookingChangePolicy.requireCheckInToday(appointment);

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

    /** READ: published future slots an officer can choose for this missed booking. */
    @Transactional(readOnly = true)
    public List<BiometricSlotView> getMissedRescheduleSlots(String reference, String serviceType) {
        var appointment = findForOfficer(reference, serviceType).appointment();
        requireReschedulableMissed(appointment);
        LocalDateTime now = LocalDateTime.now();
        return scheduleService.getSchedule(serviceType, appointment.getCentreId(),
                        now.toLocalDate(), now.toLocalDate().plusDays(90))
                .stream()
                .filter(slot -> slot.available() && slot.remainingPlaces() > 0
                        && slot.slotDate().atTime(slot.slotTime()).isAfter(now))
                .toList();
    }

    /** UPDATE: moves a missed booking to the officer's selected slot at the same centre. */
    @Transactional
    public OfficerBiometricDetails rescheduleMissed(
            String reference,
            String serviceType,
            Integer staffId,
            Integer slotId
    ) {
        if (staffId == null) throw new IllegalArgumentException("Officer session was not found.");
        OfficerBiometricDetails details = findForOfficer(reference, serviceType);
        // Checked once before locking, for a clear error, and again under the locks. The slot is locked
        // before the appointment, the same order as closing a slot, so the two cannot deadlock.
        requireReschedulableMissed(details.appointment());

        CitizenApplication application = applicationRepository.findById(details.appointment().getApplicationId())
                .orElseThrow(() -> new IllegalArgumentException("Application was not found."));
        if (slotId == null) throw new IllegalArgumentException("Select an available date and time.");
        var slot = scheduleService.lockBookableSlot(
                slotId, serviceType, details.appointment().getAppointmentId());
        Appointment appointment = lockAppointment(details.appointment().getAppointmentId());
        requireReschedulableMissed(appointment);
        if (!appointment.getCentreId().equals(slot.getCentreId())) {
            throw new IllegalArgumentException("Select a slot at this appointment's service centre.");
        }
        LocalDateTime newTime = slot.getSlotDate().atTime(slot.getSlotTime());
        appointment.setBiometricSlotId(slot.getSlotId());
        appointment.setScheduledAt(newTime);
        appointment.setAppointmentStatus("CONFIRMED");
        appointment.setQrCode(appointment.getReferenceNo());
        appointmentRepository.save(appointment);
        application.setCurrentStatus("APPOINTMENT_BOOKED");
        applicationRepository.save(application);
        notificationPublisher.sendToCitizen(
                appointment.getCitizenId(), staffId, "APPOINTMENT_CONFIRMED",
                "Missed biometric appointment " + appointment.getReferenceNo()
                        + " was moved to " + newTime + ". The same QR pass remains valid."
        );
        return findForOfficer(reference, serviceType);
    }

    private void requireReschedulableMissed(Appointment appointment) {
        BookingChangePolicy.requireMissedAppointment(appointment);
        if (checkInRepository.existsById(appointment.getAppointmentId())) {
            throw new IllegalArgumentException("A checked-in appointment cannot be rescheduled.");
        }
    }

    /**
     * Locks the booking row and re-reads it. The officer lookup has already loaded the appointment, so
     * without the re-read a request that waited for the lock would still see its old status.
     */
    @Transactional
    public Appointment lockAppointment(Integer appointmentId) {
        Appointment appointment = appointmentRepository.findByIdForChange(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment was not found."));
        entityManager.refresh(appointment);
        return appointment;
    }

    /** Joins appointment, application, citizen and centre into one DTO for the officer screens. */
    private OfficerBiometricDetails buildOfficerDetails(
            Appointment appointment,
            String serviceType,
            boolean rejectPendingPayment
    ) {
        CitizenApplication application = applicationRepository.findById(appointment.getApplicationId())
                .orElse(null);
        if (application == null || !serviceType.equals(application.getServiceType())) return null;
        if (rejectPendingPayment && !OFFICER_HISTORY_STATUSES.contains(appointment.getAppointmentStatus())) {
            throw new IllegalArgumentException("This appointment has not been paid and confirmed.");
        }
        CitizenAccount citizen = citizenRepository.findById(application.getCitizenId())
                .orElseThrow(() -> new IllegalArgumentException("Citizen details were not found."));
        Centre centre = centreRepository.findById(appointment.getCentreId())
                .orElseThrow(() -> new IllegalArgumentException("Service centre was not found."));
        String name = applicantName(citizen);

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

    /**
     * Builds the officer rows for a whole list in a few batched queries, instead of five queries per
     * appointment. Rows keep the order of the appointments passed in.
     */
    private List<OfficerBiometricDetails> buildOfficerDetails(List<Appointment> appointments, String serviceType) {
        if (appointments.isEmpty()) return List.of();
        Map<Integer, CitizenApplication> applications = indexById(
                findAllInChunks(appointments.stream().map(Appointment::getApplicationId).toList(),
                        applicationRepository::findAllById),
                CitizenApplication::getApplicationId);
        Map<Integer, CitizenAccount> citizens = indexById(
                findAllInChunks(applications.values().stream().map(CitizenApplication::getCitizenId).toList(),
                        citizenRepository::findAllById),
                CitizenAccount::getUserId);
        Map<Integer, Centre> centres = indexById(
                findAllInChunks(appointments.stream().map(Appointment::getCentreId).toList(),
                        centreRepository::findAllById),
                Centre::getCentreId);
        Set<Integer> checkedInIds = new HashSet<>();
        findAllInChunks(appointments.stream().map(Appointment::getAppointmentId).toList(),
                checkInRepository::findAllById).forEach(checkIn -> checkedInIds.add(checkIn.getAppointmentId()));
        Set<Integer> collectedIds = new HashSet<>();
        findAllInChunks(applications.keySet(), biometricRepository::findAllById)
                .forEach(collection -> collectedIds.add(collection.getApplicationId()));

        List<OfficerBiometricDetails> rows = new ArrayList<>();
        for (Appointment appointment : appointments) {
            CitizenApplication application = applications.get(appointment.getApplicationId());
            if (application == null || !serviceType.equals(application.getServiceType())) continue;
            CitizenAccount citizen = citizens.get(application.getCitizenId());
            if (citizen == null) throw new IllegalArgumentException("Citizen details were not found.");
            Centre centre = centres.get(appointment.getCentreId());
            if (centre == null) throw new IllegalArgumentException("Service centre was not found.");
            rows.add(new OfficerBiometricDetails(
                    appointment, application, citizen, centre, applicantName(citizen),
                    checkedInIds.contains(appointment.getAppointmentId()),
                    collectedIds.contains(application.getApplicationId())));
        }
        return rows;
    }

    /** Looks ids up in chunks, so a long list stays under SQL Server's limit of about 2,100 parameters. */
    private static <T> List<T> findAllInChunks(Collection<Integer> ids, Function<List<Integer>, List<T>> finder) {
        List<Integer> distinct = ids.stream().filter(java.util.Objects::nonNull).distinct().toList();
        List<T> found = new ArrayList<>();
        for (int from = 0; from < distinct.size(); from += LOOKUP_CHUNK_SIZE) {
            found.addAll(finder.apply(distinct.subList(from, Math.min(from + LOOKUP_CHUNK_SIZE, distinct.size()))));
        }
        return found;
    }

    private static <T> Map<Integer, T> indexById(List<T> items, Function<T, Integer> idOf) {
        Map<Integer, T> byId = new HashMap<>();
        items.forEach(item -> byId.put(idOf.apply(item), item));
        return byId;
    }

    private static String applicantName(CitizenAccount citizen) {
        String name = ((citizen.getGivenName() == null ? "" : citizen.getGivenName()) + " "
                + (citizen.getFamilyName() == null ? "" : citizen.getFamilyName())).trim();
        return name.isBlank() ? "Citizen #" + citizen.getUserId() : name;
    }

    /** Ownership check for applications. */
    private CitizenApplication findOwnedApplication(Integer citizenId, Integer applicationId) {
        return applicationRepository.findByApplicationIdAndCitizenId(applicationId, citizenId)
                .orElseThrow(() -> new IllegalArgumentException("Application was not found."));
    }

    /** READ: slots in a date range with their booked and remaining places, delegated to the schedule service. */
    @Transactional(readOnly = true)
    public List<BiometricSlotView> getAvailableSchedule(
            String serviceType, Integer centreId, LocalDate startDate, LocalDate endDate) {
        return scheduleService.getSchedule(serviceType, centreId, startDate, endDate);
    }
}
