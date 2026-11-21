package com.windows2000.gtnuclear.api.nuclear.accident;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class LOCADetectorTest {

    @Test
    void fullCoolantIsNotLoca() {
        assertThat(LOCADetector.isLOCA(1000.0, 1000.0, 0.7), is(false));
    }

    @Test
    void coolantBelowThresholdIsLoca() {
        assertThat(LOCADetector.isLOCA(600.0, 1000.0, 0.7), is(true));
    }

    @Test
    void coolantExactlyAtThresholdIsNotLoca() {
        assertThat(LOCADetector.isLOCA(700.0, 1000.0, 0.7), is(false));
    }

    @Test
    void zeroRatedMassNeverReportsLoca() {
        assertThat(LOCADetector.isLOCA(0.0, 0.0, 0.7), is(false));
    }
}
