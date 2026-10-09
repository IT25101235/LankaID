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
 * Audited request for a Director's recommendation on an exceptional application.
 * A withdrawal is a logical delete: the row is retained and marked WITHDRAWN.
 */
@Entity
@Table(name = "DirectorReviewReferral", schema = "dbo")
public class DirectorReviewReferral {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "referral_id")
    private Integer referralId;

    @Column(name = "application_id", nullable = false)
    private Integer applicationId;

    @Column(name = "referred_by_staff_id", nullable = false)
    private Integer referredByStaffId;

    @Column(name = "referral_category", nullable = false, length = 30)
    private String referralCategory;

    @Column(name = "referral_reason", nullable = false, length = 1000)
    private String referralReason;

    @Column(name = "referral_status", nullable = false, length = 20)
    private String referralStatus;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "director_id")
    private Integer directorId;

    @Column(name = "recommendation", length = 30)
    private String recommendation;

    @Column(name = "director_comment", length = 1000)
    private String directorComment;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @Column(name = "withdrawn_by_staff_id")
    private Integer withdrawnByStaffId;

    @Column(name = "withdrawal_reason", length = 500)
    private String withdrawalReason;

    @Column(name = "withdrawn_at")
    private LocalDateTime withdrawnAt;

    @PrePersist
    private void applyDefaults() {
        if (referralStatus == null) referralStatus = "PENDING";
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public Integer getReferralId() { return referralId; }
    public void setReferralId(Integer referralId) { this.referralId = referralId; }
    public Integer getApplicationId() { return applicationId; }
    public void setApplicationId(Integer applicationId) { this.applicationId = applicationId; }
    public Integer getReferredByStaffId() { return referredByStaffId; }
    public void setReferredByStaffId(Integer referredByStaffId) { this.referredByStaffId = referredByStaffId; }
    public String getReferralCategory() { return referralCategory; }
    public void setReferralCategory(String referralCategory) { this.referralCategory = referralCategory; }
    public String getReferralReason() { return referralReason; }
    public void setReferralReason(String referralReason) { this.referralReason = referralReason; }
    public String getReferralStatus() { return referralStatus; }
    public void setReferralStatus(String referralStatus) { this.referralStatus = referralStatus; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public Integer getDirectorId() { return directorId; }
    public void setDirectorId(Integer directorId) { this.directorId = directorId; }
    public String getRecommendation() { return recommendation; }
    public void setRecommendation(String recommendation) { this.recommendation = recommendation; }
    public String getDirectorComment() { return directorComment; }
    public void setDirectorComment(String directorComment) { this.directorComment = directorComment; }
    public LocalDateTime getRespondedAt() { return respondedAt; }
    public void setRespondedAt(LocalDateTime respondedAt) { this.respondedAt = respondedAt; }
    public Integer getWithdrawnByStaffId() { return withdrawnByStaffId; }
    public void setWithdrawnByStaffId(Integer withdrawnByStaffId) { this.withdrawnByStaffId = withdrawnByStaffId; }
    public String getWithdrawalReason() { return withdrawalReason; }
    public void setWithdrawalReason(String withdrawalReason) { this.withdrawalReason = withdrawalReason; }
    public LocalDateTime getWithdrawnAt() { return withdrawnAt; }
    public void setWithdrawnAt(LocalDateTime withdrawnAt) { this.withdrawnAt = withdrawnAt; }
}
