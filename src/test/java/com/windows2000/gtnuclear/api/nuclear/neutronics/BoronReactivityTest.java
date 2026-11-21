package com.windows2000.gtnuclear.api.nuclear.neutronics;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.lessThan;

class BoronReactivityTest {

    private static final double EPSILON = 1e-12;

    @Test
    void zeroConcentrationGivesZeroReactivity() {
        assertThat(BoronReactivity.reactivity(0.0, 1.0E-5), closeTo(0.0, EPSILON));
    }

    @Test
    void positiveConcentrationGivesNegativeReactivity() {
        assertThat(BoronReactivity.reactivity(1000.0, 1.0E-5), lessThan(0.0));
    }

    @Test
    void reactivityScalesLinearlyWithConcentration() {
        double worthPerPpm = 1.0E-5;
        double single = BoronReactivity.reactivity(500.0, worthPerPpm);
        double doubled = BoronReactivity.reactivity(1000.0, worthPerPpm);
        assertThat(doubled, closeTo(single * 2, Math.abs(single) * 1e-9));
    }
}
