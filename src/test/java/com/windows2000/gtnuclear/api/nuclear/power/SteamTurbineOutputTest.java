package com.windows2000.gtnuclear.api.nuclear.power;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SteamTurbineOutputTest {

    private static final double TICK_SECONDS = 1.0 / 20.0;

    @Test
    void maxThroughputScalesLinearlyWithRotorPower() {
        assertEquals(42083.33, SteamTurbineOutput.maxThroughputMbPerTick(42083.33, 100.0), 1e-2);
        assertEquals(21041.665, SteamTurbineOutput.maxThroughputMbPerTick(42083.33, 50.0), 1e-2);
        assertEquals(0.0, SteamTurbineOutput.maxThroughputMbPerTick(42083.33, 0.0), 1e-9);
    }

    @Test
    void stageEfficiencyFractionMatchesTemperatureDropRatio() {
        // HP: 560K -> 480K out of an overall 560K -> 300K drop (80/260 =~ 30.77%).
        double hpFraction = SteamTurbineOutput.stageEfficiencyFraction(0.33, 560.0, 480.0, 560.0, 300.0);
        assertEquals(0.33 * 80.0 / 260.0, hpFraction, 1e-9);

        // LP: 480K -> 300K out of the same overall drop (180/260 =~ 69.23%).
        double lpFraction = SteamTurbineOutput.stageEfficiencyFraction(0.33, 480.0, 300.0, 560.0, 300.0);
        assertEquals(0.33 * 180.0 / 260.0, lpFraction, 1e-9);

        // The two stages' own fractions sum back to the total -- no energy gained or lost by splitting.
        assertEquals(0.33, hpFraction + lpFraction, 1e-9);
    }

    @Test
    void stageEfficiencyFractionIsZeroWhenOverallDropIsNotPositive() {
        assertEquals(0.0, SteamTurbineOutput.stageEfficiencyFraction(0.33, 560.0, 480.0, 300.0, 300.0), 1e-9);
        assertEquals(0.0, SteamTurbineOutput.stageEfficiencyFraction(0.33, 560.0, 480.0, 300.0, 560.0), 1e-9);
    }

    @Test
    void electricalOutputMatchesReferenceReactorCalibration() {
        // The reference reactor (1010 interior cells * 5.0E6 W/cell = 5050 MWth) produces 168,333.33 mB/t of
        // HighPressureSteam at full power -- see this mod's design memory for the derivation. Feeding all of it
        // through a single combined-efficiency stage (0.33) at a 100% rotor should reproduce the 1666.5 MWe
        // total this mod's generation-side conversion constant (13,332 W per EU/t) was itself calibrated against.
        double totalElectricalWatts = SteamTurbineOutput.electricalOutputWatts(168333.33, 0.33, 100.0, TICK_SECONDS);
        assertEquals(1.6665e9, totalElectricalWatts, 1.0e6);
    }

    @Test
    void electricalOutputIsZeroWithNoSteamConsumed() {
        assertEquals(0.0, SteamTurbineOutput.electricalOutputWatts(0.0, 0.33, 100.0, TICK_SECONDS), 1e-9);
    }

    @Test
    void euPerTickDividesByTheConfiguredConversion() {
        assertEquals(125000.0, SteamTurbineOutput.euPerTick(1.6665e9, 13332.0), 1.0);
    }

    @Test
    void euPerTickIsZeroWithNonPositiveConversion() {
        assertEquals(0.0, SteamTurbineOutput.euPerTick(1.0e9, 0.0), 1e-9);
    }
}
