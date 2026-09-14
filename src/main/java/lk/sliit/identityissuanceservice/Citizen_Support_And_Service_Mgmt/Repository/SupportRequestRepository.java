package lk.sliit.identityissuanceservice.Citizen_Support_And_Service_Mgmt.Repository;

import lk.sliit.identityissuanceservice.Citizen_Support_And_Service_Mgmt.Model.SupportRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface SupportRequestRepository extends JpaRepository<SupportRequest, Integer> {
    Optional<SupportRequest> findFirstByCitizenIdAndRequestTypeAndSubjectOrderByRequestIdDesc(
            Integer citizenId, String requestType, String subject);

    Optional<SupportRequest> findByRequestIdAndCitizenId(Integer requestId, Integer citizenId);

    Page<SupportRequest> findAllByCitizenIdOrderByCreatedAtDesc(Integer citizenId, Pageable pageable);

    Page<SupportRequest> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<SupportRequest> findAllByRequestTypeOrderByCreatedAtDesc(
            String requestType,
            Pageable pageable
    );

    Page<SupportRequest> findAllByRequestStatusOrderByCreatedAtDesc(
            String requestStatus,
            Pageable pageable
    );

    Page<SupportRequest> findAllByRequestTypeAndRequestStatusOrderByCreatedAtDesc(
            String requestType,
            String requestStatus,
            Pageable pageable
    );

    long countByCitizenIdAndRequestStatus(Integer citizenId, String requestStatus);

    long countByRequestStatus(String requestStatus);

    List<SupportRequest> findAllByRespondedAtIsNotNull();
}
