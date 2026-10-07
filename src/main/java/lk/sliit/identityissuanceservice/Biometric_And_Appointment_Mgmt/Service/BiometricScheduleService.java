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
import java.util.List;
import java.util.Locale;

@Service
public class BiometricScheduleService {

    private static final List<String> OCCUPYING_STATUSES = List.of("CONFIRMED", "CHECKED_IN", "COMPLETED");
    private static final List<String> CANCELLABLE_STATUSES = List.of("CONFIRMED");

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

    @Transactional(readOnly = true)
    public List<Centre> getCentres() {
        return centres.findAllByCentreStatusOrderByCentreNameAsc("ACTIVE");
    }

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

    private void configureDay(
            String serviceType,
            Integer centreId,
            LocalDate date,
            LocalTime openingTime,
            LocalTime closingTime,
            int capacity,
            Integer officerId
    ) {
        List<BiometricAppointmentSlot> existing =
                slots.findAllByServiceTypeAndCentreIdAndSlotDateOrderBySlotTimeAsc(serviceType, centreId, date);
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

    @Transactional
    public int closeDateRange(String serviceType, Integer centreId, LocalDate startDate, LocalDate endDate) {
        String service = requireServiceType(serviceType);
        findCentre(centreId);
        validateDateRange(startDate, endDate);
        int closedDays = 0;
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            List<BiometricAppointmentSlot> daySlots =
                    slots.findAllByServiceTypeAndCentreIdAndSlotDateOrderBySlotTimeAsc(service, centreId, date);
            if (daySlots.isEmpty()) continue;
            daySlots.forEach(this::closeSlotAndCancelBookings);
            closedDays++;
        }
        if (closedDays == 0) throw new IllegalArgumentException("No published biometric dates were found in that range.");
        return closedDays;
    }

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

    @Transactional
    public BiometricAppointmentSlot lockBookableSlot(Integer slotId, String serviceType, Integer excludedAppointmentId) {
        BiometricAppointmentSlot slot = slots.findByIdForBooking(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Select an available appointment slot."));
        if (!requireServiceType(serviceType).equals(slot.getServiceType())) {
            throw new IllegalArgumentException("That slot belongs to a different document service.");
        }
        BiometricSlotView view = toView(slot, findCentre(slot.getCentreId()), excludedAppointmentId);
        if (!view.available()
                || view.remainingPlaces() < 1
                || !slot.getSlotDate().atTime(slot.getSlotTime()).isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("That appointment slot is no longer available. Choose another slot.");
        }
        return slot;
    }

    public long bookedCount(Integer slotId, Integer excludedAppointmentId) {
        return appointments.countActiveBiometricSlotBookings(
                slotId, excludedAppointmentId == null ? -1 : excludedAppointmentId);
    }

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

    private String requireServiceType(String serviceType) {
        String clean = serviceType == null ? "" : serviceType.trim().toUpperCase(Locale.ROOT);
        if (!List.of("NIC", "PASSPORT").contains(clean)) {
            throw new IllegalArgumentException("Select a valid biometric service.");
        }
        return clean;
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null || startDate.isBefore(LocalDate.now().plusDays(1))) {
            throw new IllegalArgumentException("Biometric schedules must start from tomorrow.");
        }
        if (endDate.isBefore(startDate)) throw new IllegalArgumentException("The end date cannot be before the start date.");
        if (endDate.isAfter(startDate.plusDays(62))) {
            throw new IllegalArgumentException("Publish no more than 63 days at a time.");
        }
    }

    private void validateTimes(LocalTime openingTime, LocalTime closingTime) {
        if (openingTime == null || closingTime == null || !openingTime.isBefore(closingTime)
                || openingTime.isBefore(LocalTime.of(8, 0)) || closingTime.isAfter(LocalTime.of(18, 0))
                || openingTime.getMinute() % 30 != 0 || closingTime.getMinute() % 30 != 0) {
            throw new IllegalArgumentException("Use a valid half-hour operating range between 8:00 AM and 6:00 PM.");
        }
    }

    private void validateCapacity(Integer capacity) {
        if (capacity == null || capacity < 1 || capacity > 100) {
            throw new IllegalArgumentException("Capacity must be between 1 and 100.");
        }
    }
}
