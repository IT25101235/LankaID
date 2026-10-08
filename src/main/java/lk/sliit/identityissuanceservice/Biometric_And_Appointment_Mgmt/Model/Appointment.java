package lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Entity for the Appointment table: one booking. Status flow: PENDING_PAYMENT -> CONFIRMED ->
 * CHECKED_IN -> COMPLETED (or CANCELLED).
 */
@Entity
@Table(name = "Appointment", schema = "dbo")
public class Appointment {

    // Reject a stale write if another request changed this booking while the form was open.
    @jakarta.persistence.Version
    @Column(name = "booking_version", nullable = false)
    private Long bookingVersion;

    public Long getBookingVersion() { return bookingVersion; }

    // OOP - Encapsulation: the fields are private. Other classes can only read or change them through the
    // getters and setters.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "appointment_id")
    private Integer appointmentId;

    @Column(name = "citizen_id")
    private Integer citizenId;

    @Column(name = "application_id")
    private Integer applicationId;

    @Column(name = "centre_id", nullable = false)
    private Integer centreId;

    @Column(name = "biometric_slot_id")
    private Integer biometricSlotId;

    @Column(name = "appointment_type", nullable = false, length = 20)
    private String appointmentType;

    @Column(name = "scheduled_at", nullable = false)
    private LocalDateTime scheduledAt;

    @Column(name = "appointment_status", length = 25)
    private String appointmentStatus;

    @Column(name = "reference_no", nullable = false, unique = true, length = 50)
    private String referenceNo;

    @Column(name = "qr_code", length = 500)
    private String qrCode;

    @Column(name = "visitor_phone", length = 15)
    private String visitorPhone;

    @Column(name = "visitor_name", length = 100)
    private String visitorName;

    @Column(name = "visitor_service", length = 50)
    private String visitorService;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /** Fills in the default status and created time the first time the row is saved. */
    @PrePersist
    private void applyDefaults() {
        if (appointmentStatus == null) appointmentStatus = "PENDING_PAYMENT";
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    // OOP - Encapsulation: public getters and setters give controlled access to the private fields.
    public Integer getAppointmentId() { return appointmentId; }
    public void setAppointmentId(Integer appointmentId) { this.appointmentId = appointmentId; }
    public Integer getCitizenId() { return citizenId; }
    public void setCitizenId(Integer citizenId) { this.citizenId = citizenId; }
    public Integer getApplicationId() { return applicationId; }
    public void setApplicationId(Integer applicationId) { this.applicationId = applicationId; }
    public Integer getCentreId() { return centreId; }
    public void setCentreId(Integer centreId) { this.centreId = centreId; }
    public Integer getBiometricSlotId() { return biometricSlotId; }
    public void setBiometricSlotId(Integer biometricSlotId) { this.biometricSlotId = biometricSlotId; }
    public String getAppointmentType() { return appointmentType; }
    public void setAppointmentType(String appointmentType) { this.appointmentType = appointmentType; }
    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(LocalDateTime scheduledAt) { this.scheduledAt = scheduledAt; }
    public String getAppointmentStatus() { return appointmentStatus; }
    public void setAppointmentStatus(String appointmentStatus) { this.appointmentStatus = appointmentStatus; }
    public String getReferenceNo() { return referenceNo; }
    public void setReferenceNo(String referenceNo) { this.referenceNo = referenceNo; }
    public String getQrCode() { return qrCode; }
    public void setQrCode(String qrCode) { this.qrCode = qrCode; }
    public String getVisitorPhone() { return visitorPhone; }
    public void setVisitorPhone(String visitorPhone) { this.visitorPhone = visitorPhone; }
    public String getVisitorName() { return visitorName; }
    public void setVisitorName(String visitorName) { this.visitorName = visitorName; }
    public String getVisitorService() { return visitorService; }
    public void setVisitorService(String visitorService) { this.visitorService = visitorService; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
