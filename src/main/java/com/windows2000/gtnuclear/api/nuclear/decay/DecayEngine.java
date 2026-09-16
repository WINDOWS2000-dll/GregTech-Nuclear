package com.windows2000.gtnuclear.api.nuclear.decay;

import gregtech.api.unification.DecayMode;
import gregtech.api.unification.Element;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.DoubleUnaryOperator;

/**
 * Pure-Java (no Minecraft/world dependencies) engine that advances a mixture of nuclides forward in time using the
 * generalized, branching-aware Bateman equations, walking whatever decay graph is reachable via
 * {@link Element#decayModes}. Mirrors the "simulation core has no MC dependency" pattern noted from researching
 * SymmetricDevs/Supercritical (see this mod's design memory).
 * <p>
 * <b>Why storage only needs a snapshot, not a history:</b> the decay ODEs are memoryless/time-invariant -- the
 * future evolution of a mixture depends only on the <i>current</i> amount of every nuclide in it, not on how those
 * amounts came to be. So a caller only needs to persist {@code {nuclide -> current amount}} plus a single "last
 * advanced to" timestamp: advancing to a later time, or folding in a freshly-inserted batch (by decaying the
 * existing pool up to "now" and then adding the fresh amounts on top), are both just calls to {@link #advance}.
 * <p>
 * <b>Branching:</b> the classic Bateman formula assumes a single unbranched chain. This engine generalizes it by
 * enumerating every simple path from a source nuclide to each reachable descendant (a path may cross one or more
 * {@link DecayMode} branch points), applying the standard linear-chain formula along each path (weighted by the
 * product of that path's branching ratios), and summing contributions from all paths and all source nuclides --
 * valid because the decay ODE system is linear, so distinct sources/paths superpose.
 * <p>
 * <b>Units:</b> "amount" is whatever abstract unit the caller chooses to be consistent about (e.g. 1 unit per
 * inserted item) -- this engine does no unit conversion, it just scales linearly.
 * <p>
 * <b>In-game time compression:</b> {@code elapsedSeconds} is always real-world physical time -- this class stays
 * physically pure and knows nothing about compression itself. The {@link #advance(Map, double, DoubleUnaryOperator)}
 * overload accepts a {@code halfLifeCompressor} applied to each {@link Element#halfLifeSeconds} before computing its
 * decay constant (equivalent to compressing elapsed time itself, just applied at the one place that's actually
 * per-nuclide rather than global); the plain {@link #advance(Map, double)} overload is real-world-accurate
 * (identity compressor) and is what this class's own unit tests use. See
 * {@code com.windows2000.gtnuclear.api.nuclear.decay.TimeCompression} for this mod's actual compression policy.
 */
public final class DecayEngine {

    /** Amounts below this are pruned from the result to keep the map from accumulating floating-point noise. */
    private static final double PRUNE_THRESHOLD = 1e-12;

    private DecayEngine() {}

    /**
     * As {@link #advance(Map, double, DoubleUnaryOperator)}, using real-world half-lives unchanged (identity
     * compressor). This is what this class's own unit tests use.
     *
     * @param composition    current amounts, keyed by nuclide. Entries for stable/zero-amount nuclides may be
     *                       omitted.
     * @param elapsedSeconds physical time to advance by; must be non-negative.
     * @return a new map with every nuclide reachable from the input (including the inputs themselves) mapped to
     *         its amount after {@code elapsedSeconds}, with negligible (< 1e-12) entries pruned.
     */
    @NotNull
    public static Map<Element, Double> advance(@NotNull Map<Element, Double> composition, double elapsedSeconds) {
        return advance(composition, elapsedSeconds, DoubleUnaryOperator.identity());
    }

    /**
     * @param composition       current amounts, keyed by nuclide. Entries for stable/zero-amount nuclides may be
     *                          omitted.
     * @param elapsedSeconds    real-world physical time to advance by; must be non-negative.
     * @param halfLifeCompressor applied to each {@link Element#halfLifeSeconds} before computing that nuclide's
     *                          decay constant (see this class's own JavaDoc re: time compression). Only ever
     *                          called with positive values (stable nuclides' -1 is never passed through it).
     * @return a new map with every nuclide reachable from the input (including the inputs themselves) mapped to
     *         its amount after {@code elapsedSeconds}, with negligible (< 1e-12) entries pruned.
     */
    @NotNull
    public static Map<Element, Double> advance(@NotNull Map<Element, Double> composition, double elapsedSeconds,
                                               @NotNull DoubleUnaryOperator halfLifeCompressor) {
        if (elapsedSeconds < 0) {
            throw new IllegalArgumentException("elapsedSeconds must be non-negative, was " + elapsedSeconds);
        }
        Map<Element, Double> result = new HashMap<>();
        if (elapsedSeconds == 0) {
            result.putAll(composition);
            return result;
        }
        for (Map.Entry<Element, Double> entry : composition.entrySet()) {
            double initialAmount = entry.getValue();
            if (initialAmount <= 0) continue;
            accumulatePaths(entry.getKey(), initialAmount, 1.0, new ArrayList<>(), elapsedSeconds,
                    halfLifeCompressor, result);
        }
        result.values().removeIf(amount -> amount < PRUNE_THRESHOLD);
        return result;
    }

    /**
     * Depth-first walk of the decay graph starting at {@code node}, extending {@code lambdaPath} (the chain of
     * decay constants from the original source down to {@code node}) by one at each step. At every node visited
     * (including the very first, a length-1 "path"), the Bateman contribution to that node's amount is computed
     * and merged into {@code output}; the walk then continues into each of {@code node}'s decay branches (skipped
     * entirely if {@code node} is stable).
     */
    private static void accumulatePaths(@NotNull Element node, double initialAmount, double branchProduct,
                                        @NotNull List<Double> lambdaPath, double elapsedSeconds,
                                        @NotNull DoubleUnaryOperator halfLifeCompressor,
                                        @NotNull Map<Element, Double> output) {
        double lambda = decayConstant(node, halfLifeCompressor);
        List<Double> path = new ArrayList<>(lambdaPath);
        path.add(lambda);

        double contribution = batemanTerm(path, elapsedSeconds) * initialAmount * branchProduct;
        output.merge(node, contribution, Double::sum);

        if (lambda <= 0) return; // stable (or unmodeled): nothing further decays from here
        for (DecayMode mode : node.decayModes) {
            Element daughter = mode.getDaughterElement();
            // Unresolvable (not-yet-registered) or branchless (e.g. spontaneous fission) daughters aren't tracked
            // as a single nuclide -- their share of the parent's decay is simply not carried further.
            if (daughter == null) continue;
            accumulatePaths(daughter, initialAmount, branchProduct * mode.branchingRatio, path, elapsedSeconds,
                    halfLifeCompressor, output);
        }
    }

    private static double decayConstant(@NotNull Element element, @NotNull DoubleUnaryOperator halfLifeCompressor) {
        double halfLife = element.halfLifeSeconds;
        if (halfLife <= 0) return 0.0;
        double compressed = halfLifeCompressor.applyAsDouble(halfLife);
        return Math.log(2) / compressed;
    }

    /**
     * The standard (non-branching) Bateman formula for a single linear path, given the per-node decay constants
     * from source (index 0) to target (last index). Reduces to plain exponential decay {@code exp(-lambda*t)} for
     * a length-1 path (the source is its own target).
     */
    private static double batemanTerm(@NotNull List<Double> lambdas, double t) {
        int k = lambdas.size();
        double leadCoefficient = 1.0;
        for (int j = 0; j < k - 1; j++) {
            leadCoefficient *= lambdas.get(j);
        }
        double sum = 0.0;
        for (int m = 0; m < k; m++) {
            double lm = lambdas.get(m);
            double denominator = 1.0;
            for (int j = 0; j < k; j++) {
                if (j == m) continue;
                denominator *= (lambdas.get(j) - lm);
            }
            sum += Math.exp(-lm * t) / denominator;
        }
        return leadCoefficient * sum;
    }
}
