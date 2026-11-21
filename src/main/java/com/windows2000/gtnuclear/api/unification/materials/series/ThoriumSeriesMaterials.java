package com.windows2000.gtnuclear.api.unification.materials.series;

import gregtech.api.fluids.FluidBuilder;
import gregtech.api.unification.material.Material;

import static com.windows2000.gtnuclear.api.unification.materials.GTNLElements.*;
import static com.windows2000.gtnuclear.api.unification.materials.GTNLMaterials.*;
import static com.windows2000.gtnuclear.api.util.GTNLUtils.gtnlID;
import static gregtech.api.unification.material.info.MaterialFlags.*;
import static gregtech.api.unification.material.info.MaterialIconSet.SHINY;

/**
 * The thorium series (4n natural decay chain), headed by {@code Thorium-232}. Unlike the uranium/actinium series,
 * GTST has no existing isotope-flagged "Th-232" {@link gregtech.api.unification.Element} to extend -- GTST's own
 * {@code Materials.Thorium} uses the generic, non-isotope {@code Elements.Th} (no mass number, no decay data),
 * exactly like {@code Elements.U}/{@code Elements.Pu} coexist with their specific {@code U238}/{@code Pu239}
 * isotopes. {@code Thorium232} here is a brand-new isotope Material registered entirely from this mod, following
 * that same established GTST convention; it does not replace or alias GTST's plain Thorium. See
 * {@link UraniumSeriesMaterials} for the shared conventions (branch-rounding threshold, lazy daughter resolution,
 * real-world seconds not yet time-compressed); isotope data itself lives in
 * {@link com.windows2000.gtnuclear.api.unification.materials.GTNLElements}.
 * <p>
 * One branch sits at/above the 0.1% inclusion threshold: Bismuth-212 (64.06% beta-minus -> Po-212, 35.94% alpha ->
 * Tl-208); both sides reconverge on stable Pb-208 one step later.
 */
public class ThoriumSeriesMaterials {

    public static void register() {
        Thorium232 = Material.builder(28, gtnlID("thorium_232"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(2023))
                .color(0xC0C0C0).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(Th232)
                .build();

        Radium228 = Material.builder(29, gtnlID("radium_228"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(973))
                .color(0xFFC8C8).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(Ra228)
                .build();

        Actinium228 = Material.builder(30, gtnlID("actinium_228"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(1323))
                .color(0x82FFC3).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(Ac228)
                .build();

        Thorium228 = Material.builder(31, gtnlID("thorium_228"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(2023))
                .color(0x969696).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(Th228)
                .build();

        Radium224 = Material.builder(32, gtnlID("radium_224"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(973))
                .color(0xFFB4B4).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(Ra224)
                .build();

        Radon220 = Material.builder(33, gtnlID("radon_220"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(202))
                .color(0xD264FF).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(Rn220)
                .build();

        Polonium216 = Material.builder(34, gtnlID("polonium_216"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(527))
                .color(0xBE9860).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(Po216)
                .build();

        Lead212 = Material.builder(35, gtnlID("lead_212"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(601))
                .color(0x505078).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(Pb212)
                .build();

        Bismuth212 = Material.builder(36, gtnlID("bismuth_212"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(545))
                .color(0x9678E6).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(Bi212)
                .build();

        Polonium212 = Material.builder(37, gtnlID("polonium_212"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(527))
                .color(0xD4B080).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(Po212)
                .build();

        Thallium208 = Material.builder(38, gtnlID("thallium_208"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(577))
                .color(0xAAB4FF).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(Tl208)
                .build();

        // Stable endpoint of the series.
        Lead208 = Material.builder(39, gtnlID("lead_208"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(601))
                .color(0x606060).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(Pb208)
                .build();
    }

    private ThoriumSeriesMaterials() {}
}
