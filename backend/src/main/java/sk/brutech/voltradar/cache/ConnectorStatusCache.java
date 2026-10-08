package sk.brutech.voltradar.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import sk.brutech.voltradar.domain.model.ChargingLocation;
import sk.brutech.voltradar.domain.model.Connector;
import sk.brutech.voltradar.domain.model.LiveStatus;
import sk.brutech.voltradar.domain.model.ProviderStation;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Component
public class ConnectorStatusCache {

    private static final Logger log = LoggerFactory.getLogger(ConnectorStatusCache.class);
    private static final String REDIS_HASH_KEY = "voltradar:connectors:status";
    private static final String KEY_PREFIX = "voltradar:connector:status:";
    private static final Duration DEFAULT_TTL = Duration.ofHours(24);

    private final StringRedisTemplate redisTemplate;

    public ConnectorStatusCache(StringRedisTemplate redisTemplate) {
        this.redisTemplate = Objects.requireNonNull(redisTemplate, "redisTemplate must not be null");
    }

    public void clearAll() {
        try {
            redisTemplate.delete(REDIS_HASH_KEY);
            java.util.Set<String> keys = redisTemplate.keys(KEY_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
            log.info("Cleared connector status cache in Redis");
        } catch (Exception ex) {
            log.warn("Failed to clear Redis cache: {}", ex.getMessage());
        }
    }

    public void updateStatus(String evseId, LiveStatus status) {
        if (evseId == null || status == null) {
            return;
        }
        try {
            redisTemplate.opsForHash().put(REDIS_HASH_KEY, evseId, status.name());
            redisTemplate.opsForValue().set(KEY_PREFIX + evseId, status.name(), DEFAULT_TTL);
        } catch (Exception ex) {
            log.warn("Failed to update status in Redis for evseId {}: {}", evseId, ex.getMessage());
        }
    }

    public void updateAllStatuses(Map<String, LiveStatus> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return;
        }
        try {
            Map<String, String> stringMap = new HashMap<>(statuses.size());
            for (Map.Entry<String, LiveStatus> entry : statuses.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    stringMap.put(entry.getKey(), entry.getValue().name());
                }
            }
            if (!stringMap.isEmpty()) {
                redisTemplate.opsForHash().putAll(REDIS_HASH_KEY, stringMap);
                log.debug("Updated {} connector statuses in Redis hash {}", stringMap.size(), REDIS_HASH_KEY);
            }
        } catch (Exception ex) {
            log.warn("Failed to update batch statuses in Redis: {}", ex.getMessage());
        }
    }

    public Optional<LiveStatus> getStatus(String evseId) {
        if (evseId == null) {
            return Optional.empty();
        }
        try {
            Object val = redisTemplate.opsForHash().get(REDIS_HASH_KEY, evseId);
            if (val != null) {
                return parseLiveStatus(val.toString());
            }
            String singleVal = redisTemplate.opsForValue().get(KEY_PREFIX + evseId);
            if (singleVal != null) {
                return parseLiveStatus(singleVal);
            }
        } catch (Exception ex) {
            log.warn("Failed to get status from Redis for evseId {}: {}", evseId, ex.getMessage());
        }
        return Optional.empty();
    }

    public Map<String, LiveStatus> getAllStatuses(Collection<String> evseIds) {
        if (evseIds == null || evseIds.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            List<Object> keys = new ArrayList<>(evseIds);
            List<Object> values = redisTemplate.opsForHash().multiGet(REDIS_HASH_KEY, keys);
            Map<String, LiveStatus> result = new HashMap<>();
            int i = 0;
            for (String evseId : evseIds) {
                if (i < values.size() && values.get(i) != null) {
                    parseLiveStatus(values.get(i).toString()).ifPresent(status -> result.put(evseId, status));
                }
                i++;
            }
            return result;
        } catch (Exception ex) {
            log.warn("Failed to multiGet statuses from Redis: {}", ex.getMessage());
            return Collections.emptyMap();
        }
    }

    public ChargingLocation enrichWithLiveStatus(ChargingLocation location) {
        if (location == null || location.providerStations() == null) {
            return location;
        }

        List<String> evseIds = location.providerStations().stream()
                .flatMap(s -> s.connectors().stream())
                .map(Connector::evseId)
                .toList();

        Map<String, LiveStatus> liveStatuses = getAllStatuses(evseIds);
        if (liveStatuses.isEmpty()) {
            return location;
        }

        List<ProviderStation> updatedStations = location.providerStations().stream()
                .map(station -> {
                    List<Connector> updatedConnectors = station.connectors().stream()
                            .map(c -> {
                                LiveStatus live = liveStatuses.get(c.evseId());
                                if (live != null && live != c.liveStatus()) {
                                    return new Connector(
                                            c.id(),
                                            c.evseId(),
                                            c.type(),
                                            c.currentType(),
                                            c.maxPowerKw(),
                                            live,
                                            c.powerSharing(),
                                            c.publicPricePerKwh(),
                                            c.lastStatusUpdate(),
                                            c.freeParkingMinutes()
                                    );
                                }
                                return c;
                            })
                            .toList();

                    return new ProviderStation(
                            station.providerStationId(),
                            station.provider(),
                            station.name(),
                            station.coordinates(),
                            station.address(),
                            station.rawProviderType(),
                            updatedConnectors,
                            station.rawJsonPayload()
                    );
                })
                .toList();

        return new ChargingLocation(
                location.id(),
                location.name(),
                location.coordinates(),
                location.address(),
                updatedStations,
                location.chargerUnits(),
                null, // Recalculates metadata automatically
                location.updatedAt()
        );
    }

    public List<ChargingLocation> enrichAllWithLiveStatus(List<ChargingLocation> locations) {
        if (locations == null || locations.isEmpty()) {
            return List.of();
        }
        return locations.stream()
                .map(this::enrichWithLiveStatus)
                .toList();
    }

    private Optional<LiveStatus> parseLiveStatus(String statusStr) {
        if (statusStr == null || statusStr.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(LiveStatus.valueOf(statusStr.trim().toUpperCase()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
