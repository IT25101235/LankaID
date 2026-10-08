package lk.sliit.identityissuanceservice.Verification_And_Approval_Management.Model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Persistent audit entry for one officer decision.
 * Multiple rows may belong to the same application, so corrections and later re-reviews remain
 * historically traceable instead of overwriting the previous decision.
 *
 * <p>OOP encapsulation: fields remain private and are accessed through getters and setters.</p>
 */
@Entity
@Table(name = "ApplicationReview", schema = "dbo")
public class ApplicationReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Integer reviewId;

    @Column(name = "application_id", nullable = false)
    private Integer applicationId;

    @Column(name = "staff_id", nullable = false)
    private Integer staffId;

    @Column(name = "review_sequence", nullable = false)
    private Integer reviewSequence;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "decision", nullable = false, length = 30)
    private String decision;

    @Column(name = "correction_message", length = 1000)
    private String correctionMessage;

    @PrePersist
    private void applyDefaults() {
        // JPA assigns the audit timestamp automatically when a new review row is persisted.
        if (reviewedAt == null) reviewedAt = LocalDateTime.now();
    }

    public Integer getReviewId() { return reviewId; }
    public void setReviewId(Integer reviewId) { this.reviewId = reviewId; }
    public Integer getApplicationId() { return applicationId; }
    public void setApplicationId(Integer applicationId) { this.applicationId = applicationId; }
    public Integer getStaffId() { return staffId; }
    public void setStaffId(Integer staffId) { this.staffId = staffId; }
    public Integer getReviewSequence() { return reviewSequence; }
    public void setReviewSequence(Integer reviewSequence) { this.reviewSequence = reviewSequence; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }
    public String getCorrectionMessage() { return correctionMessage; }
    public void setCorrectionMessage(String correctionMessage) { this.correctionMessage = correctionMessage; }
}
