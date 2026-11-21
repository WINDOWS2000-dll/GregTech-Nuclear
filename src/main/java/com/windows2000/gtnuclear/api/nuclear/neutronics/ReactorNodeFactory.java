package com.windows2000.gtnuclear.api.nuclear.neutronics;

import gregtech.api.unification.Element;
import gregtech.api.unification.Elements;
import gregtech.api.unification.NeutronCrossSections;

import com.windows2000.gtnuclear.api.nuclear.fuel.FuelComposition;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.function.ToDoubleFunction;

/**
 * Builds {@link ReactorNode}s for the two node "flavors" this mod's structure can currently produce: fuel-port
 * channels and everything else (plain interior/moderator cells, and control-rod-channel cells -- see
 * {@link #moderatorNode} for why control rod channels use this same flavor).
 * <p>
 * Per this mod's design memory (2026-09-17): textbook-representative macroscopic baseline values per node type,
 * with fuel nodes further scaled by the loaded {@link FuelComposition}'s cross-section ratio relative to U-235
 * (the best-characterized registered isotope, used as the reference point) -- a first-principles derivation from
 * actual atom number densities is not attempted, since this mod has no such data (see
 * {@code com.windows2000.gtnuclear.api.nuclear.neutronics.CMFDSolver}'s own JavaDoc for the wider CMFD design).
 * <p>
 * <b>Calibrated 2026-09-20</b> against the actual minimum-diameter (7), minimum-height (6-layer channel)
 * checkerboard reactor (9 fuel / 12 control-rod channels) a real playtest built, using a throwaway diagnostic
 * test (not kept -- see this mod's design memory) that reproduced the exact geometry and printed k-effective.
 * The first-pass baseline values produced k_eff around 0.10-0.27 even for an idealized all-U-235 core at that
 * size, and only around 0.6 even at a much larger (diameter 21, 20-layer) size -- confirming the issue was the
 * baseline constants themselves (checkerboard-diluted infinite-medium k-infinity well under 1), not just small-
 * reactor leakage. After raising fuel's thermal nu*fission relative to its own absorption, and thermalization
 * (downscatter) relative to the moderator's own thermal absorption, the same minimum reactor reaches k_eff
 * &asymp; 0.71 (fully control-rod-withdrawn reactivity around -40,000 pcm) while a much larger reactor reaches
 * k_eff &asymp; 1.28 (excess reactivity a control rod bank can plausibly manage) -- the intended trend (small
 * reactors meaningfully leak, larger ones have workable excess reactivity), confirmed with the user.
 * <p>
 * <b>The minimum structural size is deliberately never able to reach criticality with the lower enrichment
 * tiers</b>, even with every control rod fully withdrawn (confirmed and accepted as intended for the 2026-09-20
 * calibration's single placeholder composition). All values below remain representative pending further
 * playtesting, same as every other gameplay-balance constant in this mod's physics classes.
 * <p>
 * <b>Recalibrated 2026-09-21</b> after {@link FuelComposition#moleFractionByIsotope} was redefined to hold real
 * mole-fraction enrichment values (see {@code GTNLFuelCompositions}) instead of an arbitrary "fission share"
 * placeholder -- the old placeholder (U235 weight 0.93) was far higher than any real enrichment tier (0.711% to
 * 45%), so re-using it as a literal weight made every real composition drastically subcritical. Retuned
 * {@code FUEL_THERMAL_NU_FISSION_BASELINE_CM} alone (0.50 -&gt; 1.07, using the same throwaway-diagnostic-test
 * methodology as 2026-09-20, not kept) against {@code GTNLFuelCompositions.LEU} (4.5% U-235, the real-world
 * standard commercial power-reactor grade) at the "practical minimum" checkerboard geometry (diameter 13, 10-layer
 * channels) identified in the prior session, targeting k_eff &asymp; 1.10 (comfortable margin over the 2800 pcm
 * equilibrium xenon worth). Resulting behavior across tiers at that same geometry: NU 0.28 (deeply subcritical, as
 * expected -- light-water moderation alone cannot sustain natural uranium, matching reality), LEU 1.11 (by
 * construction), HALEU 1.82, HEU 2.23 (both far more reactive, expected -- see this mod's design memory for why
 * this is treated as an intentional "higher enrichment enables smaller/more compact reactors" tradeoff rather than
 * rebalanced away).
 * <p>
 * <b>Important consequence of the recalibration (confirmed with the user 2026-09-21)</b>: unlike the single-
 * composition 2026-09-20 result, "minimum structural size is never critical" is no longer uniformly true across
 * tiers -- at the absolute structural minimum (diameter 7, 6-layer channels), NU/LEU stay clearly subcritical
 * (0.11/0.52), but HALEU sits right at the boundary (k_eff &asymp; 0.9993) and HEU is comfortably supercritical
 * (k_eff &asymp; 1.37) even at that minimum size. Confirmed as intended, not rebalanced away: this matches why
 * real research/naval reactors use highly-enriched fuel to stay compact, and the original "never critical"
 * invariant is superseded by this tier-dependent behavior.
 * <p>
 * <b>Recalibrated again 2026-09-29</b> (real-playtest-driven, see this mod's design memory): the 2026-09-21
 * calibration's own target (k_eff &asymp; 1.10, margin over only the 2800 pcm equilibrium xenon worth) predates
 * this reactor's moderator-temperature-coefficient/Doppler feedback entirely -- a real playtest's own freshly-
 * loaded, unburned diameter-13 LEU reactor read exactly 6963 pcm shape reactivity (confirmed directly via the
 * control console's new debug SHAPE display, and cross-checked independently by back-deriving it from logged
 * REACTIVITY/BORON/MTC/DOPPLER values -- both methods agreed to within 1 pcm), nowhere near enough margin once
 * MTC (up to roughly -5400 pcm quadratic-model pcm near 610K) and Doppler (roughly -1100 pcm near 670K fuel
 * temperature) are added on top of xenon. A real playtest's own bank 0 ran to its 0% withdrawal floor and stayed
 * there indefinitely as a direct consequence. Reproducing that exact reactor in a throwaway diagnostic test (not
 * kept) confirmed the true CMFD channel height is 9, not the 10 previously assumed here -- {@code
 * MetaTileEntityPressurizedWaterReactor#addChannelColumn}'s loop upper bound is the fuel/control-rod port's own Y
 * position, i.e. the channel includes the top layer the port itself physically sits on, one layer more than the
 * structure's own visible "Housing" block count between the solid bottom cap and open top. With that corrected
 * height, {@code FUEL_THERMAL_NU_FISSION_BASELINE_CM} alone was raised again (1.07 -&gt; 1.13) via the same
 * methodology, targeting shape reactivity comfortably above the combined MTC+Doppler+xenon burden (roughly
 * -9350 pcm at the representative near-rated conditions above) plus a genuine operating margin, landing at
 * k_eff &asymp; 1.13 (shape &asymp; 11,500-12,000 pcm) for LEU at this same diameter-13/9-layer geometry --
 * pending final confirmation via the same live SHAPE display once real fission power is back up.
 */
public final class ReactorNodeFactory {

    /**
     * Downscatter (moderation) cross-section Sigma_s,1-&gt;2 (cm^-1) for light water, shared by both node flavors
     * -- even a "fuel" node in this coarse 1-channel-per-node model represents a whole channel's worth of space,
     * most of which is still coolant/moderator between fuel pins.
     */
    private static final double MODERATION_DOWNSCATTER_CM = 0.08;

    private static final double MODERATOR_FAST_DIFFUSION_CM = 1.3;
    private static final double MODERATOR_THERMAL_DIFFUSION_CM = 0.16;
    private static final double MODERATOR_FAST_ABSORPTION_CM = 0.001;
    private static final double MODERATOR_THERMAL_ABSORPTION_CM = 0.015;

    private static final double FUEL_FAST_DIFFUSION_CM = 1.13;
    private static final double FUEL_THERMAL_DIFFUSION_CM = 0.36;
    private static final double FUEL_FAST_ABSORPTION_BASELINE_CM = 0.008;
    private static final double FUEL_THERMAL_ABSORPTION_BASELINE_CM = 0.30;
    private static final double FUEL_FAST_NU_FISSION_BASELINE_CM = 0.003;
    private static final double FUEL_THERMAL_NU_FISSION_BASELINE_CM = 1.13;

    private ReactorNodeFactory() {}

    /**
     * A plain interior cell: open coolant/moderator space, or a control-rod-channel cell. Control rod insertion's
     * reactivity effect is deliberately <b>not</b> modeled here -- per the quasi-static method's own division of
     * labor (see this mod's design memory), CMFD only recomputes the reactor's flux <i>shape</i> on structural
     * change, while control rod worth is a separate term added every tick to the point-kinetics
     * <i>amplitude</i> update ({@code ControlRodWorth}). Giving control-rod channels a fixed moderator-like node
     * means CMFD never needs to re-run when a player merely adjusts insertion depth.
     */
    @NotNull
    public static ReactorNode moderatorNode(@NotNull NodePosition position) {
        NeutronGroupConstants fast = new NeutronGroupConstants(MODERATOR_FAST_DIFFUSION_CM,
                MODERATOR_FAST_ABSORPTION_CM, 0.0);
        NeutronGroupConstants thermal = new NeutronGroupConstants(MODERATOR_THERMAL_DIFFUSION_CM,
                MODERATOR_THERMAL_ABSORPTION_CM, 0.0);
        return new ReactorNode(position, fast, thermal, MODERATION_DOWNSCATTER_CM);
    }

    /**
     * A fuel channel node loaded with {@code composition}. Absorption and nu*fission are the fuel baseline scaled
     * by the composition's own weighted cross-section ratio relative to U-235 -- an empty/never-fueled channel
     * should use {@link #moderatorNode} instead, not this method with an empty composition (which
     * {@link FuelComposition}'s own constructor already forbids).
     */
    @NotNull
    public static ReactorNode fuelNode(@NotNull NodePosition position, @NotNull FuelComposition composition) {
        Map<Element, Double> shares = composition.moleFractionByIsotope;
        NeutronCrossSections reference = requireCrossSections(Elements.U235);

        double thermalCapture = weightedSum(shares, cs -> cs.thermalCaptureBarns);
        double thermalFission = weightedSum(shares, cs -> cs.thermalFissionBarns);
        double fastCapture = weightedSum(shares, cs -> cs.fastCaptureBarns);
        double fastFission = weightedSum(shares, cs -> cs.fastFissionBarns);

        double thermalAbsorptionRatio = (thermalCapture + thermalFission) /
                (reference.thermalCaptureBarns + reference.thermalFissionBarns);
        double thermalFissionRatio = thermalFission / reference.thermalFissionBarns;
        double fastAbsorptionRatio = (fastCapture + fastFission) /
                (reference.fastCaptureBarns + reference.fastFissionBarns);
        double fastFissionRatio = fastFission / reference.fastFissionBarns;

        NeutronGroupConstants fast = new NeutronGroupConstants(FUEL_FAST_DIFFUSION_CM,
                FUEL_FAST_ABSORPTION_BASELINE_CM * fastAbsorptionRatio,
                FUEL_FAST_NU_FISSION_BASELINE_CM * fastFissionRatio);
        NeutronGroupConstants thermal = new NeutronGroupConstants(FUEL_THERMAL_DIFFUSION_CM,
                FUEL_THERMAL_ABSORPTION_BASELINE_CM * thermalAbsorptionRatio,
                FUEL_THERMAL_NU_FISSION_BASELINE_CM * thermalFissionRatio);
        return new ReactorNode(position, fast, thermal, MODERATION_DOWNSCATTER_CM);
    }

    private static double weightedSum(@NotNull Map<Element, Double> shares,
                                      @NotNull ToDoubleFunction<NeutronCrossSections> extractor) {
        double totalShare = 0.0;
        for (double share : shares.values()) totalShare += share;
        double sum = 0.0;
        for (Map.Entry<Element, Double> entry : shares.entrySet()) {
            NeutronCrossSections crossSections = requireCrossSections(entry.getKey());
            sum += (entry.getValue() / totalShare) * extractor.applyAsDouble(crossSections);
        }
        return sum;
    }

    @NotNull
    private static NeutronCrossSections requireCrossSections(@NotNull Element element) {
        NeutronCrossSections crossSections = element.crossSections;
        if (crossSections == null) {
            throw new IllegalArgumentException("Element " + element + " has no neutron cross-section data registered");
        }
        return crossSections;
    }
}
