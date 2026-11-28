package com.windows2000.gtnuclear.api.nuclear.decay;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** {@link DecayHeat} (stage 2-3b): ANS-5.1-1979 U-235 groups, exact stepping, the two clocks. */
class DecayHeatTest {

    private static final double MW = 1.0e6;

    /** Runs one node at {@code power} for {@code seconds}, then shut down for {@code after}; returns P_decay / P. */
    private static double fractionAfter(double seconds, double after, double compression, double dt) {
        DecayHeat.Stepper running = new DecayHeat.Stepper(dt, compression);
        double[] e = new double[DecayHeat.GROUP_COUNT];
        for (double t = 0; t < seconds; t += dt) running.advance(e, 0, MW);
        DecayHeat.decayOnly(e, after, compression);
        return DecayHeat.power(e, 0, compression) / MW;
    }

    @Test
    void equilibriumFractionMatchesTheStandard() {
        // sum alpha / (lambda Q) for the U-235 thermal table after infinitely long operation: ~6.6% of fission power
        // (an 18-month run reaches ~6.5%; the remainder sits in groups with half-lives of centuries and more).
        assertEquals(0.0659, DecayHeat.EQUILIBRIUM_FRACTION, 0.0005);
    }

    @Test
    void longRunReachesTheEquilibriumPower() {
        // At compression 1 the longest groups need ages to saturate; with the game's 52.2 the slow ones catch up.
        double f = fractionAfter(3.0e7 / 52.2, 0.0, 52.2, 1000.0);
        assertTrue(f > 0.060 && f < DecayHeat.EQUILIBRIUM_FRACTION + 1e-9, "f=" + f);
    }

    @Test
    void realClockShutdownCurveMatchesKnownValues() {
        // Real time (compression 1) after an 18-month run, compared with the standard's closed form values:
        // ~6.1% at 1 s, ~1.24% at 1 h, ~0.29% at 100 h.
        double run = 548 * 86400.0;
        assertEquals(0.0609, fractionAfter(run, 1.0, 1.0, 600.0), 0.002);
        assertEquals(0.0124, fractionAfter(run, 3600.0, 1.0, 600.0), 0.0005);
        assertEquals(0.00288, fractionAfter(run, 360000.0, 1.0, 600.0), 0.0002);
    }

    @Test
    void slowClockShortensTheLongTailOnly() {
        // On the game clocks the first seconds are real but 0.3% comes after ~1.9 h instead of ~86 h.
        double run = 548 * 86400.0 / 52.2;
        double oneSecond = fractionAfter(run, 1.0, 52.2, 60.0);
        assertEquals(0.0608, oneSecond, 0.002);
        assertTrue(fractionAfter(run, 1.9 * 3600.0, 52.2, 60.0) < 0.0031);
        assertTrue(fractionAfter(run, 1.0 * 3600.0, 52.2, 60.0) > 0.0035);
    }

    @Test
    void groupsUnderAnHourRunInRealTime() {
        assertFalse(DecayHeat.isSlowGroup(0));
        assertFalse(DecayHeat.isSlowGroup(8));   // ~18 min
        assertTrue(DecayHeat.isSlowGroup(9));    // ~61 min
        assertEquals(DecayHeat.effectiveDecayConstant(0, 52.2), DecayHeat.effectiveDecayConstant(0, 1.0));
        assertEquals(52.2 * DecayHeat.effectiveDecayConstant(9, 1.0), DecayHeat.effectiveDecayConstant(9, 52.2),
                1e-15);
    }

    @Test
    void saturatedGroupsReleaseExactlyTheNonPromptShare() {
        // With every group's clock sped up enormously, all of them saturate: decay power = f_d * P, i.e. the
        // (1 - f_d) prompt deposit plus decay heat adds back up to 100% of fission power.
        // 10 h saturates the real-clock groups (longest half-life ~18 min); the huge compression the slow ones.
        double f = fractionAfter(36000.0, 0.0, 1.0e14, 1.0);
        assertEquals(DecayHeat.EQUILIBRIUM_FRACTION, f, DecayHeat.EQUILIBRIUM_FRACTION * 1e-3);
    }

    @Test
    void decayOnlyHandlesEveryNodeAndIgnoresNonPositiveTime() {
        DecayHeat.Stepper stepper = new DecayHeat.Stepper(1.0, 52.2);
        double[] e = new double[DecayHeat.GROUP_COUNT * 3];
        for (int node = 0; node < 3; node++) {
            for (int i = 0; i < 1000; i++) stepper.advance(e, node * DecayHeat.GROUP_COUNT, MW);
        }
        double before = DecayHeat.totalPower(e, 52.2);
        DecayHeat.decayOnly(e, -5.0, 52.2);
        assertEquals(before, DecayHeat.totalPower(e, 52.2), 0.0);
        DecayHeat.decayOnly(e, 600.0, 52.2);
        assertTrue(DecayHeat.totalPower(e, 52.2) < before);
    }
}
