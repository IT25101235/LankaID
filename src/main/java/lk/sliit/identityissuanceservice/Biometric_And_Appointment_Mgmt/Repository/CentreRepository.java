package lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Repository;

import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Model.Centre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository pattern: CRUD for centres. Method names like findAllByCentreStatus... become SQL
 * automatically.
 */
@Repository
public interface CentreRepository extends JpaRepository<Centre, Integer> {
    /** Locks the centre row while it is being used for a booking. */
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select c from Centre c where c.centreId = :id")
    java.util.Optional<Centre> findByIdForBooking(@org.springframework.data.repository.query.Param("id") Integer id);
    List<Centre> findAllByOrderByCentreNameAsc();

    List<Centre> findAllByCentreStatusOrderByCentreNameAsc(String centreStatus);

    boolean existsByCentreNameIgnoreCaseAndCityIgnoreCase(String centreName, String city);

    boolean existsByCentreNameIgnoreCaseAndCityIgnoreCaseAndCentreIdNot(
            String centreName, String city, Integer centreId);
}
