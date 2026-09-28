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

    public double distanceMetersTo(GeoCoordinates other) {
        if (other == null) {
            throw new IllegalArgumentException("Other coordinates must not be null");
        }
        double earthRadius = 6371000.0; // meters
        double dLat = Math.toRadians(other.latitude - this.latitude);
        double dLon = Math.toRadians(other.longitude - this.longitude);
        double a = Math.sin(dLat / 2.0) * Math.sin(dLat / 2.0)
                + Math.cos(Math.toRadians(this.latitude)) * Math.cos(Math.toRadians(other.latitude))
                * Math.sin(dLon / 2.0) * Math.sin(dLon / 2.0);
        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));
        return earthRadius * c;
    }
}
