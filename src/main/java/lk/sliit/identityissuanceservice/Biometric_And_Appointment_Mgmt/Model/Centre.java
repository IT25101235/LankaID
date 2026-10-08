package lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/** Entity: a service centre where citizens give their biometrics. */
@Entity
@Table(name = "Centre", schema = "dbo")
public class Centre {

    // OOP - Encapsulation: the fields are private. Other classes can only read or change them through the
    // getters and setters.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "centre_id")
    private Integer centreId;

    @Column(name = "centre_name", nullable = false, length = 100)
    private String centreName;

    @Column(name = "street", length = 150)
    private String street;

    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @Column(name = "district", nullable = false, length = 100)
    private String district;

    @Column(name = "daily_capacity", nullable = false)
    private Integer dailyCapacity;

    @Column(name = "centre_status", nullable = false, length = 20)
    private String centreStatus;

    /** New centres start as ACTIVE. */
    @PrePersist
    private void applyDefaults() {
        if (centreStatus == null) centreStatus = "ACTIVE";
    }

    // OOP - Encapsulation: public getters and setters give controlled access to the private fields.
    public Integer getCentreId() { return centreId; }
    public void setCentreId(Integer centreId) { this.centreId = centreId; }
    public String getCentreName() { return centreName; }
    public void setCentreName(String centreName) { this.centreName = centreName; }
    public String getStreet() { return street; }
    public void setStreet(String street) { this.street = street; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }
    public Integer getDailyCapacity() { return dailyCapacity; }
    public void setDailyCapacity(Integer dailyCapacity) { this.dailyCapacity = dailyCapacity; }
    public String getCentreStatus() { return centreStatus; }
    public void setCentreStatus(String centreStatus) { this.centreStatus = centreStatus; }
}
