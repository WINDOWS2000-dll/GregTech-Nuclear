package com.windows2000.gtnuclear.api.nuclear.thermal;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;

class RatedCoolantMassTest {

    private static final double EPSILON = 1e-9;

    @Test
    void scalesLinearlyWithInteriorCellCount() {
        assertThat(RatedCoolantMass.ratedMassKg(21, 50.0), closeTo(1050.0, EPSILON));
        assertThat(RatedCoolantMass.ratedMassKg(42, 50.0), closeTo(2100.0, EPSILON));
    }

    @Test
    void zeroCellsGivesZeroMass() {
        assertThat(RatedCoolantMass.ratedMassKg(0, 50.0), closeTo(0.0, EPSILON));
    }

    @Test
    void negativeCellCountDoesNotCrashAndGivesZero() {
        assertThat(RatedCoolantMass.ratedMassKg(-5, 50.0), closeTo(0.0, EPSILON));
    }
}
