package lk.sliit.identityissuanceservice.Verification_And_Approval_Management.Repository;

import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model.CitizenApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface VerificationApplicationRepository extends JpaRepository<CitizenApplication, Integer> {

    List<CitizenApplication> findByServiceTypeAndCurrentStatusInOrderBySubmittedAtAscApplicationIdAsc(
            String serviceType,
            Collection<String> statuses
    );

    Optional<CitizenApplication> findByApplicationNoIgnoreCase(String applicationNo);
}
