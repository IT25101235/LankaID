package lk.sliit.identityissuanceservice.Citizen_Support_And_Service_Mgmt.Service;

import jakarta.servlet.http.HttpSession;
import lk.sliit.identityissuanceservice.OTP_Mgmt.Otp.SmsOtpService;
import lk.sliit.identityissuanceservice.OTP_Mgmt.Otp.SriLankanMobileNumber;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Model.Appointment;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Model.AppointmentCheckIn;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Model.Centre;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Repository.AppointmentCheckInRepository;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Repository.CentreRepository;
import lk.sliit.identityissuanceservice.Citizen_Support_And_Service_Mgmt.Dto.WalkInAppointmentView;
import lk.sliit.identityissuanceservice.Citizen_Support_And_Service_Mgmt.Dto.WalkInSlotView;
import lk.sliit.identityissuanceservice.Citizen_Support_And_Service_Mgmt.Dto.WalkInPaymentDetails;
import lk.sliit.identityissuanceservice.Payment_Mgmt.Service.WalkInPaymentService;
import lk.sliit.identityissuanceservice.Payment_Mgmt.Model.Payment;
import lk.sliit.identityissuanceservice.Citizen_Support_And_Service_Mgmt.Model.WalkInSlot;
import lk.sliit.identityissuanceservice.Citizen_Support_And_Service_Mgmt.Repository.WalkInAppointmentRepository;
import lk.sliit.identityissuanceservice.Citizen_Support_And_Service_Mgmt.Repository.WalkInSlotRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class WalkInAppointmentService {

    public static final int PAGE_SIZE = 15;
    public static final String WALK_IN = "WALK_IN";

    private static final String SESSION_NAME = "walkInVisitorName";
    private static final String SESSION_PHONE = "walkInVisitorPhone";
    private static final String OTP_PURPOSE = "WALK_IN_BOOKING";
    private static final String RECOVERY_PURPOSE = "WALK_IN_RECOVERY";
    private static final String RECOVERY_PHONE = "walkInRecoveryPhone";
    private static final String CREATED_PASS = "walkInCreatedPass";
    private static final String PENDING_BOOKING = "walkInPendingBooking";
    private static final List<String> OCCUPIED_STATUSES = List.of("CONFIRMED", "CHECKED_IN", "COMPLETED");
    private static final Set<String> SERVICES = Set.of(
            "NIC_APPLICATION_HELP",
            "PASSPORT_APPLICATION_HELP"
    );

    private final WalkInAppointmentRepository appointmentRepository;
    private final WalkInSlotRepository slotRepository;
    private final CentreRepository centreRepository;
    private final AppointmentCheckInRepository checkInRepository;
    private final SmsOtpService otpService;
    private final WalkInPaymentService paymentService;

    public WalkInAppointmentService(
            WalkInAppointmentRepository appointmentRepository,
            WalkInSlotRepository slotRepository,
            CentreRepository centreRepository,
            AppointmentCheckInRepository checkInRepository,
            SmsOtpService otpService,
            WalkInPaymentService paymentService
    ) {
        this.appointmentRepository = appointmentRepository;
        this.slotRepository = slotRepository;
        this.centreRepository = centreRepository;
        this.checkInRepository = checkInRepository;
        this.otpService = otpService;
        this.paymentService = paymentService;
    }

    public void startPhoneVerification(String visitorName, String visitorPhone, HttpSession session) {
        String name = clean(visitorName);
        String phone = normalizePhone(visitorPhone);
        if (name.length() < 2 || name.length() > 100) {
            throw new IllegalArgumentException("Enter the visitor's full name.");
        }
        synchronized (session) {
            otpService.send(phone, OTP_PURPOSE, session);
            session.setAttribute(SESSION_NAME, name);
            session.setAttribute(SESSION_PHONE, phone);
            session.removeAttribute(PENDING_BOOKING);
        }
    }

    public void resendPhoneVerification(HttpSession session) {
        startPhoneVerification(sessionName(session), sessionPhone(session), session);
    }

    public long otpRetryAfterSeconds(String phone, HttpSession session) {
        return otpService.retryAfterSeconds(phone, OTP_PURPOSE, session);
    }

    public void verifyPhone(String otp, HttpSession session) {
        otpService.verify(clean(otp), OTP_PURPOSE, session);
    }

    public boolean isPhoneVerified(HttpSession session) {
        return otpService.isVerified(sessionPhone(session), OTP_PURPOSE, session);
    }

    public String sessionName(HttpSession session) {
        return clean((String) session.getAttribute(SESSION_NAME));
    }

    public String sessionPhone(HttpSession session) {
        return clean((String) session.getAttribute(SESSION_PHONE));
    }

    @Transactional(readOnly = true)
    public List<WalkInSlotView> getBookableSlots() {
        LocalDateTime now = LocalDateTime.now();
        return slotRepository
                .findAllBySlotDateGreaterThanEqualAndIsAvailableOrderBySlotDateAscSlotTimeAsc(
                        now.toLocalDate(), "Y")
                .stream()
                .map(this::buildSlotView)
                .filter(slot -> slot.slotDate().atTime(slot.slotTime()).isAfter(now))
                .filter(slot -> slot.remainingPlaces() > 0)
                .toList();
    }

    @Transactional(readOnly = true)
    public void preparePayment(String visitorService, Integer slotId, HttpSession session) {
        synchronized (session) {
            if (!isPhoneVerified(session)) {
                throw new IllegalArgumentException("Verify your phone number before booking.");
            }
            String service = clean(visitorService).toUpperCase();
            if (!SERVICES.contains(service)) {
                throw new IllegalArgumentException("Select a valid walk-in service.");
            }
            WalkInSlot slot = slotRepository.findById(slotId)
                    .orElseThrow(() -> new IllegalArgumentException("Select an available appointment slot."));
            requireAvailableSlot(slot);
            session.setAttribute(PENDING_BOOKING,
                    new PendingBooking(service, slotId, sessionPhone(session), UUID.randomUUID().toString()));
        }
    }

    @Transactional(readOnly = true)
    public WalkInPaymentDetails getPaymentDetails(HttpSession session) {
        synchronized (session) {
            PendingBooking pending = requirePendingBooking(session);
            WalkInSlot slot = slotRepository.findById(pending.slotId())
                    .orElseThrow(() -> new IllegalArgumentException("Select an available appointment slot."));
            requireAvailableSlot(slot);
            return new WalkInPaymentDetails(pending.token(), serviceLabel(pending.service()),
                    buildSlotView(slot), WalkInPaymentService.WALK_IN_FEE);
        }
    }

    @Transactional
    public WalkInAppointmentView payAndBook(String token, HttpSession session) {
        synchronized (session) {
            PendingBooking pending = requirePendingBooking(session);
            if (!pending.token().equals(token)) {
                throw new IllegalArgumentException("The selection changed. Review the appointment before paying.");
            }
            return bookPaidVisitor(pending, session);
        }
    }

    private PendingBooking requirePendingBooking(HttpSession session) {
        if (!isPhoneVerified(session)) {
            throw new IllegalArgumentException("Verify your phone number before booking.");
        }
        Object value = session.getAttribute(PENDING_BOOKING);
        if (!(value instanceof PendingBooking pending) || !pending.phone().equals(sessionPhone(session))) {
            throw new IllegalArgumentException("Select a service and slot before paying.");
        }
        return pending;
    }

    private WalkInAppointmentView bookPaidVisitor(PendingBooking pending, HttpSession session) {
        // Recheck and lock the slot: viewing the payment page does not reserve it.
        WalkInSlot slot = slotRepository.findByIdForBooking(pending.slotId())
                .orElseThrow(() -> new IllegalArgumentException("Select an available appointment slot."));
        requireAvailableSlot(slot);

        Appointment appointment = new Appointment();
        appointment.setCitizenId(null);
        appointment.setApplicationId(null);
        appointment.setCentreId(slot.getCentreId());
        appointment.setAppointmentType(WALK_IN);
        appointment.setScheduledAt(slot.getSlotDate().atTime(slot.getSlotTime()));
        appointment.setAppointmentStatus("PENDING_PAYMENT");
        appointment.setReferenceNo(createReference());
        appointment.setQrCode(appointment.getReferenceNo());
        appointment.setVisitorName(sessionName(session));
        appointment.setVisitorPhone(sessionPhone(session));
        appointment.setVisitorService(pending.service());
        appointment = appointmentRepository.save(appointment);
        paymentService.pay(appointment);
        appointment.setAppointmentStatus("CONFIRMED");
        appointment = appointmentRepository.save(appointment);
        // The booking OTP authorizes only this newly created pass after it is consumed.
        session.setAttribute(CREATED_PASS,
                new CreatedPass(appointment.getReferenceNo(), Instant.now().plusSeconds(900)));
        clearVerification(session);
        return buildAppointmentView(appointment);
    }

    void requireAvailableSlot(WalkInSlot slot) {
        WalkInSlotView view = buildSlotView(slot);
        if (!view.available() || view.remainingPlaces() < 1
                || !slot.getSlotDate().atTime(slot.getSlotTime()).isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("That appointment slot is no longer available.");
        }
    }

    public Payment getPaidPayment(Integer appointmentId) {
        return paymentService.findPaidPayment(appointmentId).orElse(null);
    }

    private record PendingBooking(String service, Integer slotId, String phone, String token)
            implements java.io.Serializable { }

    @Transactional(readOnly = true)
    public WalkInAppointmentView findByReference(String reference) {
        return buildAppointmentView(findAppointment(reference));
    }

    @Transactional(readOnly = true)
    public List<WalkInAppointmentView> recoverVerifiedBookings(HttpSession session) {
        synchronized (session) {
            if (!isRecoveryVerified(session)) {
                throw new IllegalArgumentException("Verify your mobile number to retrieve bookings.");
            }
            String phone = recoveryPhone(session);
            return appointmentRepository
                .findAllByVisitorPhoneInAndAppointmentTypeOrderByScheduledAtDesc(
                        List.of(phone, "0" + phone.substring(3)), WALK_IN)
                .stream()
                .map(this::buildAppointmentView)
                .toList();
        }
    }

    public void startRecoveryVerification(String phone, HttpSession session) {
        String normalized = normalizePhone(phone);
        synchronized (session) {
            // Send regardless of whether bookings exist; do not reveal number registration.
            otpService.send(normalized, RECOVERY_PURPOSE, session);
            session.setAttribute(RECOVERY_PHONE, normalized);
        }
    }

    public void verifyRecovery(String code, HttpSession session) {
        otpService.verify(clean(code), RECOVERY_PURPOSE, session);
    }

    public String recoveryPhone(HttpSession session) {
        return clean((String) session.getAttribute(RECOVERY_PHONE));
    }

    public boolean isRecoveryVerified(HttpSession session) {
        synchronized (session) {
            return otpService.isVerified(recoveryPhone(session), RECOVERY_PURPOSE, session);
        }
    }

    public long recoveryRetryAfterSeconds(String phone, HttpSession session) {
        return otpService.retryAfterSeconds(phone, RECOVERY_PURPOSE, session);
    }

    public void clearRecovery(HttpSession session) {
        synchronized (session) {
            otpService.clear(RECOVERY_PURPOSE, session);
            session.removeAttribute(RECOVERY_PHONE);
        }
    }

    @Transactional(readOnly = true)
    public WalkInAppointmentView getVisitorPass(String reference, HttpSession session) {
        synchronized (session) {
            Object value = session.getAttribute(CREATED_PASS);
            boolean justBooked = value instanceof CreatedPass pass
                    && pass.reference().equalsIgnoreCase(clean(reference))
                    && Instant.now().isBefore(pass.expiresAt());
            if (!justBooked && !isRecoveryVerified(session)) {
                throw new IllegalArgumentException("Verify your mobile number to access this pass.");
            }
            Appointment appointment = findAppointment(reference);
            if (!justBooked && !normalizePhone(appointment.getVisitorPhone()).equals(recoveryPhone(session))) {
                throw new IllegalArgumentException("Verify your mobile number to access this pass.");
            }
            return buildAppointmentView(appointment);
        }
    }

    private record CreatedPass(String reference, Instant expiresAt) implements java.io.Serializable { }

    @Transactional(readOnly = true)
    public Page<WalkInAppointmentView> getCsrQueue(int page) {
        return getCsrQueue(page, null);
    }

    @Transactional(readOnly = true)
    public Page<WalkInAppointmentView> getCsrQueue(int page, String visitorService) {
        String service = clean(visitorService).toUpperCase();
        PageRequest pageRequest = PageRequest.of(Math.max(page, 0), PAGE_SIZE);
        if (!service.isBlank() && SERVICES.contains(service)) {
            return appointmentRepository
                    .findAllByAppointmentTypeAndVisitorServiceOrderByScheduledAtDesc(
                            WALK_IN,
                            service,
                            pageRequest
                    )
                    .map(this::buildAppointmentView);
        }
        return appointmentRepository
                .findAllByAppointmentTypeOrderByScheduledAtDesc(
                        WALK_IN,
                        pageRequest
                )
                .map(this::buildAppointmentView);
    }

    @Transactional(readOnly = true)
    public List<Centre> getCentres() {
        return centreRepository.findAllByOrderByCentreNameAsc();
    }

    @Transactional(readOnly = true)
    public List<WalkInSlotView> getPublishedSchedule() {
        return getPublishedSchedule(null, LocalDate.now(), LocalDate.now().plusDays(14), null);
    }

    @Transactional(readOnly = true)
    public List<WalkInSlotView> getPublishedSchedule(
            Integer centreId,
            LocalDate startDate,
            LocalDate endDate,
            LocalTime slotTime
    ) {
        LocalDate from = startDate == null ? LocalDate.now() : startDate;
        LocalDate to = endDate == null ? from.plusDays(14) : endDate;
        if (to.isBefore(from)) {
            throw new IllegalArgumentException("The schedule end date cannot be before the start date.");
        }
        return slotRepository
                .findAllBySlotDateBetweenOrderBySlotDateAscSlotTimeAsc(from, to)
                .stream()
                .filter(slot -> centreId == null || centreId.equals(slot.getCentreId()))
                .filter(slot -> slotTime == null || slotTime.equals(slot.getSlotTime()))
                .map(this::buildSlotView)
                .toList();
    }

    @Transactional
    public void publishDay(Integer centreId, LocalDate date, Integer capacity) {
        publishDateRange(
                centreId,
                date,
                date,
                LocalTime.of(9, 0),
                LocalTime.of(16, 30),
                capacity
        );
    }

    @Transactional
    public int publishDateRange(
            Integer centreId,
            LocalDate startDate,
            LocalDate endDate,
            LocalTime openingTime,
            LocalTime closingTime,
            Integer capacity
    ) {
        findCentre(centreId);
        validateDateRange(startDate, endDate);
        validateOperatingTimes(openingTime, closingTime);
        validateCapacity(capacity);

        int publishedDays = 0;
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            configureDay(centreId, date, openingTime, closingTime, capacity);
            publishedDays++;
        }
        return publishedDays;
    }

    private void configureDay(
            Integer centreId,
            LocalDate date,
            LocalTime openingTime,
            LocalTime closingTime,
            Integer capacity
    ) {
        List<WalkInSlot> existingSlots =
                slotRepository.findAllByCentreIdAndSlotDateOrderBySlotTimeAsc(centreId, date);
        existingSlots.stream()
                .filter(slot -> slot.getSlotTime().isBefore(openingTime)
                        || !slot.getSlotTime().isBefore(closingTime))
                .forEach(slot -> slot.setIsAvailable("N"));
        slotRepository.saveAll(existingSlots);

        for (LocalTime time = openingTime; time.isBefore(closingTime); time = time.plusMinutes(30)) {
            WalkInSlot slot = slotRepository
                    .findByCentreIdAndSlotDateAndSlotTime(centreId, date, time)
                    .orElseGet(WalkInSlot::new);
            long booked = slot.getSlotId() == null ? 0 : bookedCount(slot);
            if (capacity < booked) {
                throw new IllegalArgumentException(
                        "Capacity cannot be lower than the existing booking count of " + booked
                                + " for " + date + " at " + time + "."
                );
            }
            slot.setCentreId(centreId);
            slot.setSlotDate(date);
            slot.setSlotTime(time);
            slot.setSlotCapacity(capacity);
            slot.setIsAvailable("Y");
            slotRepository.save(slot);
        }
    }

    @Transactional
    public void closeDay(Integer centreId, LocalDate date) {
        closeDateRange(centreId, date, date);
    }

    @Transactional
    public int closeDateRange(Integer centreId, LocalDate startDate, LocalDate endDate) {
        findCentre(centreId);
        validateDateRange(startDate, endDate);
        int closedDays = 0;
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            if (closePublishedDay(centreId, date)) closedDays++;
        }
        if (closedDays == 0) {
            throw new IllegalArgumentException("No published walk-in days were found in that range.");
        }
        return closedDays;
    }

    private boolean closePublishedDay(Integer centreId, LocalDate date) {
        List<WalkInSlot> slots = slotRepository.findAllByCentreIdAndSlotDateOrderBySlotTimeAsc(centreId, date);
        if (slots.isEmpty()) return false;
        slots.forEach(slot -> slot.setIsAvailable("N"));
        slotRepository.saveAll(slots);

        List<Appointment> appointments = appointmentRepository
                .findAllByAppointmentTypeAndCentreIdAndScheduledAtBetweenAndAppointmentStatusIn(
                        WALK_IN,
                        centreId,
                        date.atStartOfDay(),
                        date.plusDays(1).atStartOfDay(),
                        List.of("CONFIRMED", "CHECKED_IN")
                );
        appointments.forEach(appointment -> appointment.setAppointmentStatus("CANCELLED"));
        appointmentRepository.saveAll(appointments);
        return true;
    }

    @Transactional(readOnly = true)
    public List<WalkInAppointmentView> getAppointmentsForDate(LocalDate date) {
        LocalDate selectedDate = date == null ? LocalDate.now() : date;
        return appointmentRepository
                .findAllByAppointmentTypeAndScheduledAtBetweenOrderByScheduledAtAsc(
                        WALK_IN,
                        selectedDate.atStartOfDay(),
                        selectedDate.plusDays(1).atStartOfDay()
                )
                .stream()
                .map(this::buildAppointmentView)
                .toList();
    }

    @Transactional
    public void updateSlot(Integer slotId, Integer capacity, boolean available) {
        validateCapacity(capacity);
        WalkInSlot slot = slotRepository.findByIdForBooking(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Walk-in slot was not found."));
        long booked = bookedCount(slot);
        if (capacity < booked) {
            throw new IllegalArgumentException("Capacity cannot be lower than the existing booking count of " + booked + ".");
        }
        slot.setSlotCapacity(capacity);
        slot.setIsAvailable(available ? "Y" : "N");
        slotRepository.save(slot);
    }

    @Transactional
    public void deleteUnusedSlot(Integer slotId) {
        // Booking uses this same lock, so a slot cannot be booked while it is being deleted.
        WalkInSlot slot = slotRepository.findByIdForBooking(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Walk-in slot was not found."));
        LocalDateTime time = slot.getSlotDate().atTime(slot.getSlotTime());
        if (!time.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Only future unused slots can be deleted.");
        }
        if (appointmentRepository.existsByAppointmentTypeAndCentreIdAndScheduledAt(WALK_IN, slot.getCentreId(), time)) {
            throw new IllegalArgumentException("This slot has booking history and cannot be deleted. Close it if needed.");
        }
        slotRepository.delete(slot);
        slotRepository.flush();
    }

    @Transactional
    public WalkInAppointmentView checkIn(String reference, Integer staffId, String method) {
        if (staffId == null) throw new IllegalArgumentException("Officer session was not found.");
        Appointment appointment = appointmentRepository.findForChange(clean(reference))
                .orElseThrow(() -> new IllegalArgumentException("Walk-in appointment was not found."));
        if ("CANCELLED".equals(appointment.getAppointmentStatus())) {
            throw new IllegalArgumentException("This walk-in appointment was cancelled.");
        }
        if ("COMPLETED".equals(appointment.getAppointmentStatus())) {
            throw new IllegalArgumentException("This walk-in visit is already completed.");
        }
        if (!"CONFIRMED".equals(appointment.getAppointmentStatus())
                && !"CHECKED_IN".equals(appointment.getAppointmentStatus())) {
            throw new IllegalArgumentException("This walk-in appointment cannot be checked in.");
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
        return buildAppointmentView(appointment);
    }

    @Transactional
    public void completeVisit(String reference) {
        Appointment appointment = findAppointment(reference);
        if (!"CHECKED_IN".equals(appointment.getAppointmentStatus())) {
            throw new IllegalArgumentException("Check in the visitor before completing the visit.");
        }
        appointment.setAppointmentStatus("COMPLETED");
        appointmentRepository.save(appointment);
    }

    public String serviceLabel(String service) {
        return switch (clean(service).toUpperCase()) {
            case "NIC_APPLICATION_HELP" -> "NIC application assistance";
            case "PASSPORT_APPLICATION_HELP" -> "Passport application assistance";
            default -> "Walk-in service";
        };
    }

    private WalkInAppointmentView buildAppointmentView(Appointment appointment) {
        Centre centre = findCentre(appointment.getCentreId());
        String address = StreamAddress.of(centre.getStreet(), centre.getCity(), centre.getDistrict());
        return new WalkInAppointmentView(
                appointment,
                centre.getCentreName(),
                address,
                checkInRepository.existsById(appointment.getAppointmentId())
        );
    }

    private WalkInSlotView buildSlotView(WalkInSlot slot) {
        Centre centre = findCentre(slot.getCentreId());
        long booked = bookedCount(slot);
        int remaining = Math.max(0, slot.getSlotCapacity() - (int) booked);
        return new WalkInSlotView(
                slot.getSlotId(),
                slot.getCentreId(),
                centre.getCentreName(),
                slot.getSlotDate(),
                slot.getSlotTime(),
                slot.getSlotCapacity(),
                booked,
                remaining,
                "Y".equals(slot.getIsAvailable())
        );
    }

    private long bookedCount(WalkInSlot slot) {
        return appointmentRepository
                .countByAppointmentTypeAndCentreIdAndScheduledAtAndAppointmentStatusIn(
                        WALK_IN,
                        slot.getCentreId(),
                        slot.getSlotDate().atTime(slot.getSlotTime()),
                        OCCUPIED_STATUSES
                );
    }

    private Appointment findAppointment(String reference) {
        String value = clean(reference);
        if (value.isBlank()) throw new IllegalArgumentException("Enter or scan a walk-in reference.");
        return appointmentRepository.findByReferenceNoIgnoreCaseAndAppointmentType(value, WALK_IN)
                .orElseThrow(() -> new IllegalArgumentException("Walk-in appointment was not found."));
    }

    private Centre findCentre(Integer centreId) {
        return centreRepository.findById(centreId)
                .orElseThrow(() -> new IllegalArgumentException("Service centre was not found."));
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null || startDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Select today or a future date range.");
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("The end date cannot be before the start date.");
        }
        if (endDate.isAfter(startDate.plusDays(89))) {
            throw new IllegalArgumentException("A date range can contain a maximum of 90 days.");
        }
    }

    private void validateOperatingTimes(LocalTime openingTime, LocalTime closingTime) {
        if (openingTime == null || closingTime == null || !openingTime.isBefore(closingTime)) {
            throw new IllegalArgumentException("Closing time must be later than opening time.");
        }
        if (!isHalfHourTime(openingTime) || !isHalfHourTime(closingTime)) {
            throw new IllegalArgumentException("Opening and closing times must use 30-minute intervals.");
        }
    }

    private boolean isHalfHourTime(LocalTime time) {
        return (time.getMinute() == 0 || time.getMinute() == 30) && time.getSecond() == 0;
    }

    private void validateCapacity(Integer capacity) {
        if (capacity == null || capacity < 1 || capacity > 100) {
            throw new IllegalArgumentException("Slot capacity must be between 1 and 100.");
        }
    }

    private String createReference() {
        String reference;
        do {
            reference = "WALK-" + LocalDate.now().getYear() + "-"
                    + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (appointmentRepository.existsByReferenceNo(reference));
        return reference;
    }

    private void clearVerification(HttpSession session) {
        session.removeAttribute(PENDING_BOOKING);
        session.removeAttribute(SESSION_NAME);
        session.removeAttribute(SESSION_PHONE);
        otpService.clear(OTP_PURPOSE, session);
    }

    private String normalizePhone(String value) {
        return SriLankanMobileNumber.normalize(value);
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static final class StreamAddress {
        private static String of(String street, String city, String district) {
            String first = street == null || street.isBlank() ? "" : street.trim() + ", ";
            return first + city + ", " + district;
        }
    }
}
