package sk.brutech.voltradar.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChargingLocationTest {

    @Test
    @DisplayName("ChargingLocation correctly aggregates metadata across multiple provider stations and charger units")
    void testMetadataAggregation() {
        Connector ccs1 = new Connector(
                UUID.randomUUID(),
                "SK*ZSE*E2145*1",
                NormalizedConnectorType.CCS,
                CurrentType.DC,
                new BigDecimal("150.0"),
                LiveStatus.AVAILABLE,
                PowerSharingInfo.independent(new BigDecimal("150.0")),
                new BigDecimal("0.59"),
                Instant.now()
        );
        Connector ccs2 = new Connector(
                UUID.randomUUID(),
                "SK*ZSE*E2145*2",
                NormalizedConnectorType.CCS,
                CurrentType.DC,
                new BigDecimal("150.0"),
                LiveStatus.OCCUPIED,
                PowerSharingInfo.independent(new BigDecimal("150.0")),
                new BigDecimal("0.59"),
                Instant.now()
        );
        Connector type2 = new Connector(
                UUID.randomUUID(),
                "SK*ZSE*E2146*1",
                NormalizedConnectorType.TYPE_2,
                CurrentType.AC,
                new BigDecimal("22.0"),
                LiveStatus.AVAILABLE,
                PowerSharingInfo.independent(new BigDecimal("22.0")),
                new BigDecimal("0.39"),
                Instant.now()
        );

        ProviderStation station1 = new ProviderStation(
                "2145",
                CpoProvider.ZSE_DRIVE,
                "Bratislava - OC Retro DC",
                new GeoCoordinates(48.152, 17.154),
                new Address("Nevädzová 6", "Bratislava", "82101", "SK"),
                "Ultra",
                List.of(ccs1, ccs2),
                "{}"
        );

        ProviderStation station2 = new ProviderStation(
                "2146",
                CpoProvider.ZSE_DRIVE,
                "Bratislava - OC Retro AC",
                new GeoCoordinates(48.152, 17.154),
                new Address("Nevädzová 6", "Bratislava", "82101", "SK"),
                "City",
                List.of(type2),
                "{}"
        );

        ChargerUnit confirmedStand = new ChargerUnit(
                UUID.randomUUID(),
                "Stojan 1 (Alpitronic HYC300)",
                ConfidenceLevel.CONFIRMED,
                SharingStatus.SHARED,
                new BigDecimal("150.0"),
                List.of("SK*ZSE*E2145*1", "SK*ZSE*E2145*2"),
                "github:gist",
                "Verified physical stand"
        );

        ChargingLocation location = new ChargingLocation(
                UUID.randomUUID(),
                "Bratislava - OC Retro",
                new GeoCoordinates(48.152, 17.154),
                new Address("Nevädzová 6", "Bratislava", "82101", "SK"),
                List.of(station1, station2),
                List.of(confirmedStand),
                null
        );

        assertThat(location.metadata().providerStationsCount()).isEqualTo(2);
        assertThat(location.metadata().confirmedChargerUnitsCount()).hasValue(1);
        assertThat(location.metadata().totalConnectorsCount()).isEqualTo(3);
        assertThat(location.metadata().availableConnectorsCount()).isEqualTo(2);
        assertThat(location.metadata().ccsConnectorsCount()).isEqualTo(2);
        assertThat(location.metadata().type2ConnectorsCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Invalid GPS coordinates throw IllegalArgumentException")
    void testInvalidCoordinates() {
        assertThatThrownBy(() -> new GeoCoordinates(95.0, 17.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Latitude");

        assertThatThrownBy(() -> new GeoCoordinates(48.0, 195.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Longitude");
    }
}
