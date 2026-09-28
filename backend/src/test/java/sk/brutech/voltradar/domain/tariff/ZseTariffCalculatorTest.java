package sk.brutech.voltradar.domain.tariff;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sk.brutech.voltradar.domain.model.ConfidenceLevel;
import sk.brutech.voltradar.domain.model.Connector;
import sk.brutech.voltradar.domain.model.CurrentType;
import sk.brutech.voltradar.domain.model.LiveStatus;
import sk.brutech.voltradar.domain.model.NormalizedConnectorType;
import sk.brutech.voltradar.domain.model.PowerSharingInfo;
import sk.brutech.voltradar.domain.model.SharingStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ZseTariffCalculatorTest {

    private final ZseTariffCalculator calculator = new ZseTariffCalculator();

    @Test
    @DisplayName("ZSE START with 35% Photovoltaic discount on DC 50 kW charger during daytime")
    void testStartProgramWithSolarDiscountDaytime() {
        Connector dcConnector = createConnector("SK*ZSE*E100*1", CurrentType.DC, new BigDecimal("50.0"));
        UserProfile user = UserProfile.standardZseStartWithSolar("user-1");
        ChargingSessionIntent intent = new ChargingSessionIntent(
                new BigDecimal("30.0"),
                LocalTime.of(14, 0),
                null
        );

        EffectiveCostResult result = calculator.calculate(dcConnector, user, intent);

        assertThat(result.basePricePerKwh()).isEqualByComparingTo("0.49");
        assertThat(result.discountAppliedPerKwh()).isEqualByComparingTo("0.1715");
        assertThat(result.effectivePricePerKwh()).isEqualByComparingTo("0.3185");
        assertThat(result.estimatedTotalCost()).isEqualByComparingTo("9.56");
        assertThat(result.estimatedChargingMinutes()).isEqualByComparingTo("36.0");
        assertThat(result.effectivePowerKw()).isEqualByComparingTo("50.0");
    }

    @Test
    @DisplayName("ZSE START with Photovoltaic benefit outside active window (at 23:00) receives zero discount")
    void testStartProgramWithSolarDiscountNighttime() {
        Connector dcConnector = createConnector("SK*ZSE*E100*1", CurrentType.DC, new BigDecimal("50.0"));
        UserProfile user = UserProfile.standardZseStartWithSolar("user-1");
        ChargingSessionIntent intent = new ChargingSessionIntent(
                new BigDecimal("30.0"),
                LocalTime.of(23, 0),
                null
        );

        EffectiveCostResult result = calculator.calculate(dcConnector, user, intent);

        assertThat(result.basePricePerKwh()).isEqualByComparingTo("0.49");
        assertThat(result.discountAppliedPerKwh()).isEqualByComparingTo("0.0000");
        assertThat(result.effectivePricePerKwh()).isEqualByComparingTo("0.4900");
        assertThat(result.estimatedTotalCost()).isEqualByComparingTo("14.70");
    }

    @Test
    @DisplayName("Ultra-fast 150 kW charging with vehicle acceptance limit of 100 kW")
    void testUltraFastWithVehicleAcceptanceLimit() {
        Connector ufcConnector = createConnector("SK*ZSE*E200*1", CurrentType.DC, new BigDecimal("150.0"));
        UserProfile user = UserProfile.standardZseStartWithSolar("user-1");
        ChargingSessionIntent intent = new ChargingSessionIntent(
                new BigDecimal("50.0"),
                LocalTime.of(10, 0),
                new BigDecimal("100.0")
        );

        EffectiveCostResult result = calculator.calculate(ufcConnector, user, intent);

        assertThat(result.basePricePerKwh()).isEqualByComparingTo("0.59");
        assertThat(result.discountAppliedPerKwh()).isEqualByComparingTo("0.2065");
        assertThat(result.effectivePricePerKwh()).isEqualByComparingTo("0.3835");
        assertThat(result.estimatedTotalCost()).isEqualByComparingTo("19.18");
        assertThat(result.effectivePowerKw()).isEqualByComparingTo("100.0");
        assertThat(result.estimatedChargingMinutes()).isEqualByComparingTo("30.0");
    }

    @Test
    @DisplayName("Power sharing scenario limits effective available charging power")
    void testPowerSharingReduction() {
        PowerSharingInfo sharing = new PowerSharingInfo(
                SharingStatus.SHARED,
                ConfidenceLevel.CONFIRMED,
                new BigDecimal("150.0"),
                new BigDecimal("150.0"),
                new BigDecimal("75.0"),
                1
        );
        Connector sharedConnector = new Connector(
                UUID.randomUUID(),
                "SK*ZSE*E300*1",
                NormalizedConnectorType.CCS,
                CurrentType.DC,
                new BigDecimal("150.0"),
                LiveStatus.AVAILABLE,
                sharing,
                new BigDecimal("0.59"),
                Instant.now()
        );

        UserProfile user = UserProfile.standardZseStart("user-1");
        ChargingSessionIntent intent = new ChargingSessionIntent(
                new BigDecimal("75.0"),
                LocalTime.of(12, 0),
                null
        );

        EffectiveCostResult result = calculator.calculate(sharedConnector, user, intent);

        assertThat(result.effectivePowerKw()).isEqualByComparingTo("75.0");
        assertThat(result.estimatedChargingMinutes()).isEqualByComparingTo("60.0");
    }

    @Test
    @DisplayName("AC charging category rate resolution across programs")
    void testAcRatesResolution() {
        Connector acConnector = createConnector("SK*ZSE*E400*1", CurrentType.AC, new BigDecimal("22.0"));

        assertThat(calculator.resolveBasePrice(acConnector, ZseTariffProgram.START))
                .isEqualByComparingTo("0.39");
        assertThat(calculator.resolveBasePrice(acConnector, ZseTariffProgram.PARTNER))
                .isEqualByComparingTo("0.29");
        assertThat(calculator.resolveBasePrice(acConnector, ZseTariffProgram.PREMIUM))
                .isEqualByComparingTo("0.24");
        assertThat(calculator.resolveBasePrice(acConnector, ZseTariffProgram.ECO))
                .isEqualByComparingTo("0.19");
        assertThat(calculator.resolveBasePrice(acConnector, ZseTariffProgram.GUEST))
                .isEqualByComparingTo("0.49");
    }

    private Connector createConnector(String evseId, CurrentType currentType, BigDecimal powerKw) {
        return new Connector(
                UUID.randomUUID(),
                evseId,
                currentType == CurrentType.AC ? NormalizedConnectorType.TYPE_2 : NormalizedConnectorType.CCS,
                currentType,
                powerKw,
                LiveStatus.AVAILABLE,
                PowerSharingInfo.independent(powerKw),
                null,
                Instant.now()
        );
    }
}
