package lk.sliit.identityissuanceservice.Citizen_Support_And_Service_Mgmt.Service;

import jakarta.servlet.http.HttpSession;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Model.Appointment;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Model.AppointmentCheckIn;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Model.Centre;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Repository.AppointmentCheckInRepository;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Repository.CentreRepository;
import lk.sliit.identityissuanceservice.Citizen_Support_And_Service_Mgmt.Dto.WalkInAppointmentView;
import lk.sliit.identityissuanceservice.Citizen_Support_And_Service_Mgmt.Dto.WalkInSlotView;
import lk.sliit.identityissuanceservice.Citizen_Support_And_Service_Mgmt.Model.WalkInSlot;
import lk.sliit.identityissuanceservice.Citizen_Support_And_Service_Mgmt.Repository.WalkInAppointmentRepository;
import lk.sliit.identityissuanceservice.Citizen_Support_And_Service_Mgmt.Repository.WalkInSlotRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class WalkInAppointmentService {

    public static final String DEFAULT_OTP = "111111";
    public static final int PAGE_SIZE = 15;
    public static final String WALK_IN = "WALK_IN";

    private static final String SESSION_NAME = "walkInVisitorName";
    private static final String SESSION_PHONE = "walkInVisitorPhone";
    private static final String SESSION_VERIFIED = "walkInPhoneVerified";
    private static final String SESSION_EXPIRES = "walkInOtpExpiresAt";
    private static final String SESSION_ATTEMPTS = "walkInOtpAttempts";
    private static final List<String> OCCUPIED_STATUSES = List.of("CONFIRMED", "CHECKED_IN", "COMPLETED");
    private static final Set<String> SERVICES = Set.of(
            "NIC_APPLICATION_HELP",
            "PASSPORT_APPLICATION_HELP"
    );

    private final WalkInAppointmentRepository appointmentRepository;
    private final WalkInSlotRepository slotRepository;
    private final CentreRepository centreRepository;
    private final AppointmentCheckInRepository checkInRepository;

    public WalkInAppointmentService(
            WalkInAppointmentRepository appointmentRepository,
            WalkInSlotRepository slotRepository,
            CentreRepository centreRepository,
            AppointmentCheckInRepository checkInRepository
    ) {
        this.appointmentRepository = appointmentRepository;
        this.slotRepository = slotRepository;
        this.centreRepository = centreRepository;
        this.checkInRepository = checkInRepository;
    }

    public void startPhoneVerification(String visitorName, String visitorPhone, HttpSession session) {
        String name = clean(visitorName);
        String phone = normalizePhone(visitorPhone);
        if (name.length() < 2 || name.length() > 100) {
            throw new IllegalArgumentException("Enter the visitor's full name.");
        }
        if (!phone.matches("\\+?[0-9]{10,14}")) {
            throw new IllegalArgumentException("Enter a valid phone number with 10 to 14 digits.");
        }
        session.setAttribute(SESSION_NAME, name);
        session.setAttribute(SESSION_PHONE, phone);
        session.setAttribute(SESSION_VERIFIED, false);
        session.setAttribute(SESSION_EXPIRES, LocalDateTime.now().plusMinutes(5));
        session.setAttribute(SESSION_ATTEMPTS, 0);
    }

    public void verifyPhone(String otp, HttpSession session) {
        if (session.getAttribute(SESSION_PHONE) == null) {
            throw new IllegalArgumentException("Enter your name and phone number first.");
        }
        Object expiresValue = session.getAttribute(SESSION_EXPIRES);
        if (!(expiresValue instanceof LocalDateTime expiresAt) || LocalDateTime.now().isAfter(expiresAt)) {
            throw new IllegalArgumentException("The OTP expired. Request a new OTP.");
        }
        int attempts = session.getAttribute(SESSION_ATTEMPTS) instanceof Integer value ? value : 0;
        if (attempts >= 3) {
            throw new IllegalArgumentException("Too many incorrect attempts. Request a new OTP.");
        }
        if (!DEFAULT_OTP.equals(clean(otp))) {
            session.setAttribute(SESSION_ATTEMPTS, attempts + 1);
            throw new IllegalArgumentException("Incorrect OTP. Please try again.");
        }
        session.setAttribute(SESSION_VERIFIED, true);
    }

    public boolean isPhoneVerified(HttpSession session) {
        return Boolean.TRUE.equals(session.getAttribute(SESSION_VERIFIED));
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

    @Transactional
    public WalkInAppointmentView book(String visitorService, Integer slotId, HttpSession session) {
        if (!isPhoneVerified(session)) {
            throw new IllegalArgumentException("Verify your phone number before booking.");
        }
        String service = clean(visitorService).toUpperCase();
        if (!SERVICES.contains(service)) {
            throw new IllegalArgumentException("Select a valid walk-in service.");
        }
        // Lock this one slot until the booking is saved so simultaneous visitors cannot exceed its capacity.
        WalkInSlot slot = slotRepository.findByIdForBooking(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Select an available appointment slot."));
        WalkInSlotView slotView = buildSlotView(slot);
        if (!slotView.available() || slotView.remainingPlaces() < 1
                || !slot.getSlotDate().atTime(slot.getSlotTime()).isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("That appointment slot is no longer available.");
        }

        Appointment appointment = new Appointment();
        appointment.setCitizenId(null);
        appointment.setApplicationId(null);
        appointment.setCentreId(slot.getCentreId());
        appointment.setAppointmentType(WALK_IN);
        appointment.setScheduledAt(slot.getSlotDate().atTime(slot.getSlotTime()));
        appointment.setAppointmentStatus("CONFIRMED");
        appointment.setReferenceNo(createReference());
        appointment.setQrCode(appointment.getReferenceNo());
        appointment.setVisitorName(sessionName(session));
        appointment.setVisitorPhone(sessionPhone(session));
        appointment.setVisitorService(service);
        appointment = appointmentRepository.save(appointment);
        clearVerification(session);
        return buildAppointmentView(appointment);
    }

    @Transactional(readOnly = true)
    public WalkInAppointmentView findByReference(String reference) {
        return buildAppointmentView(findAppointment(reference));
    }

    @Transactional(readOnly = true)
    public List<WalkInAppointmentView> recoverByPhone(String visitorPhone) {
        String phone = normalizePhone(visitorPhone);
        if (!phone.matches("\\+?[0-9]{10,14}")) {
            throw new IllegalArgumentException("Enter the phone number used for booking.");
        }
        return appointmentRepository
                .findAllByVisitorPhoneAndAppointmentTypeOrderByScheduledAtDesc(phone, WALK_IN)
                .stream()
                .map(this::buildAppointmentView)
                .toList();
    }

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
    public WalkInAppointmentView checkIn(String reference, Integer staffId, String method) {
        if (staffId == null) throw new IllegalArgumentException("Officer session was not found.");
        Appointment appointment = findAppointment(reference);
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
        session.removeAttribute(SESSION_NAME);
        session.removeAttribute(SESSION_PHONE);
        session.removeAttribute(SESSION_VERIFIED);
        session.removeAttribute(SESSION_EXPIRES);
        session.removeAttribute(SESSION_ATTEMPTS);
    }

    private String normalizePhone(String value) {
        return clean(value).replace(" ", "").replace("-", "");
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
