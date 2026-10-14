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

// Holds the citizen details shared by NIC and passport application forms.
public class CitizenProfileForm {

    // Personal, birth, address, citizenship, and declaration form values.
    @NotBlank(message = "Family name is required.")
    @Size(max = 100, message = "Family name cannot exceed 100 characters.")
    @Pattern(regexp = "[\\p{L}][\\p{L}\\p{M} ]*", message = "Family name must contain only letters and spaces.")
    private String familyName;

    @NotBlank(message = "Given name is required.")
    @Size(max = 150, message = "Given name cannot exceed 150 characters.")
    @Pattern(regexp = "[\\p{L}][\\p{L}\\p{M} ]*", message = "Given name must contain only letters and spaces.")
    private String givenName;

    @NotBlank(message = "Surname is required.")
    @Size(max = 100, message = "Surname cannot exceed 100 characters.")
    @Pattern(regexp = "[\\p{L}][\\p{L}\\p{M} ]*", message = "Surname must contain only letters and spaces.")
    private String surname;

    @NotBlank(message = "Gender is required.")
    @Pattern(regexp = "MALE|FEMALE", message = "Select a valid gender.")
    private String gender;

    @NotBlank(message = "Civil status is required.")
    @Pattern(regexp = "SINGLE|MARRIED|WIDOWED|DIVORCED", message = "Select a valid civil status.")
    private String civilStatus;

    @Size(max = 150, message = "Occupation cannot exceed 150 characters.")
    private String occupation;

    @NotNull(message = "Date of birth is required.")
    @Past(message = "Date of birth must be in the past.")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateOfBirth;

    @NotBlank(message = "Birth certificate number is required.")
    @Size(max = 50, message = "Birth certificate no cannot exceed 50 characters.")
    private String birthCertificateNo;

    @NotBlank(message = "Birth place is required.")
    @Size(max = 100, message = "Birth place cannot exceed 100 characters.")
    private String birthPlace;

    @Size(max = 100, message = "Birth division cannot exceed 100 characters.")
    private String birthDivision;

    @Size(max = 100, message = "Birth district cannot exceed 100 characters.")
    private String birthDistrict;

    @NotBlank(message = "Country of birth is required.")
    @Size(max = 100, message = "Country of birth cannot exceed 100 characters.")
    private String countryOfBirth;

    @NotBlank(message = "Birth city is required.")
    @Size(max = 100, message = "Birth city cannot exceed 100 characters.")
    private String birthCity;

    @NotBlank(message = "Permanent house number is required.")
    @Size(max = 50, message = "Permanent house no cannot exceed 50 characters.")
    private String permanentHouseNo;

    @NotBlank(message = "Permanent street is required.")
    @Size(max = 150, message = "Permanent street cannot exceed 150 characters.")
    private String permanentStreet;

    @NotBlank(message = "Permanent city is required.")
    @Size(max = 100, message = "Permanent city cannot exceed 100 characters.")
    private String permanentCity;

    @NotBlank(message = "Permanent postal code is required.")
    @Pattern(regexp = "[0-9]{5}", message = "Permanent postal code must contain exactly 5 digits.")
    private String permanentPostalCode;

    private boolean postalSameAsPermanent = true;

    @Size(max = 50, message = "Postal house no cannot exceed 50 characters.")
    private String postalHouseNo;

    @Size(max = 150, message = "Postal street cannot exceed 150 characters.")
    private String postalStreet;

    @Size(max = 100, message = "Postal city cannot exceed 100 characters.")
    private String postalCity;

    @Size(max = 10, message = "Postal postal code cannot exceed 10 characters.")
    @Pattern(regexp = "^$|[0-9]{5}", message = "Postal address postal code must contain exactly 5 digits.")
    private String postalPostalCode;

    private boolean dualCitizen;

    @Size(max = 50, message = "Citizenship certificate no cannot exceed 50 characters.")
    private String citizenshipCertificateNo;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @PastOrPresent(message = "Citizenship issued date cannot be in the future.")
    private LocalDate citizenshipIssuedDate;

    @AssertTrue(message = "You must accept the declaration.")
    private boolean declarationAccepted;

    // Returns the family name.
    public String getFamilyName() { return familyName; }
    // Sets the family name.
    public void setFamilyName(String familyName) { this.familyName = familyName; }
    // Returns the given name.
    public String getGivenName() { return givenName; }
    // Sets the given name.
    public void setGivenName(String givenName) { this.givenName = givenName; }
    // Returns the surname.
    public String getSurname() { return surname; }
    // Sets the surname.
    public void setSurname(String surname) { this.surname = surname; }
    // Returns the selected gender.
    public String getGender() { return gender; }
    // Sets the selected gender.
    public void setGender(String gender) { this.gender = gender; }
    // Returns the civil status.
    public String getCivilStatus() { return civilStatus; }
    // Sets the civil status.
    public void setCivilStatus(String civilStatus) { this.civilStatus = civilStatus; }
    // Returns the occupation.
    public String getOccupation() { return occupation; }
    // Sets the occupation.
    public void setOccupation(String occupation) { this.occupation = occupation; }
    // Returns the date of birth.
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    // Sets the date of birth.
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    // Returns the birth certificate number.
    public String getBirthCertificateNo() { return birthCertificateNo; }
    // Sets the birth certificate number.
    public void setBirthCertificateNo(String birthCertificateNo) { this.birthCertificateNo = birthCertificateNo; }
    // Returns the birth place.
    public String getBirthPlace() { return birthPlace; }
    // Sets the birth place.
    public void setBirthPlace(String birthPlace) { this.birthPlace = birthPlace; }
    // Returns the birth division.
    public String getBirthDivision() { return birthDivision; }
    // Sets the birth division.
    public void setBirthDivision(String birthDivision) { this.birthDivision = birthDivision; }
    // Returns the birth district.
    public String getBirthDistrict() { return birthDistrict; }
    // Sets the birth district.
    public void setBirthDistrict(String birthDistrict) { this.birthDistrict = birthDistrict; }
    // Returns the country of birth.
    public String getCountryOfBirth() { return countryOfBirth; }
    // Sets the country of birth.
    public void setCountryOfBirth(String countryOfBirth) { this.countryOfBirth = countryOfBirth; }
    // Returns the birth city.
    public String getBirthCity() { return birthCity; }
    // Sets the birth city.
    public void setBirthCity(String birthCity) { this.birthCity = birthCity; }
    // Returns the permanent house number.
    public String getPermanentHouseNo() { return permanentHouseNo; }
    // Sets the permanent house number.
    public void setPermanentHouseNo(String permanentHouseNo) { this.permanentHouseNo = permanentHouseNo; }
    // Returns the permanent street.
    public String getPermanentStreet() { return permanentStreet; }
    // Sets the permanent street.
    public void setPermanentStreet(String permanentStreet) { this.permanentStreet = permanentStreet; }
    // Returns the permanent city.
    public String getPermanentCity() { return permanentCity; }
    // Sets the permanent city.
    public void setPermanentCity(String permanentCity) { this.permanentCity = permanentCity; }
    // Returns the permanent postal code.
    public String getPermanentPostalCode() { return permanentPostalCode; }
    // Sets the permanent postal code.
    public void setPermanentPostalCode(String permanentPostalCode) { this.permanentPostalCode = permanentPostalCode; }
    // Returns whether the postal address matches the permanent address.
    public boolean isPostalSameAsPermanent() { return postalSameAsPermanent; }
    // Sets whether the postal address matches the permanent address.
    public void setPostalSameAsPermanent(boolean postalSameAsPermanent) { this.postalSameAsPermanent = postalSameAsPermanent; }
    // Returns the postal house number.
    public String getPostalHouseNo() { return postalHouseNo; }
    // Sets the postal house number.
    public void setPostalHouseNo(String postalHouseNo) { this.postalHouseNo = postalHouseNo; }
    // Returns the postal street.
    public String getPostalStreet() { return postalStreet; }
    // Sets the postal street.
    public void setPostalStreet(String postalStreet) { this.postalStreet = postalStreet; }
    // Returns the postal city.
    public String getPostalCity() { return postalCity; }
    // Sets the postal city.
    public void setPostalCity(String postalCity) { this.postalCity = postalCity; }
    // Returns the postal address code.
    public String getPostalPostalCode() { return postalPostalCode; }
    // Sets the postal address code.
    public void setPostalPostalCode(String postalPostalCode) { this.postalPostalCode = postalPostalCode; }
    // Returns whether the citizen has dual citizenship.
    public boolean isDualCitizen() { return dualCitizen; }
    // Sets whether the citizen has dual citizenship.
    public void setDualCitizen(boolean dualCitizen) { this.dualCitizen = dualCitizen; }
    // Returns the citizenship certificate number.
    public String getCitizenshipCertificateNo() { return citizenshipCertificateNo; }
    // Sets the citizenship certificate number.
    public void setCitizenshipCertificateNo(String citizenshipCertificateNo) { this.citizenshipCertificateNo = citizenshipCertificateNo; }
    // Returns the citizenship certificate issue date.
    public LocalDate getCitizenshipIssuedDate() { return citizenshipIssuedDate; }
    // Sets the citizenship certificate issue date.
    public void setCitizenshipIssuedDate(LocalDate citizenshipIssuedDate) { this.citizenshipIssuedDate = citizenshipIssuedDate; }
    // Returns whether the declaration was accepted.
    public boolean isDeclarationAccepted() { return declarationAccepted; }
    // Sets whether the declaration was accepted.
    public void setDeclarationAccepted(boolean declarationAccepted) { this.declarationAccepted = declarationAccepted; }
}
