package lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Repository;

import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model.CitizenApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface CitizenApplicationRepository extends JpaRepository<CitizenApplication, Integer> {

    boolean existsByApplicationNo(String applicationNo);

    boolean existsByCitizenIdAndServiceTypeAndCurrentStatusIn(
            Integer citizenId,
            String serviceType,
            Collection<String> statuses
    );

    Optional<CitizenApplication> findByApplicationIdAndCitizenId(
            Integer applicationId,
            Integer citizenId
    );

    List<CitizenApplication> findAllByCitizenIdOrderByCreatedAtDesc(Integer citizenId);
}
