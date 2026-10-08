package sk.brutech.voltradar.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;

/**
 * Aggregated domain entity representing a unified physical charging location.
 * May combine multiple provider stations (e.g. adjacent stations on the same parking lot).
 */
public record ChargingLocation(
        String id,
        String name,
        GeoCoordinates coordinates,
        Address address,
        List<ProviderStation> providerStations,
        List<ChargerUnit> chargerUnits,
        LocationMetadata metadata,
        Instant updatedAt
) {
    public ChargingLocation {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(name, "name must not be null");
        providerStations = providerStations != null
                ? providerStations.stream()
                        .sorted(java.util.Comparator.comparing(ProviderStation::name, String.CASE_INSENSITIVE_ORDER)
                                .thenComparing(ProviderStation::providerStationId))
                        .toList()
                : List.of();
        chargerUnits = chargerUnits != null ? List.copyOf(chargerUnits) : List.of();
        if (metadata == null) {
            metadata = LocationMetadata.from(providerStations, chargerUnits);
        }
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
    }

    public ChargingLocation(
            String id,
            String name,
            GeoCoordinates coordinates,
            Address address,
            List<ProviderStation> providerStations,
            List<ChargerUnit> chargerUnits,
            LocationMetadata metadata
    ) {
        this(id, name, coordinates, address, providerStations, chargerUnits, metadata, Instant.now());
    }

    public record LocationMetadata(
            int providerStationsCount,
            OptionalInt confirmedChargerUnitsCount,
            int totalConnectorsCount,
            int availableConnectorsCount,
            int ccsConnectorsCount,
            int availableCcsConnectorsCount,
            int type2ConnectorsCount,
            int availableType2ConnectorsCount,
            int maxPowerKw,
            int maxCcsPowerKw,
            int maxType2PowerKw
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
            int availableCcs = (int) allConnectors.stream()
                    .filter(c -> c.type() == NormalizedConnectorType.CCS && c.liveStatus() == LiveStatus.AVAILABLE)
                    .count();

            int type2 = (int) allConnectors.stream()
                    .filter(c -> c.type() == NormalizedConnectorType.TYPE_2)
                    .count();
            int availableType2 = (int) allConnectors.stream()
                    .filter(c -> c.type() == NormalizedConnectorType.TYPE_2 && c.liveStatus() == LiveStatus.AVAILABLE)
                    .count();

            int maxPower = allConnectors.stream()
                    .map(c -> c.maxPowerKw() != null ? c.maxPowerKw().intValue() : 0)
                    .max(Integer::compareTo)
                    .orElse(0);

            int maxCcsPower = allConnectors.stream()
                    .filter(c -> c.type() == NormalizedConnectorType.CCS)
                    .map(c -> c.maxPowerKw() != null ? c.maxPowerKw().intValue() : 0)
                    .max(Integer::compareTo)
                    .orElse(0);

            int maxType2Power = allConnectors.stream()
                    .filter(c -> c.type() == NormalizedConnectorType.TYPE_2)
                    .map(c -> c.maxPowerKw() != null ? c.maxPowerKw().intValue() : 0)
                    .max(Integer::compareTo)
                    .orElse(0);

            return new LocationMetadata(
                    stationsCount,
                    confirmedChargerUnits,
                    total,
                    available,
                    ccs,
                    availableCcs,
                    type2,
                    availableType2,
                    maxPower,
                    maxCcsPower,
                    maxType2Power
            );
        }
    }
}
