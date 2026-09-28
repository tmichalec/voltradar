package sk.brutech.voltradar.ingestion.zse;

import sk.brutech.voltradar.domain.model.Address;
import sk.brutech.voltradar.domain.model.ChargerUnit;
import sk.brutech.voltradar.domain.model.ChargingLocation;
import sk.brutech.voltradar.domain.model.ConfidenceLevel;
import sk.brutech.voltradar.domain.model.Connector;
import sk.brutech.voltradar.domain.model.GeoCoordinates;
import sk.brutech.voltradar.domain.model.LiveStatus;
import sk.brutech.voltradar.domain.model.PowerSharingInfo;
import sk.brutech.voltradar.domain.model.ProviderStation;
import sk.brutech.voltradar.domain.model.SharingStatus;
import sk.brutech.voltradar.domain.override.ChargerUnitOverride;
import sk.brutech.voltradar.domain.override.LocationOverride;
import sk.brutech.voltradar.domain.override.LocationsGistDocument;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Aggregates provider station records into unified physical charging locations.
 * Prioritizes verified community overrides before falling back to spatial heuristic clustering.
 */
public final class ZseLocationAggregator {
    private static final double MAX_CLUSTER_DISTANCE_METERS = 50.0;

    public List<ChargingLocation> aggregate(
            List<ProviderStation> stations,
            LocationsGistDocument gistDocument
    ) {
        if (stations == null || stations.isEmpty()) {
            return List.of();
        }

        List<LocationOverride> overrides = gistDocument != null && gistDocument.locations() != null
                ? gistDocument.locations()
                : List.of();

        List<ChargingLocation> result = new ArrayList<>();
        Set<String> consumedStationIds = new HashSet<>();
        Map<String, ProviderStation> stationById = stations.stream()
                .collect(Collectors.toMap(ProviderStation::providerStationId, s -> s, (s1, s2) -> s1));

        // 1. Process explicit Gist overrides
        for (LocationOverride override : overrides) {
            List<ProviderStation> matchedStations = override.providerStationIds().stream()
                    .map(stationById::get)
                    .filter(s -> s != null && !consumedStationIds.contains(s.providerStationId()))
                    .toList();

            if (!matchedStations.isEmpty()) {
                matchedStations.forEach(s -> consumedStationIds.add(s.providerStationId()));
                result.add(buildOverriddenLocation(override, matchedStations));
            }
        }

        // 2. Process remaining stations using spatial clustering
        List<ProviderStation> remaining = stations.stream()
                .filter(s -> !consumedStationIds.contains(s.providerStationId()))
                .toList();

        List<List<ProviderStation>> clusters = clusterStations(remaining);
        for (List<ProviderStation> cluster : clusters) {
            result.add(buildInferredLocation(cluster));
        }

        return Collections.unmodifiableList(result);
    }

    private ChargingLocation buildOverriddenLocation(
            LocationOverride override,
            List<ProviderStation> stations
    ) {
        ProviderStation primary = stations.getFirst();
        String locationId = override.locationId();

        GeoCoordinates coordinates = computeCentroid(stations);
        Address address = primary.address();
        String name = override.name() != null && !override.name().isBlank() ? override.name() : primary.name();

        List<ChargerUnit> chargerUnits = new ArrayList<>();
        Map<String, ChargerUnitOverride> unitByEvseId = new HashMap<>();

        if (override.chargerUnits() != null) {
            for (ChargerUnitOverride unitOverride : override.chargerUnits()) {
                String unitId = unitOverride.unitId();
                ConfidenceLevel confidence = unitOverride.confidence() != null
                        ? unitOverride.confidence()
                        : ConfidenceLevel.CONFIRMED;
                SharingStatus sharing = unitOverride.sharingStatus() != null
                        ? unitOverride.sharingStatus()
                        : SharingStatus.UNKNOWN;

                ChargerUnit unit = new ChargerUnit(
                        unitId,
                        unitOverride.label(),
                        confidence,
                        sharing,
                        unitOverride.totalPowerKw(),
                        unitOverride.evseIds(),
                        "gist:" + override.locationId(),
                        unitOverride.notes()
                );
                chargerUnits.add(unit);

                for (String evseId : unitOverride.evseIds()) {
                    unitByEvseId.put(evseId, unitOverride);
                }
            }
        }

        // Enhance connectors with power sharing info from overrides
        List<ProviderStation> enhancedStations = new ArrayList<>();
        for (ProviderStation st : stations) {
            List<Connector> enhancedConnectors = new ArrayList<>();
            for (Connector conn : st.connectors()) {
                ChargerUnitOverride unit = unitByEvseId.get(conn.evseId());
                if (unit != null) {
                    PowerSharingInfo updatedSharing = computePowerSharing(conn, unit, stations);
                    enhancedConnectors.add(new Connector(
                            conn.id(),
                            conn.evseId(),
                            conn.type(),
                            conn.currentType(),
                            conn.maxPowerKw(),
                            conn.liveStatus(),
                            updatedSharing,
                            conn.publicPricePerKwh(),
                            conn.lastStatusUpdate()
                    ));
                } else {
                    enhancedConnectors.add(conn);
                }
            }
            enhancedStations.add(new ProviderStation(
                    st.providerStationId(),
                    st.provider(),
                    st.name(),
                    st.coordinates(),
                    st.address(),
                    st.rawProviderType(),
                    enhancedConnectors,
                    st.rawJsonPayload()
            ));
        }

        return new ChargingLocation(
                locationId,
                name,
                coordinates,
                address,
                enhancedStations,
                chargerUnits,
                null
        );
    }

    private ChargingLocation buildInferredLocation(List<ProviderStation> cluster) {
        ProviderStation primary = cluster.getFirst();
        String clusterKey = cluster.stream()
                .map(ProviderStation::providerStationId)
                .sorted()
                .collect(Collectors.joining("-"));

        String locationId = "zse-" + clusterKey;

        GeoCoordinates coordinates = computeCentroid(cluster);
        Address address = primary.address();
        String name = primary.name();

        // Heuristically infer physical units (e.g. shared EVSE IDs)
        Map<String, List<Connector>> byEvseId = cluster.stream()
                .flatMap(s -> s.connectors().stream())
                .collect(Collectors.groupingBy(Connector::evseId));

        List<ChargerUnit> chargerUnits = new ArrayList<>();
        for (Map.Entry<String, List<Connector>> entry : byEvseId.entrySet()) {
            String evseId = entry.getKey();
            List<Connector> evseConnectors = entry.getValue();
            if (evseConnectors.size() > 1) {
                // Multiple plugs on same EVSE ID -> Shared stand
                BigDecimal maxPower = evseConnectors.stream()
                        .map(Connector::maxPowerKw)
                        .max(BigDecimal::compareTo)
                        .orElse(new BigDecimal("150"));

                String unitId = "stand-" + evseId.toLowerCase().replace('*', '-');
                chargerUnits.add(new ChargerUnit(
                        unitId,
                        "Stand " + evseId,
                        ConfidenceLevel.INFERRED,
                        SharingStatus.SHARED,
                        maxPower,
                        List.of(evseId),
                        "system:heuristic-evseid",
                        "Shared EVSE detected"
                ));
            }
        }

        return new ChargingLocation(
                locationId,
                name,
                coordinates,
                address,
                cluster,
                chargerUnits,
                null
        );
    }

    private List<List<ProviderStation>> clusterStations(List<ProviderStation> stations) {
        List<List<ProviderStation>> clusters = new ArrayList<>();
        boolean[] visited = new boolean[stations.size()];

        for (int i = 0; i < stations.size(); i++) {
            if (visited[i]) {
                continue;
            }
            List<ProviderStation> currentCluster = new ArrayList<>();
            currentCluster.add(stations.get(i));
            visited[i] = true;

            for (int j = i + 1; j < stations.size(); j++) {
                if (!visited[j] && areNearby(stations.get(i), stations.get(j))) {
                    currentCluster.add(stations.get(j));
                    visited[j] = true;
                }
            }
            clusters.add(currentCluster);
        }
        return clusters;
    }

    private boolean areNearby(ProviderStation s1, ProviderStation s2) {
        double distance = s1.coordinates().distanceMetersTo(s2.coordinates());
        if (distance > MAX_CLUSTER_DISTANCE_METERS) {
            return false;
        }
        // Additional sanity: check city or street name if available
        if (s1.address() != null && s2.address() != null
                && s1.address().city() != null && s2.address().city() != null
                && !s1.address().city().isBlank() && !s2.address().city().isBlank()) {
            return s1.address().city().equalsIgnoreCase(s2.address().city());
        }
        return true;
    }

    private GeoCoordinates computeCentroid(List<ProviderStation> stations) {
        if (stations.isEmpty()) {
            return new GeoCoordinates(48.1486, 17.1077);
        }
        double sumLat = 0;
        double sumLon = 0;
        for (ProviderStation s : stations) {
            sumLat += s.coordinates().latitude();
            sumLon += s.coordinates().longitude();
        }
        return new GeoCoordinates(sumLat / stations.size(), sumLon / stations.size());
    }

    private PowerSharingInfo computePowerSharing(
            Connector connector,
            ChargerUnitOverride override,
            List<ProviderStation> allStations
    ) {
        SharingStatus status = override.sharingStatus() != null ? override.sharingStatus() : SharingStatus.UNKNOWN;
        ConfidenceLevel confidence = override.confidence() != null ? override.confidence() : ConfidenceLevel.CONFIRMED;
        BigDecimal standTotal = override.totalPowerKw() != null ? override.totalPowerKw() : connector.maxPowerKw();

        List<Connector> standConnectors = allStations.stream()
                .flatMap(s -> s.connectors().stream())
                .filter(c -> override.evseIds().contains(c.evseId()))
                .toList();

        int activeSessions = (int) standConnectors.stream()
                .filter(c -> !c.id().equals(connector.id()) && c.liveStatus() == LiveStatus.OCCUPIED)
                .count();

        BigDecimal effectivePower = connector.maxPowerKw();
        if (status == SharingStatus.SHARED && activeSessions > 0) {
            effectivePower = standTotal.divide(BigDecimal.valueOf(activeSessions + 1), 1, RoundingMode.HALF_UP)
                    .min(connector.maxPowerKw());
        }

        return new PowerSharingInfo(
                status,
                confidence,
                connector.maxPowerKw(),
                standTotal,
                effectivePower,
                activeSessions
        );
    }
}
