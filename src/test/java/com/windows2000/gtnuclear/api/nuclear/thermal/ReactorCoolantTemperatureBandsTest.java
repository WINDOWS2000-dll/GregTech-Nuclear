package com.windows2000.gtnuclear.api.nuclear.thermal;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class ReactorCoolantTemperatureBandsTest {

    private static final double[] BANDS = { 300.0, 350.0, 400.0, 450.0, 500.0 };

    @Test
    void exactMatchReturnsThatIndex() {
        assertThat(ReactorCoolantTemperatureBands.nearestBandIndex(BANDS, 400.0), is(2));
    }

    @Test
    void belowLowestBandClampsToFirstIndex() {
        assertThat(ReactorCoolantTemperatureBands.nearestBandIndex(BANDS, 0.0), is(0));
    }

    @Test
    void aboveHighestBandClampsToLastIndex() {
        assertThat(ReactorCoolantTemperatureBands.nearestBandIndex(BANDS, 10000.0), is(4));
    }

    @Test
    void midpointBetweenTwoBandsBreaksTowardLowerBand() {
        // Exactly between band 0 (300) and band 1 (350).
        assertThat(ReactorCoolantTemperatureBands.nearestBandIndex(BANDS, 325.0), is(0));
    }

    @Test
    void closerToUpperBandRoundsUp() {
        assertThat(ReactorCoolantTemperatureBands.nearestBandIndex(BANDS, 326.0), is(1));
    }

    @Test
    void realBandArrayHasNoDuplicatesAndIsSorted() {
        double[] bands = ReactorCoolantTemperatureBands.BAND_TEMPERATURES_KELVIN;
        for (int i = 1; i < bands.length; i++) {
            assertThat(bands[i] > bands[i - 1], is(true));
        }
    }
}
