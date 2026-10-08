package lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** Entity: proof that biometrics were collected for an application, and which officer collected them. */
@Entity
@Table(name = "BiometricCollection", schema = "dbo")
public class BiometricCollection {

    // OOP - Encapsulation: the fields are private. Other classes can only read or change them through the
    // getters and setters.
    @Id
    @Column(name = "application_id")
    private Integer applicationId;

    @Column(name = "staff_id", nullable = false)
    private Integer staffId;

    @Column(name = "collected_at")
    private LocalDateTime collectedAt;

    @Column(name = "collection_status", length = 20)
    private String collectionStatus;

    /** Fills in the collected time and COLLECTED status on first save. */
    @PrePersist
    private void applyDefaults() {
        if (collectedAt == null) collectedAt = LocalDateTime.now();
        if (collectionStatus == null) collectionStatus = "COLLECTED";
    }

    // OOP - Encapsulation: public getters and setters give controlled access to the private fields.
    public Integer getApplicationId() { return applicationId; }
    public void setApplicationId(Integer applicationId) { this.applicationId = applicationId; }
    public Integer getStaffId() { return staffId; }
    public void setStaffId(Integer staffId) { this.staffId = staffId; }
    public LocalDateTime getCollectedAt() { return collectedAt; }
    public void setCollectedAt(LocalDateTime collectedAt) { this.collectedAt = collectedAt; }
    public String getCollectionStatus() { return collectionStatus; }
    public void setCollectionStatus(String collectionStatus) { this.collectionStatus = collectionStatus; }
}
