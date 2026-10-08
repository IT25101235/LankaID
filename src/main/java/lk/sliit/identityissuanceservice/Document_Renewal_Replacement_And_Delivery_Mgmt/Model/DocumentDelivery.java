package lk.sliit.identityissuanceservice.Document_Renewal_Replacement_And_Delivery_Mgmt.Model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

// Persists a document's chosen destination and delivery progress.
@Entity
@Table(name = "DocumentDelivery", schema = "dbo")
public class DocumentDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "delivery_id")
    private Integer deliveryId;

    // The unique application ID allows only one delivery row per application.
    @Column(name = "application_id", nullable = false, unique = true)
    private Integer applicationId;

    @Column(name = "centre_id")
    private Integer centreId;

    @Column(name = "delivery_method", nullable = false, length = 25)
    private String deliveryMethod;

    @Column(name = "delivery_address", length = 500)
    private String deliveryAddress;

    @Column(name = "delivery_status", nullable = false, length = 25)
    private String deliveryStatus;

    // A unique reference identifies the delivery for citizen tracking and officer lookup.
    @Column(name = "tracking_reference", nullable = false, unique = true, length = 50)
    private String trackingReference;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    // Give new delivery rows their initial status and creation time.
    @PrePersist
    private void applyDefaults() {
        if (deliveryStatus == null) deliveryStatus = "PREPARING";
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public Integer getDeliveryId() { return deliveryId; }
    public void setDeliveryId(Integer deliveryId) { this.deliveryId = deliveryId; }
    public Integer getApplicationId() { return applicationId; }
    public void setApplicationId(Integer applicationId) { this.applicationId = applicationId; }
    public Integer getCentreId() { return centreId; }
    public void setCentreId(Integer centreId) { this.centreId = centreId; }
    public String getDeliveryMethod() { return deliveryMethod; }
    public void setDeliveryMethod(String deliveryMethod) { this.deliveryMethod = deliveryMethod; }
    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }
    public String getDeliveryStatus() { return deliveryStatus; }
    public void setDeliveryStatus(String deliveryStatus) { this.deliveryStatus = deliveryStatus; }
    public String getTrackingReference() { return trackingReference; }
    public void setTrackingReference(String trackingReference) { this.trackingReference = trackingReference; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}
