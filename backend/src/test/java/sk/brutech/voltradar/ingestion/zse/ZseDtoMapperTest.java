package sk.brutech.voltradar.ingestion.zse;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import sk.brutech.voltradar.domain.model.CurrentType;
import sk.brutech.voltradar.domain.model.LiveStatus;
import sk.brutech.voltradar.domain.model.NormalizedConnectorType;
import sk.brutech.voltradar.domain.model.ProviderStation;
import sk.brutech.voltradar.ingestion.zse.dto.ZseDriveDtos;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class ZseDtoMapperTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void mapsUltraFastStationCorrectly() throws IOException {
        String json = new ClassPathResource("zse/station.json").getContentAsString(StandardCharsets.UTF_8);
        ZseDriveDtos.StationResponse response = objectMapper.readValue(json, ZseDriveDtos.StationResponse.class);

        ProviderStation providerStation = ZseDtoMapper.toProviderStation(response.station());

        assertThat(providerStation.providerStationId()).isEqualTo("459600");
        assertThat(providerStation.name()).isEqualTo("ZSE DriveX BA D2 Jarovce smer HU HDV 6-7 Ultra");
        assertThat(providerStation.coordinates().latitude()).isEqualTo(48.08653113);
        assertThat(providerStation.coordinates().longitude()).isEqualTo(17.09845873);
        assertThat(providerStation.address().street()).isEqualTo("Diaľnica D2");
        assertThat(providerStation.address().city()).isEqualTo("Jarovce");

        assertThat(providerStation.connectors()).hasSize(2);
        var firstConnector = providerStation.connectors().getFirst();
        assertThat(firstConnector.type()).isEqualTo(NormalizedConnectorType.CCS);
        assertThat(firstConnector.currentType()).isEqualTo(CurrentType.DC);
        assertThat(firstConnector.maxPowerKw()).isEqualByComparingTo("400");
        assertThat(firstConnector.liveStatus()).isEqualTo(LiveStatus.AVAILABLE);
        assertThat(firstConnector.publicPricePerKwh()).isEqualByComparingTo("0.79");
    }

    @Test
    void mapsMixedStationCorrectly() throws IOException {
        String json = new ClassPathResource("zse/mixed-station.json").getContentAsString(StandardCharsets.UTF_8);
        ZseDriveDtos.StationResponse response = objectMapper.readValue(json, ZseDriveDtos.StationResponse.class);

        ProviderStation providerStation = ZseDtoMapper.toProviderStation(response.station());

        assertThat(providerStation.providerStationId()).isEqualTo("283373");
        assertThat(providerStation.connectors()).isNotEmpty();
    }
}
