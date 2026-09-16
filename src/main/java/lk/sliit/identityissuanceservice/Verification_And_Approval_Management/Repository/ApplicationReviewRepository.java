package lk.sliit.identityissuanceservice.Verification_And_Approval_Management.Repository;

import lk.sliit.identityissuanceservice.Verification_And_Approval_Management.Dto.ReviewedApplicationRow;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;

import lk.sliit.identityissuanceservice.Verification_And_Approval_Management.Model.ApplicationReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationReviewRepository extends JpaRepository<ApplicationReview, Integer> {

    @Query("""
            select new lk.sliit.identityissuanceservice.Verification_And_Approval_Management.Dto.ReviewedApplicationRow(r, a, c)
            from ApplicationReview r join CitizenApplication a on a.applicationId = r.applicationId
            left join CitizenAccount c on c.userId = a.citizenId
            where a.serviceType = :serviceType
            and (:decision = '' or r.decision = :decision)
            and (:start is null or r.reviewedAt >= :start)
            and (:end is null or r.reviewedAt < :end)
            and (:search = '' or locate(:search, lower(a.applicationNo)) > 0
                or locate(:search, lower(concat(coalesce(c.givenName, ''), ' ', coalesce(c.familyName, '')))) > 0
                or locate(:search, lower(coalesce(c.surname, ''))) > 0)
            order by r.reviewedAt desc, r.reviewId desc
            """)
    Page<ReviewedApplicationRow> findReviewed(
            String serviceType, String decision, String search, LocalDateTime start,
            LocalDateTime end, Pageable pageable);

    Optional<ApplicationReview> findTopByApplicationIdOrderByReviewSequenceDesc(Integer applicationId);

    List<ApplicationReview> findAllByOrderByReviewedAtDesc();
}
