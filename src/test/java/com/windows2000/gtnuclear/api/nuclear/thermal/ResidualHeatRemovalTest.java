package com.windows2000.gtnuclear.api.nuclear.thermal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResidualHeatRemovalTest {

    private static final double CP_PRIMARY = 5300.0;
    private static final double CP_COOLING = ResidualHeatRemoval.COOLING_WATER_SPECIFIC_HEAT;

    @Test
    void interlocks() {
        assertNull(ResidualHeatRemoval.startBlocker(450.0, 3.1, 450.0, 3.1));
        assertEquals(ResidualHeatRemoval.StartBlocker.TEMPERATURE_TOO_HIGH,
                ResidualHeatRemoval.startBlocker(451.0, 0.1, 450.0, 3.1));
        assertEquals(ResidualHeatRemoval.StartBlocker.PRESSURE_TOO_HIGH,
                ResidualHeatRemoval.startBlocker(400.0, 3.2, 450.0, 3.1));
        assertTrue(ResidualHeatRemoval.mustIsolate(4.3, 4.2));
        assertFalse(ResidualHeatRemoval.mustIsolate(4.2, 4.2));
    }

    @Test
    void effectivenessLimits() {
        assertEquals(0.0, ResidualHeatRemoval.counterflowEffectiveness(0.0, 0.5), 1e-12);
        // Balanced counterflow: NTU / (1 + NTU).
        assertEquals(0.5, ResidualHeatRemoval.counterflowEffectiveness(1.0, 1.0), 1e-9);
        // Cr = 0 (one side infinite): 1 - exp(-NTU).
        assertEquals(1.0 - Math.exp(-2.0), ResidualHeatRemoval.counterflowEffectiveness(2.0, 0.0), 1e-12);
        // Never above 1, approaching it for a huge exchanger.
        double big = ResidualHeatRemoval.counterflowEffectiveness(1.0e3, 0.8);
        assertTrue(big <= 1.0 && big > 0.999);
    }

    @Test
    void referencePlantDutyAtRhrEntry() {
        // Default config values at the reference plant: 480 kg/s primary, 600 kg/s cooling water, UA 6.2e5 W/K.
        ResidualHeatRemoval.Exchange exchange = ResidualHeatRemoval.exchange(450.0, 300.0, 480.0, 600.0, 6.2e5,
                CP_PRIMARY, CP_COOLING, 370.0);
        assertTrue(exchange.heatWatts() > 60e6 && exchange.heatWatts() < 90e6, "Q=" + exchange.heatWatts());
        assertFalse(exchange.outletCapped());
        assertEquals(300.0 + exchange.heatWatts() / (600.0 * CP_COOLING), exchange.coolingOutletKelvin(), 1e-9);
    }

    @Test
    void heatNeverExceedsWhatEitherStreamCanCarry() {
        ResidualHeatRemoval.Exchange exchange = ResidualHeatRemoval.exchange(400.0, 300.0, 10.0, 1.0e4, 1.0e9,
                CP_PRIMARY, CP_COOLING, 370.0);
        assertTrue(exchange.heatWatts() <= 10.0 * CP_PRIMARY * 100.0 + 1e-6);
    }

    @Test
    void outletCapLimitsHeatWhenCoolingWaterIsScarce() {
        ResidualHeatRemoval.Exchange exchange = ResidualHeatRemoval.exchange(450.0, 300.0, 480.0, 5.0, 6.2e5,
                CP_PRIMARY, CP_COOLING, 370.0);
        assertTrue(exchange.outletCapped());
        assertEquals(370.0, exchange.coolingOutletKelvin(), 1e-9);
        assertEquals(5.0 * CP_COOLING * 70.0, exchange.heatWatts(), 1e-6);
    }

    @Test
    void noHeatWithoutDrivingDifferenceOrFlow() {
        assertEquals(0.0, ResidualHeatRemoval.exchange(300.0, 300.0, 480, 600, 6.2e5, CP_PRIMARY, CP_COOLING, 370)
                .heatWatts());
        assertEquals(0.0, ResidualHeatRemoval.exchange(290.0, 300.0, 480, 600, 6.2e5, CP_PRIMARY, CP_COOLING, 370)
                .heatWatts());
        assertEquals(0.0, ResidualHeatRemoval.exchange(450.0, 300.0, 0, 600, 6.2e5, CP_PRIMARY, CP_COOLING, 370)
                .heatWatts());
        assertEquals(0.0, ResidualHeatRemoval.exchange(450.0, 300.0, 480, 0, 6.2e5, CP_PRIMARY, CP_COOLING, 370)
                .heatWatts());
        // Inlet already at the outlet limit: nothing can be absorbed.
        assertEquals(0.0, ResidualHeatRemoval.exchange(450.0, 370.0, 480, 600, 6.2e5, CP_PRIMARY, CP_COOLING, 370)
                .heatWatts());
    }

    @Test
    void coolingWaterBands() {
        double[] bands = CoolingWaterTemperatureBands.BAND_TEMPERATURES_KELVIN;
        assertEquals(15, bands.length);
        assertEquals(300.0, bands[0]);
        assertEquals(370.0, bands[bands.length - 1]);
        assertEquals(0, CoolingWaterTemperatureBands.nearestBandIndex(250.0));
        assertEquals(3, CoolingWaterTemperatureBands.nearestBandIndex(316.0));
        assertEquals(14, CoolingWaterTemperatureBands.nearestBandIndex(400.0));
    }
}
