package lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Repository;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;

import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model.CitizenApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
// Provides application queries shared by citizen and officer workflows.
public interface CitizenApplicationRepository extends JpaRepository<CitizenApplication, Integer> {

    // Small query result used when only application totals are needed.
    interface ApplicationCount {
        // Returns the citizen included in this count.
        Integer getCitizenId();
        // Returns the service included in this count.
        String getServiceType();
        // Returns the number of matching applications.
        Long getApplicationCount();
    }

    @Query("""
            select a.citizenId as citizenId, a.serviceType as serviceType, count(a) as applicationCount
            from CitizenApplication a
            where a.citizenId in :citizenIds
            group by a.citizenId, a.serviceType
            """)
    // Counts applications for each citizen and service without loading full entities.
    List<ApplicationCount> countApplicationsByCitizenAndServiceType(
            @Param("citizenIds") Collection<Integer> citizenIds
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select a from CitizenApplication a where a.applicationId = :applicationId and a.citizenId = :citizenId
            and a.currentStatus in ('DRAFT', 'CORRECTION_REQUIRED')
            """)
    // Locks and returns a citizen-owned draft or correction that can be edited.
    Optional<CitizenApplication> findEditableForUpdate(Integer applicationId, Integer citizenId);

    // Checks whether a generated public application number already exists.
    boolean existsByApplicationNo(String applicationNo);

    // Checks whether the citizen has ever applied for the service.
    boolean existsByCitizenIdAndServiceType(Integer citizenId, String serviceType);

    // Checks for an application whose status is in the supplied active-status list.
    boolean existsByCitizenIdAndServiceTypeAndCurrentStatusIn(
            Integer citizenId,
            String serviceType,
            Collection<String> statuses
    );

    // Finds an application only when it belongs to the citizen.
    Optional<CitizenApplication> findByApplicationIdAndCitizenId(
            Integer applicationId,
            Integer citizenId
    );

    // Finds an application from its public number without case sensitivity.
    Optional<CitizenApplication> findByApplicationNoIgnoreCase(String applicationNo);

    // Returns a citizen's applications with the newest first.
    List<CitizenApplication> findAllByCitizenIdOrderByCreatedAtDesc(Integer citizenId);

    // Returns applications in a status with the oldest update first.
    List<CitizenApplication> findAllByCurrentStatusOrderByUpdatedAtAsc(String currentStatus);

    // Returns applications in a status with the newest update first.
    List<CitizenApplication> findAllByCurrentStatusOrderByUpdatedAtDesc(String currentStatus);

    // Returns a page of applications ordered by latest update.
    Page<CitizenApplication> findAllByOrderByUpdatedAtDesc(Pageable pageable);

    // Returns a page filtered by service type.
    Page<CitizenApplication> findAllByServiceTypeOrderByUpdatedAtDesc(
            String serviceType,
            Pageable pageable
    );

    // Searches application numbers without case sensitivity.
    Page<CitizenApplication> findAllByApplicationNoContainingIgnoreCaseOrderByUpdatedAtDesc(
            String applicationNo,
            Pageable pageable
    );

    // Searches application numbers within one service type.
    Page<CitizenApplication> findAllByServiceTypeAndApplicationNoContainingIgnoreCaseOrderByUpdatedAtDesc(
            String serviceType,
            String applicationNo,
            Pageable pageable
    );
}
