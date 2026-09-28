package sk.brutech.voltradar.ingestion.zse;

import sk.brutech.voltradar.domain.model.ConnectorStatus;
import sk.brutech.voltradar.domain.model.ConnectorType;
import sk.brutech.voltradar.ingestion.zse.dto.ZseDriveDtos.Connector;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.regex.Pattern;

/** Conservative normalization; retain the wire DTO alongside these derived values. */
public final class ZseConnectorValues {
    private static final Pattern POWER = Pattern.compile("^\\s*([0-9]+(?:[.,][0-9]+)?)\\s*kW\\s*$");

    private ZseConnectorValues() {
    }

    public static ConnectorStatus status(String state) {
        if (state == null) {
            return ConnectorStatus.UNKNOWN;
        }
        return switch (state) {
            case "AVAILABLE" -> ConnectorStatus.AVAILABLE;
            case "BUSY", "RESERVED" -> ConnectorStatus.OCCUPIED;
            // DISCONNECTED does not establish that the charger is physically broken.
            default -> ConnectorStatus.UNKNOWN;
        };
    }

    public static Optional<ConnectorType> type(Connector connector) {
        if (connector.type() == null || connector.type().name() == null) {
            return Optional.empty();
        }
        // Numeric type IDs differ between the catalog and roaming station details.
        return switch (connector.type().name()) {
            case "CCS" -> Optional.of(ConnectorType.CCS);
            case "Mennekes Type 2" -> Optional.of(ConnectorType.TYPE_2);
            default -> Optional.empty();
        };
    }

    public static Optional<BigDecimal> maxPowerKw(Connector connector) {
        if (connector.pricing() == null) {
            return Optional.empty();
        }
        return connector.pricing().stream()
                .filter(line -> "Výkon".equals(line.name()) || "Power".equals(line.name()))
                .filter(line -> line.value() != null)
                .map(line -> POWER.matcher(line.value()))
                .filter(matcher -> matcher.matches())
                .map(matcher -> new BigDecimal(matcher.group(1).replace(',', '.')))
                .filter(value -> value.signum() > 0)
                .findFirst();
    }
}
