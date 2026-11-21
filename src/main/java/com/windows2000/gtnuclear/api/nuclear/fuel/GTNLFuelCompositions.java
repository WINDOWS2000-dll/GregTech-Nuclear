package com.windows2000.gtnuclear.api.nuclear.fuel;

import gregtech.api.unification.Element;
import gregtech.api.unification.Elements;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Registered {@link FuelComposition} instances for this mod's uranium fuel cycle (2026-09-21 catalog, real
 * enrichment tiers -- MOX/plutonium and thorium/U-233 cycles are explicitly out of scope for now, see this mod's
 * design memory). Each tier's mole fractions are the real published enrichment figure for that grade; only U-235/
 * U-238 are modeled (trace U-234 in real enriched uranium is not represented).
 * <p>
 * Because {@link FuelComposition#moleFractionByIsotope} is a physically real weight (not an arbitrary tuning
 * knob), {@link com.windows2000.gtnuclear.api.nuclear.neutronics.ReactorNodeFactory}'s baseline constants were
 * recalibrated against {@link #LEU} specifically (2026-09-21) -- LEU is the real-world standard power-reactor
 * grade, so it is the natural calibration target; see {@code ReactorNodeFactory}'s own JavaDoc for the resulting
 * numbers. {@link #NATURAL_URANIUM} is expected to stay subcritical in this mod's light-water-moderated PWR design
 * even at large reactor sizes -- this matches reality (natural uranium needs heavy water or graphite moderation to
 * sustain a chain reaction; light-water reactors require enrichment) and is intended, not a bug.
 */
public final class GTNLFuelCompositions {

    /** Natural uranium (0.711% U-235) -- expected to stay subcritical in a light-water-moderated core. */
    public static final FuelComposition NATURAL_URANIUM = new FuelComposition("natural_uranium",
            uranium(0.00711));

    /** Low-enriched uranium (4.5% U-235) -- the real-world standard commercial LWR fuel grade. */
    public static final FuelComposition LEU = new FuelComposition("leu", uranium(0.045));

    /** High-assay low-enriched uranium (15% U-235) -- used by some advanced/compact reactor designs. */
    public static final FuelComposition HALEU = new FuelComposition("haleu", uranium(0.15));

    /** Highly enriched uranium (45% U-235) -- a conservative HEU figure, well above weapons-usable purity. */
    public static final FuelComposition HEU = new FuelComposition("heu", uranium(0.45));

    private static Map<Element, Double> uranium(double u235MoleFraction) {
        Map<Element, Double> composition = new LinkedHashMap<>();
        composition.put(Elements.U235, u235MoleFraction);
        composition.put(Elements.U238, 1.0 - u235MoleFraction);
        return composition;
    }

    private GTNLFuelCompositions() {}
}
