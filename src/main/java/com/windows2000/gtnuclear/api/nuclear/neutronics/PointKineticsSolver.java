package com.windows2000.gtnuclear.api.nuclear.neutronics;

import org.jetbrains.annotations.NotNull;

/**
 * Standard point kinetics equations:
 * 
 * <pre>
 *   dn/dt   = ((rho - beta) / Lambda) * n + sum_i(lambda_i * C_i)
 *   dC_i/dt = (beta_i / Lambda) * n - lambda_i * C_i
 * </pre>
 * 
 * where {@code n} is the relative neutron population (proportional to reactor power), {@code rho} the total
 * reactivity ({@code (k-1)/k} from the last {@link CMFDSolver} shape solve, plus feedback terms such as
 * {@code XenonIodineState#reactivity} and {@code BoronReactivity} that this mod updates every step without
 * re-running the expensive shape solve -- the "amplitude" half of the Quasi-Static method), {@code Lambda} the
 * prompt neutron generation time, and {@code C_i}/{@code beta_i}/{@code lambda_i} the delayed neutron precursor
 * groups from {@link PointKineticsConstants}.
 * <p>
 * <b>Integration method:</b> backward Euler (fully implicit), with the precursor equations eliminated in closed
 * form before solving the single remaining scalar equation for {@code n} -- avoiding a general matrix solve while
 * still being numerically well-behaved regardless of the ratio between the step size and the prompt neutron
 * lifetime (~1e-5 s). This stability is required here: Minecraft's tick-scale time steps are many orders of
 * magnitude larger than the prompt lifetime, making this system numerically stiff enough that explicit methods
 * (forward Euler, RK4) would need an infeasibly small step size to remain stable.
 * <p>
 * <b>Sub-stepping (2026-09-22):</b> backward Euler's implicit coefficient for the growth term is
 * {@code 1 - dt*(rho-beta)/Lambda}. For strongly prompt-supercritical reactivity (small cores, fresh HEU, all
 * rods withdrawn) this can flip sign within a single 0.05s tick -- confirmed via a real playtest, reactivity
 * +26795 pcm never producing any power despite a valid neutron source seed -- silently producing a negative
 * {@code newPopulation} that the existing {@code Math.max(..., 0.0)} clamp then floors to exactly zero forever,
 * masking the runaway rather than modeling it. {@link #advance} is therefore a driver that subdivides
 * {@code dtSeconds} into however many equal sub-steps keep each one's own growth factor bounded by
 * {@link #MAX_STEP_GROWTH_FACTOR}, calling the original single-step math ({@link #advanceStep}) that many times.
 * Sub-critical/near-critical steps need only 1 sub-step (identical to the old unconditional single-step
 * behavior), so this changes nothing for normal operation -- only strongly supercritical excursions get
 * subdivided.
 */
public final class PointKineticsSolver {

    /**
     * Upper bound on {@code dtSubSeconds * (reactivity-beta)/lifetime} per sub-step, keeping backward Euler's
     * {@code 1 - dtSub*(rho-beta)/Lambda} coefficient safely positive (>= 1 - this value) so it can never sign-flip
     * regardless of how extreme the reactivity is. 0.5 is a conservative margin, not a precisely derived bound.
     */
    private static final double MAX_STEP_GROWTH_FACTOR = 0.5;

    private PointKineticsSolver() {}

    /** As {@link #advance(PointKineticsState, double, double, PointKineticsConstants, double)} with no source. */
    @NotNull
    public static PointKineticsState advance(@NotNull PointKineticsState state, double reactivity, double dtSeconds,
                                             @NotNull PointKineticsConstants constants) {
        return advance(state, reactivity, dtSeconds, constants, 0.0);
    }

    /**
     * @param sourceRate external neutron source in relative units per second ({@code dn/dt += S}, stage 2-1 of this
     *                   mod's reactor physics rework, see {@link NeutronSourceStrength}): it keeps a subcritical core
     *                   at {@code n = S * Lambda / -rho} instead of decaying toward zero, which is both the physical
     *                   source-range count rate and what lets a reactor start from a true-zero population.
     */
    @NotNull
    public static PointKineticsState advance(@NotNull PointKineticsState state, double reactivity, double dtSeconds,
                                             @NotNull PointKineticsConstants constants, double sourceRate) {
        double growthRate = (reactivity - constants.totalDelayedNeutronFraction) / constants.promptNeutronLifetime;
        int subSteps = 1;
        if (growthRate > 0) {
            double subStepLimitSeconds = MAX_STEP_GROWTH_FACTOR / growthRate;
            subSteps = Math.max(1, (int) Math.ceil(dtSeconds / subStepLimitSeconds));
        }
        double subDtSeconds = dtSeconds / subSteps;

        PointKineticsState result = state;
        for (int i = 0; i < subSteps; i++) {
            result = advanceStep(result, reactivity, subDtSeconds, constants, sourceRate);
        }
        return result;
    }

    @NotNull
    private static PointKineticsState advanceStep(@NotNull PointKineticsState state, double reactivity,
                                                  double dtSeconds, @NotNull PointKineticsConstants constants,
                                                  double sourceRate) {
        int groups = constants.groupCount();
        double lifetime = constants.promptNeutronLifetime;
        double beta = constants.totalDelayedNeutronFraction;

        // Each precursor group's implicit update, C_i^new = (C_i^old + dt*(beta_i/Lambda)*n^new) / (1+lambda_i*dt),
        // is linear in the not-yet-known n^new. Substituting that into the neutron population's own implicit
        // update and collecting terms turns the whole (groups+1)-equation implicit system into one scalar equation
        // for n^new -- these two accumulators are exactly that substitution's coefficient and constant term.
        double precursorFeedbackCoefficient = 0.0;
        double precursorCarryoverTerm = 0.0;
        for (int i = 0; i < groups; i++) {
            double lambda = constants.precursorDecayConstants[i];
            double betaI = constants.delayedNeutronFractions[i];
            double denominator = 1 + lambda * dtSeconds;
            precursorFeedbackCoefficient += (lambda * betaI) / (lifetime * denominator);
            precursorCarryoverTerm += lambda * state.precursorConcentrations[i] / denominator;
        }

        double coefficient = 1 - dtSeconds * (reactivity - beta) / lifetime -
                dtSeconds * dtSeconds * precursorFeedbackCoefficient;
        // The source enters the population's implicit update as a constant term (dt * S on the right-hand side).
        double newPopulation = (state.relativeNeutronPopulation + dtSeconds * precursorCarryoverTerm +
                dtSeconds * Math.max(0.0, sourceRate)) / coefficient;

        double[] newPrecursors = new double[groups];
        for (int i = 0; i < groups; i++) {
            double lambda = constants.precursorDecayConstants[i];
            double betaI = constants.delayedNeutronFractions[i];
            newPrecursors[i] = (state.precursorConcentrations[i] + dtSeconds * (betaI / lifetime) * newPopulation) /
                    (1 + lambda * dtSeconds);
        }

        return new PointKineticsState(Math.max(newPopulation, 0.0), newPrecursors);
    }
}
