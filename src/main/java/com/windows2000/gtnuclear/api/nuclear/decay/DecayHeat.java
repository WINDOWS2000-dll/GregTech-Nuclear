package com.windows2000.gtnuclear.api.nuclear.decay;

import org.jetbrains.annotations.NotNull;

/**
 * Fission product decay heat (2026-11-28, stage 2-3b of this mod's reactor physics rework, see this mod's design
 * memory): the ANSI/ANS-5.1-1979 23-group exponential fit for U-235 thermal fission (Table 7), used for every fuel
 * until per-nuclide data arrives with the fuel catalog. Source of the coefficients: the standard's Table 7 as
 * transcribed in github.com/ccclin/decay_heat_with_nuclear (lib/decay_heat_with_nuclear/thermal_data.rb), cross-checked
 * against NRC report ML050910161 Table 8-1 (WCOBRA/TRAC) and LANL NUREG/CR-0349; reproduces ~0.29% of fission
 * power 100 h after an 18-month cycle.
 * <p>
 * Model: each group {@code i} holds an energy inventory {@code E_i} (J) still to be released. A fission power
 * {@code P} feeds it at {@code P * alpha_i / (lambda_i * Q)} and it decays at {@code lambda'_i}, releasing
 * {@code lambda'_i * E_i} as heat. At equilibrium the groups together give {@link #EQUILIBRIUM_FRACTION} (~6.5%)
 * of {@code P}, which is exactly the share of fission energy that is <i>not</i> deposited promptly -- so the
 * reactor deposits {@code (1 - EQUILIBRIUM_FRACTION) * P} promptly plus the decay heat (decision 1).
 * <p>
 * Two clocks (decision of this mod's discussion point 4): groups with a real half-life under
 * {@link #SLOW_CLOCK_HALF_LIFE_THRESHOLD_SECONDS} decay in real time; the longer ones on the compressed slow clock
 * ({@code lambda' = s * lambda}). Their production is not scaled, so their equilibrium power is unchanged and their
 * build-up and decay are {@code s} times faster -- consistent with the compressed burnup -- while their total
 * post-shutdown energy is {@code 1/s} of reality.
 */
public final class DecayHeat {

    public static final int GROUP_COUNT = 23;
    /** Recoverable energy per fission used by the standard (MeV). */
    public static final double ENERGY_PER_FISSION_MEV = 200.0;
    /** Groups at or above this real half-life (s) run on the slow (compressed) clock. */
    public static final double SLOW_CLOCK_HALF_LIFE_THRESHOLD_SECONDS = 3600.0;

    /** alpha_i, MeV / (fission s). */
    private static final double[] ALPHA = { 6.5057E-01, 5.1264E-01, 2.4384E-01, 1.3850E-01, 5.5440E-02, 2.2225E-02,
            3.3088E-03, 9.3015E-04, 8.0943E-04, 1.9567E-04, 3.2535E-05, 7.5595E-06, 2.5232E-06, 4.9948E-07,
            1.8531E-07, 2.6608E-08, 2.2398E-09, 8.1641E-12, 8.7797E-11, 2.5131E-14, 3.2176E-16, 4.5038E-17,
            7.4791E-17 };
    /** lambda_i, 1/s. */
    private static final double[] LAMBDA = { 2.2138E+01, 5.1587E-01, 1.9594E-01, 1.0314E-01, 3.3656E-02, 1.1681E-02,
            3.5870E-03, 1.3930E-03, 6.2630E-04, 1.8906E-04, 5.4988E-05, 2.0958E-05, 1.0010E-05, 2.5438E-06,
            6.6361E-07, 1.2290E-07, 2.7213E-08, 4.3714E-09, 7.5780E-10, 2.4786E-10, 2.2384E-13, 2.4600E-14,
            1.5699E-14 };

    /** Equilibrium decay heat as a fraction of fission power, {@code sum alpha_i / (lambda_i Q)} (~0.0652). */
    public static final double EQUILIBRIUM_FRACTION;

    static {
        double sum = 0.0;
        for (int i = 0; i < GROUP_COUNT; i++) sum += ALPHA[i] / (LAMBDA[i] * ENERGY_PER_FISSION_MEV);
        EQUILIBRIUM_FRACTION = sum;
    }

    private DecayHeat() {}

    /** Whether group {@code i} runs on the slow clock. */
    public static boolean isSlowGroup(int i) {
        return Math.log(2) / LAMBDA[i] >= SLOW_CLOCK_HALF_LIFE_THRESHOLD_SECONDS;
    }

    /** Group {@code i}'s decay constant on its own clock (1/s of game time). */
    public static double effectiveDecayConstant(int i, double slowClockCompression) {
        return isSlowGroup(i) ? LAMBDA[i] * slowClockCompression : LAMBDA[i];
    }

    /**
     * Per-step coefficients for a fixed step, so the per-tick update of every node is only multiply-adds. The update
     * is the exact solution for constant power over the step, unconditionally stable and energy-conserving.
     */
    public static final class Stepper {

        private final double[] retain = new double[GROUP_COUNT];
        private final double[] gainPerWatt = new double[GROUP_COUNT];
        private final double[] powerPerJoule = new double[GROUP_COUNT];

        public Stepper(double dtSeconds, double slowClockCompression) {
            for (int i = 0; i < GROUP_COUNT; i++) {
                double lambda = effectiveDecayConstant(i, slowClockCompression);
                double retained = Math.exp(-lambda * dtSeconds);
                retain[i] = retained;
                gainPerWatt[i] = ALPHA[i] / (LAMBDA[i] * ENERGY_PER_FISSION_MEV) * (1.0 - retained) / lambda;
                powerPerJoule[i] = lambda;
            }
        }

        /**
         * Advances one node's groups ({@code energies[offset .. offset+22]}, J) by one step at fission power
         * {@code fissionWatts}.
         *
         * @return the node's decay heat (W) at the end of the step
         */
        public double advance(@NotNull double[] energies, int offset, double fissionWatts) {
            double power = 0.0;
            for (int i = 0; i < GROUP_COUNT; i++) {
                double e = energies[offset + i] * retain[i] + fissionWatts * gainPerWatt[i];
                energies[offset + i] = e;
                power += powerPerJoule[i] * e;
            }
            return power;
        }
    }

    /** Decay heat (W) currently released by one node's groups. */
    public static double power(@NotNull double[] energies, int offset, double slowClockCompression) {
        double power = 0.0;
        for (int i = 0; i < GROUP_COUNT; i++) {
            power += effectiveDecayConstant(i, slowClockCompression) * energies[offset + i];
        }
        return power;
    }

    /** Lets every group of every node decay for {@code elapsedSeconds} with no fission (out of the core). */
    public static void decayOnly(@NotNull double[] energies, double elapsedSeconds, double slowClockCompression) {
        if (elapsedSeconds <= 0) return;
        for (int j = 0; j + GROUP_COUNT <= energies.length; j += GROUP_COUNT) {
            for (int i = 0; i < GROUP_COUNT; i++) {
                energies[j + i] *= Math.exp(-effectiveDecayConstant(i, slowClockCompression) * elapsedSeconds);
            }
        }
    }

    /** Sum of {@link #power} over every node of {@code energies}. */
    public static double totalPower(@NotNull double[] energies, double slowClockCompression) {
        double total = 0.0;
        for (int j = 0; j + GROUP_COUNT <= energies.length; j += GROUP_COUNT) {
            total += power(energies, j, slowClockCompression);
        }
        return total;
    }
}
