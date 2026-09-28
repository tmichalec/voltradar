package sk.brutech.voltradar.ingestion.zse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriBuilder;
import sk.brutech.voltradar.ingestion.CpoIngestionService;
import sk.brutech.voltradar.ingestion.zse.dto.ZseDriveDtos.*;

import java.net.URI;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

/** Read-only, unauthenticated API used by the public ZSE Drive web application. */
public final class ZseDriveClient implements CpoIngestionService<StationResponse, ProgramsResponse> {
    private static final Logger log = LoggerFactory.getLogger(ZseDriveClient.class);

    private final RestClient restClient;

    public ZseDriveClient(RestClient restClient) {
        this.restClient = Objects.requireNonNull(restClient);
    }

    public StationsResponse fetchStations(ZseStationQuery query) {
        Objects.requireNonNull(query);
        return get(builder -> {
            builder.path("/api/web/v1/stations").queryParam("limit", query.limit());
            if (query.query() != null) {
                builder.queryParam("query", "{search}");
            }
            if (query.bounds() != null) {
                var bounds = query.bounds();
                builder.queryParam("gpsNorthWestLat", bounds.north())
                        .queryParam("gpsNorthWestLon", bounds.west())
                        .queryParam("gpsSouthEastLat", bounds.south())
                        .queryParam("gpsSouthEastLon", bounds.east());
            }
            query.categories().forEach(value -> builder.queryParam("category[]", value));
            query.contractors().forEach(value -> builder.queryParam("contractor[]", value));
            return query.query() == null ? builder.build() : builder.build(query.query());
        }, StationsResponse.class, result -> result.list() != null && result.count() != null);
    }

    @Override
    public StationResponse fetchStation(String stationId) {
        if (stationId == null || !stationId.matches("[1-9][0-9]*")) {
            throw new IllegalArgumentException("Station ID must be a positive decimal identifier");
        }
        return get(builder -> builder.path("/api/v4.7/stations/{id}").build(stationId),
                StationResponse.class, result -> result.station() != null && result.station().id() != null
                        && result.station().connectors() != null);
    }

    public ConnectorTypesResponse fetchConnectorTypes() {
        return get(builder -> builder.path("/api/v4.2/connector-types").build(),
                ConnectorTypesResponse.class, result -> result.connectorTypes() != null);
    }

    @Override
    public ProgramsResponse fetchTariffs() {
        return fetchPrograms(true);
    }

    public ProgramsResponse fetchPrograms(boolean businessToConsumer) {
        return get(builder -> builder.path("/api/v4.7/program")
                        .queryParam("partial", true).queryParam("b2c", businessToConsumer).build(),
                ProgramsResponse.class, result -> result.programs() != null);
    }

    private <T> T get(Function<UriBuilder, URI> uri, Class<T> type, Predicate<T> valid) {
        try {
            T result = restClient.get().uri(uriBuilder -> {
                URI targetUri = uri.apply(uriBuilder);
                log.debug("HTTP GET -> ZSE Drive API: {}", targetUri);
                return targetUri;
            }).accept(MediaType.APPLICATION_JSON)
                    .header("language", "sk").header("Accept-Language", "sk")
                    .retrieve().body(type);
            if (result == null || !valid.test(result)) {
                throw new ZseDriveException("Missing required ZSE response envelope: " + type.getSimpleName(),
                        null, null);
            }
            return result;
        } catch (RestClientResponseException exception) {
            log.warn("ZSE API returned HTTP error {}: {}", exception.getStatusCode().value(), exception.getMessage());
            throw new ZseDriveException("ZSE API returned HTTP " + exception.getStatusCode().value(),
                    exception.getStatusCode().value(), exception);
        } catch (RestClientException exception) {
            log.warn("Cannot read ZSE API response: {}", exception.getMessage());
            throw new ZseDriveException("Cannot read ZSE API response", null, exception);
        }
    }
}
