package sk.brutech.voltradar.domain.tariff;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Result of personalized tariff calculation for a specific charging connector.
 */
public record EffectiveCostResult(
        BigDecimal basePricePerKwh,
        BigDecimal discountAppliedPerKwh,
        BigDecimal effectivePricePerKwh,
        BigDecimal estimatedTotalCost,
        BigDecimal estimatedChargingMinutes,
        BigDecimal effectivePowerKw
) {
    public EffectiveCostResult {
        Objects.requireNonNull(basePricePerKwh, "basePricePerKwh must not be null");
        Objects.requireNonNull(discountAppliedPerKwh, "discountAppliedPerKwh must not be null");
        Objects.requireNonNull(effectivePricePerKwh, "effectivePricePerKwh must not be null");
        Objects.requireNonNull(estimatedTotalCost, "estimatedTotalCost must not be null");
        Objects.requireNonNull(estimatedChargingMinutes, "estimatedChargingMinutes must not be null");
        Objects.requireNonNull(effectivePowerKw, "effectivePowerKw must not be null");
    }
}
