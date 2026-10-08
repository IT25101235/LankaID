package lk.sliit.identityissuanceservice.Document_Renewal_Replacement_And_Delivery_Mgmt.Repository;

import lk.sliit.identityissuanceservice.Document_Renewal_Replacement_And_Delivery_Mgmt.Model.DocumentDelivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

// Spring Data supplies persistence and derives these delivery queries from method names.
@Repository
public interface DocumentDeliveryRepository extends JpaRepository<DocumentDelivery, Integer> {

    // Each application has at most one delivery record.
    Optional<DocumentDelivery> findByApplicationId(Integer applicationId);

    Optional<DocumentDelivery> findByTrackingReferenceIgnoreCase(String trackingReference);

    // Load tracking rows together for a list of the citizen's applications.
    List<DocumentDelivery> findAllByApplicationIdIn(Collection<Integer> applicationIds);

    // The collection queue uses centre collection plus READY_FOR_COLLECTION, oldest first.
    List<DocumentDelivery> findAllByDeliveryMethodAndDeliveryStatusOrderByCreatedAtAsc(
            String deliveryMethod,
            String deliveryStatus
    );

    boolean existsByTrackingReference(String trackingReference);
}
