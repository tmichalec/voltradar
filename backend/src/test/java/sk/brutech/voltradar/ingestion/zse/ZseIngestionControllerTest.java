package sk.brutech.voltradar.ingestion.zse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import sk.brutech.voltradar.cache.ConnectorStatusCache;
import sk.brutech.voltradar.domain.model.Address;
import sk.brutech.voltradar.domain.model.ChargingLocation;
import sk.brutech.voltradar.domain.model.CpoProvider;
import sk.brutech.voltradar.domain.model.GeoCoordinates;
import sk.brutech.voltradar.domain.model.ProviderStation;
import sk.brutech.voltradar.persistence.service.ChargingLocationPersistenceService;
import sk.brutech.voltradar.scheduler.RefreshResult;
import sk.brutech.voltradar.scheduler.ZseDataRefreshScheduler;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ZseIngestionControllerTest {
    private MockMvc mockMvc;
    private ZseDataIngestionService ingestionService;
    private ChargingLocationPersistenceService persistenceService;
    private ConnectorStatusCache statusCache;
    private ZseDataRefreshScheduler refreshScheduler;

    @BeforeEach
    void setUp() {
        ingestionService = Mockito.mock(ZseDataIngestionService.class);
        persistenceService = Mockito.mock(ChargingLocationPersistenceService.class);
        statusCache = Mockito.mock(ConnectorStatusCache.class);
        refreshScheduler = Mockito.mock(ZseDataRefreshScheduler.class);
        ZseIngestionController controller = new ZseIngestionController(
                ingestionService,
                persistenceService,
                statusCache,
                refreshScheduler
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void returnsBratislavaLocations() throws Exception {
        ChargingLocation location = new ChargingLocation(
                "loc-ba-retro",
                "Bratislava - OC Retro",
                new GeoCoordinates(48.152, 17.155),
                new Address("Nevädzová 6", "Bratislava", "82101", "SK"),
                List.of(),
                List.of(),
                null
        );

        when(ingestionService.ingestStations(ZseDataRefreshScheduler.RETRO_STATION_IDS, null)).thenReturn(List.of(location));

        mockMvc.perform(get("/api/v1/ingestion/zse/bratislava"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Bratislava - OC Retro"));
    }

    @Test
    void returnsStationDetails() throws Exception {
        ProviderStation station = new ProviderStation(
                "459600",
                CpoProvider.ZSE_DRIVE,
                "ZSE DriveX Jarovce",
                new GeoCoordinates(48.086, 17.098),
                new Address("D2", "Jarovce", "80000", "SK"),
                "Ultra",
                List.of(),
                "{}"
        );

        when(ingestionService.fetchAndMapStation(459600L)).thenReturn(station);

        mockMvc.perform(get("/api/v1/ingestion/zse/stations/459600"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("ZSE DriveX Jarovce"));
    }

    @Test
    void triggersManualRefreshSuccessfully() throws Exception {
        RefreshResult result = RefreshResult.success(Instant.now(), 150, 5, 5, 10);
        when(refreshScheduler.executeRefresh()).thenReturn(result);

        mockMvc.perform(post("/api/v1/ingestion/zse/refresh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.locationsCount").value(5))
                .andExpect(jsonPath("$.connectorsCount").value(10));
    }

    @Test
    void triggersRetroRefreshSuccessfully() throws Exception {
        RefreshResult result = RefreshResult.success(Instant.now(), 80, 1, 2, 4);
        when(refreshScheduler.refreshRetro()).thenReturn(result);

        mockMvc.perform(post("/api/v1/ingestion/zse/refresh/retro"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.locationsCount").value(1))
                .andExpect(jsonPath("$.connectorsCount").value(4));
    }

    @Test
    void returnsPersistedLocationsWithLiveStatus() throws Exception {
        ChargingLocation location = new ChargingLocation(
                "loc-ba-einsteinova",
                "Bratislava - Einsteinova",
                new GeoCoordinates(48.13, 17.11),
                new Address("Einsteinova", "Bratislava", "85101", "SK"),
                List.of(),
                List.of(),
                null
        );

        when(persistenceService.findAll()).thenReturn(List.of(location));
        when(statusCache.enrichAllWithLiveStatus(anyList())).thenReturn(List.of(location));

        mockMvc.perform(get("/api/v1/ingestion/zse/persisted"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Bratislava - Einsteinova"));
    }

    @Test
    void clearsAllDataSuccessfully() throws Exception {
        mockMvc.perform(post("/api/v1/ingestion/zse/clear"))
                .andExpect(status().isOk());

        Mockito.verify(persistenceService).deleteAll();
        Mockito.verify(statusCache).clearAll();
    }
}
