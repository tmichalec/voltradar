package sk.brutech.voltradar.ingestion.zse;

import java.util.List;

/** A map viewport or text search, not an offset-based catalog page. */
public record ZseStationQuery(Bounds bounds, String query, int limit,
                              List<String> categories, List<String> contractors) {
    public ZseStationQuery {
        query = query == null || query.isBlank() ? null : query.strip();
        if (bounds == null && query == null) {
            throw new IllegalArgumentException("A viewport or search query is required");
        }
        if (limit < 1) {
            throw new IllegalArgumentException("Limit must be positive");
        }
        categories = categories == null ? List.of() : List.copyOf(categories);
        contractors = contractors == null ? List.of() : List.copyOf(contractors);
    }

    public static ZseStationQuery search(String query) {
        return new ZseStationQuery(null, query, 20, List.of(), List.of());
    }

    public static ZseStationQuery viewport(Bounds bounds) {
        return new ZseStationQuery(bounds, null, 40, List.of(), List.of("ZSE"));
    }

    public record Bounds(double north, double west, double south, double east) {
        public Bounds {
            if (!Double.isFinite(north) || !Double.isFinite(south)
                    || !Double.isFinite(west) || !Double.isFinite(east)
                    || north > 90 || south < -90 || west < -180 || east > 180
                    || north <= south || west >= east) {
                throw new IllegalArgumentException("Invalid non-antimeridian viewport bounds");
            }
        }
    }
}
