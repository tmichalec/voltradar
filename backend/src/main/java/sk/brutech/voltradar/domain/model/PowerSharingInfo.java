package sk.brutech.voltradar.domain.model;

import java.math.BigDecimal;

/**
 * Encapsulates dynamic and physical power sharing attributes for a connector.
 */
public record PowerSharingInfo(
        SharingStatus status,
        ConfidenceLevel confidence,
        BigDecimal advertisedPowerKw,
        BigDecimal totalStandPowerKw,
        BigDecimal effectiveAvailablePowerKw,
        int activeSessionsOnStand
) {
    public PowerSharingInfo {
        if (activeSessionsOnStand < 0) {
            throw new IllegalArgumentException("activeSessionsOnStand cannot be negative");
        }
    }

    public static PowerSharingInfo independent(BigDecimal powerKw) {
        return new PowerSharingInfo(
                SharingStatus.INDEPENDENT,
                ConfidenceLevel.CONFIRMED,
                powerKw,
                powerKw,
                powerKw,
                0
        );
    }

    public static PowerSharingInfo unknown(BigDecimal advertisedPowerKw) {
        return new PowerSharingInfo(
                SharingStatus.UNKNOWN,
                ConfidenceLevel.UNKNOWN,
                advertisedPowerKw,
                advertisedPowerKw,
                advertisedPowerKw,
                0
        );
    }
}
