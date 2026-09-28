package sk.brutech.voltradar.ingestion.zse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import sk.brutech.voltradar.domain.model.Address;
import sk.brutech.voltradar.domain.model.ChargingLocation;
import sk.brutech.voltradar.domain.model.CpoProvider;
import sk.brutech.voltradar.domain.model.GeoCoordinates;
import sk.brutech.voltradar.domain.model.ProviderStation;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ZseIngestionControllerTest {
    private MockMvc mockMvc;
    private ZseDataIngestionService ingestionService;

    @BeforeEach
    void setUp() {
        ingestionService = Mockito.mock(ZseDataIngestionService.class);
        ZseIngestionController controller = new ZseIngestionController(ingestionService);
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

        when(ingestionService.ingestBratislava()).thenReturn(List.of(location));

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
}
