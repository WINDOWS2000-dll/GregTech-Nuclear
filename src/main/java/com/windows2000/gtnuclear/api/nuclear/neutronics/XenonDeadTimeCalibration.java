package com.windows2000.gtnuclear.api.nuclear.neutronics;

import org.jetbrains.annotations.NotNull;

import java.util.function.DoubleUnaryOperator;

/**
 * Derives the linear time-scaling factor applied to the real I-135/Xe-135 decay constants so that the
 * config-configured "xenon dead time" (in real seconds, {@code ConfigHolder.reactor.xenonDeadTimeSeconds}) matches
 * the emergent dead time computed under reference conditions, using this mod's real, unscaled nuclear data. See
 * this mod's design memory for why a linear (not power-law, unlike
 * {@link com.windows2000.gtnuclear.api.nuclear.decay.TimeCompression})
 * scaling was chosen here -- I-135 and Xe-135's real half-lives (6.57h, 9.14h) are close enough that a linear
 * scale suffices, unlike the ~21-order-of-magnitude spread {@code TimeCompression} has to handle.
 * <p>
 * "Dead time" is defined as the time after an instantaneous full-power-to-zero-power shutdown for xenon reactivity
 * worth to decay back down to its pre-shutdown (equilibrium) value -- the standard practical restart-availability
 * criterion (before that point, xenon worth exceeds its steady-state value, i.e. the "iodine pit").
 * <p>
 * Because every rate constant in the {@link XenonIodineSolver} model (iodine decay, xenon decay, xenon burnup --
 * all tied to the same physical {@code lambda_I}/{@code lambda_X} via the fixed ratios in
 * {@link XenonIodineConstants}) scales uniformly by this factor, the resulting dead time scales by exactly the
 * inverse: doubling every rate constant halves how long every transient takes, including this one. That lets the
 * real-physics dead time be computed once -- a fixed number, independent of any configured target -- and the
 * scaling factor derived by simple division rather than iterative re-solving per config value.
 */
public final class XenonDeadTimeCalibration {

    /** Real (unscaled) I-135 decay constant (s^-1): ln2 / 6.57 hours. */
    public static final double REAL_IODINE_DECAY_CONSTANT = Math.log(2) / (6.57 * 3600.0);
    /** Real (unscaled) Xe-135 decay constant (s^-1): ln2 / 9.14 hours. */
    public static final double REAL_XENON_DECAY_CONSTANT = Math.log(2) / (9.14 * 3600.0);

    private static final double BISECTION_TOLERANCE_SECONDS = 1.0;

    private static final double REAL_DEAD_TIME_SECONDS = computeRealDeadTimeSeconds();

    private XenonDeadTimeCalibration() {}

    /**
     * @return the reference (real-physics, unscaled) dead time in seconds -- exposed for diagnostics/testing, not
     *         normally needed by callers (use {@link #scalingFactorFor} directly instead).
     */
    public static double realDeadTimeSeconds() {
        return REAL_DEAD_TIME_SECONDS;
    }

    /**
     * @return the multiplier applied to the real decay constants so the emergent reference dead time matches
     *         {@code configuredDeadTimeSeconds}.
     */
    public static double scalingFactorFor(double configuredDeadTimeSeconds) {
        return REAL_DEAD_TIME_SECONDS / configuredDeadTimeSeconds;
    }

    /**
     * Step size for {@link #estimateRemainingDeadTimeSeconds}'s forward lookahead -- coarse, since it only backs a
     * display estimate, not a gameplay gate (this mod's design memory explicitly treats dead time as an emergent
     * simulation result, not something artificially gated).
     */
    private static final double LOOKAHEAD_STEP_SECONDS = 60.0;

    /**
     * Console display estimate (stage 6 of this mod's startup/shutdown sequence work, 2026-09-21) of how much
     * longer the "iodine pit" will last, by forward-simulating {@link XenonIodineSolver} from {@code currentState}
     * assuming zero neutron population from here on (a reasonable approximation once the reactor is actually
     * shut down/tripped, which is the only time this estimate is shown) until xenon reactivity worth decays back
     * to {@code equilibriumXenonWorth}. Deliberately just a UI estimate, not a precondition gate on the console's
     * 'startup' command -- see this mod's design memory (2026-09-17): the real dead time is whatever falls out of
     * the actual simulation (reactivity margin, rod position), which this class cannot know in advance any better
     * than by literally running the simulation forward, so nothing here blocks an actual restart attempt.
     *
     * @param maxLookaheadSeconds gives up and returns this value if the pit hasn't cleared by then (keeps the
     *                            loop bounded for a reactor stuck at rated-power-equivalent xenon for some reason)
     * @return 0 if already at or below equilibrium (no pit), otherwise the estimated remaining seconds
     */
    public static double estimateRemainingDeadTimeSeconds(@NotNull XenonIodineState currentState,
                                                          @NotNull XenonIodineConstants constants,
                                                          double equilibriumXenonWorth, double maxLookaheadSeconds) {
        if (currentState.xenonConcentration() <= equilibriumXenonWorth) return 0.0;
        XenonIodineState state = currentState;
        double elapsed = 0.0;
        while (elapsed < maxLookaheadSeconds) {
            state = XenonIodineSolver.advance(state, 0.0, LOOKAHEAD_STEP_SECONDS, constants);
            elapsed += LOOKAHEAD_STEP_SECONDS;
            if (state.xenonConcentration() <= equilibriumXenonWorth) return elapsed;
        }
        return maxLookaheadSeconds;
    }

    private static double computeRealDeadTimeSeconds() {
        double lambdaI = REAL_IODINE_DECAY_CONSTANT;
        double lambdaX = REAL_XENON_DECAY_CONSTANT;
        double sigmaEff = XenonIodineConstants.BURNUP_TO_DECAY_RATIO_AT_RATED_POWER * lambdaX;
        double directFraction = XenonIodineConstants.DIRECT_XENON_FRACTION;

        // The equilibrium ratio I_eq/X_eq is independent of the (arbitrary) worth magnitude W, so W=1 is used as
        // an arbitrary normalization -- the crossing time back to X_eq does not depend on this choice.
        double xEq = 1.0;
        double iEq = (1 - directFraction) * (lambdaX + sigmaEff) / lambdaI * xEq;

        // Post-shutdown (n=0) transient, starting from full-power equilibrium (I(0)=iEq, X(0)=xEq):
        // I(t) = iEq * exp(-lambdaI*t)
        // X(t) = dCoefficient*exp(-lambdaX*t) + coefficient*exp(-lambdaI*t)
        // where coefficient/dCoefficient come from solving dX/dt = lambdaI*I(t) - lambdaX*X with X(0)=xEq.
        double coefficient = lambdaI * iEq / (lambdaX - lambdaI);
        double dCoefficient = xEq - coefficient;

        // f(t) = X(t) - xEq: zero at the trivial root t=0, then positive through the post-shutdown xenon peak,
        // then negative forever after crossing back down through xEq -- the root sought here.
        DoubleUnaryOperator f = t -> dCoefficient * Math.exp(-lambdaX * t) + coefficient * Math.exp(-lambdaI * t) - xEq;

        double t = 1.0;
        while (f.applyAsDouble(t) > 0) {
            t *= 2;
        }
        double upper = t;
        double lower = t / 2;
        while (upper - lower > BISECTION_TOLERANCE_SECONDS) {
            double mid = (lower + upper) / 2;
            if (f.applyAsDouble(mid) > 0) {
                lower = mid;
            } else {
                upper = mid;
            }
        }
        return (lower + upper) / 2;
    }
}
