package com.windows2000.gtnuclear.api.nuclear.neutronics;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.is;

class FluxWeightedAverageTest {

    private static final double EPSILON = 1e-9;

    @Test
    void uniformValuesReturnThatValueRegardlessOfWeightDistribution() {
        double[] values = { 7.0, 7.0, 7.0, 7.0 };
        double[] weights = { 0.1, 50.0, 3.3, 1.0 };

        assertThat(FluxWeightedAverage.average(values, weights), closeTo(7.0, EPSILON));
    }

    @Test
    void equalWeightsReduceToPlainAverage() {
        double[] values = { 2.0, 4.0, 6.0 };
        double[] weights = { 1.0, 1.0, 1.0 };

        assertThat(FluxWeightedAverage.average(values, weights), closeTo(4.0, EPSILON));
    }

    @Test
    void higherWeightPullsAverageTowardThatValue() {
        double[] values = { 0.0, 100.0 };
        double[] weights = { 1.0, 9.0 };

        // 90% weight on 100.0, 10% on 0.0 -> 90.0
        assertThat(FluxWeightedAverage.average(values, weights), closeTo(90.0, EPSILON));
    }

    @Test
    void emptyArraysReturnZero() {
        assertThat(FluxWeightedAverage.average(new double[0], new double[0]), is(0.0));
    }

    @Test
    void allZeroWeightsReturnZeroRatherThanDividingByZero() {
        double[] values = { 5.0, 10.0 };
        double[] weights = { 0.0, 0.0 };

        assertThat(FluxWeightedAverage.average(values, weights), is(0.0));
    }

    @Test
    void multiplyingByCountRecoversPlainSumWhenWeightsAreEqual() {
        // The documented "weighted sum for discrete items" use (e.g. control rods): average(...) * N must
        // equal the plain sum whenever every weight is equal, reproducing the pre-existing flat-sum model.
        double[] values = { -100.0, -200.0, -150.0 };
        double[] weights = { 4.0, 4.0, 4.0 };

        double weightedSumNormalized = FluxWeightedAverage.average(values, weights) * values.length;

        assertThat(weightedSumNormalized, closeTo(-450.0, EPSILON));
    }

    @Test
    void mismatchedLengthsThrow() {
        try {
            FluxWeightedAverage.average(new double[] { 1.0 }, new double[] { 1.0, 2.0 });
            throw new AssertionError("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }
}
