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
     */
    public RefreshResult executeRefresh() {
        Instant startTime = Instant.now();
        long startMillis = System.currentTimeMillis();

        try {
            // 1. Ingest locations
            List<ChargingLocation> locations = ingestionService.ingestBratislava();
            if (locations.isEmpty()) {
                long duration = System.currentTimeMillis() - startMillis;
                log.warn("Refresh finished with 0 locations ingested (duration: {} ms)", duration);
                return RefreshResult.success(startTime, duration, 0, 0, 0);
            }

            // 2. Persist to PostgreSQL database
            persistenceService.saveAll(locations);

            // 3. Cache live statuses into Redis
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
                    "Daily refresh completed successfully in {} ms. Saved {} locations, {} stations, cached {} connector statuses in Redis.",
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
        } catch (Exception ex) {
            long duration = System.currentTimeMillis() - startMillis;
            log.error("Failed executing daily refresh pipeline: {}", ex.getMessage(), ex);
            return RefreshResult.failed(startTime, duration, ex.getMessage());
        }
    }
}
