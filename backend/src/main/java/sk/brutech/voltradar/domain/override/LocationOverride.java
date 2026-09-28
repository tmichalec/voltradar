package sk.brutech.voltradar.domain.override;

import java.util.List;

/**
 * Community and verified charging location override specification from GitHub Gist.
 */
public record LocationOverride(
        String locationId,
        String name,
        List<String> providerStationIds,
        List<ChargerUnitOverride> chargerUnits
) {
    public LocationOverride {
        providerStationIds = providerStationIds != null ? List.copyOf(providerStationIds) : List.of();
        chargerUnits = chargerUnits != null ? List.copyOf(chargerUnits) : List.of();
    }
}
