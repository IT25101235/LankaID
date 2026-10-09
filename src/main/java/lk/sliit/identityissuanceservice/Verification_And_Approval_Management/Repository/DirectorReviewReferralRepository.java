package lk.sliit.identityissuanceservice.Verification_And_Approval_Management.Repository;

import lk.sliit.identityissuanceservice.Verification_And_Approval_Management.Dto.DirectorReferralQueueItem;
import lk.sliit.identityissuanceservice.Verification_And_Approval_Management.Model.DirectorReviewReferral;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

@Repository
public interface DirectorReviewReferralRepository extends JpaRepository<DirectorReviewReferral, Integer> {

    Optional<DirectorReviewReferral> findByApplicationIdAndReferralStatus(
            Integer applicationId, String referralStatus);

    Optional<DirectorReviewReferral> findTopByApplicationIdOrderByCreatedAtDescReferralIdDesc(
            Integer applicationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from DirectorReviewReferral r where r.referralId = :referralId")
    Optional<DirectorReviewReferral> findByIdForUpdate(Integer referralId);

    @Query("""
            select new lk.sliit.identityissuanceservice.Verification_And_Approval_Management.Dto.DirectorReferralQueueItem(r, a, c)
            from DirectorReviewReferral r
            join CitizenApplication a on a.applicationId = r.applicationId
            left join CitizenAccount c on c.userId = a.citizenId
            where r.referralStatus = 'PENDING'
            order by r.createdAt asc, r.referralId asc
            """)
    List<DirectorReferralQueueItem> findPendingQueue();
}
