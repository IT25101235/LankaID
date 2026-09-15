package lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Repository;

import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Model.Centre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CentreRepository extends JpaRepository<Centre, Integer> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select c from Centre c where c.centreId = :id")
    java.util.Optional<Centre> findByIdForBooking(@org.springframework.data.repository.query.Param("id") Integer id);
    List<Centre> findAllByOrderByCentreNameAsc();

    boolean existsByCentreNameIgnoreCaseAndCityIgnoreCase(String centreName, String city);
}
