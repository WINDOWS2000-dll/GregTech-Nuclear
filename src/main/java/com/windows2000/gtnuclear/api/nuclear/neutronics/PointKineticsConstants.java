package com.windows2000.gtnuclear.api.nuclear.neutronics;

import gregtech.api.unification.Element;
import gregtech.api.unification.NeutronCrossSections;

import com.windows2000.gtnuclear.api.nuclear.fuel.FuelComposition;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
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
     * Builds effective composite point kinetics constants for a reactor whose fuel is a mix of fissile isotopes,
     * combining each isotope's own six-group delayed neutron data weighted by that isotope's mole fraction in the
     * fuel -- the standard reactor-physics technique for core-averaged delayed neutron parameters, using mole
     * fraction as a stand-in for true fission share (each isotope's contribution to core beta is properly its own
     * beta weighted by its fraction of total fissions; mole fraction is exact when the contributing isotopes share
     * comparable fission cross-sections, and a reasonable standard approximation otherwise -- same caveat as
     * {@link com.windows2000.gtnuclear.api.nuclear.fuel.FuelComposition}'s own weighting). This is exact when
     * every contributing isotope also shares identical group decay constants (true of every isotope currently
     * registered in GTST, see {@code Elements}' own delayed neutron data provenance comment).
     *
     * @param moleFractionByIsotope each isotope's mole fraction of the fuel (need not be pre-normalized to sum to
     *                              1 -- normalized internally); every key must have {@link Element#crossSections}
     *                              populated with delayed neutron data. See {@link
     *                              com.windows2000.gtnuclear.api.nuclear.fuel.FuelComposition}'s own JavaDoc for
     *                              why mole fraction is the physically appropriate weight here.
     */
    @NotNull
    public static PointKineticsConstants fissionWeighted(@NotNull Map<Element, Double> moleFractionByIsotope,
                                                         double promptNeutronLifetime) {
        if (moleFractionByIsotope.isEmpty()) {
            throw new IllegalArgumentException("moleFractionByIsotope must not be empty");
        }

        int groupCount = -1;
        double totalShare = 0.0;
        for (Map.Entry<Element, Double> entry : moleFractionByIsotope.entrySet()) {
            NeutronCrossSections crossSections = requireDelayedNeutronData(entry.getKey());
            if (groupCount == -1) {
                groupCount = crossSections.delayedNeutronFractions.length;
            } else if (groupCount != crossSections.delayedNeutronFractions.length) {
                throw new IllegalArgumentException(
                        "Mismatched delayed neutron group counts among contributing isotopes");
            }
            totalShare += entry.getValue();
        }

        double[] fractions = new double[groupCount];
        double[] decayConstants = new double[groupCount];
        for (Map.Entry<Element, Double> entry : moleFractionByIsotope.entrySet()) {
            NeutronCrossSections crossSections = entry.getKey().crossSections;
            double weight = entry.getValue() / totalShare;
            for (int i = 0; i < groupCount; i++) {
                fractions[i] += weight * crossSections.delayedNeutronFractions[i];
                decayConstants[i] += weight * crossSections.delayedNeutronDecayConstants[i];
            }
        }

        return new PointKineticsConstants(decayConstants, fractions, promptNeutronLifetime);
    }

    /**
     * Combines every loaded fuel composition's own {@code moleFractionByIsotope} by simple summation (each
     * composition weighted equally) -- a reasonable approximation pending calibration, same as every other
     * "combine per-channel data into one reactor-wide value" simplification in this mod's physics wiring
     * (2026-10-07, extracted from {@code MetaTileEntityPressurizedWaterReactor#aggregateFissionShares} as part
     * of that class's own decomposition -- see this mod's design memory). Already had no reactor-multiblock
     * dependency beyond reading each port's own composition before the move.
     * <p>
     * Isotopes with no delayed-neutron group data (zero-length {@code delayedNeutronFractions}, e.g. {@code
     * GTNLElements#U236}/{@code Pu240} -- non-fissile neutron-capture "terminal poison" products {@code
     * FuelBurnupEngine} accumulates in a rod's live inventory over time) are skipped here: {@link
     * #fissionWeighted} requires every contributing isotope's group count to match, and these isotopes genuinely
     * cannot fission or contribute delayed neutrons, so including them is never correct for this particular
     * blend (unlike the CMFD shape solve, where their absorption cross section legitimately still matters as a
     * poison -- this filtering is specific to the point-kinetics constants). Without this filter, burnup
     * producing even a trace amount of U-236/Pu-240 would crash the next shape recompute (confirmed via a real
     * playtest crash, 2026-09-22: {@code IllegalArgumentException: Mismatched delayed neutron group counts}).
     */
    @NotNull
    public static Map<Element, Double> aggregateFissionShares(@NotNull Iterable<FuelComposition> compositions) {
        Map<Element, Double> combined = new HashMap<>();
        for (FuelComposition composition : compositions) {
            for (Map.Entry<Element, Double> entry : composition.moleFractionByIsotope.entrySet()) {
                if (!hasDelayedNeutronData(entry.getKey())) continue;
                combined.merge(entry.getKey(), entry.getValue(), Double::sum);
            }
        }
        return combined;
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
