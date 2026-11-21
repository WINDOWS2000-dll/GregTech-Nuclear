package com.windows2000.gtnuclear.api.nuclear.thermal;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;

class CoolantThermalSolverTest {

    private static final double MASS_KG = 1000.0;
    private static final double SPECIFIC_HEAT = CoolantThermalSolver.REPRESENTATIVE_SPECIFIC_HEAT_CAPACITY;

    @Test
    void positiveNetHeatIncreasesTemperature() {
        double newTemperature = CoolantThermalSolver.advance(573.15, 1.0e6, MASS_KG, SPECIFIC_HEAT, 1.0);
        assertThat(newTemperature, greaterThan(573.15));
    }

    @Test
    void negativeNetHeatDecreasesTemperature() {
        double newTemperature = CoolantThermalSolver.advance(573.15, -1.0e6, MASS_KG, SPECIFIC_HEAT, 1.0);
        assertThat(newTemperature, lessThan(573.15));
    }

    @Test
    void zeroNetHeatKeepsTemperatureConstant() {
        double newTemperature = CoolantThermalSolver.advance(573.15, 0.0, MASS_KG, SPECIFIC_HEAT, 1.0);
        assertThat(newTemperature, closeTo(573.15, 1e-9));
    }

    @Test
    void matchesClosedFormEnergyBalanceForOneSecondStep() {
        double heatWatts = 2.0e6;
        double dt = 1.0;
        double expected = 573.15 + dt * heatWatts / (MASS_KG * SPECIFIC_HEAT);

        double actual = CoolantThermalSolver.advance(573.15, heatWatts, MASS_KG, SPECIFIC_HEAT, dt);

        assertThat(actual, closeTo(expected, 1e-9));
    }

    @Test
    void zeroMassLeavesTemperatureUnchanged() {
        double newTemperature = CoolantThermalSolver.advance(573.15, 1.0e6, 0.0, SPECIFIC_HEAT, 1.0);
        assertThat(newTemperature, closeTo(573.15, 1e-9));
    }
}
