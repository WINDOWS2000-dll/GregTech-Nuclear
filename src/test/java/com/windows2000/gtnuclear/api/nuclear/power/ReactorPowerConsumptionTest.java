package com.windows2000.gtnuclear.api.nuclear.power;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;

class ReactorPowerConsumptionTest {

    private static final double BASE_COEFFICIENT = 2.0;
    private static final double PUMP_COEFFICIENT = 512.0;
    private static final double EPSILON = 1e-9;

    @Test
    void onlyBaselineDrawnWhenStoppedAtZeroThermalOutput() {
        double euPerTick = ReactorPowerConsumption.euPerTick(10, 0.0, 1000.0, BASE_COEFFICIENT, PUMP_COEFFICIENT);
        assertThat(euPerTick, closeTo(BASE_COEFFICIENT * 10, EPSILON));
    }

    @Test
    void fullPumpLoadDrawnAtRatedOutput() {
        double euPerTick = ReactorPowerConsumption.euPerTick(10, 1000.0, 1000.0, BASE_COEFFICIENT, PUMP_COEFFICIENT);
        assertThat(euPerTick, closeTo(BASE_COEFFICIENT * 10 + PUMP_COEFFICIENT, EPSILON));
    }

    @Test
    void halfPumpLoadDrawnAtHalfRatedOutput() {
        double euPerTick = ReactorPowerConsumption.euPerTick(10, 500.0, 1000.0, BASE_COEFFICIENT, PUMP_COEFFICIENT);
        assertThat(euPerTick, closeTo(BASE_COEFFICIENT * 10 + PUMP_COEFFICIENT * 0.5, EPSILON));
    }

    @Test
    void scalesLinearlyWithChannelCount() {
        double single = ReactorPowerConsumption.euPerTick(1, 0.0, 1000.0, BASE_COEFFICIENT, PUMP_COEFFICIENT);
        double quadruple = ReactorPowerConsumption.euPerTick(4, 0.0, 1000.0, BASE_COEFFICIENT, PUMP_COEFFICIENT);
        assertThat(quadruple, closeTo(single * 4, EPSILON));
    }

    @Test
    void zeroRatedOutputDoesNotCrashAndDrawsOnlyBaseline() {
        double euPerTick = ReactorPowerConsumption.euPerTick(10, 500.0, 0.0, BASE_COEFFICIENT, PUMP_COEFFICIENT);
        assertThat(euPerTick, closeTo(BASE_COEFFICIENT * 10, EPSILON));
    }
}
