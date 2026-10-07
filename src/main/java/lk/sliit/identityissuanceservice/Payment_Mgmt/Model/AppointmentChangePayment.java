package lk.sliit.identityissuanceservice.Payment_Mgmt.Model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** A separate receipt for a paid reschedule or biometric rebooking. */
@Entity
@Table(name = "AppointmentChangePayment", schema = "dbo")
public class AppointmentChangePayment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "change_payment_id")
    private Integer changePaymentId;
    @Column(name = "appointment_id", nullable = false)
    private Integer appointmentId;
    @Column(name = "request_token", nullable = false, unique = true, length = 36)
    private String requestToken;
    @Column(name = "change_type", nullable = false, length = 20)
    private String changeType;
    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
    @Column(name = "previous_scheduled_at", nullable = false)
    private LocalDateTime previousScheduledAt;
    @Column(name = "new_scheduled_at", nullable = false)
    private LocalDateTime newScheduledAt;
    @Column(name = "paid_at", nullable = false)
    private LocalDateTime paidAt;

    public Integer getChangePaymentId() { return changePaymentId; }
    public Integer getAppointmentId() { return appointmentId; }
    public void setAppointmentId(Integer value) { appointmentId = value; }
    public String getRequestToken() { return requestToken; }
    public void setRequestToken(String value) { requestToken = value; }
    public String getChangeType() { return changeType; }
    public void setChangeType(String value) { changeType = value; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal value) { amount = value; }
    public LocalDateTime getPreviousScheduledAt() { return previousScheduledAt; }
    public void setPreviousScheduledAt(LocalDateTime value) { previousScheduledAt = value; }
    public LocalDateTime getNewScheduledAt() { return newScheduledAt; }
    public void setNewScheduledAt(LocalDateTime value) { newScheduledAt = value; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime value) { paidAt = value; }
}
