package lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "Application", schema = "dbo")
// Stores the common database fields shared by every citizen application.
public class CitizenApplication {

    // Database-generated internal application ID.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "application_id")
    private Integer applicationId;

    @Column(name = "application_no", nullable = false, unique = true, length = 30)
    private String applicationNo;

    @Column(name = "citizen_id", nullable = false)
    private Integer citizenId;

    @Column(name = "service_type", nullable = false, length = 20)
    private String serviceType;

    @Column(name = "application_kind", nullable = false, length = 20)
    private String applicationKind;

    @Column(name = "current_status", length = 30)
    private String currentStatus;

    @Column(name = "issued_document_number", length = 30)
    private String issuedDocumentNumber;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "declaration_accepted", columnDefinition = "CHAR(1)")
    private String declarationAccepted;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Supplies default status, declaration, and timestamps before first save.
    @PrePersist
    private void applyDefaults() {
        LocalDateTime now = LocalDateTime.now();
        if (currentStatus == null) currentStatus = "DRAFT";
        if (declarationAccepted == null) declarationAccepted = "N";
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    // Refreshes the updated time before an existing record is saved.
    @PreUpdate
    private void updateTimestamp() {
        updatedAt = LocalDateTime.now();
    }

    // Returns the internal application ID.
    public Integer getApplicationId() { return applicationId; }
    // Sets the internal application ID.
    public void setApplicationId(Integer applicationId) { this.applicationId = applicationId; }
    // Returns the public application number.
    public String getApplicationNo() { return applicationNo; }
    // Sets the public application number.
    public void setApplicationNo(String applicationNo) { this.applicationNo = applicationNo; }
    // Returns the owner citizen ID.
    public Integer getCitizenId() { return citizenId; }
    // Sets the owner citizen ID.
    public void setCitizenId(Integer citizenId) { this.citizenId = citizenId; }
    // Returns the service type, such as NIC or PASSPORT.
    public String getServiceType() { return serviceType; }
    // Sets the service type.
    public void setServiceType(String serviceType) { this.serviceType = serviceType; }
    // Returns whether the application is new, renewal, or replacement.
    public String getApplicationKind() { return applicationKind; }
    // Sets the application kind.
    public void setApplicationKind(String applicationKind) { this.applicationKind = applicationKind; }
    // Returns the current workflow status.
    public String getCurrentStatus() { return currentStatus; }
    // Sets the current workflow status.
    public void setCurrentStatus(String currentStatus) { this.currentStatus = currentStatus; }
    public String getIssuedDocumentNumber() { return issuedDocumentNumber; }
    public void setIssuedDocumentNumber(String issuedDocumentNumber) { this.issuedDocumentNumber = issuedDocumentNumber; }
    // Returns the most recent submission time.
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    // Sets the most recent submission time.
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
    // Returns whether the citizen accepted the declaration.
    public String getDeclarationAccepted() { return declarationAccepted; }
    // Sets whether the citizen accepted the declaration.
    public void setDeclarationAccepted(String declarationAccepted) { this.declarationAccepted = declarationAccepted; }
    // Returns when the application was created.
    public LocalDateTime getCreatedAt() { return createdAt; }
    // Sets when the application was created.
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    // Returns when the application was last updated.
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    // Sets when the application was last updated.
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
