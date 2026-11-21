package com.windows2000.gtnuclear.api.unification.materials.series;

import gregtech.api.fluids.FluidBuilder;
import gregtech.api.unification.material.Material;

import static com.windows2000.gtnuclear.api.unification.materials.GTNLElements.*;
import static com.windows2000.gtnuclear.api.unification.materials.GTNLMaterials.*;
import static com.windows2000.gtnuclear.api.util.GTNLUtils.gtnlID;
import static gregtech.api.unification.material.info.MaterialFlags.*;
import static gregtech.api.unification.material.info.MaterialIconSet.SHINY;

/**
 * Two non-fissile neutron-capture products this mod's in-reactor fuel burnup simulation (see
 * {@code com.windows2000.gtnuclear.api.nuclear.fuel}) can produce, distinct from the four natural primordial decay
 * chains ({@link UraniumSeriesMaterials}/{@link ActiniumSeriesMaterials}/{@link ThoriumSeriesMaterials}/
 * {@link NeptuniumSeriesMaterials}) registered earlier: U-235 and Pu-239 each occasionally capture a neutron
 * <i>without</i> fissioning, becoming U-236/Pu-240 respectively -- reactor physics' standard "terminal poison"
 * simplification for this mod's burnup chain (2026-09-20 design decision), rounding off the much rarer further
 * capture step each could undergo in reality (U-236 -&gt; U-237 -&gt; Np-237, already registered via
 * {@link NeptuniumSeriesMaterials} through a different path; Pu-240 -&gt; Pu-241) -- mirrors this mod's existing
 * branch-rounding convention for decay chains, applied here to burnup chains instead.
 * <p>
 * Both isotopes are registered with real half-life/decay data too (not just burnup-relevant capture cross
 * sections), and happen to alpha-decay into each other and then into {@link ThoriumSeriesMaterials}'
 * {@code Thorium-232} -- so a burnup product that eventually leaves a reactor (e.g. once the spent-fuel-pool
 * mechanic this mod's design memory already anticipates exists) decays like any other registered isotope via the
 * existing {@code DecayEngine}, with no special-casing needed. Neither isotope is referenced by any of GTST's own
 * native {@code Material}s, so -- like every isotope in the four decay-chain series above except U-235/U-238/
 * Pu-239/Pu-241 themselves -- this registration needed no GTST-side changes.
 * <p>
 * Real thermal capture cross-sections are used as-is (not rounded to a "representative" placeholder like this
 * mod's CMFD baseline diffusion constants): U-236 is a comparatively mild absorber (~5.1b), while Pu-240 is a
 * well-known strong thermal poison (~289.5b) -- both numbers are confident, well-documented real values, matching
 * this mod's existing convention of using real cross-sections wherever they are actually known (see
 * {@code UraniumSeriesMaterials}' own U-235/U-238/Pu-239/Pu-241 data). Thermal fission cross-sections are set to
 * exactly zero for both: U-236 is genuinely non-fissile at these energies, and while Pu-240 has a real (tiny,
 * ~0.06b) thermal fission cross-section, it is over three orders of magnitude below its own capture cross-section
 * -- rounding it to zero keeps the burnup chain's "terminal poison" property exact, consistent with this mod's
 * existing sub-0.1%-branch-rounding convention.
 */
public class ReactorBurnupProductMaterials {

    public static void register() {
        Uranium236 = Material.builder(58, gtnlID("uranium_236"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(1405))
                .color(0x64C864).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(U236)
                .build();

        Plutonium240 = Material.builder(59, gtnlID("plutonium_240"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(1449))
                .color(0xC86464).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(Pu240)
                .build();
    }
}
