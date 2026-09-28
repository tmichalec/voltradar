package sk.brutech.voltradar.ingestion.zse;

import sk.brutech.voltradar.domain.model.Address;
import sk.brutech.voltradar.domain.model.Connector;
import sk.brutech.voltradar.domain.model.CpoProvider;
import sk.brutech.voltradar.domain.model.CurrentType;
import sk.brutech.voltradar.domain.model.GeoCoordinates;
import sk.brutech.voltradar.domain.model.LiveStatus;
import sk.brutech.voltradar.domain.model.NormalizedConnectorType;
import sk.brutech.voltradar.domain.model.PowerSharingInfo;
import sk.brutech.voltradar.domain.model.ProviderStation;
import sk.brutech.voltradar.ingestion.zse.dto.ZseDriveDtos;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Maps raw ZSE Drive DTO payloads to VoltRadar domain models (ProviderStation, Connector, etc.).
 */
public final class ZseDtoMapper {
    private static final Pattern POWER_PATTERN = Pattern.compile("^\\s*([0-9]+(?:[.,][0-9]+)?)\\s*kW\\s*$");
    private static final Pattern PRICE_PATTERN =
            Pattern.compile("^\\s*([0-9]+(?:[.,][0-9]+)?)\\s*(?:€/kWh|EUR/kWh)\\s*$");

    private ZseDtoMapper() {
    }

    public static ProviderStation toProviderStation(ZseDriveDtos.Station station) {
        Objects.requireNonNull(station, "station DTO must not be null");
        Objects.requireNonNull(station.id(), "station id must not be null");

        String stationId = String.valueOf(station.id());
        String name = station.name() != null ? station.name().strip() : "ZSE Drive " + stationId;

        GeoCoordinates coordinates = extractCoordinates(station.location());
        Address address = extractAddress(station.address());

        List<Connector> domainConnectors = new ArrayList<>();
        if (station.connectors() != null) {
            for (ZseDriveDtos.Connector rawConnector : station.connectors()) {
                mapConnector(station, rawConnector).ifPresent(domainConnectors::add);
            }
        }

        return new ProviderStation(
                stationId,
                CpoProvider.ZSE_DRIVE,
                name,
                coordinates,
                address,
                station.exeStationType(),
                domainConnectors,
                station.toString()
        );
    }

    public static Optional<Connector> mapConnector(
            ZseDriveDtos.Station station,
            ZseDriveDtos.Connector rawConnector
    ) {
        if (rawConnector == null) {
            return Optional.empty();
        }

        Optional<NormalizedConnectorType> normalizedType = resolveConnectorType(rawConnector);
        if (normalizedType.isEmpty()) {
            return Optional.empty();
        }

        BigDecimal maxPower = resolveMaxPower(rawConnector, station.exeStationType());
        CurrentType currentType = resolveCurrentType(rawConnector, normalizedType.get(), maxPower);
        LiveStatus liveStatus = resolveLiveStatus(rawConnector.state());
        BigDecimal pricePerKwh = resolvePricePerKwh(rawConnector);

        String stationId = String.valueOf(station.id());
        Long connectorId = rawConnector.id() != null ? rawConnector.id() : 0L;
        UUID id = UUID.nameUUIDFromBytes(
                ("ZSE:" + stationId + ":" + connectorId).getBytes(StandardCharsets.UTF_8)
        );

        String evseId = rawConnector.evseId() != null
                ? String.valueOf(rawConnector.evseId())
                : (station.remoteId() != null ? station.remoteId() : stationId + "-" + connectorId);

        PowerSharingInfo powerSharing = PowerSharingInfo.unknown(maxPower);

        return Optional.of(new Connector(
                id,
                evseId,
                normalizedType.get(),
                currentType,
                maxPower,
                liveStatus,
                powerSharing,
                pricePerKwh,
                Instant.now()
        ));
    }

    public static Optional<NormalizedConnectorType> resolveConnectorType(ZseDriveDtos.Connector connector) {
        if (connector.type() == null || connector.type().name() == null) {
            return Optional.empty();
        }
        String typeName = connector.type().name().strip();
        if (typeName.equalsIgnoreCase("CCS")) {
            return Optional.of(NormalizedConnectorType.CCS);
        }
        if (typeName.equalsIgnoreCase("Mennekes Type 2") || typeName.equalsIgnoreCase("Type 2")) {
            return Optional.of(NormalizedConnectorType.TYPE_2);
        }
        return Optional.empty();
    }

    public static CurrentType resolveCurrentType(
            ZseDriveDtos.Connector connector,
            NormalizedConnectorType type,
            BigDecimal powerKw
    ) {
        if (connector.type() != null && connector.type().socketType() != null) {
            String socket = connector.type().socketType().strip();
            if (socket.equalsIgnoreCase("DC")) {
                return CurrentType.DC;
            }
            if (socket.equalsIgnoreCase("AC")) {
                return CurrentType.AC;
            }
        }
        if (type == NormalizedConnectorType.CCS) {
            return CurrentType.DC;
        }
        return powerKw.compareTo(new BigDecimal("22")) > 0 ? CurrentType.DC : CurrentType.AC;
    }

    public static BigDecimal resolveMaxPower(ZseDriveDtos.Connector connector, String exeStationType) {
        if (connector.pricing() != null) {
            for (ZseDriveDtos.PriceLine line : connector.pricing()) {
                if (line.name() != null && (line.name().equalsIgnoreCase("Výkon")
                        || line.name().equalsIgnoreCase("Power"))) {
                    if (line.value() != null) {
                        Matcher matcher = POWER_PATTERN.matcher(line.value());
                        if (matcher.matches()) {
                            return new BigDecimal(matcher.group(1).replace(',', '.'));
                        }
                    }
                }
            }
        }
        if (exeStationType != null) {
            if (exeStationType.equalsIgnoreCase("DriveX") || exeStationType.equalsIgnoreCase("Ultra")) {
                return new BigDecimal("150");
            }
            if (exeStationType.equalsIgnoreCase("City")) {
                return new BigDecimal("50");
            }
        }
        return new BigDecimal("22");
    }

    public static BigDecimal resolvePricePerKwh(ZseDriveDtos.Connector connector) {
        if (connector.pricing() != null) {
            for (ZseDriveDtos.PriceLine line : connector.pricing()) {
                if (line.name() != null && (line.name().equalsIgnoreCase("Cena")
                        || line.name().equalsIgnoreCase("Price"))) {
                    if (line.value() != null) {
                        Matcher matcher = PRICE_PATTERN.matcher(line.value());
                        if (matcher.matches()) {
                            return new BigDecimal(matcher.group(1).replace(',', '.'));
                        }
                    }
                }
            }
        }
        return null;
    }

    public static LiveStatus resolveLiveStatus(String state) {
        if (state == null) {
            return LiveStatus.UNKNOWN;
        }
        return switch (state.toUpperCase()) {
            case "AVAILABLE" -> LiveStatus.AVAILABLE;
            case "BUSY", "RESERVED", "CHARGING", "OCCUPIED" -> LiveStatus.OCCUPIED;
            case "OUT_OF_ORDER", "INOPERATIVE", "FAULTED" -> LiveStatus.OUT_OF_ORDER;
            default -> LiveStatus.UNKNOWN;
        };
    }

    private static GeoCoordinates extractCoordinates(ZseDriveDtos.Location location) {
        if (location == null || location.lat() == null || location.lon() == null) {
            return new GeoCoordinates(48.1486, 17.1077); // Default Bratislava centroid if missing
        }
        return new GeoCoordinates(location.lat().doubleValue(), location.lon().doubleValue());
    }

    private static Address extractAddress(ZseDriveDtos.Address address) {
        if (address == null) {
            return new Address("", "", "", "SK");
        }
        String street = address.street() != null ? address.street() : "";
        if (address.number() != null && !address.number().isBlank()) {
            street = (street + " " + address.number()).strip();
        }
        String city = address.city() != null ? address.city() : "";
        String zip = address.zip() != null ? address.zip() : "";
        String country = address.country() != null ? address.country() : "SK";
        return new Address(street, city, zip, country);
    }
}
