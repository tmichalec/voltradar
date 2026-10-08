package sk.brutech.voltradar.scheduler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import sk.brutech.voltradar.cache.ConnectorStatusCache;
import sk.brutech.voltradar.domain.model.Address;
import sk.brutech.voltradar.domain.model.ChargingLocation;
import sk.brutech.voltradar.domain.model.Connector;
import sk.brutech.voltradar.domain.model.CpoProvider;
import sk.brutech.voltradar.domain.model.CurrentType;
import sk.brutech.voltradar.domain.model.GeoCoordinates;
import sk.brutech.voltradar.domain.model.LiveStatus;
import sk.brutech.voltradar.domain.model.NormalizedConnectorType;
import sk.brutech.voltradar.domain.model.PowerSharingInfo;
import sk.brutech.voltradar.domain.model.ProviderStation;
import sk.brutech.voltradar.ingestion.zse.ZseDataIngestionService;
import sk.brutech.voltradar.persistence.service.ChargingLocationPersistenceService;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ZseDataRefreshSchedulerTest {

    private ZseDataIngestionService ingestionService;
    private ChargingLocationPersistenceService persistenceService;
    private ConnectorStatusCache statusCache;
    private ZseDataRefreshScheduler scheduler;

    @BeforeEach
    void setUp() {
        ingestionService = Mockito.mock(ZseDataIngestionService.class);
        persistenceService = Mockito.mock(ChargingLocationPersistenceService.class);
        statusCache = Mockito.mock(ConnectorStatusCache.class);
        scheduler = new ZseDataRefreshScheduler(ingestionService, persistenceService, statusCache);
    }

    @Test
    void executesRefreshPipelineSuccessfully() {
        UUID connectorId = UUID.randomUUID();
        Connector connector = new Connector(
                connectorId,
                "EVSE-101",
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
                "{}"
        );

        ChargingLocation location = new ChargingLocation(
                "zse_ba_einsteinova",
                "Bratislava - Einsteinova",
                new GeoCoordinates(48.13, 17.11),
                new Address("Einsteinova 1", "Bratislava", "85101", "SK"),
                List.of(station),
                List.of(),
                null
        );

        when(ingestionService.ingestStations(ZseDataRefreshScheduler.RETRO_STATION_IDS, null)).thenReturn(List.of(location));

        RefreshResult result = scheduler.executeRefresh();

        assertEquals("SUCCESS", result.status());
        assertEquals(1, result.locationsCount());
        assertEquals(1, result.providerStationsCount());
        assertEquals(1, result.connectorsCount());

        verify(persistenceService).saveAll(anyList());
        verify(statusCache).updateAllStatuses(any());
    }

    @Test
    void executesRetroRefreshSuccessfully() {
        UUID connectorId = UUID.randomUUID();
        Connector connector = new Connector(
                connectorId,
                "SK*ZSE*E79480*1",
                NormalizedConnectorType.CCS,
                CurrentType.DC,
                new BigDecimal("150.0"),
                LiveStatus.AVAILABLE,
                PowerSharingInfo.independent(new BigDecimal("150.0")),
                new BigDecimal("0.59"),
                Instant.now()
        );

        ProviderStation station = new ProviderStation(
                "79480",
                CpoProvider.ZSE_DRIVE,
                "Bratislava - OC Retro Ultra 1",
                new GeoCoordinates(48.152, 17.155),
                new Address("Nevädzová 6", "Bratislava", "82101", "SK"),
                "Ultra",
                List.of(connector),
                "{}"
        );

        ChargingLocation location = new ChargingLocation(
                "zse-79480",
                "Bratislava - OC Retro Ultra",
                new GeoCoordinates(48.152, 17.155),
                new Address("Nevädzová 6", "Bratislava", "82101", "SK"),
                List.of(station),
                List.of(),
                null
        );

        when(ingestionService.ingestStations(ZseDataRefreshScheduler.RETRO_STATION_IDS, null))
                .thenReturn(List.of(location));

        RefreshResult result = scheduler.refreshRetro();

        assertEquals("SUCCESS", result.status());
        assertEquals(1, result.locationsCount());
        assertEquals(1, result.providerStationsCount());
        assertEquals(1, result.connectorsCount());

        verify(persistenceService).saveAll(anyList());
        verify(statusCache).updateAllStatuses(any());
    }

    @Test
    void handlesEmptyIngestionGracefully() {
        when(ingestionService.ingestStations(ZseDataRefreshScheduler.RETRO_STATION_IDS, null)).thenReturn(List.of());

        RefreshResult result = scheduler.executeRefresh();

        assertEquals("SUCCESS", result.status());
        assertEquals(0, result.locationsCount());
    }

    @Test
    void handlesIngestionErrorGracefully() {
        when(ingestionService.ingestStations(ZseDataRefreshScheduler.RETRO_STATION_IDS, null))
                .thenThrow(new RuntimeException("API connection timeout"));

        RefreshResult result = scheduler.executeRefresh();

        assertEquals("FAILED", result.status());
        assertEquals("API connection timeout", result.message());
    }
}
