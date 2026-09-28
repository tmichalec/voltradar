package sk.brutech.voltradar.ingestion.zse.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.util.List;

/** Public ZSE wire formats. Nullable fields and raw provider codes are intentionally preserved. */
public final class ZseDriveDtos {
    private ZseDriveDtos() {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Location(BigDecimal lat, BigDecimal lon) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Address(String street, String number, String city, String zip, String country, String slug) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Viewport(Location northeast, Location southwest) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Place(String id, String name, BigDecimal lat, BigDecimal lon,
                        String formattedAddress, Viewport viewport) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Cluster(Location location, Integer count) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StationSummary(Long id, String name, String contractor, Address address, Location location,
                                 BigDecimal distance, Integer connectors, Integer freeConnectors,
                                 String exeStationType, Boolean isPaidParking) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StationsResponse(List<Place> places, List<Cluster> grid, List<StationSummary> list, Integer count) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Rating(BigDecimal average, Integer total) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ConnectorType(Long id, String name, String scheme, String socketType) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ConnectorTypesResponse(List<ConnectorType> connectorTypes) {
    }

    /** Includes power, energy price, parking price and grace period as localized display strings. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PriceLine(String name, String value) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record NightTariff(String name, BigDecimal amount, Integer starthour, Integer startminute,
                              Integer stophour, Integer stopminute) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Connector(Long id, @JsonProperty("evseID") String evseId, String state, ConnectorType type,
                            List<PriceLine> pricing, NightTariff nightTariff, Boolean subscribed) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Station(Long id, String name, @JsonProperty("remoteID") String remoteId,
                          @JsonProperty("evseID") String evseId, String note, String contractor,
                          JsonNode openHours, String cover, Boolean canUserCharge, Location location,
                          Rating rating, Address address, List<Connector> connectors,
                          String exeStationType, Boolean isPaidParking) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StationResponse(Station station) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Pricing(
            @JsonProperty("AC_CHARGING") BigDecimal acCharging,
            @JsonProperty("DC_CHARGING") BigDecimal dcCharging,
            @JsonProperty("UFC_CHARGING") BigDecimal ufcCharging,
            @JsonProperty("DRIVEX_CHARGING") BigDecimal driveXCharging,
            @JsonProperty("AC_CHARGING_NIGHT_TARIFF") NightTariff acNightTariff,
            @JsonProperty("DC_CHARGING_NIGHT_TARIFF") NightTariff dcNightTariff,
            @JsonProperty("UFC_CHARGING_NIGHT_TARIFF") NightTariff ufcNightTariff,
            @JsonProperty("DRIVEX_CHARGING_NIGHT_TARIFF") NightTariff driveXNightTariff,
            @JsonProperty("PARKING") BigDecimal parking,
            @JsonProperty("MONTHLY_FEE") BigDecimal monthlyFee,
            @JsonProperty("RFID_FEE") BigDecimal rfidFee,
            @JsonProperty("FOREIGN_RFID_ISSUE") BigDecimal foreignRfidIssue) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Program(String id, String name, BigDecimal freeMonthlyCharging, Boolean isPrivate,
                          Pricing pricing, String label, String description, String note,
                          Boolean isInsuranceConsentRequired, Boolean isEvidenceNumberRequired) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ProgramsResponse(List<Program> programs, List<Program> homePrograms, String addendum) {
    }
}
