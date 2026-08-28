package lk.sliit.identityissuanceservice.Admin_Mgmt.Service;

import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Model.Centre;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Repository.CentreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ServiceCentreService {

    private final CentreRepository centreRepository;

    public ServiceCentreService(CentreRepository centreRepository) {
        this.centreRepository = centreRepository;
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

        if (dailyCapacity == null || dailyCapacity < 1 || dailyCapacity > 2000) {
            throw new IllegalArgumentException("Daily capacity must be between 1 and 2000.");
        }
        if (centreRepository.existsByCentreNameIgnoreCaseAndCityIgnoreCase(cleanName, cleanCity)) {
            throw new IllegalArgumentException("That service centre already exists in this city.");
        }

        Centre centre = new Centre();
        centre.setCentreName(cleanName);
        centre.setStreet(cleanStreet);
        centre.setCity(cleanCity);
        centre.setDistrict(cleanDistrict);
        centre.setDailyCapacity(dailyCapacity);
        return centreRepository.save(centre);
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
