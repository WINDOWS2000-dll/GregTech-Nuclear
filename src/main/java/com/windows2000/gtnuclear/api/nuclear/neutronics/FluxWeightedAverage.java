package com.windows2000.gtnuclear.api.nuclear.neutronics;

/**
 * Combines per-channel/per-node local reactivity values into a single reactor-wide value, weighted by that
 * node's own squared neutron flux from the last {@link CMFDSolver} solve -- the standard first-order
 * perturbation-theory weighting (importance is proportional to flux squared for a self-adjoint system) used
 * throughout this mod's node-level local feedback (local Doppler/Xenon/moderator-temperature/control-rod
 * reactivity -- see this mod's design memory for the full derivation).
 * <p>
 * Using a weighted <i>average</i> (not a weighted sum) is deliberate: when every node carries the same local
 * value (the reactor-wide-scalar case this mod used before introducing local feedback), the result is exactly
 * that shared value regardless of how the weights are distributed -- the backward-compatibility property this
 * mod's design memory relies on instead of a calibration diagnostic test. Callers needing a weighted
 * <i>sum</i> over a fixed number of discrete items (e.g. control rods, where the pre-existing model summed
 * each rod's own worth rather than averaging them) multiply this method's result by that item count themselves
 * -- {@code average(values, weights) * values.length} is algebraically the weighted sum normalized by the mean
 * weight, which reduces to the plain sum whenever every weight is equal.
 */
public final class FluxWeightedAverage {

    private FluxWeightedAverage() {}

    /**
     * @param values  one local reactivity-like value per node (same units as the desired result)
     * @param weights that node's own weighting (e.g. squared flux); need not be normalized
     * @return the weighted average of {@code values}, or 0.0 if {@code values} is empty or every weight is
     *         non-positive (nothing to weight by)
     */
    public static double average(double[] values, double[] weights) {
        if (values.length != weights.length) {
            throw new IllegalArgumentException("values and weights must have the same length");
        }
        double weightedSum = 0.0;
        double totalWeight = 0.0;
        for (int i = 0; i < values.length; i++) {
            weightedSum += values[i] * weights[i];
            totalWeight += weights[i];
        }
        return totalWeight > 0.0 ? weightedSum / totalWeight : 0.0;
    }

    /**
     * @return {@code values}' own max minus min, or 0.0 if empty -- a debug-only companion to {@link #average}
     *         for displaying how much a set of per-node local values actually diverges (2026-10-07, extracted
     *         from {@code MetaTileEntityPressurizedWaterReactor#spread} as part of that class's own
     *         decomposition -- see this mod's design memory).
     */
    public static double spread(double[] values) {
        if (values.length == 0) return 0.0;
        double min = values[0];
        double max = values[0];
        for (double value : values) {
            min = Math.min(min, value);
            max = Math.max(max, value);
        }
        return max - min;
    }
}
