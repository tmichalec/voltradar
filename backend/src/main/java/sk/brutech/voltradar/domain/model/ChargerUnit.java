package sk.brutech.voltradar.domain.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Represents a physical charging dispenser/stand on a location.
 * May host multiple connectors/EVSEs that share total physical power.
 */
public record ChargerUnit(
        String id,
        String label,
        ConfidenceLevel confidence,
        SharingStatus sharingStatus,
        BigDecimal totalPowerKw,
        List<String> evseIds,
        String verifiedBy,
        String notes
) {
    public ChargerUnit {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(confidence, "confidence must not be null");
        Objects.requireNonNull(sharingStatus, "sharingStatus must not be null");
        evseIds = evseIds != null ? List.copyOf(evseIds) : List.of();
    }
}
