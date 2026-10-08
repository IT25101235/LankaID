package lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Entity: records that an officer checked a citizen in. One row per appointment (same id as the
 * appointment).
 */
@Entity
@Table(name = "AppointmentCheckIn", schema = "dbo")
public class AppointmentCheckIn {

    // OOP - Encapsulation: the fields are private. Other classes can only read or change them through the
    // getters and setters.
    @Id
    @Column(name = "appointment_id")
    private Integer appointmentId;

    @Column(name = "staff_id", nullable = false)
    private Integer staffId;

    @Column(name = "checked_in_at")
    private LocalDateTime checkedInAt;

    @Column(name = "check_in_method", nullable = false, length = 25)
    private String checkInMethod;

    /** Sets the check-in time automatically on first save. */
    @PrePersist
    private void applyDefaults() {
        if (checkedInAt == null) checkedInAt = LocalDateTime.now();
    }

    // OOP - Encapsulation: public getters and setters give controlled access to the private fields.
    public Integer getAppointmentId() { return appointmentId; }
    public void setAppointmentId(Integer appointmentId) { this.appointmentId = appointmentId; }
    public Integer getStaffId() { return staffId; }
    public void setStaffId(Integer staffId) { this.staffId = staffId; }
    public LocalDateTime getCheckedInAt() { return checkedInAt; }
    public void setCheckedInAt(LocalDateTime checkedInAt) { this.checkedInAt = checkedInAt; }
    public String getCheckInMethod() { return checkInMethod; }
    public void setCheckInMethod(String checkInMethod) { this.checkInMethod = checkInMethod; }
}
