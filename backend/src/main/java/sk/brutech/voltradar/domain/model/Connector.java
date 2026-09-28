package sk.brutech.voltradar.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Normalized representation of an individual physical charging plug/socket (EVSE).
 */
public record Connector(
        UUID id,
        String evseId,
        NormalizedConnectorType type,
        CurrentType currentType,
        BigDecimal maxPowerKw,
        LiveStatus liveStatus,
        PowerSharingInfo powerSharing,
        BigDecimal publicPricePerKwh,
        Instant lastStatusUpdate
) {
    public Connector {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(evseId, "evseId must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(currentType, "currentType must not be null");
        Objects.requireNonNull(maxPowerKw, "maxPowerKw must not be null");
        Objects.requireNonNull(liveStatus, "liveStatus must not be null");
        Objects.requireNonNull(powerSharing, "powerSharing must not be null");
    }
}
