package com.windows2000.gtnuclear.api.nuclear.fuel;

import gregtech.api.unification.Element;
import gregtech.api.unification.NeutronCrossSections;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Advances a fuel rod's actual isotope inventory (absolute amounts, not fission shares -- contrast
 * {@link FuelComposition}) by one time step of in-reactor neutron capture, replacing the earlier placeholder
 * "durability bar" (see this mod's design memory, 2026-09-20): a rod's declining performance now falls
 * naturally out of {@link com.windows2000.gtnuclear.api.nuclear.neutronics.ReactorNodeFactory#fuelNode} being fed
 * a {@link FuelComposition} re-derived from this evolving inventory every physics-shape recompute, rather than
 * from a separate synthetic counter.
 * <p>
 * <b>Reaction model</b>: only isotopes present in the caller-supplied {@code captureDaughters} map react at all
 * (see {@link #advance}'s own parameter doc) -- anything without an entry (e.g. a terminal poison, or any isotope
 * with zero present amount) is left untouched. This mod's production capture-daughter chain (U-235 -&gt; U-236,
 * U-238 -&gt; Pu-239, Pu-239 -&gt; Pu-240) deliberately stops after one step, rounding off the much rarer further
 * capture each could undergo in reality -- the same convention already used for decay-chain branch rounding --
 * but that chain is <b>not</b> hardcoded here: like {@link com.windows2000.gtnuclear.api.nuclear.decay.DecayEngine}
 * and every other MC-independent physics class in this mod, this class takes its nuclide data as an explicit
 * parameter rather than reaching for specific registered {@code Element}s itself, keeping it independently
 * testable and reusable for a future fuel cycle (e.g. thorium) with a different chain.
 * <p>
 * For a reacting isotope, both a fission branch (destroys the atom; the resulting fission-product mass is not
 * tracked in this pass, deliberately deferred alongside spent-fuel handling in general) and a
 * capture-without-fission branch (transmutes to that isotope's mapped daughter) draw from the same total reaction
 * rate, split by that isotope's own fission-vs-capture cross-section ratio -- <i>and both branches are weighted by
 * the fast/thermal flux split at this rod's own channel</i> (from the last
 * {@link com.windows2000.gtnuclear.api.nuclear.neutronics.CMFDResult}), not a group-agnostic combined
 * cross-section, so a thermal-flux-heavy core burns U-235/Pu-239 (large thermal cross-sections) faster than a
 * fast-flux-heavy one would.
 */
public final class FuelBurnupEngine {

    private FuelBurnupEngine() {}

    /**
     * @param composition      current isotope inventory (absolute amounts, e.g. kg -- any consistent unit, since
     *                         only ratios matter downstream); not mutated, a new map is returned
     * @param captureDaughters which isotope becomes which upon capture-without-fission; an isotope with no entry
     *                         here does not react at all, regardless of its own cross-section data
     * @param localFastFlux    this channel's own fast-group relative flux from the last CMFD solve (1.0 = core
     *                         average)
     * @param localThermalFlux this channel's own thermal-group relative flux from the last CMFD solve
     * @param relativePower    the reactor's current relative neutron population (point kinetics amplitude)
     * @param rateCoefficient  gameplay-balance scaling constant (see
     *                         {@code ConfigHolder.reactor.fuelBurnupRateCoefficient})
     * @param dtSeconds        elapsed time this step covers
     * @return the new isotope inventory after this step
     */
    @NotNull
    public static Map<Element, Double> advance(@NotNull Map<Element, Double> composition,
                                               @NotNull Map<Element, Element> captureDaughters, double localFastFlux,
                                               double localThermalFlux, double relativePower, double rateCoefficient,
                                               double dtSeconds) {
        Map<Element, Double> next = new HashMap<>(composition);
        for (Map.Entry<Element, Double> entry : composition.entrySet()) {
            Element isotope = entry.getKey();
            double amount = entry.getValue();
            Element daughter = captureDaughters.get(isotope);
            if (daughter == null || amount <= 0) continue;
            NeutronCrossSections crossSections = isotope.crossSections;
            if (crossSections == null) continue;

            double reactionWeight = localFastFlux * (crossSections.fastCaptureBarns + crossSections.fastFissionBarns) +
                    localThermalFlux * (crossSections.thermalCaptureBarns + crossSections.thermalFissionBarns);
            if (reactionWeight <= 0) continue;
            double fissionWeight = localFastFlux * crossSections.fastFissionBarns +
                    localThermalFlux * crossSections.thermalFissionBarns;
            double fissionFraction = fissionWeight / reactionWeight;

            double totalReacted = Math.min(
                    rateCoefficient * relativePower * amount * reactionWeight * dtSeconds, amount);
            double captured = totalReacted * (1.0 - fissionFraction);

            next.merge(isotope, -totalReacted, Double::sum);
            next.merge(daughter, captured, Double::sum);
        }
        next.replaceAll((isotope, amount) -> Math.max(amount, 0.0));
        return next;
    }
}
