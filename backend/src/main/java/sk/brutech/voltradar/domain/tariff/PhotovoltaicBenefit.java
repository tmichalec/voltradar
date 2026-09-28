package sk.brutech.voltradar.domain.tariff;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Objects;

/**
 * Photovoltaic / Virtual Battery benefit granting a discount during specific daily time windows.
 */
public record PhotovoltaicBenefit(
        boolean active,
        BigDecimal discountPercentage,
        LocalTime validFrom,
        LocalTime validTo
) {
    public PhotovoltaicBenefit {
        if (discountPercentage == null) {
            discountPercentage = BigDecimal.ZERO;
        }
        if (validFrom == null) {
            validFrom = LocalTime.of(6, 0);
        }
        if (validTo == null) {
            validTo = LocalTime.of(22, 0);
        }
    }

    /**
     * Standard ZSE photovoltaic / virtual battery benefit: 35% discount between 06:00 and 22:00.
     */
    public static PhotovoltaicBenefit standardZseVirtualBattery() {
        return new PhotovoltaicBenefit(true, new BigDecimal("0.35"), LocalTime.of(6, 0), LocalTime.of(22, 0));
    }

    public static PhotovoltaicBenefit none() {
        return new PhotovoltaicBenefit(false, BigDecimal.ZERO, LocalTime.MIN, LocalTime.MAX);
    }

    public boolean isApplicableAt(LocalTime time) {
        if (!active || discountPercentage.compareTo(BigDecimal.ZERO) <= 0 || time == null) {
            return false;
        }
        if (validFrom.isBefore(validTo)) {
            return !time.isBefore(validFrom) && time.isBefore(validTo);
        } else {
            // Spans overnight if validFrom > validTo
            return !time.isBefore(validFrom) || time.isBefore(validTo);
        }
    }
}
