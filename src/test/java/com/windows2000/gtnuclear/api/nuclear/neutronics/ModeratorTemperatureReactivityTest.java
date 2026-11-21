package com.windows2000.gtnuclear.api.nuclear.neutronics;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.lessThan;

class ModeratorTemperatureReactivityTest {

    private static final double EPSILON = 1e-9;
    private static final double AMBIENT_KELVIN = 300.0;
    private static final double REFERENCE_KELVIN = 565.0;
    private static final double REFERENCE_COEFFICIENT_PCM_PER_KELVIN = -30.0;

    @Test
    void atAmbientTemperatureReactivityIsZero() {
        double reactivity = ModeratorTemperatureReactivity.reactivityPcm(AMBIENT_KELVIN, AMBIENT_KELVIN,
                REFERENCE_KELVIN, REFERENCE_COEFFICIENT_PCM_PER_KELVIN);
        assertThat(reactivity, closeTo(0.0, EPSILON));
    }

    @Test
    void atReferenceTemperatureReactivityIsHalfOfFlatLinearModel() {
        double referenceSpanKelvin = REFERENCE_KELVIN - AMBIENT_KELVIN;
        double flatLinearValue = REFERENCE_COEFFICIENT_PCM_PER_KELVIN * referenceSpanKelvin;
        double reactivity = ModeratorTemperatureReactivity.reactivityPcm(REFERENCE_KELVIN, AMBIENT_KELVIN,
                REFERENCE_KELVIN, REFERENCE_COEFFICIENT_PCM_PER_KELVIN);
        assertThat(reactivity, closeTo(flatLinearValue / 2.0, Math.abs(flatLinearValue) * 1e-9));
    }

    @Test
    void positiveTemperatureRiseGivesNegativeReactivityForNegativeCoefficient() {
        double reactivity = ModeratorTemperatureReactivity.reactivityPcm(400.0, AMBIENT_KELVIN, REFERENCE_KELVIN,
                REFERENCE_COEFFICIENT_PCM_PER_KELVIN);
        assertThat(reactivity, lessThan(0.0));
    }

    @Test
    void reactivityScalesQuadraticallyNotLinearlyWithTemperatureRise() {
        // Quarter of the way from ambient to reference should give 1/16th of the reference-point value (a
        // quadratic curve), not 1/4 (which a flat-linear model would give instead).
        double quarterSpanKelvin = AMBIENT_KELVIN + (REFERENCE_KELVIN - AMBIENT_KELVIN) / 4.0;
        double atQuarterSpan = ModeratorTemperatureReactivity.reactivityPcm(quarterSpanKelvin, AMBIENT_KELVIN,
                REFERENCE_KELVIN, REFERENCE_COEFFICIENT_PCM_PER_KELVIN);
        double atFullReference = ModeratorTemperatureReactivity.reactivityPcm(REFERENCE_KELVIN, AMBIENT_KELVIN,
                REFERENCE_KELVIN, REFERENCE_COEFFICIENT_PCM_PER_KELVIN);
        assertThat(atQuarterSpan, closeTo(atFullReference / 16.0, Math.abs(atFullReference) * 1e-9));
    }

    @Test
    void degenerateReferenceSpanReturnsZero() {
        double reactivity = ModeratorTemperatureReactivity.reactivityPcm(400.0, AMBIENT_KELVIN, AMBIENT_KELVIN,
                REFERENCE_COEFFICIENT_PCM_PER_KELVIN);
        assertThat(reactivity, closeTo(0.0, EPSILON));
    }
}
