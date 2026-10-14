package lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Repository;

import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model.PassportApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
// Provides standard database operations for passport-specific application details.
public interface PassportApplicationRepository extends JpaRepository<PassportApplication, Integer> {
}
