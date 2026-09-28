package sk.brutech.voltradar.ingestion.zse;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.io.ClassPathResource;
import sk.brutech.voltradar.domain.model.ChargingLocation;
import sk.brutech.voltradar.ingestion.zse.dto.ZseDriveDtos;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class ZseDataIngestionServiceTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private ZseDriveClient zseClient;
    private ZseLocationAggregator aggregator;
    private ZseDataIngestionService ingestionService;

    @BeforeEach
    void setUp() {
        zseClient = Mockito.mock(ZseDriveClient.class);
        aggregator = new ZseLocationAggregator();
        ingestionService = new ZseDataIngestionService(zseClient, aggregator);
    }

    @Test
    void ingestsBratislavaStationsConcurrently() throws IOException {
        String stationsJson = new ClassPathResource("zse/stations.json").getContentAsString(StandardCharsets.UTF_8);
        ZseDriveDtos.StationsResponse stationsResponse =
                objectMapper.readValue(stationsJson, ZseDriveDtos.StationsResponse.class);

        String stationJson = new ClassPathResource("zse/station.json").getContentAsString(StandardCharsets.UTF_8);
        ZseDriveDtos.StationResponse stationResponse =
                objectMapper.readValue(stationJson, ZseDriveDtos.StationResponse.class);

        when(zseClient.fetchStations(any())).thenReturn(stationsResponse);
        when(zseClient.fetchStation(eq("459600"))).thenReturn(stationResponse);
        when(zseClient.fetchStation(any())).thenReturn(stationResponse);

        List<ChargingLocation> locations = ingestionService.ingestBratislava();

        assertThat(locations).isNotEmpty();
        var firstLoc = locations.getFirst();
        assertThat(firstLoc.metadata().totalConnectorsCount()).isGreaterThan(0);
    }

    @Test
    void handlesEmptyStationListGracefully() {
        when(zseClient.fetchStations(any()))
                .thenReturn(new ZseDriveDtos.StationsResponse(List.of(), List.of(), List.of(), 0));

        List<ChargingLocation> locations = ingestionService.ingestBratislava();

        assertThat(locations).isEmpty();
    }
}
