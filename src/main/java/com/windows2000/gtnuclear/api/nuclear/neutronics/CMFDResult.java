package com.windows2000.gtnuclear.api.nuclear.neutronics;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Result of a {@link CMFDSolver#solve} call: k-effective, per-node relative flux for both groups, and convergence
 * diagnostics.
 * <p>
 * A record (2026-10-06, Java 25 upgrade pass) -- see {@link NodePosition}'s own JavaDoc for the general
 * reasoning.
 *
 * @param kEffective  The multiplication factor for this geometry snapshot.
 * @param fastFlux    Fast-group relative flux per node, normalized (together with {@code thermalFlux}) so the
 *                    average total flux across all nodes is 1.
 * @param thermalFlux Thermal-group relative flux per node, normalized (together with {@code fastFlux}) so the
 *                    average total flux across all nodes is 1.
 * @param iterations  How many outer (power) iterations were run -- for diagnostics/logging, not required by
 *                    callers.
 */
public record CMFDResult(double kEffective, @NotNull Map<NodePosition, Double> fastFlux,
                         @NotNull Map<NodePosition, Double> thermalFlux, boolean converged, int iterations) {}
