package com.windows2000.gtnuclear.api.nuclear.fuel;

import gregtech.api.unification.Element;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * The isotope mix inside one fuel rod type, expressed as each isotope's mole (equivalently, mass -- U-235/U-238
 * differ in atomic mass by under 1.3%) fraction of the total fissile+fertile material -- i.e. real enrichment
 * figures (e.g. LEU's 4.5% is {@code U235 -> 0.045, U238 -> 0.955}) can be used directly. This is the physically
 * correct weight for both consumers that share this map shape:
 * {@link com.windows2000.gtnuclear.api.nuclear.neutronics.ReactorNodeFactory#fuelNode} blends microscopic
 * cross-sections into a macroscopic one via <code>Sigma = sum(N_i * sigma_i)</code>, which is exactly a mole-
 * fraction-weighted average; and
 * {@link com.windows2000.gtnuclear.api.nuclear.neutronics.PointKineticsConstants#fissionWeighted} combines each
 * isotope's delayed-neutron data by its share of core fissions, which for isotopes sharing the same neutron
 * spectrum reduces to the same mole-fraction weighting (fission rate is proportional to N_i * sigma_f,i, so this
 * is only exact when the fissile isotopes present have comparable fission cross-sections -- a standard
 * simplification also used by {@code fissionWeighted}'s own JavaDoc). Every key must have
 * {@link Element#crossSections} populated (only U-235/U-238/Pu-239/Pu-241 currently do, in GTST's own
 * {@code Elements}).
 * <p>
 * This class is deliberately just the data shape -- the concrete catalog of fuel rod types lives in
 * {@link GTNLFuelCompositions}.
 */
public final class FuelComposition {

    @NotNull
    public final String id;
    @NotNull
    public final Map<Element, Double> moleFractionByIsotope;

    public FuelComposition(@NotNull String id, @NotNull Map<Element, Double> moleFractionByIsotope) {
        if (moleFractionByIsotope.isEmpty()) {
            throw new IllegalArgumentException("moleFractionByIsotope must not be empty");
        }
        this.id = id;
        this.moleFractionByIsotope = moleFractionByIsotope;
    }
}
