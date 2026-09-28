package sk.brutech.voltradar.ingestion.zse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import sk.brutech.voltradar.domain.model.ChargingLocation;
import sk.brutech.voltradar.domain.model.ProviderStation;
import sk.brutech.voltradar.domain.override.LocationsGistDocument;
import sk.brutech.voltradar.ingestion.zse.dto.ZseDriveDtos;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

/**
 * Service orchestrating real-time and batch ingestion of ZSE Drive charging stations,
 * mapping them to domain models and aggregating them into unified charging locations.
 */
@Service
public class ZseDataIngestionService {
    private static final Logger log = LoggerFactory.getLogger(ZseDataIngestionService.class);

    /**
     * Bounding box covering the greater Bratislava metropolitan area.
     * North: 48.26, West: 16.94, South: 48.04, East: 17.29
     */
    public static final ZseStationQuery.Bounds BRATISLAVA_BOUNDS =
            new ZseStationQuery.Bounds(48.26, 16.94, 48.04, 17.29);

    private final ZseDriveClient zseClient;
    private final ZseLocationAggregator aggregator;
    private final int concurrencyLimit;

    public ZseDataIngestionService(ZseDriveClient zseClient, ZseLocationAggregator aggregator) {
        this(zseClient, aggregator, 8);
    }

    @Autowired
    public ZseDataIngestionService(
            ZseDriveClient zseClient,
            ZseLocationAggregator aggregator,
            @Value("${integrations.zse-drive.concurrency-limit:8}") int concurrencyLimit
    ) {
        this.zseClient = Objects.requireNonNull(zseClient, "zseClient must not be null");
        this.aggregator = Objects.requireNonNull(aggregator, "aggregator must not be null");
        this.concurrencyLimit = Math.max(1, concurrencyLimit);
    }

    /**
     * Ingests and aggregates stations within the greater Bratislava region.
     */
    public List<ChargingLocation> ingestBratislava() {
        return ingestBratislava(null);
    }

    /**
     * Ingests and aggregates stations within the greater Bratislava region with optional Gist overrides.
     */
    public List<ChargingLocation> ingestBratislava(LocationsGistDocument gistDocument) {
        return ingestViewport(BRATISLAVA_BOUNDS, 50, gistDocument);
    }

    /**
     * Ingests and aggregates stations within specified geographical bounds.
     */
    public List<ChargingLocation> ingestViewport(
            ZseStationQuery.Bounds bounds,
            int limit,
            LocationsGistDocument gistDocument
    ) {
        Objects.requireNonNull(bounds, "bounds must not be null");
        log.info("Fetching ZSE stations for bounds [N: {}, W: {}, S: {}, E: {}] with limit {}",
                bounds.north(), bounds.west(), bounds.south(), bounds.east(), limit);

        ZseStationQuery query = new ZseStationQuery(bounds, null, limit, List.of(), List.of("ZSE"));
        ZseDriveDtos.StationsResponse stationsResponse = zseClient.fetchStations(query);

        if (stationsResponse == null || stationsResponse.list() == null || stationsResponse.list().isEmpty()) {
            log.info("No ZSE stations found in given viewport.");
            return List.of();
        }

        List<Long> stationIds = stationsResponse.list().stream()
                .map(ZseDriveDtos.StationSummary::id)
                .filter(Objects::nonNull)
                .toList();

        return ingestStations(stationIds, gistDocument);
    }

    /**
     * Concurrently fetches station details for given station IDs and aggregates them.
     */
    public List<ChargingLocation> ingestStations(List<Long> stationIds, LocationsGistDocument gistDocument) {
        if (stationIds == null || stationIds.isEmpty()) {
            return List.of();
        }

        log.info(
                "Fetching details for {} ZSE stations using Java Virtual Threads (concurrency limit: {})",
                stationIds.size(),
                concurrencyLimit
        );
        List<ProviderStation> providerStations = new ArrayList<>();
        Semaphore semaphore = new Semaphore(concurrencyLimit);

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<CompletableFuture<ProviderStation>> futures = stationIds.stream()
                    .map(id -> CompletableFuture.supplyAsync(() -> {
                        try {
                            semaphore.acquire();
                            try {
                                return fetchAndMapStation(id);
                            } finally {
                                semaphore.release();
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            log.warn("Interrupted while fetching station {}", id);
                            return null;
                        }
                    }, executor))
                    .toList();

            for (CompletableFuture<ProviderStation> future : futures) {
                try {
                    ProviderStation station = future.join();
                    if (station != null) {
                        providerStations.add(station);
                    }
                } catch (Exception ex) {
                    log.warn("Failed to retrieve or map station: {}", ex.getMessage());
                }
            }
        }

        log.info("Successfully fetched {} station details, aggregating into locations...", providerStations.size());
        List<ChargingLocation> locations = aggregator.aggregate(providerStations, gistDocument);
        log.info("Aggregated into {} unified ChargingLocations.", locations.size());
        return locations;
    }

    /**
     * Fetches details of a single station by its ID and maps it to a ProviderStation.
     */
    public ProviderStation fetchAndMapStation(Long stationId) {
        if (stationId == null) {
            return null;
        }
        try {
            ZseDriveDtos.StationResponse response = zseClient.fetchStation(String.valueOf(stationId));
            if (response != null && response.station() != null) {
                return ZseDtoMapper.toProviderStation(response.station());
            }
        } catch (Exception ex) {
            log.warn("Error fetching station {}: {}", stationId, ex.getMessage());
        }
        return null;
    }
}
