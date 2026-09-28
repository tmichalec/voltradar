package sk.brutech.voltradar.domain.model;

import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.UUID;

/**
 * Aggregated domain entity representing a unified physical charging location.
 * May combine multiple provider stations (e.g. adjacent stations on the same parking lot).
 */
public record ChargingLocation(
        UUID id,
        String name,
        GeoCoordinates coordinates,
        Address address,
        List<ProviderStation> providerStations,
        List<ChargerUnit> chargerUnits,
        LocationMetadata metadata
) {
    public ChargingLocation {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(name, "name must not be null");
        providerStations = providerStations != null ? List.copyOf(providerStations) : List.of();
        chargerUnits = chargerUnits != null ? List.copyOf(chargerUnits) : List.of();
        if (metadata == null) {
            metadata = LocationMetadata.from(providerStations, chargerUnits);
        }
    }

    public record LocationMetadata(
            int providerStationsCount,
            OptionalInt confirmedChargerUnitsCount,
            int totalConnectorsCount,
            int availableConnectorsCount,
            int ccsConnectorsCount,
            int type2ConnectorsCount
    ) {
        public static LocationMetadata from(
                List<ProviderStation> providerStations,
                List<ChargerUnit> chargerUnits
        ) {
            int stationsCount = providerStations.size();
            long confirmedUnits = chargerUnits.stream()
                    .filter(unit -> unit.confidence() == ConfidenceLevel.CONFIRMED)
                    .count();
            OptionalInt confirmedChargerUnits = confirmedUnits > 0
                    ? OptionalInt.of((int) confirmedUnits)
                    : OptionalInt.empty();

            List<Connector> allConnectors = providerStations.stream()
                    .flatMap(s -> s.connectors().stream())
                    .toList();

            int total = allConnectors.size();
            int available = (int) allConnectors.stream()
                    .filter(c -> c.liveStatus() == LiveStatus.AVAILABLE)
                    .count();
            int ccs = (int) allConnectors.stream()
                    .filter(c -> c.type() == NormalizedConnectorType.CCS)
                    .count();
            int type2 = (int) allConnectors.stream()
                    .filter(c -> c.type() == NormalizedConnectorType.TYPE_2)
                    .count();

            return new LocationMetadata(
                    stationsCount,
                    confirmedChargerUnits,
                    total,
                    available,
                    ccs,
                    type2
            );
        }
    }
}
