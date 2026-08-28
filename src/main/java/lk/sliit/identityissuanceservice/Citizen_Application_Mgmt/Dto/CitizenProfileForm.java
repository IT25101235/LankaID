package lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public class CitizenProfileForm {

    @NotBlank(message = "Family name is required.")
    @Size(max = 100)
    private String familyName;

    @NotBlank(message = "Given name is required.")
    @Size(max = 150)
    private String givenName;

    @NotBlank(message = "Surname is required.")
    @Size(max = 100)
    private String surname;

    @NotBlank(message = "Gender is required.")
    @Pattern(regexp = "MALE|FEMALE", message = "Select a valid gender.")
    private String gender;

    @NotBlank(message = "Civil status is required.")
    @Pattern(regexp = "SINGLE|MARRIED|WIDOWED|DIVORCED", message = "Select a valid civil status.")
    private String civilStatus;

    @Size(max = 150)
    private String occupation;

    @NotNull(message = "Date of birth is required.")
    @Past(message = "Date of birth must be in the past.")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateOfBirth;

    @NotBlank(message = "Birth certificate number is required.")
    @Size(max = 50)
    private String birthCertificateNo;

    @NotBlank(message = "Birth place is required.")
    @Size(max = 100)
    private String birthPlace;

    @NotBlank(message = "Birth division is required.")
    @Size(max = 100)
    private String birthDivision;

    @NotBlank(message = "Birth district is required.")
    @Size(max = 100)
    private String birthDistrict;

    @NotBlank(message = "Country of birth is required.")
    @Size(max = 100)
    private String countryOfBirth;

    @NotBlank(message = "Birth city is required.")
    @Size(max = 100)
    private String birthCity;

    @NotBlank(message = "Permanent house number is required.")
    @Size(max = 50)
    private String permanentHouseNo;

    @NotBlank(message = "Permanent street is required.")
    @Size(max = 150)
    private String permanentStreet;

    @NotBlank(message = "Permanent city is required.")
    @Size(max = 100)
    private String permanentCity;

    @NotBlank(message = "Permanent postal code is required.")
    @Size(max = 10)
    private String permanentPostalCode;

    private boolean postalSameAsPermanent = true;

    @Size(max = 50)
    private String postalHouseNo;

    @Size(max = 150)
    private String postalStreet;

    @Size(max = 100)
    private String postalCity;

    @Size(max = 10)
    private String postalPostalCode;

    private boolean dualCitizen;

    @Size(max = 50)
    private String citizenshipCertificateNo;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @PastOrPresent(message = "Citizenship issued date cannot be in the future.")
    private LocalDate citizenshipIssuedDate;

    @AssertTrue(message = "You must accept the declaration.")
    private boolean declarationAccepted;

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
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }
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
    public boolean isPostalSameAsPermanent() { return postalSameAsPermanent; }
    public void setPostalSameAsPermanent(boolean postalSameAsPermanent) { this.postalSameAsPermanent = postalSameAsPermanent; }
    public String getPostalHouseNo() { return postalHouseNo; }
    public void setPostalHouseNo(String postalHouseNo) { this.postalHouseNo = postalHouseNo; }
    public String getPostalStreet() { return postalStreet; }
    public void setPostalStreet(String postalStreet) { this.postalStreet = postalStreet; }
    public String getPostalCity() { return postalCity; }
    public void setPostalCity(String postalCity) { this.postalCity = postalCity; }
    public String getPostalPostalCode() { return postalPostalCode; }
    public void setPostalPostalCode(String postalPostalCode) { this.postalPostalCode = postalPostalCode; }
    public boolean isDualCitizen() { return dualCitizen; }
    public void setDualCitizen(boolean dualCitizen) { this.dualCitizen = dualCitizen; }
    public String getCitizenshipCertificateNo() { return citizenshipCertificateNo; }
    public void setCitizenshipCertificateNo(String citizenshipCertificateNo) { this.citizenshipCertificateNo = citizenshipCertificateNo; }
    public LocalDate getCitizenshipIssuedDate() { return citizenshipIssuedDate; }
    public void setCitizenshipIssuedDate(LocalDate citizenshipIssuedDate) { this.citizenshipIssuedDate = citizenshipIssuedDate; }
    public boolean isDeclarationAccepted() { return declarationAccepted; }
    public void setDeclarationAccepted(boolean declarationAccepted) { this.declarationAccepted = declarationAccepted; }
}
