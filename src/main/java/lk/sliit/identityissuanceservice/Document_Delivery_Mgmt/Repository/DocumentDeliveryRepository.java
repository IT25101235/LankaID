package lk.sliit.identityissuanceservice.Document_Delivery_Mgmt.Repository;

import lk.sliit.identityissuanceservice.Document_Delivery_Mgmt.Model.DocumentDelivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentDeliveryRepository extends JpaRepository<DocumentDelivery, Integer> {

    Optional<DocumentDelivery> findByApplicationId(Integer applicationId);

    Optional<DocumentDelivery> findByTrackingReferenceIgnoreCase(String trackingReference);

    List<DocumentDelivery> findAllByApplicationIdIn(Collection<Integer> applicationIds);

    List<DocumentDelivery> findAllByDeliveryMethodAndDeliveryStatusOrderByCreatedAtAsc(
            String deliveryMethod,
            String deliveryStatus
    );

    boolean existsByTrackingReference(String trackingReference);
}
