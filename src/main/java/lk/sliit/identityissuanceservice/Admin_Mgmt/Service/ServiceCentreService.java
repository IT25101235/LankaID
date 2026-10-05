package lk.sliit.identityissuanceservice.Admin_Mgmt.Service;

import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Model.Centre;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Repository.AppointmentRepository;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Repository.CentreRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ServiceCentreService {

    private static final List<String> ACTIVE_BOOKING_STATUSES =
            List.of("PENDING_PAYMENT", "CONFIRMED", "CHECKED_IN", "COMPLETED");

    private final CentreRepository centreRepository;
    private final AppointmentRepository appointmentRepository;

    public ServiceCentreService(
            CentreRepository centreRepository,
            AppointmentRepository appointmentRepository
    ) {
        this.centreRepository = centreRepository;
        this.appointmentRepository = appointmentRepository;
    }

    @Transactional(readOnly = true)
    public List<Centre> getAllCentres() {
        return centreRepository.findAllByOrderByCentreNameAsc();
    }

    @Transactional
    public Centre addCentre(
            String centreName,
            String street,
            String city,
            String district,
            Integer dailyCapacity
    ) {
        String cleanName = requiredText(centreName, "Enter the service centre name.", 100);
        String cleanCity = requiredText(city, "Enter the city.", 100);
        String cleanDistrict = requiredText(district, "Enter the district.", 100);
        String cleanStreet = optionalText(street, 150);

        validateCapacity(dailyCapacity);
        if (centreRepository.existsByCentreNameIgnoreCaseAndCityIgnoreCase(cleanName, cleanCity)) {
            throw new IllegalArgumentException("That service centre already exists in this city.");
        }

        Centre centre = new Centre();
        centre.setCentreName(cleanName);
        centre.setStreet(cleanStreet);
        centre.setCity(cleanCity);
        centre.setDistrict(cleanDistrict);
        centre.setDailyCapacity(dailyCapacity);
        centre.setCentreStatus("ACTIVE");
        return centreRepository.save(centre);
    }

    @Transactional
    public Centre updateCentre(
            Integer centreId,
            String centreName,
            String street,
            String city,
            String district,
            Integer dailyCapacity
    ) {
        Centre centre = findCentre(centreId);
        String cleanName = requiredText(centreName, "Enter the service centre name.", 100);
        String cleanCity = requiredText(city, "Enter the city.", 100);
        String cleanDistrict = requiredText(district, "Enter the district.", 100);
        String cleanStreet = optionalText(street, 150);
        validateCapacity(dailyCapacity);

        if (centreRepository.existsByCentreNameIgnoreCaseAndCityIgnoreCaseAndCentreIdNot(
                cleanName, cleanCity, centreId)) {
            throw new IllegalArgumentException("That service centre already exists in this city.");
        }

        centre.setCentreName(cleanName);
        centre.setStreet(cleanStreet);
        centre.setCity(cleanCity);
        centre.setDistrict(cleanDistrict);
        centre.setDailyCapacity(dailyCapacity);
        return centreRepository.save(centre);
    }

    @Transactional
    public String toggleCentreStatus(Integer centreId) {
        Centre centre = findCentre(centreId);
        String newStatus = "ACTIVE".equals(centre.getCentreStatus()) ? "INACTIVE" : "ACTIVE";
        centre.setCentreStatus(newStatus);
        centreRepository.save(centre);
        return newStatus;
    }

    @Transactional
    public void removeCentre(Integer centreId) {
        Centre centre = findCentre(centreId);
        boolean hasFutureBookings = appointmentRepository
                .existsByCentreIdAndScheduledAtAfterAndAppointmentStatusIn(
                        centreId, LocalDateTime.now(), ACTIVE_BOOKING_STATUSES);
        if (hasFutureBookings) {
            throw new IllegalArgumentException(
                    "This centre has future appointments. Deactivate it instead of deleting it.");
        }

        try {
            centreRepository.delete(centre);
            centreRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw new IllegalArgumentException(
                    "This centre has existing records. Deactivate it instead of deleting it.");
        }
    }

    @Transactional(readOnly = true)
    public long getTodayBookingCount(Integer centreId) {
        LocalDate today = LocalDate.now();
        return appointmentRepository.countByCentreIdAndScheduledAtBetweenAndAppointmentStatusIn(
                centreId,
                today.atStartOfDay(),
                today.plusDays(1).atStartOfDay().minusNanos(1),
                ACTIVE_BOOKING_STATUSES
        );
    }

    private Centre findCentre(Integer centreId) {
        return centreRepository.findById(centreId)
                .orElseThrow(() -> new IllegalArgumentException("Service centre was not found."));
    }

    private void validateCapacity(Integer dailyCapacity) {
        if (dailyCapacity == null || dailyCapacity < 1 || dailyCapacity > 2000) {
            throw new IllegalArgumentException("Daily capacity must be between 1 and 2000.");
        }
    }

    private String requiredText(String value, String errorMessage, int maximumLength) {
        String cleanValue = value == null ? "" : value.trim();
        if (cleanValue.isBlank()) throw new IllegalArgumentException(errorMessage);
        if (cleanValue.length() > maximumLength) {
            throw new IllegalArgumentException("The entered value is too long.");
        }
        return cleanValue;
    }

    private String optionalText(String value, int maximumLength) {
        String cleanValue = value == null ? "" : value.trim();
        if (cleanValue.length() > maximumLength) {
            throw new IllegalArgumentException("The street address is too long.");
        }
        return cleanValue.isBlank() ? null : cleanValue;
    }
}
