package com.windows2000.gtnuclear.api.nuclear.neutronics;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.lessThan;

class ControlRodWorthTest {

    private static final double EPSILON = 1e-12;

    @Test
    void fullyWithdrawnGivesZeroReactivity() {
        assertThat(ControlRodWorth.reactivity(0.0, 0.003), closeTo(0.0, EPSILON));
    }

    @Test
    void fullyInsertedGivesFullNegativeWorth() {
        assertThat(ControlRodWorth.reactivity(1.0, 0.003), closeTo(-0.003, EPSILON));
    }

    @Test
    void midTravelGivesHalfOfFullWorth() {
        assertThat(ControlRodWorth.reactivity(0.5, 0.003), closeTo(-0.0015, EPSILON));
    }

    @Test
    void differentialWorthIsGreatestNearMidTravel() {
        double maxWorth = 0.003;
        double stepNearMiddle = ControlRodWorth.reactivity(0.55, maxWorth) - ControlRodWorth.reactivity(0.45, maxWorth);
        double stepNearBottom = ControlRodWorth.reactivity(0.1, maxWorth) - ControlRodWorth.reactivity(0.0, maxWorth);
        double stepNearTop = ControlRodWorth.reactivity(1.0, maxWorth) - ControlRodWorth.reactivity(0.9, maxWorth);
        assertThat(Math.abs(stepNearBottom), lessThan(Math.abs(stepNearMiddle)));
        assertThat(Math.abs(stepNearTop), lessThan(Math.abs(stepNearMiddle)));
    }

    @Test
    void insertionDepthIsClampedToValidRange() {
        assertThat(ControlRodWorth.reactivity(-0.5, 0.003), closeTo(ControlRodWorth.reactivity(0.0, 0.003), EPSILON));
        assertThat(ControlRodWorth.reactivity(1.5, 0.003), closeTo(ControlRodWorth.reactivity(1.0, 0.003), EPSILON));
    }
}
