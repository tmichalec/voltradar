package sk.brutech.voltradar.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import sk.brutech.voltradar.cache.ConnectorStatusCache;
import sk.brutech.voltradar.domain.model.ChargingLocation;
import sk.brutech.voltradar.domain.model.Connector;
import sk.brutech.voltradar.domain.model.LiveStatus;
import sk.brutech.voltradar.ingestion.zse.ZseDataIngestionService;
import sk.brutech.voltradar.persistence.service.ChargingLocationPersistenceService;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
@ConditionalOnProperty(name = "scheduling.zse-refresh.enabled", havingValue = "true", matchIfMissing = true)
public class ZseDataRefreshScheduler {

    public static final List<Long> RETRO_STATION_IDS = List.of(79480L, 316067L);

    private static final Logger log = LoggerFactory.getLogger(ZseDataRefreshScheduler.class);

    private final ZseDataIngestionService ingestionService;
    private final ChargingLocationPersistenceService persistenceService;
    private final ConnectorStatusCache statusCache;

    public ZseDataRefreshScheduler(
            ZseDataIngestionService ingestionService,
            ChargingLocationPersistenceService persistenceService,
            ConnectorStatusCache statusCache
    ) {
        this.ingestionService = Objects.requireNonNull(ingestionService, "ingestionService must not be null");
        this.persistenceService = Objects.requireNonNull(persistenceService, "persistenceService must not be null");
        this.statusCache = Objects.requireNonNull(statusCache, "statusCache must not be null");
    }

    /**
     * Scheduled daily ingestion job (default: 03:00 AM every day).
     */
    @Scheduled(cron = "${scheduling.zse-refresh.cron:0 0 3 * * ?}")
    public RefreshResult runDailyRefresh() {
        log.info("Starting scheduled daily ZSE Drive infrastructure and live status refresh...");
        return executeRefresh();
    }

    /**
     * Executes complete refresh pipeline: Ingestion -> DB Persistence -> Redis Cache update.
     * Restricted to OC Retro charging location stations (IDs 79480 and 316067) for targeted testing and execution.
     */
    public RefreshResult executeRefresh() {
        return refreshRetro();
    }

    /**
     * Refreshes specifically the OC Retro charging location stations (IDs 79480 and 316067).
     */
    public RefreshResult refreshRetro() {
        return refreshStationIds(RETRO_STATION_IDS, "OC Retro");
    }

    /**
     * Executes targeted refresh pipeline for specific station IDs: Ingest -> Persist to DB -> Cache in Redis.
     */
    public RefreshResult refreshStationIds(List<Long> stationIds, String targetName) {
        Instant startTime = Instant.now();
        long startMillis = System.currentTimeMillis();

        try {
            List<ChargingLocation> locations = ingestionService.ingestStations(stationIds, null);
            return persistAndCacheLocations(locations, startTime, startMillis, targetName);
        } catch (Exception ex) {
            long duration = System.currentTimeMillis() - startMillis;
            log.error("Failed executing targeted refresh for {}: {}", targetName, ex.getMessage(), ex);
            return RefreshResult.failed(startTime, duration, ex.getMessage());
        }
    }

    private RefreshResult persistAndCacheLocations(
            List<ChargingLocation> locations,
            Instant startTime,
            long startMillis,
            String contextDescription
    ) {
        if (locations.isEmpty()) {
            long duration = System.currentTimeMillis() - startMillis;
            log.warn("Refresh for {} finished with 0 locations ingested (duration: {} ms)", contextDescription, duration);
            return RefreshResult.success(startTime, duration, 0, 0, 0);
        }

        // Persist to PostgreSQL database
        persistenceService.saveAll(locations);

        // Cache live statuses into Redis
        Map<String, LiveStatus> liveStatusMap = new HashMap<>();
        int stationCount = 0;
        for (ChargingLocation location : locations) {
            if (location.providerStations() != null) {
                stationCount += location.providerStations().size();
                for (var station : location.providerStations()) {
                    if (station.connectors() != null) {
                        for (Connector connector : station.connectors()) {
                            if (connector.evseId() != null && connector.liveStatus() != null) {
                                liveStatusMap.put(connector.evseId(), connector.liveStatus());
                            }
                        }
                    }
                }
            }
        }

        statusCache.updateAllStatuses(liveStatusMap);

        long duration = System.currentTimeMillis() - startMillis;
        log.info(
                "Refresh for {} completed successfully in {} ms. Saved {} locations, {} stations, cached {} connector statuses in Redis.",
                contextDescription,
                duration,
                locations.size(),
                stationCount,
                liveStatusMap.size()
        );

        return RefreshResult.success(
                startTime,
                duration,
                locations.size(),
                stationCount,
                liveStatusMap.size()
        );
    }
}
