package com.windows2000.gtnuclear.api.unification.materials.series;

import gregtech.api.fluids.FluidBuilder;
import gregtech.api.unification.material.Material;

import static com.windows2000.gtnuclear.api.unification.materials.GTNLElements.*;
import static com.windows2000.gtnuclear.api.unification.materials.GTNLMaterials.*;
import static com.windows2000.gtnuclear.api.util.GTNLUtils.gtnlID;
import static gregtech.api.unification.material.info.MaterialFlags.*;
import static gregtech.api.unification.material.info.MaterialIconSet.SHINY;

/**
 * The actinium series (4n+3 natural decay chain), headed by {@code Uranium-235} (already defined natively in GTST,
 * see {@code Elements.U235} and its {@code decayModes}, pointing at {@code Th-231}). See
 * {@link UraniumSeriesMaterials} for the shared conventions (branch-rounding threshold, lazy daughter resolution,
 * real-world seconds not yet time-compressed); isotope data itself lives in
 * {@link com.windows2000.gtnuclear.api.unification.materials.GTNLElements}.
 * <p>
 * Unlike the uranium series, two branches here sit at/above the 0.1% inclusion threshold and are modeled with two
 * {@link gregtech.api.unification.DecayMode}s each rather than rounded away: Actinium-227 (98.62% beta-minus ->
 * Th-227, 1.38% alpha -> Fr-223) and Bismuth-211 (99.724% alpha -> Tl-207, 0.276% beta-minus -> Po-211). Both side
 * branches reconverge on the main chain one step later (Fr-223 -> Ra-223; Po-211 -> Pb-207), so the chain is still
 * a DAG, not a tree with dead ends.
 */
public class ActiniumSeriesMaterials {

    public static void register() {
        Thorium231 = Material.builder(15, gtnlID("thorium_231"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(2023))
                .color(0xA8A8A8).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(Th231)
                .build();

        Protactinium231 = Material.builder(16, gtnlID("protactinium_231"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(1841))
                .color(0xC8A8FF).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(Pa231)
                .build();

        Actinium227 = Material.builder(17, gtnlID("actinium_227"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(1323))
                .color(0x9BFFD1).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(Ac227)
                .build();

        Francium223 = Material.builder(18, gtnlID("francium_223"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(300))
                .color(0xFFD27F).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(Fr223)
                .build();

        Thorium227 = Material.builder(19, gtnlID("thorium_227"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(2023))
                .color(0x8C8C8C).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(Th227)
                .build();

        Radium223 = Material.builder(20, gtnlID("radium_223"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(973))
                .color(0xFFD2D2).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(Ra223)
                .build();

        Radon219 = Material.builder(21, gtnlID("radon_219"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(202))
                .color(0xE664FF).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(Rn219)
                .build();

        Polonium215 = Material.builder(22, gtnlID("polonium_215"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(527))
                .color(0xC8A878).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(Po215)
                .build();

        Lead211 = Material.builder(23, gtnlID("lead_211"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(601))
                .color(0x5A5A82).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(Pb211)
                .build();

        Bismuth211 = Material.builder(24, gtnlID("bismuth_211"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(545))
                .color(0xB48CFF).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(Bi211)
                .build();

        Polonium211 = Material.builder(25, gtnlID("polonium_211"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(527))
                .color(0xDCC08C).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(Po211)
                .build();

        Thallium207 = Material.builder(26, gtnlID("thallium_207"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(577))
                .color(0xC8D2FF).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(Tl207)
                .build();

        // Stable endpoint of the series.
        Lead207 = Material.builder(27, gtnlID("lead_207"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(601))
                .color(0x6E6E6E).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(Pb207)
                .build();
    }

    private ActiniumSeriesMaterials() {}
}
