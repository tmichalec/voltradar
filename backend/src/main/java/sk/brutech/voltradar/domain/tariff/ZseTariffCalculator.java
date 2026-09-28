package sk.brutech.voltradar.domain.tariff;

import sk.brutech.voltradar.domain.model.Connector;
import sk.brutech.voltradar.domain.model.CurrentType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Deterministic pricing and time estimation engine for ZSE Drive charging sessions.
 */
public class ZseTariffCalculator {

    private static final BigDecimal AC_POWER_THRESHOLD = new BigDecimal("22.0");
    private static final BigDecimal DC_POWER_THRESHOLD = new BigDecimal("50.0");
    private static final BigDecimal MINUTES_IN_HOUR = new BigDecimal("60");

    public EffectiveCostResult calculate(
            Connector connector,
            UserProfile userProfile,
            ChargingSessionIntent intent
    ) {
        Objects.requireNonNull(connector, "connector must not be null");
        Objects.requireNonNull(userProfile, "userProfile must not be null");
        Objects.requireNonNull(intent, "intent must not be null");

        BigDecimal basePrice = resolveBasePrice(connector, userProfile.tariffProgram());

        BigDecimal discount = BigDecimal.ZERO;
        PhotovoltaicBenefit pvBenefit = userProfile.photovoltaicBenefit();
        if (pvBenefit != null && pvBenefit.isApplicableAt(intent.estimatedStartTime())) {
            discount = basePrice.multiply(pvBenefit.discountPercentage())
                    .setScale(4, RoundingMode.HALF_UP);
        }

        BigDecimal effectivePrice = basePrice.subtract(discount)
                .setScale(4, RoundingMode.HALF_UP);

        BigDecimal totalCost = intent.targetEnergyKwh().multiply(effectivePrice)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal effectivePower = resolveEffectivePower(connector, intent.maxVehicleAcceptanceKw());

        BigDecimal estimatedChargingMinutes = BigDecimal.ZERO;
        if (effectivePower.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal hours = intent.targetEnergyKwh().divide(effectivePower, 6, RoundingMode.HALF_UP);
            estimatedChargingMinutes = hours.multiply(MINUTES_IN_HOUR).setScale(1, RoundingMode.HALF_UP);
        }

        return new EffectiveCostResult(
                basePrice,
                discount,
                effectivePrice,
                totalCost,
                estimatedChargingMinutes,
                effectivePower
        );
    }

    public BigDecimal resolveBasePrice(Connector connector, ZseTariffProgram program) {
        ChargingCategory category = resolveCategory(connector);
        return switch (program) {
            case START -> switch (category) {
                case AC -> new BigDecimal("0.39");
                case DC -> new BigDecimal("0.49");
                case ULTRA -> new BigDecimal("0.59");
            };
            case PARTNER -> switch (category) {
                case AC -> new BigDecimal("0.29");
                case DC -> new BigDecimal("0.39");
                case ULTRA -> new BigDecimal("0.49");
            };
            case PREMIUM -> switch (category) {
                case AC -> new BigDecimal("0.24");
                case DC -> new BigDecimal("0.34");
                case ULTRA -> new BigDecimal("0.39");
            };
            case ECO -> switch (category) {
                case AC -> new BigDecimal("0.19");
                case DC -> new BigDecimal("0.29");
                case ULTRA -> new BigDecimal("0.39");
            };
            case GUEST -> switch (category) {
                case AC -> new BigDecimal("0.49");
                case DC -> new BigDecimal("0.59");
                case ULTRA -> new BigDecimal("0.69");
            };
        };
    }

    public ChargingCategory resolveCategory(Connector connector) {
        if (connector.currentType() == CurrentType.AC || connector.maxPowerKw().compareTo(AC_POWER_THRESHOLD) <= 0) {
            return ChargingCategory.AC;
        }
        if (connector.maxPowerKw().compareTo(DC_POWER_THRESHOLD) <= 0) {
            return ChargingCategory.DC;
        }
        return ChargingCategory.ULTRA;
    }

    private BigDecimal resolveEffectivePower(Connector connector, BigDecimal vehicleAcceptanceKw) {
        BigDecimal power = connector.powerSharing() != null
                && connector.powerSharing().effectiveAvailablePowerKw() != null
                ? connector.powerSharing().effectiveAvailablePowerKw()
                : connector.maxPowerKw();

        if (vehicleAcceptanceKw != null
                && vehicleAcceptanceKw.compareTo(BigDecimal.ZERO) > 0
                && vehicleAcceptanceKw.compareTo(power) < 0) {
            power = vehicleAcceptanceKw;
        }
        return power;
    }

    public enum ChargingCategory {
        AC,
        DC,
        ULTRA
    }
}
