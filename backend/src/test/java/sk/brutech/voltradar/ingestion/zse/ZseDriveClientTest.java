package sk.brutech.voltradar.ingestion.zse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import sk.brutech.voltradar.domain.model.ConnectorStatus;
import sk.brutech.voltradar.domain.model.ConnectorType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class ZseDriveClientTest {
    private MockRestServiceServer server;
    private ZseDriveClient client;

    @BeforeEach
    void setUp() {
        var builder = RestClient.builder().baseUrl("https://zsedrive.sk");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new ZseDriveClient(builder.build());
    }

    @Test
    void readsViewportWithoutTreatingClustersAsStations() throws IOException {
        server.expect(requestTo(org.hamcrest.Matchers.startsWith("https://zsedrive.sk/api/web/v1/stations?")))
                .andExpect(queryParam("gpsNorthWestLat", "49.7"))
                .andExpect(queryParam("gpsNorthWestLon", "16.8"))
                .andExpect(queryParam("gpsSouthEastLat", "47.7"))
                .andExpect(queryParam("gpsSouthEastLon", "22.6"))
                .andExpect(request -> assertThat(request.getURI().getQuery()).contains("contractor[]=ZSE"))
                .andExpect(queryParam("limit", "40"))
                .andExpect(header("language", "sk"))
                .andRespond(withSuccess(fixture("stations.json"), MediaType.APPLICATION_JSON));

        var result = client.fetchStations(ZseStationQuery.viewport(
                new ZseStationQuery.Bounds(49.7, 16.8, 47.7, 22.6)));

        assertThat(result.list()).hasSize(40);
        assertThat(result.count()).isGreaterThan(result.list().size());
        assertThat(result.grid()).isNotEmpty();
        assertThat(result.list().getFirst().connectors()).isEqualTo(2);
        assertThat(result.list().getFirst().location().lat()).isEqualByComparingTo("48.08653");
        server.verify();
    }

    @Test
    void encodesSearchTextAsOneQueryValue() throws IOException {
        server.expect(requestTo(org.hamcrest.Matchers.containsString("query=%C5%BDilina%20%26%20okolie")))
                .andRespond(withSuccess(fixture("stations.json"), MediaType.APPLICATION_JSON));
        client.fetchStations(ZseStationQuery.search("Žilina & okolie"));
        server.verify();
    }

    @Test
    void readsStationPowerAvailabilityAndGuestPrice() throws IOException {
        server.expect(requestTo("https://zsedrive.sk/api/v4.7/stations/459600"))
                .andRespond(withSuccess(fixture("station.json"), MediaType.APPLICATION_JSON));
        var station = client.fetchStation("459600").station();
        var connector = station.connectors().getFirst();
        assertThat(station.remoteId()).isEqualTo("310437");
        assertThat(connector.evseId()).isEqualTo("310437");
        assertThat(ZseConnectorValues.maxPowerKw(connector)).hasValue(new java.math.BigDecimal("400"));
        assertThat(ZseConnectorValues.status(connector.state())).isEqualTo(ConnectorStatus.AVAILABLE);
        assertThat(connector.pricing().get(1).value()).isEqualTo("0.79 €/kWh");
        server.verify();
    }

    @Test
    void roamingTypeIdsDoNotOverrideConnectorNames() throws IOException {
        server.expect(requestTo("https://zsedrive.sk/api/v4.7/stations/440826"))
                .andRespond(withSuccess(fixture("roaming-station.json"), MediaType.APPLICATION_JSON));
        var connector = client.fetchStation("440826").station().connectors().getFirst();
        assertThat(connector.type().id()).isEqualTo(1L);
        assertThat(ZseConnectorValues.type(connector)).hasValue(ConnectorType.DC_CCS);
        server.verify();
    }

    @Test
    void preservesSharedEvseAndDistinctConnectorIds() throws IOException {
        server.expect(requestTo("https://zsedrive.sk/api/v4.7/stations/283373"))
                .andRespond(withSuccess(fixture("mixed-station.json"), MediaType.APPLICATION_JSON));
        var connectors = client.fetchStation("283373").station().connectors();
        assertThat(connectors).hasSize(3);
        assertThat(connectors.get(1).evseId()).isEqualTo(connectors.get(2).evseId());
        assertThat(connectors.get(1).id()).isNotEqualTo(connectors.get(2).id());
        server.verify();
    }

    @Test
    void readsDecimalTariffsAndNullableNightTimes() throws IOException {
        server.expect(requestTo("https://zsedrive.sk/api/v4.7/program?partial=true&b2c=true"))
                .andRespond(withSuccess(fixture("programs.json"), MediaType.APPLICATION_JSON));
        var result = client.fetchTariffs();
        var eco = result.programs().getFirst();
        var start = result.programs().get(1);
        assertThat(eco.freeMonthlyCharging()).isNull();
        assertThat(eco.pricing().acNightTariff()).isNull();
        assertThat(start.pricing().monthlyFee()).isEqualByComparingTo("2.99");
        assertThat(start.pricing().acNightTariff().amount()).isEqualByComparingTo("0.24");
        assertThat(start.pricing().acNightTariff().starthour()).isEqualTo(22);
        assertThat(start.pricing().acNightTariff().startminute()).isNull();
        assertThat(result.homePrograms()).hasSize(2);
        server.verify();
    }

    @Test
    void readsConnectorCatalogAndIgnoresNewFields() throws IOException {
        server.expect(requestTo("https://zsedrive.sk/api/v4.2/connector-types"))
                .andRespond(withSuccess(fixture("connector-types.json").replace(
                        "\"connectorTypes\":", "\"futureField\":true,\"connectorTypes\":"),
                        MediaType.APPLICATION_JSON));
        assertThat(client.fetchConnectorTypes().connectorTypes()).hasSize(3);
        server.verify();
    }

    @Test
    void preservesHttpFailureStatus() {
        server.expect(anything()).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        assertThatThrownBy(client::fetchTariffs).isInstanceOfSatisfying(ZseDriveException.class,
                failure -> assertThat(failure.getStatusCode()).isEqualTo(429));
        server.verify();
    }

    @Test
    void wrapsTransportFailureWithoutReturningEmptyData() {
        server.expect(anything()).andRespond(withException(new IOException("Connection failed")));
        assertThatThrownBy(client::fetchTariffs).isInstanceOfSatisfying(ZseDriveException.class,
                failure -> assertThat(failure.getStatusCode()).isNull());
        server.verify();
    }

    @Test
    void rejectsMissingEnvelopeAndMalformedJson() {
        server.expect(anything()).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        server.expect(anything()).andRespond(withSuccess("{bad", MediaType.APPLICATION_JSON));
        server.expect(anything()).andRespond(withNoContent());
        assertThatThrownBy(client::fetchTariffs).isInstanceOf(ZseDriveException.class);
        assertThatThrownBy(client::fetchTariffs).isInstanceOf(ZseDriveException.class);
        assertThatThrownBy(client::fetchTariffs).isInstanceOf(ZseDriveException.class);
        server.verify();
    }

    @Test
    void rejectsInvalidInputsBeforeNetworkCalls() {
        assertThatThrownBy(() -> client.fetchStation("../me")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ZseStationQuery.search(" ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ZseStationQuery.Bounds(Double.NaN, 17, 48, 18))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ZseStationQuery(null, "Bratislava", 0, List.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void unknownOrDisconnectedStatesDoNotBecomeAvailable() {
        assertThat(ZseConnectorValues.status("BUSY")).isEqualTo(ConnectorStatus.OCCUPIED);
        assertThat(ZseConnectorValues.status("RESERVED")).isEqualTo(ConnectorStatus.OCCUPIED);
        assertThat(ZseConnectorValues.status("DISCONNECTED")).isEqualTo(ConnectorStatus.UNKNOWN);
        assertThat(ZseConnectorValues.status("NEW_STATE")).isEqualTo(ConnectorStatus.UNKNOWN);
        assertThat(ZseConnectorValues.status(null)).isEqualTo(ConnectorStatus.UNKNOWN);
    }

    private String fixture(String name) throws IOException {
        return new ClassPathResource("zse/" + name).getContentAsString(StandardCharsets.UTF_8);
    }
}
