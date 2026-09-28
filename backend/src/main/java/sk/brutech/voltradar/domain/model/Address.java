package sk.brutech.voltradar.domain.model;

/**
 * Standardized postal and physical address representation.
 */
public record Address(
        String street,
        String city,
        String postalCode,
        String countryCode
) {
}
