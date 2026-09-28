package sk.brutech.voltradar.ingestion.zse;

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
public class ZseIngestionController {
    private final ZseDataIngestionService ingestionService;

    public ZseIngestionController(ZseDataIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    /**
     * Ingests and returns aggregated charging locations in the Bratislava area.
     */
    @GetMapping("/bratislava")
    public ResponseEntity<List<ChargingLocation>> getBratislavaLocations() {
        List<ChargingLocation> locations = ingestionService.ingestBratislava();
        return ResponseEntity.ok(locations);
    }

    /**
     * Ingests and returns aggregated charging locations for custom bounding box.
     */
    @GetMapping("/viewport")
    public ResponseEntity<List<ChargingLocation>> getViewportLocations(
            @RequestParam double north,
            @RequestParam double west,
            @RequestParam double south,
            @RequestParam double east,
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
    public ResponseEntity<ProviderStation> getStation(@PathVariable Long stationId) {
        ProviderStation station = ingestionService.fetchAndMapStation(stationId);
        if (station == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(station);
    }
}
