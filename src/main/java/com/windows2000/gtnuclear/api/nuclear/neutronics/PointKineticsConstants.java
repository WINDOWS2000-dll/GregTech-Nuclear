package com.windows2000.gtnuclear.api.nuclear.neutronics;

import gregtech.api.unification.Element;
import gregtech.api.unification.NeutronCrossSections;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Six-group delayed neutron precursor data and prompt neutron generation time for {@link PointKineticsSolver}.
 * <p>
 * There is deliberately no single "default" constant set here: different fissile isotopes have substantially
 * different delayed neutron behavior (famously, Pu-239's delayed neutron fraction is roughly a third of U-235's,
 * making plutonium-heavy cores respond faster/"twitchier" to reactivity changes), and this mod intends to support
 * multiple fuel types (see this mod's design memory). {@link #fissionWeighted} builds the effective composite
 * constants for whatever isotope mix a given reactor's fuel channels actually contain, sourced from each
 * isotope's own
 * {@link NeutronCrossSections#delayedNeutronDecayConstants}/{@link NeutronCrossSections#delayedNeutronFractions}
 * (registered per-isotope in GTST's {@code Elements}) -- this is the only way to obtain a
 * {@code PointKineticsConstants} instance, so callers cannot accidentally reach for a fuel-agnostic default.
 */
public final class PointKineticsConstants {

    /**
     * Representative prompt neutron generation time (s) for a thermal (light-water) reactor -- a reasonable starting
     * point pending per-reactor-type calibration; not fuel-isotope-dependent data like the group constants below.
     */
    public static final double STANDARD_PROMPT_NEUTRON_LIFETIME = 2.0e-5;

    @NotNull
    public final double[] precursorDecayConstants;
    @NotNull
    public final double[] delayedNeutronFractions;
    public final double promptNeutronLifetime;
    /** Sum of {@link #delayedNeutronFractions} -- the total delayed neutron fraction beta. */
    public final double totalDelayedNeutronFraction;

    private PointKineticsConstants(@NotNull double[] precursorDecayConstants, @NotNull double[] delayedNeutronFractions,
                                   double promptNeutronLifetime) {
        this.precursorDecayConstants = precursorDecayConstants;
        this.delayedNeutronFractions = delayedNeutronFractions;
        this.promptNeutronLifetime = promptNeutronLifetime;

        double total = 0.0;
        for (double beta : delayedNeutronFractions) total += beta;
        this.totalDelayedNeutronFraction = total;
    }

    public int groupCount() {
        return precursorDecayConstants.length;
    }

    /**
     * Builds effective composite point kinetics constants for a core whose fissions are shared among several
     * nuclides. Each group's delayed fraction is the <b>fission-rate</b>-weighted mean of the nuclides' own
     * fractions, {@code beta_i = sum_k w_k beta_ik} with {@code w_k = R_k / sum R} -- delayed neutrons come from
     * fissions, so a nuclide contributes in proportion to how often it fissions, not to how much of it is present.
     * (Until 2026-11-27, stage 2-2 of this mod's reactor physics rework, the weights were mole fractions: U-238, 95%
     * of LEU but under 2% of its fissions here, then dominated and beta came out ~2.1x too large -- 0.0137 instead
     * of ~0.0065 for LEU 4.5%.) Each group's decay constant is the beta-weighted mean
     * {@code lambda_i = sum_k w_k beta_ik lambda_ik / beta_i}, exact for the group's precursor population (all
     * nuclides currently share one set of lambdas, so this only matters once per-nuclide data is added). The
     * importance correction {@code beta_eff / beta} (~1.0-1.05 in a light water reactor) is not applied.
     *
     * @param fissionWeightByIsotope each nuclide's relative fission rate (any common scale -- normalized here);
     *                               every key must have delayed neutron data, see
     *                               {@link #accumulateFissionRates}
     */
    @NotNull
    public static PointKineticsConstants fissionWeighted(@NotNull Map<Element, Double> fissionWeightByIsotope,
                                                         double promptNeutronLifetime) {
        if (fissionWeightByIsotope.isEmpty()) {
            throw new IllegalArgumentException("fissionWeightByIsotope must not be empty");
        }

        int groupCount = -1;
        double totalWeight = 0.0;
        for (Map.Entry<Element, Double> entry : fissionWeightByIsotope.entrySet()) {
            NeutronCrossSections crossSections = requireDelayedNeutronData(entry.getKey());
            if (groupCount == -1) {
                groupCount = crossSections.delayedNeutronFractions.length;
            } else if (groupCount != crossSections.delayedNeutronFractions.length) {
                throw new IllegalArgumentException(
                        "Mismatched delayed neutron group counts among contributing isotopes");
            }
            totalWeight += entry.getValue();
        }
        if (!(totalWeight > 0)) throw new IllegalArgumentException("total fission weight must be positive");

        double[] fractions = new double[groupCount];
        double[] betaLambdaSums = new double[groupCount];
        double[] plainLambdaSums = new double[groupCount];
        for (Map.Entry<Element, Double> entry : fissionWeightByIsotope.entrySet()) {
            NeutronCrossSections crossSections = entry.getKey().crossSections;
            double weight = entry.getValue() / totalWeight;
            for (int i = 0; i < groupCount; i++) {
                double weightedBeta = weight * crossSections.delayedNeutronFractions[i];
                fractions[i] += weightedBeta;
                betaLambdaSums[i] += weightedBeta * crossSections.delayedNeutronDecayConstants[i];
                plainLambdaSums[i] += weight * crossSections.delayedNeutronDecayConstants[i];
            }
        }
        double[] decayConstants = new double[groupCount];
        for (int i = 0; i < groupCount; i++) {
            decayConstants[i] = fractions[i] > 0 ? betaLambdaSums[i] / fractions[i] : plainLambdaSums[i];
        }

        return new PointKineticsConstants(decayConstants, fractions, promptNeutronLifetime);
    }

    /**
     * Adds one fuel region's fission rate per nuclide into {@code into}: {@code amount_k * (sigma_f,fast,k * phi_fast
     * + sigma_f,thermal,k * phi_thermal)}, with the fluxes summed over the region's fuel nodes (stage 2-2). Units
     * are arbitrary but must be consistent across calls (amounts per initial heavy-metal atom and CMFD-normalized
     * fluxes are). Nuclides without delayed neutron data (e.g. U-236, Pu-240 -- they do not fission here) or without
     * fission cross sections are skipped; see {@link #fissionWeighted}.
     */
    public static void accumulateFissionRates(@NotNull Map<Element, Double> into,
                                              @NotNull Map<Element, Double> amounts, double fastFluxSum,
                                              double thermalFluxSum) {
        for (Map.Entry<Element, Double> entry : amounts.entrySet()) {
            Element element = entry.getKey();
            if (!hasDelayedNeutronData(element) || entry.getValue() <= 0) continue;
            NeutronCrossSections crossSections = element.crossSections;
            double rate = entry.getValue() * (crossSections.fastFissionBarns * fastFluxSum +
                    crossSections.thermalFissionBarns * thermalFluxSum);
            if (rate > 0) into.merge(element, rate, Double::sum);
        }
    }

    private static boolean hasDelayedNeutronData(@NotNull Element element) {
        return element.crossSections != null && element.crossSections.delayedNeutronFractions != null &&
                element.crossSections.delayedNeutronFractions.length > 0;
    }

    @NotNull
    private static NeutronCrossSections requireDelayedNeutronData(@NotNull Element element) {
        NeutronCrossSections crossSections = element.crossSections;
        if (crossSections == null || crossSections.delayedNeutronFractions == null ||
                crossSections.delayedNeutronDecayConstants == null) {
            throw new IllegalArgumentException("Element " + element + " has no delayed neutron data registered");
        }
        return crossSections;
    }
}
