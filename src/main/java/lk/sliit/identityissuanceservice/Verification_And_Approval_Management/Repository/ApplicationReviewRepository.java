package lk.sliit.identityissuanceservice.Verification_And_Approval_Management.Repository;

import lk.sliit.identityissuanceservice.Verification_And_Approval_Management.Model.ApplicationReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationReviewRepository extends JpaRepository<ApplicationReview, Integer> {

    Optional<ApplicationReview> findTopByApplicationIdOrderByReviewSequenceDesc(Integer applicationId);

    List<ApplicationReview> findAllByOrderByReviewedAtDesc();
}
