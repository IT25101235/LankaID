package lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Validation;

import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Dto.CitizenProfileForm;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Loads the editable, project-local birth-location JSON once and validates posted values.
 * Browser dropdowns improve usability, while this class prevents forged combinations.
 */
public final class BirthLocationReference {

    public static final String SRI_LANKA = "Sri Lanka";
    private static final String LOCATION_RESOURCE =
            "static/Citizen_Application_Mgmt/reference-data/sri-lanka-locations.json";
    private static final String COUNTRY_RESOURCE =
            "static/Citizen_Application_Mgmt/reference-data/countries.json";
    private static final Pattern SAFE_FOREIGN_PLACE = Pattern.compile(
            "[\\p{L}\\p{M}][\\p{L}\\p{M} .'-]*"
    );

    private static final Map<String, District> DISTRICTS;
    private static final Set<String> COUNTRIES;

    static {
        try {
            ObjectMapper mapper = new ObjectMapper();
            try (var input = new ClassPathResource(LOCATION_RESOURCE).getInputStream()) {
                LocationFile locations = mapper.readValue(input, LocationFile.class);
                DISTRICTS = locations.districts().stream().collect(Collectors.toUnmodifiableMap(
                        district -> key(district.name()),
                        Function.identity()
                ));
            }
            try (var input = new ClassPathResource(COUNTRY_RESOURCE).getInputStream()) {
                CountryFile countries = mapper.readValue(input, CountryFile.class);
                COUNTRIES = countries.countries().stream()
                        .map(country -> key(country.name()))
                        .collect(Collectors.toUnmodifiableSet());
            }
        } catch (IOException exception) {
            throw new ExceptionInInitializerError("Birth-location reference data could not be loaded: "
                    + exception.getMessage());
        }
    }

    private BirthLocationReference() {
    }

    /** Validates either a strict Sri Lankan hierarchy or safe foreign free-text places. */
    public static void validate(CitizenProfileForm form) {
        String country = clean(form.getCountryOfBirth());
        String districtOrRegion = clean(form.getBirthDistrict());
        String division = clean(form.getBirthDivision());
        String city = clean(form.getBirthCity());

        if (country == null || !COUNTRIES.contains(key(country))) {
            throw new IllegalArgumentException("Select a valid country of birth.");
        }

        if (SRI_LANKA.equalsIgnoreCase(country)) {
            District district = districtOrRegion == null ? null : DISTRICTS.get(key(districtOrRegion));
            if (district == null) {
                throw new IllegalArgumentException("Select a valid Sri Lankan birth district.");
            }
            if (division == null || !containsIgnoreCase(district.divisions(), division)) {
                throw new IllegalArgumentException("Select a division that belongs to the birth district.");
            }
            if (city == null || !containsIgnoreCase(district.cities(), city)) {
                throw new IllegalArgumentException("Select a city or town that belongs to the birth district.");
            }
            return;
        }

        // Foreign locations cannot be exhaustively listed locally, so accept only restrained text.
        if (!safeForeignPlace(districtOrRegion)) {
            throw new IllegalArgumentException("Enter a valid state, province or region of birth.");
        }
        if (!safeForeignPlace(city)) {
            throw new IllegalArgumentException("Enter a valid foreign birth city or town.");
        }
    }

    public static boolean isSriLanka(String country) {
        return country != null && SRI_LANKA.equalsIgnoreCase(country.trim());
    }

    private static boolean safeForeignPlace(String value) {
        return value != null && value.length() <= 100 && SAFE_FOREIGN_PLACE.matcher(value).matches();
    }

    private static boolean containsIgnoreCase(List<String> values, String candidate) {
        return values.stream().anyMatch(value -> value.equalsIgnoreCase(candidate));
    }

    private static String clean(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        return value.trim();
    }

    private static String key(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    public record LocationFile(String dataVersion, List<String> sources, List<District> districts) {
    }

    public record District(String name, List<String> divisions, List<String> cities) {
    }

    public record CountryFile(String dataVersion, String source, List<Country> countries) {
    }

    public record Country(String code, String name) {
    }
}
