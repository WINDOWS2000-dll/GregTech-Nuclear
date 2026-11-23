package com.windows2000.gtnuclear.api.nuclear.thermal;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.is;

class WaterSaturationCurveTest {

    @Test
    void saturationPressureAtNormalBoilingPointIsOneAtmosphere() {
        double pressure = WaterSaturationCurve.saturationPressure(373.15);
        assertThat(pressure, closeTo(101325.0, 101325.0 * 0.01));
    }

    @Test
    void saturationTemperatureAtAtmosphericPressureIsNormalBoilingPoint() {
        // The floor a depressurized reactor now sits at (stage 1-2, 2026-11-21): ~373 K, not the triple point.
        double temperature = WaterSaturationCurve.saturationTemperature(
                WaterSaturationCurve.ATMOSPHERIC_PRESSURE_PASCALS);
        assertThat(temperature, closeTo(373.12, 0.2));
    }

    @Test
    void saturationPressureAtTypicalSteamTableCheckPointMatchesKnownValue() {
        // 300 degC (573.15 K) -> ~8.581 MPa is a standard steam-table reference value.
        double pressure = WaterSaturationCurve.saturationPressure(573.15);
        assertThat(pressure, closeTo(8.581e6, 8.581e6 * 0.02));
    }

    @Test
    void saturationTemperatureIsInverseOfSaturationPressure() {
        double temperature = 600.0;
        double pressure = WaterSaturationCurve.saturationPressure(temperature);
        double recovered = WaterSaturationCurve.saturationTemperature(pressure);
        assertThat(recovered, closeTo(temperature, 0.01));
    }

    @Test
    void isBoilingTrueWhenTemperatureExceedsSaturationForGivenPressure() {
        double pressure = WaterSaturationCurve.saturationPressure(573.15);
        assertThat(WaterSaturationCurve.isBoiling(600.0, pressure), is(true));
    }

    @Test
    void isBoilingFalseWhenSubcooledBelowSaturation() {
        // Typical PWR operating point: ~583 K primary coolant kept subcooled at ~15.5 MPa (saturation at that
        // pressure is well above 583 K), the entire point of pressurizing the primary loop.
        double pwrOperatingPressure = 15.5e6;
        assertThat(WaterSaturationCurve.isBoiling(583.0, pwrOperatingPressure), is(false));
    }
}
