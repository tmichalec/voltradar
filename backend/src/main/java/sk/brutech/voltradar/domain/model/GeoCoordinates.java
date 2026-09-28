package sk.brutech.voltradar.domain.model;

/**
 * Geographical coordinates represented as WGS84 latitude and longitude.
 */
public record GeoCoordinates(double latitude, double longitude) {
    public GeoCoordinates {
        if (latitude < -90.0 || latitude > 90.0) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90, got: " + latitude);
        }
        if (longitude < -180.0 || longitude > 180.0) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180, got: " + longitude);
        }
    }
}
