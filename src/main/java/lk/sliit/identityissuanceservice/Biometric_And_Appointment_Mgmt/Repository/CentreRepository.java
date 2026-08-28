package lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Repository;

import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Model.Centre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CentreRepository extends JpaRepository<Centre, Integer> {
    List<Centre> findAllByOrderByCentreNameAsc();

    boolean existsByCentreNameIgnoreCaseAndCityIgnoreCase(String centreName, String city);
}
