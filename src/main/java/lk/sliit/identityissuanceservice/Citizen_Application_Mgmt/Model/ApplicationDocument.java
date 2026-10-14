package lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "ApplicationDocument", schema = "dbo")
// Stores the database details for one uploaded application document.
public class ApplicationDocument {

    // Database-generated document ID.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "document_id")
    private Integer documentId;

    @Column(name = "application_id", nullable = false)
    private Integer applicationId;

    @Column(name = "document_type", nullable = false, length = 40)
    private String documentType;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;

    @Column(name = "verification_status", length = 20)
    private String verificationStatus;

    @Column(name = "verified_by_staff_id")
    private Integer verifiedByStaffId;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    // Supplies the upload time and initial verification status before first save.
    @PrePersist
    private void applyDefaults() {
        if (uploadedAt == null) uploadedAt = LocalDateTime.now();
        if (verificationStatus == null) verificationStatus = "PENDING";
    }

    // Returns the document ID.
    public Integer getDocumentId() { return documentId; }
    // Sets the document ID.
    public void setDocumentId(Integer documentId) { this.documentId = documentId; }
    // Returns the owning application ID.
    public Integer getApplicationId() { return applicationId; }
    // Sets the owning application ID.
    public void setApplicationId(Integer applicationId) { this.applicationId = applicationId; }
    // Returns the document type.
    public String getDocumentType() { return documentType; }
    // Sets the document type.
    public void setDocumentType(String documentType) { this.documentType = documentType; }
    // Returns the stored relative file path.
    public String getFilePath() { return filePath; }
    // Sets the stored relative file path.
    public void setFilePath(String filePath) { this.filePath = filePath; }
    // Returns when the document was uploaded.
    public LocalDateTime getUploadedAt() { return uploadedAt; }
    // Sets when the document was uploaded.
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
    // Returns the document verification status.
    public String getVerificationStatus() { return verificationStatus; }
    // Sets the document verification status.
    public void setVerificationStatus(String verificationStatus) { this.verificationStatus = verificationStatus; }
    // Returns the staff member who verified the document.
    public Integer getVerifiedByStaffId() { return verifiedByStaffId; }
    // Sets the staff member who verified the document.
    public void setVerifiedByStaffId(Integer verifiedByStaffId) { this.verifiedByStaffId = verifiedByStaffId; }
    // Returns when the document was verified.
    public LocalDateTime getVerifiedAt() { return verifiedAt; }
    // Sets when the document was verified.
    public void setVerifiedAt(LocalDateTime verifiedAt) { this.verifiedAt = verifiedAt; }
}
