package sk.brutech.voltradar.persistence.mapper;

import org.junit.jupiter.api.Test;
import sk.brutech.voltradar.domain.model.Address;
import sk.brutech.voltradar.domain.model.ChargerUnit;
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
import sk.brutech.voltradar.persistence.entity.ChargingLocationEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ChargingLocationEntityMapperTest {

    @Test
    void mapsDomainToEntityAndBackCorrectly() {
        UUID connectorId = UUID.randomUUID();
        Connector connector = new Connector(
                connectorId,
                "SK-ZSE-E001",
                NormalizedConnectorType.CCS,
                CurrentType.DC,
                new BigDecimal("150.0"),
                LiveStatus.AVAILABLE,
                PowerSharingInfo.independent(new BigDecimal("150.0")),
                new BigDecimal("0.49"),
                Instant.now()
        );

        ProviderStation station = new ProviderStation(
                "2145",
                CpoProvider.ZSE_DRIVE,
                "ZSE Einsteinova",
                new GeoCoordinates(48.13, 17.11),
                new Address("Einsteinova 1", "Bratislava", "85101", "SK"),
                "Ultra",
                List.of(connector),
                "{\"raw\":\"ok\"}"
        );

        ChargerUnit unit = new ChargerUnit(
                "unit-1",
                "Stand 1",
                ConfidenceLevel.CONFIRMED,
                SharingStatus.INDEPENDENT,
                new BigDecimal("150.0"),
                List.of("SK-ZSE-E001"),
                "admin",
                "note"
        );

        ChargingLocation location = new ChargingLocation(
                "zse_ba_einsteinova",
                "Bratislava - Einsteinova",
                new GeoCoordinates(48.13, 17.11),
                new Address("Einsteinova 1", "Bratislava", "85101", "SK"),
                List.of(station),
                List.of(unit),
                null
        );

        ChargingLocationEntity entity = ChargingLocationEntityMapper.toEntity(location);
        assertNotNull(entity);
        assertEquals("zse_ba_einsteinova", entity.getId());
        assertEquals("Bratislava - Einsteinova", entity.getName());
        assertEquals(48.13, entity.getLatitude());
        assertEquals(1, entity.getProviderStations().size());
        assertEquals(1, entity.getChargerUnits().size());
        assertEquals(1, entity.getProviderStations().iterator().next().getConnectors().size());

        ChargingLocation mappedBack = ChargingLocationEntityMapper.toDomain(entity);
        assertNotNull(mappedBack);
        assertEquals(location.id(), mappedBack.id());
        assertEquals(location.name(), mappedBack.name());
        assertEquals(location.coordinates().latitude(), mappedBack.coordinates().latitude());
        assertEquals(location.providerStations().size(), mappedBack.providerStations().size());
        assertEquals(location.chargerUnits().size(), mappedBack.chargerUnits().size());

        Connector backConnector = mappedBack.providerStations().getFirst().connectors().getFirst();
        assertEquals(connector.id(), backConnector.id());
        assertEquals(connector.evseId(), backConnector.evseId());
        assertEquals(connector.maxPowerKw(), backConnector.maxPowerKw());
        assertEquals(connector.liveStatus(), backConnector.liveStatus());
    }
}
