package sk.brutech.voltradar.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
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

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConnectorStatusCacheTest {

    private StringRedisTemplate redisTemplate;
    private HashOperations<String, Object, Object> hashOperations;
    private ValueOperations<String, String> valueOperations;
    private ConnectorStatusCache cache;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = Mockito.mock(StringRedisTemplate.class);
        hashOperations = Mockito.mock(HashOperations.class);
        valueOperations = Mockito.mock(ValueOperations.class);

        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        cache = new ConnectorStatusCache(redisTemplate);
    }

    @Test
    void updatesSingleStatus() {
        cache.updateStatus("EVSE-1", LiveStatus.AVAILABLE);
        verify(hashOperations).put("voltradar:connectors:status", "EVSE-1", "AVAILABLE");
        verify(valueOperations).set(eq("voltradar:connector:status:EVSE-1"), eq("AVAILABLE"), any(Duration.class));
    }

    @Test
    void updatesBatchStatuses() {
        Map<String, LiveStatus> map = Map.of(
                "EVSE-1", LiveStatus.AVAILABLE,
                "EVSE-2", LiveStatus.OCCUPIED
        );
        cache.updateAllStatuses(map);
        verify(hashOperations).putAll(eq("voltradar:connectors:status"), any());
    }

    @Test
    void retrievesStatusFromHash() {
        when(hashOperations.get("voltradar:connectors:status", "EVSE-1")).thenReturn("OCCUPIED");
        Optional<LiveStatus> status = cache.getStatus("EVSE-1");
        assertTrue(status.isPresent());
        assertEquals(LiveStatus.OCCUPIED, status.get());
    }

    @Test
    void enrichesLocationWithLiveStatus() {
        UUID connectorId = UUID.randomUUID();
        Connector connector = new Connector(
                connectorId,
                "EVSE-1",
                NormalizedConnectorType.CCS,
                CurrentType.DC,
                new BigDecimal("150.0"),
                LiveStatus.UNKNOWN,
                PowerSharingInfo.independent(new BigDecimal("150.0")),
                new BigDecimal("0.49"),
                Instant.now()
        );

        ProviderStation station = new ProviderStation(
                "2145",
                CpoProvider.ZSE_DRIVE,
                "Station",
                new GeoCoordinates(48.1, 17.1),
                new Address("Street", "City", "12345", "SK"),
                "Ultra",
                List.of(connector),
                "{}"
        );

        ChargingLocation location = new ChargingLocation(
                "loc-1",
                "Location 1",
                new GeoCoordinates(48.1, 17.1),
                new Address("Street", "City", "12345", "SK"),
                List.of(station),
                List.of(),
                null
        );

        when(hashOperations.multiGet(eq("voltradar:connectors:status"), any())).thenReturn(List.of("AVAILABLE"));

        ChargingLocation enriched = cache.enrichWithLiveStatus(location);
        Connector updatedConnector = enriched.providerStations().getFirst().connectors().getFirst();
        assertEquals(LiveStatus.AVAILABLE, updatedConnector.liveStatus());
    }
}
