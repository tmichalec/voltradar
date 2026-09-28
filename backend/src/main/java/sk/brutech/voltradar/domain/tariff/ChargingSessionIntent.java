package sk.brutech.voltradar.domain.tariff;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Objects;

/**
 * Parameters representing the user's intended charging session request.
 */
public record ChargingSessionIntent(
        BigDecimal targetEnergyKwh,
        LocalTime estimatedStartTime,
        BigDecimal maxVehicleAcceptanceKw
) {
    public ChargingSessionIntent {
        Objects.requireNonNull(targetEnergyKwh, "targetEnergyKwh must not be null");
        if (targetEnergyKwh.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("targetEnergyKwh must be positive");
        }
        if (estimatedStartTime == null) {
            estimatedStartTime = LocalTime.now();
        }
    }

    public static ChargingSessionIntent simple(BigDecimal targetEnergyKwh, LocalTime startTime) {
        return new ChargingSessionIntent(targetEnergyKwh, startTime, null);
    }
}
