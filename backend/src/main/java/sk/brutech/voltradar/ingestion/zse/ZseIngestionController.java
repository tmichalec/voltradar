package sk.brutech.voltradar.ingestion.zse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sk.brutech.voltradar.cache.ConnectorStatusCache;
import sk.brutech.voltradar.domain.model.ChargingLocation;
import sk.brutech.voltradar.domain.model.ProviderStation;
import sk.brutech.voltradar.persistence.service.ChargingLocationPersistenceService;
import sk.brutech.voltradar.scheduler.RefreshResult;
import sk.brutech.voltradar.scheduler.ZseDataRefreshScheduler;

import java.util.List;

/**
 * REST controller providing endpoints to trigger and inspect ZSE Drive ingestion data.
 */
@RestController
@RequestMapping("/api/v1/ingestion/zse")
@Tag(name = "ZSE Ingestion", description = "Endpoints for fetching and aggregating ZSE Drive charging stations")
public class ZseIngestionController {
    private final ZseDataIngestionService ingestionService;
    private final ChargingLocationPersistenceService persistenceService;
    private final ConnectorStatusCache statusCache;
    private final ZseDataRefreshScheduler refreshScheduler;

    public ZseIngestionController(
            ZseDataIngestionService ingestionService,
            ChargingLocationPersistenceService persistenceService,
            ConnectorStatusCache statusCache,
            ZseDataRefreshScheduler refreshScheduler
    ) {
        this.ingestionService = ingestionService;
        this.persistenceService = persistenceService;
        this.statusCache = statusCache;
        this.refreshScheduler = refreshScheduler;
    }

    /**
     * Ingests and returns aggregated charging locations in the Bratislava area (restricted to OC Retro for focused testing).
     */
    @GetMapping("/bratislava")
    @Operation(
            summary = "Ingest Bratislava stations",
            description = "Fetches live ZSE Drive stations for OC Retro (IDs 79480, 316067), parallelly loads details via "
                    + "virtual threads, and aggregates them into physical locations."
    )
    @ApiResponse(responseCode = "200", description = "List of aggregated charging locations in Bratislava")
    public ResponseEntity<List<ChargingLocation>> getBratislavaLocations() {
        List<ChargingLocation> locations = ingestionService.ingestStations(ZseDataRefreshScheduler.RETRO_STATION_IDS, null);
        return ResponseEntity.ok(locations);
    }

    /**
     * Triggers full refresh pipeline (ZSE API -> PostgreSQL DB + Redis status cache).
     */
    @PostMapping("/refresh")
    @Operation(
            summary = "Trigger daily refresh",
            description = "Fetches ZSE Drive stations, updates PostgreSQL database and synchronizes Redis live statuses."
    )
    @ApiResponse(responseCode = "200", description = "Refresh execution summary result")
    public ResponseEntity<RefreshResult> triggerRefresh() {
        RefreshResult result = refreshScheduler.executeRefresh();
        return ResponseEntity.ok(result);
    }

    /**
     * Triggers refresh for only OC Retro stations (ZSE API -> PostgreSQL DB + Redis status cache).
     */
    @PostMapping("/refresh/retro")
    @Operation(
            summary = "Trigger refresh for OC Retro",
            description = "Fetches live ZSE Drive stations for OC Retro (IDs 79480, 316067), updates PostgreSQL database and synchronizes Redis live statuses."
    )
    @ApiResponse(responseCode = "200", description = "Refresh execution summary result")
    public ResponseEntity<RefreshResult> triggerRetroRefresh() {
        RefreshResult result = refreshScheduler.refreshRetro();
        return ResponseEntity.ok(result);
    }

    /**
     * Triggers targeted refresh for a specific charging location (ZSE API -> PostgreSQL DB + Redis status cache).
     */
    @PostMapping("/refresh/location/{locationId}")
    @Operation(
            summary = "Trigger refresh for single location",
            description = "Fetches live ZSE Drive stations for the specified location ID, updates PostgreSQL database and synchronizes Redis live statuses."
    )
    @ApiResponse(responseCode = "200", description = "Refresh execution summary result")
    public ResponseEntity<RefreshResult> triggerLocationRefresh(
            @Parameter(description = "Location ID (e.g. zse-316067-79480)", example = "zse-316067-79480")
            @PathVariable String locationId
    ) {
        RefreshResult result = refreshScheduler.refreshLocation(locationId);
        return ResponseEntity.ok(result);
    }

    /**
     * Request payload for batch station/location refresh.
     */
    public record BatchRefreshRequest(List<String> locationIds, List<Long> stationIds) {
    }

    /**
     * Request payload for viewport GPS bounding box refresh.
     */
    public record ViewportRefreshRequest(
            Double north,
            Double west,
            Double south,
            Double east,
            Integer limit
    ) {
    }

    /**
     * Triggers viewport refresh (ZSE API -> PostgreSQL DB + Redis status cache) based on GPS bounding box.
     */
    @PostMapping("/refresh/viewport")
    @Operation(
            summary = "Trigger refresh by map viewport",
            description = "Fetches ZSE Drive stations in a GPS bounding box, updates PostgreSQL database and synchronizes Redis live statuses."
    )
    @ApiResponse(responseCode = "200", description = "Refresh execution summary result")
    public ResponseEntity<RefreshResult> triggerViewportRefresh(@RequestBody(required = false) ViewportRefreshRequest request) {
        if (request == null || request.north() == null || request.west() == null || request.south() == null || request.east() == null) {
            return ResponseEntity.badRequest().body(RefreshResult.failed(java.time.Instant.now(), 0, "Missing required GPS bounding box coordinates"));
        }
        ZseStationQuery.Bounds bounds = new ZseStationQuery.Bounds(request.north(), request.west(), request.south(), request.east());
        int limit = request.limit() != null ? request.limit() : 100;
        RefreshResult result = refreshScheduler.refreshViewport(bounds, limit);
        return ResponseEntity.ok(result);
    }

    /**
     * Triggers batch refresh for specific locations or stations (ZSE API -> PostgreSQL DB + Redis status cache).
     */
    @PostMapping("/refresh/batch")
    @Operation(
            summary = "Trigger batch refresh for specific locations or stations",
            description = "Fetches live ZSE Drive stations for given location or station IDs, updates PostgreSQL database and synchronizes Redis live statuses."
    )
    @ApiResponse(responseCode = "200", description = "Refresh execution summary result")
    public ResponseEntity<RefreshResult> triggerBatchRefresh(@RequestBody(required = false) BatchRefreshRequest request) {
        if (request == null || ((request.locationIds() == null || request.locationIds().isEmpty())
                && (request.stationIds() == null || request.stationIds().isEmpty()))) {
            return ResponseEntity.ok(refreshScheduler.executeRefresh());
        }
        RefreshResult result = refreshScheduler.refreshBatch(request.locationIds(), request.stationIds());
        return ResponseEntity.ok(result);
    }

    /**
     * Clears all persisted charging locations from PostgreSQL database and removes cached statuses from Redis.
     */
    @PostMapping("/clear")
    @Operation(
            summary = "Clear all data",
            description = "Deletes all charging locations and child entities from PostgreSQL database and clears Redis status cache."
    )
    @ApiResponse(responseCode = "200", description = "All data cleared successfully")
    public ResponseEntity<Void> clearAllData() {
        persistenceService.deleteAll();
        statusCache.clearAll();
        return ResponseEntity.ok().build();
    }

    /**
     * Returns all persisted charging locations from the database, enriched with current live Redis status.
     */
    @GetMapping("/persisted")
    @Operation(
            summary = "Get persisted locations",
            description = "Fetches all saved charging locations from PostgreSQL database and enriches connector states with live Redis statuses."
    )
    @ApiResponse(responseCode = "200", description = "List of persisted charging locations with live status")
    public ResponseEntity<List<ChargingLocation>> getPersistedLocations() {
        List<ChargingLocation> locations = persistenceService.findAll();
        List<ChargingLocation> enriched = statusCache.enrichAllWithLiveStatus(locations);
        return ResponseEntity.ok(enriched);
    }

    /**
     * Ingests and returns aggregated charging locations for custom bounding box.
     */
    @GetMapping("/viewport")
    @Operation(
            summary = "Ingest stations by viewport",
            description = "Fetches ZSE Drive stations in a custom GPS bounding box and returns aggregated locations."
    )
    @ApiResponse(responseCode = "200", description = "List of aggregated charging locations in viewport")
    public ResponseEntity<List<ChargingLocation>> getViewportLocations(
            @Parameter(description = "North latitude", example = "48.25") @RequestParam double north,
            @Parameter(description = "West longitude", example = "17.00") @RequestParam double west,
            @Parameter(description = "South latitude", example = "48.05") @RequestParam double south,
            @Parameter(description = "East longitude", example = "17.25") @RequestParam double east,
            @Parameter(description = "Max stations to fetch details for", example = "50")
            @RequestParam(defaultValue = "50") int limit
    ) {
        ZseStationQuery.Bounds bounds = new ZseStationQuery.Bounds(north, west, south, east);
        List<ChargingLocation> locations = ingestionService.ingestViewport(bounds, limit, null);
        return ResponseEntity.ok(locations);
    }

    /**
     * Fetches real-time mapped details for a specific single station ID.
     */
    @GetMapping("/stations/{stationId}")
    @Operation(
            summary = "Get single station details",
            description = "Fetches live details from ZSE Drive API for a single station ID and maps to domain model."
    )
    @ApiResponse(responseCode = "200", description = "Mapped provider station")
    @ApiResponse(responseCode = "404", description = "Station not found")
    public ResponseEntity<ProviderStation> getStation(
            @Parameter(description = "ZSE Station ID", example = "2145") @PathVariable Long stationId
    ) {
        ProviderStation station = ingestionService.fetchAndMapStation(stationId);
        if (station == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(station);
    }
}
