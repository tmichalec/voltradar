package sk.brutech.voltradar.domain.override;

import java.util.List;

/**
 * Root JSON document structure stored in GitHub Gist for community-verified location overrides.
 */
public record LocationsGistDocument(
        String version,
        List<LocationOverride> locations
) {
    public LocationsGistDocument {
        locations = locations != null ? List.copyOf(locations) : List.of();
    }
}
