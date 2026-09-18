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
public interface CitizenApplicationRepository extends JpaRepository<CitizenApplication, Integer> {

    interface ApplicationCount {
        Integer getCitizenId();
        String getServiceType();
        Long getApplicationCount();
    }

    @Query("""
            select a.citizenId as citizenId, a.serviceType as serviceType, count(a) as applicationCount
            from CitizenApplication a
            where a.citizenId in :citizenIds
            group by a.citizenId, a.serviceType
            """)
    List<ApplicationCount> countApplicationsByCitizenAndServiceType(
            @Param("citizenIds") Collection<Integer> citizenIds
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select a from CitizenApplication a where a.applicationId = :applicationId and a.citizenId = :citizenId
            and a.currentStatus in ('DRAFT', 'CORRECTION_REQUIRED')
            """)
    Optional<CitizenApplication> findEditableForUpdate(Integer applicationId, Integer citizenId);

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

    Optional<CitizenApplication> findByApplicationNoIgnoreCase(String applicationNo);

    List<CitizenApplication> findAllByCitizenIdOrderByCreatedAtDesc(Integer citizenId);

    List<CitizenApplication> findAllByCurrentStatusOrderByUpdatedAtAsc(String currentStatus);

    List<CitizenApplication> findAllByCurrentStatusOrderByUpdatedAtDesc(String currentStatus);

    Page<CitizenApplication> findAllByOrderByUpdatedAtDesc(Pageable pageable);

    Page<CitizenApplication> findAllByServiceTypeOrderByUpdatedAtDesc(
            String serviceType,
            Pageable pageable
    );

    Page<CitizenApplication> findAllByApplicationNoContainingIgnoreCaseOrderByUpdatedAtDesc(
            String applicationNo,
            Pageable pageable
    );

    Page<CitizenApplication> findAllByServiceTypeAndApplicationNoContainingIgnoreCaseOrderByUpdatedAtDesc(
            String serviceType,
            String applicationNo,
            Pageable pageable
    );
}
