package lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Dto;

import lk.sliit.identityissuanceservice.User_Mgmt.Model.CitizenAccount;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Validation.BirthLocationReference;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;

/** Shared mapping for the citizen fields used by both application workflows. */
public final class CitizenProfileMapper {

    // Prevents construction because this class only contains shared helper methods.
    private CitizenProfileMapper() {
    }

    // Copies stored citizen details into an application form.
    public static void copyToForm(CitizenAccount citizen, CitizenProfileForm form) {
        form.setFamilyName(citizen.getFamilyName());
        form.setGivenName(citizen.getGivenName());
        form.setSurname(citizen.getSurname());
        form.setGender(citizen.getGender());
        form.setCivilStatus(citizen.getCivilStatus());
        form.setOccupation(citizen.getOccupation());
        form.setDateOfBirth(toLocalDate(citizen.getDateOfBirth()));
        form.setBirthCertificateNo(citizen.getBirthCertificateNo());
        form.setBirthPlace(citizen.getBirthPlace());
        form.setBirthDivision(citizen.getBirthDivision());
        form.setBirthDistrict(citizen.getBirthDistrict());
        form.setCountryOfBirth(blankToNull(citizen.getCountryOfBirth()) == null
                ? BirthLocationReference.SRI_LANKA : citizen.getCountryOfBirth());
        form.setBirthCity(citizen.getBirthCity());
        form.setPermanentHouseNo(citizen.getPermanentHouseNo());
        form.setPermanentStreet(citizen.getPermanentStreet());
        form.setPermanentCity(citizen.getPermanentCity());
        form.setPermanentPostalCode(citizen.getPermanentPostalCode());
        form.setPostalSameAsPermanent(!"N".equals(citizen.getPostalSameAsPermanent()));
        form.setPostalHouseNo(citizen.getPostalHouseNo());
        form.setPostalStreet(citizen.getPostalStreet());
        form.setPostalCity(citizen.getPostalCity());
        form.setPostalPostalCode(citizen.getPostalPostalCode());
        form.setDualCitizen("Y".equals(citizen.getDualCitizen()));
        form.setCitizenshipCertificateNo(citizen.getCitizenshipCertificateNo());
        form.setCitizenshipIssuedDate(toLocalDate(citizen.getCitizenshipIssuedDate()));
    }

    // Copies cleaned form values back into the citizen profile.
    public static void updateCitizen(CitizenAccount citizen, CitizenProfileForm form) {
        citizen.setFamilyName(clean(form.getFamilyName()));
        citizen.setGivenName(clean(form.getGivenName()));
        citizen.setSurname(clean(form.getSurname()));
        citizen.setGender(form.getGender().toUpperCase(Locale.ROOT));
        citizen.setCivilStatus(form.getCivilStatus().toUpperCase(Locale.ROOT));
        citizen.setOccupation(blankToNull(form.getOccupation()));
        citizen.setDateOfBirth(atStartOfDay(form.getDateOfBirth()));
        citizen.setBirthCertificateNo(clean(form.getBirthCertificateNo()));
        citizen.setBirthPlace(clean(form.getBirthPlace()));
        citizen.setBirthDivision(BirthLocationReference.isSriLanka(form.getCountryOfBirth())
                ? clean(form.getBirthDivision()) : null);
        citizen.setBirthDistrict(clean(form.getBirthDistrict()));
        citizen.setCountryOfBirth(clean(form.getCountryOfBirth()));
        citizen.setBirthCity(clean(form.getBirthCity()));
        citizen.setPermanentHouseNo(clean(form.getPermanentHouseNo()));
        citizen.setPermanentStreet(clean(form.getPermanentStreet()));
        citizen.setPermanentCity(clean(form.getPermanentCity()));
        citizen.setPermanentPostalCode(clean(form.getPermanentPostalCode()));
        citizen.setPostalSameAsPermanent(form.isPostalSameAsPermanent() ? "Y" : "N");

        if (form.isPostalSameAsPermanent()) {
            citizen.setPostalHouseNo(citizen.getPermanentHouseNo());
            citizen.setPostalStreet(citizen.getPermanentStreet());
            citizen.setPostalCity(citizen.getPermanentCity());
            citizen.setPostalPostalCode(citizen.getPermanentPostalCode());
        } else {
            citizen.setPostalHouseNo(clean(form.getPostalHouseNo()));
            citizen.setPostalStreet(clean(form.getPostalStreet()));
            citizen.setPostalCity(clean(form.getPostalCity()));
            citizen.setPostalPostalCode(clean(form.getPostalPostalCode()));
        }

        citizen.setDualCitizen(form.isDualCitizen() ? "Y" : "N");
        citizen.setCitizenshipCertificateNo(
                form.isDualCitizen() ? blankToNull(form.getCitizenshipCertificateNo()) : null
        );
        citizen.setCitizenshipIssuedDate(
                form.isDualCitizen() ? atStartOfDay(form.getCitizenshipIssuedDate()) : null
        );
    }

    // Validates address and dual-citizenship fields that depend on other choices.
    public static void validateConditionalFields(CitizenProfileForm form) {
        // Validate the posted hierarchy on the server; browser dropdowns are not a trust boundary.
        BirthLocationReference.validate(form);
        if (!form.isPostalSameAsPermanent()
                && (blankToNull(form.getPostalHouseNo()) == null
                || blankToNull(form.getPostalStreet()) == null
                || blankToNull(form.getPostalCity()) == null
                || blankToNull(form.getPostalPostalCode()) == null)) {
            throw new IllegalArgumentException(
                    "Complete the postal address or select same as permanent address."
            );
        }
        if (!form.isPostalSameAsPermanent()
                && !form.getPostalPostalCode().matches("[0-9]{5}")) {
            throw new IllegalArgumentException("Postal address postal code must contain exactly 5 digits.");
        }
        if (form.isDualCitizen()
                && (blankToNull(form.getCitizenshipCertificateNo()) == null
                || form.getCitizenshipIssuedDate() == null)) {
            throw new IllegalArgumentException(
                    "Citizenship certificate details are required for dual citizens."
            );
        }
    }

    // Returns null for empty text and a trimmed value otherwise.
    public static String blankToNull(String value) {
        String cleaned = clean(value);
        return cleaned == null || cleaned.isEmpty() ? null : cleaned;
    }

    // Converts a date to midnight for database storage.
    public static LocalDateTime atStartOfDay(LocalDate date) {
        return date == null ? null : date.atStartOfDay();
    }

    // Removes outer spaces while preserving null values.
    private static String clean(String value) {
        return value == null ? null : value.trim();
    }

    // Converts a stored date-time value back to a form date.
    private static LocalDate toLocalDate(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.toLocalDate();
    }
}
