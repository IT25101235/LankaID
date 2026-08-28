package lk.sliit.identityissuanceservice.User_Mgmt.Model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "CitizenAccount", schema = "dbo")
public class CitizenAccount {

    @Id
    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "government_record_id")
    private Integer governmentRecordId;

    @Column(name = "family_name", length = 100)
    private String familyName;

    @Column(name = "given_name", length = 150)
    private String givenName;

    @Column(name = "surname", length = 100)
    private String surname;

    @Column(name = "gender", length = 10)
    private String gender;

    @Column(name = "civil_status", length = 15)
    private String civilStatus;

    @Column(name = "occupation", length = 150)
    private String occupation;

    @Column(name = "date_of_birth")
    private LocalDateTime dateOfBirth;

    @Column(name = "birth_certificate_no", length = 50)
    private String birthCertificateNo;

    @Column(name = "birth_place", length = 100)
    private String birthPlace;

    @Column(name = "birth_division", length = 100)
    private String birthDivision;

    @Column(name = "birth_district", length = 100)
    private String birthDistrict;

    @Column(name = "country_of_birth", length = 100)
    private String countryOfBirth;

    @Column(name = "birth_city", length = 100)
    private String birthCity;

    @Column(name = "permanent_house_no", length = 50)
    private String permanentHouseNo;

    @Column(name = "permanent_street", length = 150)
    private String permanentStreet;

    @Column(name = "permanent_city", length = 100)
    private String permanentCity;

    @Column(name = "permanent_postal_code", length = 10)
    private String permanentPostalCode;

    @Column(name = "postal_same_as_permanent", columnDefinition = "CHAR(1)")
    private String postalSameAsPermanent;

    @Column(name = "postal_house_no", length = 50)
    private String postalHouseNo;

    @Column(name = "postal_street", length = 150)
    private String postalStreet;

    @Column(name = "postal_city", length = 100)
    private String postalCity;

    @Column(name = "postal_postal_code", length = 10)
    private String postalPostalCode;

    @Column(name = "dual_citizen", columnDefinition = "CHAR(1)")
    private String dualCitizen;

    @Column(name = "citizenship_certificate_no", length = 50)
    private String citizenshipCertificateNo;

    @Column(name = "citizenship_issued_date")
    private LocalDateTime citizenshipIssuedDate;

    public CitizenAccount() {
    }

    public CitizenAccount(Integer userId) {
        this.userId = userId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getGovernmentRecordId() { return governmentRecordId; }
    public void setGovernmentRecordId(Integer governmentRecordId) { this.governmentRecordId = governmentRecordId; }
    public String getFamilyName() { return familyName; }
    public void setFamilyName(String familyName) { this.familyName = familyName; }
    public String getGivenName() { return givenName; }
    public void setGivenName(String givenName) { this.givenName = givenName; }
    public String getSurname() { return surname; }
    public void setSurname(String surname) { this.surname = surname; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getCivilStatus() { return civilStatus; }
    public void setCivilStatus(String civilStatus) { this.civilStatus = civilStatus; }
    public String getOccupation() { return occupation; }
    public void setOccupation(String occupation) { this.occupation = occupation; }
    public LocalDateTime getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDateTime dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    public String getBirthCertificateNo() { return birthCertificateNo; }
    public void setBirthCertificateNo(String birthCertificateNo) { this.birthCertificateNo = birthCertificateNo; }
    public String getBirthPlace() { return birthPlace; }
    public void setBirthPlace(String birthPlace) { this.birthPlace = birthPlace; }
    public String getBirthDivision() { return birthDivision; }
    public void setBirthDivision(String birthDivision) { this.birthDivision = birthDivision; }
    public String getBirthDistrict() { return birthDistrict; }
    public void setBirthDistrict(String birthDistrict) { this.birthDistrict = birthDistrict; }
    public String getCountryOfBirth() { return countryOfBirth; }
    public void setCountryOfBirth(String countryOfBirth) { this.countryOfBirth = countryOfBirth; }
    public String getBirthCity() { return birthCity; }
    public void setBirthCity(String birthCity) { this.birthCity = birthCity; }
    public String getPermanentHouseNo() { return permanentHouseNo; }
    public void setPermanentHouseNo(String permanentHouseNo) { this.permanentHouseNo = permanentHouseNo; }
    public String getPermanentStreet() { return permanentStreet; }
    public void setPermanentStreet(String permanentStreet) { this.permanentStreet = permanentStreet; }
    public String getPermanentCity() { return permanentCity; }
    public void setPermanentCity(String permanentCity) { this.permanentCity = permanentCity; }
    public String getPermanentPostalCode() { return permanentPostalCode; }
    public void setPermanentPostalCode(String permanentPostalCode) { this.permanentPostalCode = permanentPostalCode; }
    public String getPostalSameAsPermanent() { return postalSameAsPermanent; }
    public void setPostalSameAsPermanent(String postalSameAsPermanent) { this.postalSameAsPermanent = postalSameAsPermanent; }
    public String getPostalHouseNo() { return postalHouseNo; }
    public void setPostalHouseNo(String postalHouseNo) { this.postalHouseNo = postalHouseNo; }
    public String getPostalStreet() { return postalStreet; }
    public void setPostalStreet(String postalStreet) { this.postalStreet = postalStreet; }
    public String getPostalCity() { return postalCity; }
    public void setPostalCity(String postalCity) { this.postalCity = postalCity; }
    public String getPostalPostalCode() { return postalPostalCode; }
    public void setPostalPostalCode(String postalPostalCode) { this.postalPostalCode = postalPostalCode; }
    public String getDualCitizen() { return dualCitizen; }
    public void setDualCitizen(String dualCitizen) { this.dualCitizen = dualCitizen; }
    public String getCitizenshipCertificateNo() { return citizenshipCertificateNo; }
    public void setCitizenshipCertificateNo(String citizenshipCertificateNo) { this.citizenshipCertificateNo = citizenshipCertificateNo; }
    public LocalDateTime getCitizenshipIssuedDate() { return citizenshipIssuedDate; }
    public void setCitizenshipIssuedDate(LocalDateTime citizenshipIssuedDate) { this.citizenshipIssuedDate = citizenshipIssuedDate; }
}
