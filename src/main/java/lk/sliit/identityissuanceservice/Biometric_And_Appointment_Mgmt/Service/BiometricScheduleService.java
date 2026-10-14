package lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Service;

import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Dto.BiometricSlotView;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Model.Appointment;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Model.BiometricAppointmentSlot;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Model.Centre;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Repository.AppointmentRepository;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Repository.BiometricAppointmentSlotRepository;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Repository.CentreRepository;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Repository.CitizenApplicationRepository;
import lk.sliit.identityissuanceservice.Notification_Mgmt.Observer.NotificationPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service: slot calendar and capacity. Used by officers to publish slots and by citizens to book them.
 * <p>
 * Design Pattern - Service Layer, Repository and Dependency Injection (see the field comment below).<br>
 * Design Pattern - Observer: closing a slot notifies each affected citizen through NotificationPublisher.<br>
 * Exception handling: every rule below throws IllegalArgumentException with a clear message, and
 * @Transactional rolls the change back.<br>
 * Concurrency: slot rows are locked (pessimistic lock) so two citizens cannot take the last place.
 */
@Service
public class BiometricScheduleService {

    private static final List<String> OCCUPYING_STATUSES = List.of("CONFIRMED", "CHECKED_IN", "COMPLETED");
    private static final List<String> CANCELLABLE_STATUSES = List.of("CONFIRMED");

    // OOP - Composition and Encapsulation: the repositories are private final fields handed in through the
    // constructor (Design Pattern - Dependency Injection), so this class is loosely coupled and the objects
    // are never created here.
    private final BiometricAppointmentSlotRepository slots;
    private final AppointmentRepository appointments;
    private final CentreRepository centres;
    private final CitizenApplicationRepository applications;
    private final NotificationPublisher notificationPublisher;

    public BiometricScheduleService(
            BiometricAppointmentSlotRepository slots,
            AppointmentRepository appointments,
            CentreRepository centres,
            CitizenApplicationRepository applications,
            NotificationPublisher notificationPublisher
    ) {
        this.slots = slots;
        this.appointments = appointments;
        this.centres = centres;
        this.applications = applications;
        this.notificationPublisher = notificationPublisher;
    }

    /** READ: active centres. */
    @Transactional(readOnly = true)
    public List<Centre> getCentres() {
        return centres.findAllByCentreStatusOrderByCentreNameAsc("ACTIVE");
    }

    /** READ: slots in a date range with booked and remaining counts. */
    @Transactional(readOnly = true)
    public List<BiometricSlotView> getSchedule(
            String serviceType,
            Integer centreId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        String service = requireServiceType(serviceType);
        if (centreId == null) return List.of();
        Centre centre = findCentre(centreId);
        LocalDate from = startDate == null ? LocalDate.now() : startDate;
        LocalDate to = endDate == null ? from.plusMonths(1) : endDate;
        if (to.isBefore(from)) throw new IllegalArgumentException("The end date cannot be before the start date.");
        return slots.findAllByServiceTypeAndCentreIdAndSlotDateBetweenOrderBySlotDateAscSlotTimeAsc(
                        service, centreId, from, to)
                .stream()
                .map(slot -> toView(slot, centre))
                .toList();
    }

    /** CREATE: generate 30-minute slots for every day in the range, after validating the input. */
    @Transactional
    public int publishDateRange(
            String serviceType,
            Integer centreId,
            LocalDate startDate,
            LocalDate endDate,
            LocalTime openingTime,
            LocalTime closingTime,
            Integer capacity,
            Integer officerId
    ) {
        String service = requireServiceType(serviceType);
        findCentre(centreId);
        validateDateRange(startDate, endDate);
        validateTimes(openingTime, closingTime);
        validateCapacity(capacity);
        if (officerId == null) throw new IllegalArgumentException("Officer session was not found.");

        int days = 0;
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            configureDay(service, centreId, date, openingTime, closingTime, capacity, officerId);
            days++;
        }
        return days;
    }

    /** Creates or updates one day's slots and closes slots that fall outside the new opening hours. */
    private void configureDay(
            String serviceType,
            Integer centreId,
            LocalDate date,
            LocalTime openingTime,
            LocalTime closingTime,
            int capacity,
            Integer officerId
    ) {
        List<BiometricAppointmentSlot> existing = slots.findDayForUpdate(serviceType, centreId, date);
        for (BiometricAppointmentSlot slot : existing) {
            if (slot.getSlotTime().isBefore(openingTime) || !slot.getSlotTime().isBefore(closingTime)) {
                closeSlotAndCancelBookings(slot);
            }
        }

        for (LocalTime time = openingTime; time.isBefore(closingTime); time = time.plusMinutes(30)) {
            BiometricAppointmentSlot slot = slots
                    .findByCentreIdAndServiceTypeAndSlotDateAndSlotTime(centreId, serviceType, date, time)
                    .orElseGet(BiometricAppointmentSlot::new);
            long booked = slot.getSlotId() == null ? 0 : bookedCount(slot.getSlotId(), null);
            if (capacity < booked) {
                throw new IllegalArgumentException("Capacity cannot be lower than the existing booking count of "
                        + booked + " for " + date + " at " + time + ".");
            }
            slot.setCentreId(centreId);
            slot.setServiceType(serviceType);
            slot.setSlotDate(date);
            slot.setSlotTime(time);
            slot.setSlotCapacity(capacity);
            slot.setIsAvailable("Y");
            if (slot.getSlotId() == null) slot.setCreatedBy(officerId);
            slots.save(slot);
        }
    }

    /** UPDATE: close every slot in the range and cancel its bookings. */
    @Transactional
    public int closeDateRange(String serviceType, Integer centreId, LocalDate startDate, LocalDate endDate) {
        String service = requireServiceType(serviceType);
        findCentre(centreId);
        validateDateRange(startDate, endDate);
        int closedDays = 0;
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            List<BiometricAppointmentSlot> daySlots = slots.findDayForUpdate(service, centreId, date);
            if (daySlots.isEmpty()) continue;
            daySlots.forEach(this::closeSlotAndCancelBookings);
            closedDays++;
        }
        if (closedDays == 0) throw new IllegalArgumentException("No published biometric dates were found in that range.");
        return closedDays;
    }

    /** UPDATE: change capacity or open/close one slot. Capacity cannot go below existing bookings. */
    @Transactional
    public void updateSlot(String serviceType, Integer slotId, Integer capacity, boolean available) {
        String service = requireServiceType(serviceType);
        validateCapacity(capacity);
        BiometricAppointmentSlot slot = findOwnedSlot(slotId, service);
        long booked = bookedCount(slotId, null);
        if (capacity < booked) {
            throw new IllegalArgumentException("Capacity cannot be lower than the existing booking count of " + booked + ".");
        }
        slot.setSlotCapacity(capacity);
        if (available) {
            slot.setIsAvailable("Y");
            slots.save(slot);
        } else {
            closeSlotAndCancelBookings(slot);
        }
    }

    /** DELETE: only future slots that never had a booking. */
    @Transactional
    public void deleteUnusedSlot(String serviceType, Integer slotId) {
        BiometricAppointmentSlot slot = findOwnedSlot(slotId, requireServiceType(serviceType));
        if (!slot.getSlotDate().atTime(slot.getSlotTime()).isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Only future unused slots can be deleted.");
        }
        if (appointments.existsByBiometricSlotId(slotId)) {
            throw new IllegalArgumentException("This slot has booking history and cannot be deleted. Close it instead.");
        }
        slots.delete(slot);
        slots.flush();
    }

    /** READ: ids of the slots deleteUnusedSlot would accept: in the future and never booked, even if cancelled. */
    @Transactional(readOnly = true)
    public Set<Integer> getDeletableSlotIds(List<BiometricSlotView> schedule) {
        LocalDateTime now = LocalDateTime.now();
        List<Integer> future = schedule.stream()
                .filter(slot -> slot.slotDate().atTime(slot.slotTime()).isAfter(now))
                .map(BiometricSlotView::slotId)
                .toList();
        if (future.isEmpty()) return Set.of();
        Set<Integer> used = new HashSet<>(appointments.findUsedBiometricSlotIds(future));
        return future.stream().filter(id -> !used.contains(id)).collect(Collectors.toSet());
    }

    /** READ: one slot with its availability. */
    @Transactional(readOnly = true)
    public BiometricSlotView getBookableSlot(Integer slotId, String serviceType, Integer excludedAppointmentId) {
        BiometricAppointmentSlot slot = slots.findById(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Select an available appointment slot."));
        if (!requireServiceType(serviceType).equals(slot.getServiceType())) {
            throw new IllegalArgumentException("That slot belongs to a different document service.");
        }
        Centre centre = findCentre(slot.getCentreId());
        return toView(slot, centre, excludedAppointmentId);
    }

    /**
     * Takes the slot's row lock before any appointment lock. Closing a slot locks the slot and then its
     * appointments, so booking paths must use the same order or the two can deadlock. A missing slot is
     * ignored here; lockBookableSlot reports it later.
     */
    @Transactional
    public void lockSlotBeforeAppointment(Integer slotId) {
        if (slotId != null) slots.findByIdForBooking(slotId);
    }

    /**
     * Locks the slot row, then checks it is open, has space and is inside the booking window. Stops
     * double booking.
     */
    @Transactional
    public BiometricAppointmentSlot lockBookableSlot(Integer slotId, String serviceType, Integer excludedAppointmentId) {
        BiometricAppointmentSlot slot = slots.findByIdForBooking(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Select an available appointment slot."));
        if (!requireServiceType(serviceType).equals(slot.getServiceType())) {
            throw new IllegalArgumentException("That slot belongs to a different document service.");
        }
        Centre centre = findCentre(slot.getCentreId());
        LocalDate today = LocalDate.now();
        if (!"ACTIVE".equals(centre.getCentreStatus())
                || slot.getSlotDate().isBefore(today)
                || slot.getSlotDate().isAfter(today.plusDays(90))) {
            throw new IllegalArgumentException("That appointment slot is no longer available. Choose another slot.");
        }
        BiometricSlotView view = toView(slot, centre, excludedAppointmentId);
        if (!view.available()
                || view.remainingPlaces() < 1
                || !slot.getSlotDate().atTime(slot.getSlotTime()).isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("That appointment slot is no longer available. Choose another slot.");
        }
        return slot;
    }

    /** Number of bookings using this slot. */
    public long bookedCount(Integer slotId, Integer excludedAppointmentId) {
        return appointments.countActiveBiometricSlotBookings(
                slotId, excludedAppointmentId == null ? -1 : excludedAppointmentId);
    }

    /** Closes a slot, cancels its confirmed bookings and notifies each citizen (Observer pattern). */
    private void closeSlotAndCancelBookings(BiometricAppointmentSlot slot) {
        slot.setIsAvailable("N");
        slots.save(slot);
        List<Appointment> affected = appointments.findAllByBiometricSlotIdAndAppointmentStatusIn(
                slot.getSlotId(), CANCELLABLE_STATUSES);
        for (Appointment appointment : affected) {
            appointment.setAppointmentStatus("CANCELLED");
            appointment.setQrCode(null);
            appointments.save(appointment);
            applications.findById(appointment.getApplicationId()).ifPresent(application -> {
                application.setCurrentStatus("APPROVED");
                applications.save(application);
            });
            notificationPublisher.sendToCitizen(
                    appointment.getCitizenId(),
                    null,
                    "APPOINTMENT_CANCELLED",
                    "Biometric appointment " + appointment.getReferenceNo()
                            + " was cancelled because its schedule was closed by an officer. "
                            + "Your original payment is retained; book another available slot without paying again."
            );
        }
    }

    /** Converts a slot entity into the DTO shown on screen. */
    // OOP - Polymorphism (method overloading): two toView methods share a name but take different
    // parameters.
    private BiometricSlotView toView(BiometricAppointmentSlot slot, Centre centre) {
        return toView(slot, centre, null);
    }

    private BiometricSlotView toView(BiometricAppointmentSlot slot, Centre centre, Integer excludedAppointmentId) {
        long booked = bookedCount(slot.getSlotId(), excludedAppointmentId);
        int remaining = Math.max(0, slot.getSlotCapacity() - (int) booked);
        boolean available = "Y".equals(slot.getIsAvailable());
        return new BiometricSlotView(
                slot.getSlotId(), slot.getCentreId(), centre.getCentreName(), slot.getServiceType(),
                slot.getSlotDate(), slot.getSlotTime(), slot.getSlotCapacity(), booked, remaining, available);
    }

    /** Finds a slot (locked) and checks it belongs to this officer's service. */
    private BiometricAppointmentSlot findOwnedSlot(Integer slotId, String serviceType) {
        BiometricAppointmentSlot slot = slots.findByIdForBooking(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Biometric appointment slot was not found."));
        if (!serviceType.equals(slot.getServiceType())) {
            throw new IllegalArgumentException("That slot belongs to a different document service.");
        }
        return slot;
    }

    private Centre findCentre(Integer centreId) {
        if (centreId == null) throw new IllegalArgumentException("Select a service centre.");
        return centres.findById(centreId)
                .orElseThrow(() -> new IllegalArgumentException("Select a valid service centre."));
    }

    /** Input validation: only NIC or PASSPORT. */
    private String requireServiceType(String serviceType) {
        String clean = serviceType == null ? "" : serviceType.trim().toUpperCase(Locale.ROOT);
        if (!List.of("NIC", "PASSPORT").contains(clean)) {
            throw new IllegalArgumentException("Select a valid biometric service.");
        }
        return clean;
    }

    /**
     * Input validation: start from today, end after start, at most 63 days. Today is allowed on purpose,
     * even for times that have passed, so a demo can show check-in and missed appointments the same day.
     * Booking still rejects past slots.
     */
    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null || startDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Biometric schedules must start from today or a future date.");
        }
        if (endDate.isBefore(startDate)) throw new IllegalArgumentException("The end date cannot be before the start date.");
        if (endDate.isAfter(startDate.plusDays(62))) {
            throw new IllegalArgumentException("Publish no more than 63 days at a time.");
        }
    }

    /** Input validation: half-hour times between 8:00 AM and 6:00 PM. */
    private void validateTimes(LocalTime openingTime, LocalTime closingTime) {
        if (openingTime == null || closingTime == null || !openingTime.isBefore(closingTime)
                || openingTime.isBefore(LocalTime.of(8, 0)) || closingTime.isAfter(LocalTime.of(18, 0))
                || openingTime.getMinute() % 30 != 0 || closingTime.getMinute() % 30 != 0) {
            throw new IllegalArgumentException("Use a valid half-hour operating range between 8:00 AM and 6:00 PM.");
        }
    }

    /** Input validation: capacity from 1 to 100. */
    private void validateCapacity(Integer capacity) {
        if (capacity == null || capacity < 1 || capacity > 100) {
            throw new IllegalArgumentException("Capacity must be between 1 and 100.");
        }
    }
}
