package lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Repository;

import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model.NicApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
// Provides standard database operations for NIC-specific application details.
public interface NicApplicationRepository extends JpaRepository<NicApplication, Integer> {
}
