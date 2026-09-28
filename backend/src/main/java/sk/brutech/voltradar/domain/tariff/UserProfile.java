package sk.brutech.voltradar.domain.tariff;

import java.util.Objects;

/**
 * Encapsulates the user's active billing contract, tariff program, and discount benefits.
 */
public record UserProfile(
        String userId,
        ZseTariffProgram tariffProgram,
        PhotovoltaicBenefit photovoltaicBenefit
) {
    public UserProfile {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(tariffProgram, "tariffProgram must not be null");
        if (photovoltaicBenefit == null) {
            photovoltaicBenefit = PhotovoltaicBenefit.none();
        }
    }

    public static UserProfile standardZseStartWithSolar(String userId) {
        return new UserProfile(userId, ZseTariffProgram.START, PhotovoltaicBenefit.standardZseVirtualBattery());
    }

    public static UserProfile standardZseStart(String userId) {
        return new UserProfile(userId, ZseTariffProgram.START, PhotovoltaicBenefit.none());
    }
}
