package com.windows2000.gtnuclear.api.unification.materials.series;

import gregtech.api.fluids.FluidBuilder;
import gregtech.api.unification.material.Material;

import static com.windows2000.gtnuclear.api.unification.materials.GTNLElements.*;
import static com.windows2000.gtnuclear.api.unification.materials.GTNLMaterials.*;
import static com.windows2000.gtnuclear.api.util.GTNLUtils.gtnlID;
import static gregtech.api.unification.material.info.MaterialFlags.*;
import static gregtech.api.unification.material.info.MaterialIconSet.SHINY;

/**
 * Californium-252, a real primary neutron source isotope (spontaneous fission emitter, half-life 2.645 years)
 * used by this mod's dedicated startup neutron source hatch item (see
 * {@code com.windows2000.gtnuclear.common.item.ItemNeutronSourceRod}) to seed the reactor's point kinetics state
 * away from a degenerate exact-zero population -- see
 * {@code com.windows2000.gtnuclear.common.metatileentities.multi.MetaTileEntityPressurizedWaterReactor}'s own
 * JavaDoc for why a real, depletable source (rather than the earlier unconditional placeholder seed) makes this a
 * genuine structural requirement, not free.
 * <p>
 * Curium-248, its alpha-decay daughter, is registered too (this mod's convention of Material-izing every
 * intermediate nuclide) but modeled as stable -- see {@link com.windows2000.gtnuclear.api.unification.materials.
 * GTNLElements}'s own comment on why its real further decay is out of this mod's simulated scope.
 * <p>
 * Unlike this mod's fuel/burnup isotopes, decay here uses a <b>dedicated</b> compression scale
 * ({@code ConfigHolder.reactor.neutronSourceCompressedHalfLifeDays}), not the shared
 * {@code com.windows2000.gtnuclear.api.nuclear.decay.TimeCompression} used by the four primordial decay series --
 * applying that shared calibration (tuned around Thorium-232's ~4.4e17-second half-life) to Cf-252's comparatively
 * short 2.645-year half-life would compress it down to roughly 9 real-world minutes, useless for a component
 * meant to last a meaningful chunk of a play session. See the reactor MTE's own neutron-source-decay method.
 */
public class NeutronSourceMaterials {

    public static void register() {
        Californium252 = Material.builder(60, gtnlID("californium_252"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(1173))
                .color(0xC8FFC8).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(Cf252)
                .build();

        Curium248 = Material.builder(61, gtnlID("curium_248"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(1613))
                .color(0xE0C8FF).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(Cm248)
                .build();
    }

    private NeutronSourceMaterials() {}
}
