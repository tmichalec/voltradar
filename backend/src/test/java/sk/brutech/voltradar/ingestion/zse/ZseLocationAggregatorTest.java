package sk.brutech.voltradar.ingestion.zse;

import org.junit.jupiter.api.Test;
import sk.brutech.voltradar.domain.model.Address;
import sk.brutech.voltradar.domain.model.ChargingLocation;
import sk.brutech.voltradar.domain.model.ConfidenceLevel;
import sk.brutech.voltradar.domain.model.Connector;
import sk.brutech.voltradar.domain.model.CpoProvider;
import sk.brutech.voltradar.domain.model.CurrentType;
import sk.brutech.voltradar.domain.model.GeoCoordinates;
import sk.brutech.voltradar.domain.model.LiveStatus;
import sk.brutech.voltradar.domain.model.NormalizedConnectorType;
import sk.brutech.voltradar.domain.model.PowerSharingInfo;
import sk.brutech.voltradar.domain.model.ProviderStation;
import sk.brutech.voltradar.domain.model.SharingStatus;
import sk.brutech.voltradar.domain.override.ChargerUnitOverride;
import sk.brutech.voltradar.domain.override.LocationOverride;
import sk.brutech.voltradar.domain.override.LocationsGistDocument;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ZseLocationAggregatorTest {
    private final ZseLocationAggregator aggregator = new ZseLocationAggregator();

    @Test
    void clustersNearbyStationsWithoutOverrides() {
        ProviderStation station1 = createStation("101", "ZSE Hub 1", 48.15000, 17.10000, "SK*ZSE*E101*1", 150);
        ProviderStation station2 = createStation("102", "ZSE Hub 2", 48.15010, 17.10010, "SK*ZSE*E102*1", 150);
        ProviderStation farStation = createStation("201", "ZSE Far", 48.20000, 17.20000, "SK*ZSE*E201*1", 50);

        List<ChargingLocation> locations = aggregator.aggregate(List.of(station1, station2, farStation), null);

        assertThat(locations).hasSize(2);

        ChargingLocation clusterLocation = locations.stream()
                .filter(l -> l.providerStations().size() == 2)
                .findFirst()
                .orElseThrow();
        assertThat(clusterLocation.id()).isEqualTo("zse-101-102");
        assertThat(clusterLocation.metadata().totalConnectorsCount()).isEqualTo(2);
        assertThat(clusterLocation.metadata().providerStationsCount()).isEqualTo(2);

        ChargingLocation singleLocation = locations.stream()
                .filter(l -> l.providerStations().size() == 1)
                .findFirst()
                .orElseThrow();
        assertThat(singleLocation.id()).isEqualTo("zse-201");
        assertThat(singleLocation.providerStations().getFirst().providerStationId()).isEqualTo("201");
    }

    @Test
    void deduplicatesDuplicateStationEntries() {
        ProviderStation station1 = createStation("101", "ZSE Hub 1", 48.15000, 17.10000, "SK*ZSE*E101*1", 150);
        ProviderStation station1Duplicate = createStation("101", "ZSE Hub 1 Duplicate", 48.15000, 17.10000, "SK*ZSE*E101*1", 150);

        List<ChargingLocation> locations = aggregator.aggregate(List.of(station1, station1Duplicate), null);

        assertThat(locations).hasSize(1);
        ChargingLocation location = locations.getFirst();
        assertThat(location.id()).isEqualTo("zse-101");
        assertThat(location.providerStations()).hasSize(1);
    }

    @Test
    void appliesGistOverridesAndUpdatesPowerSharing() {
        ProviderStation stationA = createStationWithTwoConnectors(
                "2145", "OC Retro 1", 48.1520, 17.1550, "SK*ZSE*E2145*1", "SK*ZSE*E2145*2", 150, LiveStatus.OCCUPIED
        );
        ProviderStation stationB = createStation(
                "2146", "OC Retro 2", 48.1521, 17.1551, "SK*ZSE*E2146*1", 150
        );

        ChargerUnitOverride unit1 = new ChargerUnitOverride(
                "unit-1",
                "Stojan 1",
                ConfidenceLevel.CONFIRMED,
                SharingStatus.SHARED,
                new BigDecimal("150"),
                List.of("SK*ZSE*E2145*1", "SK*ZSE*E2145*2"),
                "Shared stand 150kW"
        );

        LocationOverride locationOverride = new LocationOverride(
                "loc-retro",
                "Bratislava - OC Retro",
                List.of("2145", "2146"),
                List.of(unit1)
        );

        LocationsGistDocument gistDoc = new LocationsGistDocument("1.0.0", List.of(locationOverride));

        List<ChargingLocation> locations = aggregator.aggregate(List.of(stationA, stationB), gistDoc);

        assertThat(locations).hasSize(1);
        ChargingLocation retro = locations.getFirst();
        assertThat(retro.id()).isEqualTo("loc-retro");
        assertThat(retro.name()).isEqualTo("Bratislava - OC Retro");
        assertThat(retro.chargerUnits()).hasSize(1);
        assertThat(retro.chargerUnits().getFirst().id()).isEqualTo("unit-1");
        assertThat(retro.chargerUnits().getFirst().confidence()).isEqualTo(ConfidenceLevel.CONFIRMED);

        // Check power sharing calculation:
        // Connector 1 is OCCUPIED, connector 2 is AVAILABLE.
        // For available connector 2, effective power should be calculated accounting for active session on stand 1.
        ProviderStation enhancedA = retro.providerStations().stream()
                .filter(s -> s.providerStationId().equals("2145"))
                .findFirst()
                .orElseThrow();

        Connector conn2 = enhancedA.connectors().get(1);
        assertThat(conn2.powerSharing().status()).isEqualTo(SharingStatus.SHARED);
        assertThat(conn2.powerSharing().confidence()).isEqualTo(ConfidenceLevel.CONFIRMED);
        assertThat(conn2.powerSharing().activeSessionsOnStand()).isEqualTo(1);
        assertThat(conn2.powerSharing().effectiveAvailablePowerKw()).isEqualByComparingTo("75.0");
    }

    private ProviderStation createStation(
            String id, String name, double lat, double lon, String evseId, int power
    ) {
        Connector conn = new Connector(
                UUID.randomUUID(),
                evseId,
                NormalizedConnectorType.CCS,
                CurrentType.DC,
                new BigDecimal(power),
                LiveStatus.AVAILABLE,
                PowerSharingInfo.unknown(new BigDecimal(power)),
                new BigDecimal("0.49"),
                Instant.now()
        );
        return new ProviderStation(
                id,
                CpoProvider.ZSE_DRIVE,
                name,
                new GeoCoordinates(lat, lon),
                new Address("Nevädzová 6", "Bratislava", "82101", "SK"),
                "Ultra",
                List.of(conn),
                "{}"
        );
    }

    private ProviderStation createStationWithTwoConnectors(
            String id, String name, double lat, double lon,
            String evseId1, String evseId2, int power, LiveStatus status1
    ) {
        Connector conn1 = new Connector(
                UUID.randomUUID(),
                evseId1,
                NormalizedConnectorType.CCS,
                CurrentType.DC,
                new BigDecimal(power),
                status1,
                PowerSharingInfo.unknown(new BigDecimal(power)),
                new BigDecimal("0.49"),
                Instant.now()
        );
        Connector conn2 = new Connector(
                UUID.randomUUID(),
                evseId2,
                NormalizedConnectorType.CCS,
                CurrentType.DC,
                new BigDecimal(power),
                LiveStatus.AVAILABLE,
                PowerSharingInfo.unknown(new BigDecimal(power)),
                new BigDecimal("0.49"),
                Instant.now()
        );
        return new ProviderStation(
                id,
                CpoProvider.ZSE_DRIVE,
                name,
                new GeoCoordinates(lat, lon),
                new Address("Nevädzová 6", "Bratislava", "82101", "SK"),
                "Ultra",
                List.of(conn1, conn2),
                "{}"
        );
    }
}
