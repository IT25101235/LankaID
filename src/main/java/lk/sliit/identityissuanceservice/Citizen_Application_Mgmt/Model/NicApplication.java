package lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "NICApplication", schema = "dbo")
// Stores the NIC-specific details attached to a common application record.
public class NicApplication {

    // Uses the common application ID as its primary key.
    @Id
    @Column(name = "application_id")
    private Integer applicationId;

    @Column(name = "last_nic_no", length = 12)
    private String lastNicNo;

    @Column(name = "last_issued_date")
    private LocalDateTime lastIssuedDate;

    @Column(name = "police_station", length = 100)
    private String policeStation;

    @Column(name = "police_report_date")
    private LocalDateTime policeReportDate;

    // Returns the common application ID.
    public Integer getApplicationId() { return applicationId; }
    // Sets the common application ID.
    public void setApplicationId(Integer applicationId) { this.applicationId = applicationId; }
    // Returns the previous NIC number, when applicable.
    public String getLastNicNo() { return lastNicNo; }
    // Sets the previous NIC number.
    public void setLastNicNo(String lastNicNo) { this.lastNicNo = lastNicNo; }
    // Returns the previous NIC issue date.
    public LocalDateTime getLastIssuedDate() { return lastIssuedDate; }
    // Sets the previous NIC issue date.
    public void setLastIssuedDate(LocalDateTime lastIssuedDate) { this.lastIssuedDate = lastIssuedDate; }
    // Returns the police station used for a loss report.
    public String getPoliceStation() { return policeStation; }
    // Sets the police station used for a loss report.
    public void setPoliceStation(String policeStation) { this.policeStation = policeStation; }
    // Returns the police report date.
    public LocalDateTime getPoliceReportDate() { return policeReportDate; }
    // Sets the police report date.
    public void setPoliceReportDate(LocalDateTime policeReportDate) { this.policeReportDate = policeReportDate; }
}
