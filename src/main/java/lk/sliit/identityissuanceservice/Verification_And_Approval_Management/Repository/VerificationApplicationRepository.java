package lk.sliit.identityissuanceservice.Verification_And_Approval_Management.Repository;

import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model.CitizenApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data abstraction for applications used by the verification workflow.
 * Extending {@link JpaRepository} demonstrates inheritance and polymorphism: Spring supplies the
 * runtime implementation of CRUD and derived-query methods declared by this interface.
 */
@Repository
public interface VerificationApplicationRepository extends JpaRepository<CitizenApplication, Integer> {

    // Select only actionable statuses and preserve first-submitted-first-reviewed ordering.
    List<CitizenApplication> findByServiceTypeAndCurrentStatusInOrderBySubmittedAtAscApplicationIdAsc(
            String serviceType,
            Collection<String> statuses
    );

    // Application numbers are user-facing references, so matching is case-insensitive.
    Optional<CitizenApplication> findByApplicationNoIgnoreCase(String applicationNo);
}
