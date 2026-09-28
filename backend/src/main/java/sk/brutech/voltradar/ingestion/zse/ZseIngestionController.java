package sk.brutech.voltradar.ingestion.zse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sk.brutech.voltradar.domain.model.ChargingLocation;
import sk.brutech.voltradar.domain.model.ProviderStation;

import java.util.List;

/**
 * REST controller providing endpoints to trigger and inspect ZSE Drive ingestion data.
 */
@RestController
@RequestMapping("/api/v1/ingestion/zse")
@Tag(name = "ZSE Ingestion", description = "Endpoints for fetching and aggregating ZSE Drive charging stations")
public class ZseIngestionController {
    private final ZseDataIngestionService ingestionService;

    public ZseIngestionController(ZseDataIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    /**
     * Ingests and returns aggregated charging locations in the Bratislava area.
     */
    @GetMapping("/bratislava")
    @Operation(
            summary = "Ingest Bratislava stations",
            description = "Fetches live ZSE Drive stations in Bratislava bounds, parallelly loads details via "
                    + "virtual threads, and aggregates them into physical locations."
    )
    @ApiResponse(responseCode = "200", description = "List of aggregated charging locations in Bratislava")
    public ResponseEntity<List<ChargingLocation>> getBratislavaLocations() {
        List<ChargingLocation> locations = ingestionService.ingestBratislava();
        return ResponseEntity.ok(locations);
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
