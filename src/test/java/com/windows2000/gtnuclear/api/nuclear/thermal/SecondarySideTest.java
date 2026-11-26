package com.windows2000.gtnuclear.api.nuclear.thermal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** {@link SecondarySideBoiling} and {@link SecondaryPressureControl} (stage 1-4b). */
class SecondarySideTest {

    private static final double CP = CoolantThermalSolver.REPRESENTATIVE_SPECIFIC_HEAT_CAPACITY;
    private static final double L = SteamProperties.LATENT_HEAT_OF_VAPORIZATION_J_PER_KG;
    private static final double T_SET = WaterSaturationCurve.saturationTemperature(7.0e6);
    private static final double T_RELIEF = WaterSaturationCurve.saturationTemperature(8.3e6);

    private static double energyIn(double massKg, double kelvin) {
        return massKg * CP * (kelvin - 300.0);
    }

    private static double energyOut(double massKg, SecondarySideBoiling.Result r) {
        return energyIn(r.waterMassKg(), r.waterTemperatureKelvin()) + r.exportJoules() + r.dumpJoules() +
                r.reliefJoules();
    }

    @Test
    void subcooledWaterDoesNotBoil() {
        SecondarySideBoiling.Result r = SecondarySideBoiling.boil(2000, T_SET - 5, T_SET, 1e9, 1e9, T_RELIEF, CP, L);
        assertEquals(T_SET - 5, r.waterTemperatureKelvin(), 1e-12);
        assertEquals(0.0, r.exportedKg() + r.dumpedKg() + r.reliefKg());
    }

    @Test
    void steamGoesToExportFirstThenDump() {
        double candidate = T_SET + 20;
        SecondarySideBoiling.Result r = SecondarySideBoiling.boil(2000, candidate, T_SET, 10.0, 1e9, T_RELIEF, CP, L);
        assertEquals(10.0, r.exportedKg(), 1e-9);
        assertTrue(r.dumpedKg() > 0);
        assertEquals(T_SET, r.waterTemperatureKelvin(), 1e-9);
        assertEquals(energyIn(2000, candidate), energyOut(2000, r), 1e-3);
    }

    @Test
    void heatThatCannotLeaveStaysInTheWater() {
        // Before stage 1-4b this energy was deleted (water clamped to saturation).
        double candidate = T_SET + 3;
        SecondarySideBoiling.Result r = SecondarySideBoiling.boil(2000, candidate, T_SET, 0.0, 0.0, T_RELIEF, CP, L);
        assertEquals(candidate, r.waterTemperatureKelvin(), 1e-9);
        assertEquals(2000, r.waterMassKg(), 1e-12);
        assertEquals(energyIn(2000, candidate), energyOut(2000, r), 1e-3);
    }

    @Test
    void partialOutletKeepsRemainderAsSensibleHeat() {
        double candidate = T_SET + 10;
        SecondarySideBoiling.Result r = SecondarySideBoiling.boil(2000, candidate, T_SET, 5.0, 3.0, T_RELIEF, CP, L);
        assertEquals(5.0, r.exportedKg(), 1e-9);
        assertEquals(3.0, r.dumpedKg(), 1e-9);
        assertTrue(r.waterTemperatureKelvin() > T_SET && r.waterTemperatureKelvin() < candidate);
        assertEquals(energyIn(2000, candidate), energyOut(2000, r), 1e-3);
    }

    @Test
    void safetyValvesCapTheTemperature() {
        double candidate = T_RELIEF + 15;
        SecondarySideBoiling.Result r = SecondarySideBoiling.boil(2000, candidate, T_SET, 0.0, 0.0, T_RELIEF, CP, L);
        assertTrue(r.reliefKg() > 0);
        assertEquals(T_RELIEF, r.waterTemperatureKelvin(), 1e-9);
        assertEquals(energyIn(2000, candidate), energyOut(2000, r), 1e-3);
    }

    @Test
    void dryOutKeepsTheBalance() {
        double candidate = T_SET + 5000;
        SecondarySideBoiling.Result r = SecondarySideBoiling.boil(10, candidate, T_SET, 1e9, 0, T_RELIEF, CP, L);
        assertEquals(0.0, r.waterMassKg(), 1e-12);
        assertEquals(energyIn(10, candidate), energyOut(10, r), 1e-3);
    }

    @Test
    void setpointRisesImmediatelyButFallsAtTheRateLimit() {
        assertEquals(T_SET, SecondaryPressureControl.rampSetpointSaturationKelvin(500.0, T_SET, 55.6, 0.05), 1e-12);

        double k = T_SET;
        double target = SecondaryPressureControl.MIN_SETPOINT_SATURATION_KELVIN;
        for (int i = 0; i < 20 * 3600; i++)
            k = SecondaryPressureControl.rampSetpointSaturationKelvin(k, target, 55.6, 0.05);
        assertEquals(55.6, T_SET - k, 1e-6);
    }

    @Test
    void setpointReachesTargetAndNeverGoesBelowAtmospheric() {
        double k = 400.0;
        for (int i = 0; i < 20 * 3600 * 4; i++)
            k = SecondaryPressureControl.rampSetpointSaturationKelvin(k, 250.0, 55.6, 0.05);
        assertEquals(SecondaryPressureControl.MIN_SETPOINT_SATURATION_KELVIN, k, 1e-9);
        assertEquals(SecondaryPressureControl.MIN_SETPOINT_SATURATION_KELVIN,
                SecondaryPressureControl.saturationKelvinOf(0.0), 1e-12);
    }

    @Test
    void zeroRateHoldsTheSetpoint() {
        assertEquals(T_SET, SecondaryPressureControl.rampSetpointSaturationKelvin(T_SET, 400.0, 0.0, 0.05), 1e-12);
    }

    /**
     * Stiffness check required whenever a forward-Euler thermal coupling changes (this mod's design memory): with the
     * secondary no longer clamped at saturation, the default UA (1.6833e8 W/K) against a 2000 kg secondary gives
     * UA*dt/C = ~0.79 per tick. The two nodes must approach each other monotonically, without overshoot.
     */
    @Test
    void primarySecondaryCouplingDoesNotOvershoot() {
        double ua = 1.6833e8;
        double dt = 0.05;
        double primaryMass = 272224.0;
        double secondaryMass = 2000.0;
        double tp = 600.0;
        double ts = T_SET;
        double previousGap = tp - ts;
        for (int i = 0; i < 200; i++) {
            double q = ua * Math.max(0.0, tp - ts);
            tp = CoolantThermalSolver.advance(tp, -q, primaryMass, CP, dt);
            double candidate = CoolantThermalSolver.advance(ts, q, secondaryMass, CP, dt);
            SecondarySideBoiling.Result r = SecondarySideBoiling.boil(secondaryMass, candidate, T_SET, 0, 0, 1e9,
                    CP, L);
            ts = r.waterTemperatureKelvin();
            double gap = tp - ts;
            assertTrue(gap >= -1e-9, "overshoot at tick " + i + ": gap " + gap);
            assertTrue(gap <= previousGap + 1e-9, "gap grew at tick " + i);
            previousGap = gap;
        }
    }
}
