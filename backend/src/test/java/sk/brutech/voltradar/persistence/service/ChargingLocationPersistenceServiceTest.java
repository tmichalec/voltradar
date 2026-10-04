package sk.brutech.voltradar.persistence.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
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
import sk.brutech.voltradar.persistence.entity.ChargerUnitEntity;
import sk.brutech.voltradar.persistence.entity.ChargingLocationEntity;
import sk.brutech.voltradar.persistence.entity.ConnectorEntity;
import sk.brutech.voltradar.persistence.entity.ProviderStationEntity;
import sk.brutech.voltradar.persistence.repository.ChargingLocationRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChargingLocationPersistenceServiceTest {

    private ChargingLocationRepository repository;
    private ChargingLocationPersistenceService service;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(ChargingLocationRepository.class);
        service = new ChargingLocationPersistenceService(repository);
    }

    @Test
    void savesNewLocation() {
        ChargingLocation location = new ChargingLocation(
                "loc-1",
                "Location 1",
                new GeoCoordinates(48.1, 17.1),
                new Address("Street", "City", "12345", "SK"),
                List.of(),
                List.of(),
                null
        );

        when(repository.findById("loc-1")).thenReturn(Optional.empty());
        when(repository.save(any(ChargingLocationEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChargingLocationEntity saved = service.save(location);
        assertNotNull(saved);
        assertEquals("loc-1", saved.getId());
        assertEquals("Location 1", saved.getName());
        verify(repository).save(any(ChargingLocationEntity.class));
    }

    @Test
    void updatesExistingLocationInPlace() {
        UUID connectorId = UUID.randomUUID();
        Connector updatedConnector = new Connector(
                connectorId,
                "EVSE-100",
                NormalizedConnectorType.CCS,
                CurrentType.DC,
                new BigDecimal("150.00"),
                LiveStatus.OCCUPIED,
                PowerSharingInfo.unknown(new BigDecimal("150.00")),
                new BigDecimal("0.5900"),
                Instant.now()
        );

        ProviderStation updatedStation = new ProviderStation(
                "station-1",
                CpoProvider.ZSE_DRIVE,
                "Updated Station Name",
                new GeoCoordinates(48.2, 17.2),
                new Address("New Street 2", "Bratislava", "82101", "SK"),
                "UFC",
                List.of(updatedConnector),
                "{}"
        );

        ChargerUnit updatedUnit = new ChargerUnit(
                "unit-1",
                "Stand 1",
                ConfidenceLevel.CONFIRMED,
                SharingStatus.SHARED,
                new BigDecimal("150.00"),
                List.of("EVSE-100"),
                "admin",
                "Updated notes"
        );

        ChargingLocation location = new ChargingLocation(
                "loc-1",
                "Updated Location Name",
                new GeoCoordinates(48.2, 17.2),
                new Address("New Street 2", "Bratislava", "82101", "SK"),
                List.of(updatedStation),
                List.of(updatedUnit),
                null
        );

        ChargingLocationEntity existingLocation = new ChargingLocationEntity("loc-1", "Old Location Name", 48.1, 17.1);
        ProviderStationEntity existingStation = new ProviderStationEntity();
        existingStation.setId(10L); // Existing DB primary key
        existingStation.setProviderStationId("station-1");
        existingStation.setProvider(CpoProvider.ZSE_DRIVE);
        existingStation.setName("Old Station Name");

        ConnectorEntity existingConnectorEntity = new ConnectorEntity();
        existingConnectorEntity.setId(connectorId);
        existingConnectorEntity.setEvseId("EVSE-100");
        existingConnectorEntity.setType(NormalizedConnectorType.CCS);
        existingConnectorEntity.setCurrentType(CurrentType.DC);
        existingConnectorEntity.setMaxPowerKw(new BigDecimal("50.00"));
        existingConnectorEntity.setLastKnownStatus(LiveStatus.AVAILABLE);
        existingConnectorEntity.setPowerSharingStatus(SharingStatus.UNKNOWN);
        existingConnectorEntity.setPowerSharingConfidence(ConfidenceLevel.UNKNOWN);
        existingStation.addConnector(existingConnectorEntity);

        existingLocation.addProviderStation(existingStation);

        ChargerUnitEntity existingUnitEntity = new ChargerUnitEntity();
        existingUnitEntity.setId("unit-1");
        existingUnitEntity.setLabel("Old Stand");
        existingUnitEntity.setConfidence(ConfidenceLevel.INFERRED);
        existingUnitEntity.setSharingStatus(SharingStatus.UNKNOWN);
        existingLocation.addChargerUnit(existingUnitEntity);

        when(repository.findById("loc-1")).thenReturn(Optional.of(existingLocation));
        when(repository.save(any(ChargingLocationEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChargingLocationEntity saved = service.save(location);
        assertNotNull(saved);
        assertEquals("Updated Location Name", saved.getName());
        assertEquals(48.2, saved.getLatitude());

        // Verify station preserved DB ID 10L and updated properties in place
        assertEquals(1, saved.getProviderStations().size());
        ProviderStationEntity savedStation = saved.getProviderStations().iterator().next();
        assertEquals(10L, savedStation.getId());
        assertEquals("Updated Station Name", savedStation.getName());

        // Verify connector updated in place without duplicate entity instances
        assertEquals(1, savedStation.getConnectors().size());
        ConnectorEntity savedConnector = savedStation.getConnectors().iterator().next();
        assertEquals(connectorId, savedConnector.getId());
        assertEquals(new BigDecimal("150.00"), savedConnector.getMaxPowerKw());
        assertEquals(LiveStatus.OCCUPIED, savedConnector.getLastKnownStatus());

        // Verify charger unit updated in place
        assertEquals(1, saved.getChargerUnits().size());
        ChargerUnitEntity savedUnit = saved.getChargerUnits().iterator().next();
        assertEquals("unit-1", savedUnit.getId());
        assertEquals("Stand 1", savedUnit.getLabel());
        assertEquals(ConfidenceLevel.CONFIRMED, savedUnit.getConfidence());
        assertEquals(SharingStatus.SHARED, savedUnit.getSharingStatus());
    }

    @Test
    void saveAllDeduplicatesLocationsById() {
        ChargingLocation location1 = new ChargingLocation(
                "loc-1",
                "Location 1",
                new GeoCoordinates(48.1, 17.1),
                new Address("Street", "City", "12345", "SK"),
                List.of(),
                List.of(),
                null
        );
        ChargingLocation location1Duplicate = new ChargingLocation(
                "loc-1",
                "Location 1 Duplicate",
                new GeoCoordinates(48.1, 17.1),
                new Address("Street", "City", "12345", "SK"),
                List.of(),
                List.of(),
                null
        );
        ChargingLocation location2 = new ChargingLocation(
                "loc-2",
                "Location 2",
                new GeoCoordinates(48.2, 17.2),
                new Address("Street 2", "City", "12345", "SK"),
                List.of(),
                List.of(),
                null
        );

        when(repository.findById("loc-1")).thenReturn(Optional.empty());
        when(repository.findById("loc-2")).thenReturn(Optional.empty());
        when(repository.save(any(ChargingLocationEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<ChargingLocationEntity> saved = service.saveAll(List.of(location1, location1Duplicate, location2));
        assertNotNull(saved);
        assertEquals(2, saved.size());
        assertEquals("loc-1", saved.get(0).getId());
        assertEquals("loc-2", saved.get(1).getId());
    }

    @Test
    void findsLocationById() {
        ChargingLocationEntity entity = new ChargingLocationEntity("loc-1", "Location 1", 48.1, 17.1);
        when(repository.findById("loc-1")).thenReturn(Optional.of(entity));

        Optional<ChargingLocation> found = service.findById("loc-1");
        assertTrue(found.isPresent());
        assertEquals("Location 1", found.get().name());
    }
}
