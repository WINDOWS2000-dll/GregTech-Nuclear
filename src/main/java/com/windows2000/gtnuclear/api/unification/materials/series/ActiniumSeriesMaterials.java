package com.windows2000.gtnuclear.api.unification.materials.series;

import gregtech.api.fluids.FluidBuilder;
import gregtech.api.unification.DecayMode;
import gregtech.api.unification.DecayType;
import gregtech.api.unification.Elements;
import gregtech.api.unification.material.Material;

import java.util.Arrays;
import java.util.Collections;

import static com.windows2000.gtnuclear.api.unification.materials.GTNLMaterials.*;
import static com.windows2000.gtnuclear.api.util.GTNLUtils.gtnlID;
import static gregtech.api.unification.material.info.MaterialFlags.*;
import static gregtech.api.unification.material.info.MaterialIconSet.SHINY;

/**
 * The actinium series (4n+3 natural decay chain), headed by {@code Uranium-235} (already defined natively in GTST,
 * see {@code Elements.U235} and its {@code decayModes}, pointing at {@code Th-231}). See
 * {@link UraniumSeriesMaterials} for the shared conventions (branch-rounding threshold, lazy daughter resolution,
 * real-world seconds not yet time-compressed).
 * <p>
 * Unlike the uranium series, two branches here sit at/above the 0.1% inclusion threshold and are modeled with two
 * {@link DecayMode}s each rather than rounded away: Actinium-227 (98.62% beta-minus -> Th-227, 1.38% alpha ->
 * Fr-223) and Bismuth-211 (99.724% alpha -> Tl-207, 0.276% beta-minus -> Po-211). Both side branches reconverge on
 * the main chain one step later (Fr-223 -> Ra-223; Po-211 -> Pb-207), so the chain is still a DAG, not a tree with
 * dead ends.
 */
public class ActiniumSeriesMaterials {

    public static void register() {
        Thorium231 = Material.builder(15, gtnlID("thorium_231"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(2023))
                .color(0xA8A8A8).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(Elements.add(90, 141, 91872.0,
                        Collections.singletonList(new DecayMode(DecayType.BETA_MINUS, 1.0, "Pa-231", 0.389)),
                        null, "Thorium-231", "Th-231", true))
                .build();

        Protactinium231 = Material.builder(16, gtnlID("protactinium_231"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(1841))
                .color(0xC8A8FF).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(Elements.add(91, 140, 1.033827e12,
                        Collections.singletonList(new DecayMode(DecayType.ALPHA, 1.0, "Ac-227", 5.150)),
                        null, "Protactinium-231", "Pa-231", true))
                .build();

        Actinium227 = Material.builder(17, gtnlID("actinium_227"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(1323))
                .color(0x9BFFD1).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(Elements.add(89, 138, 6.8707e8,
                        Arrays.asList(
                                new DecayMode(DecayType.BETA_MINUS, 0.9862, "Th-227", 0.045),
                                new DecayMode(DecayType.ALPHA, 0.0138, "Fr-223", 5.042)),
                        null, "Actinium-227", "Ac-227", true))
                .build();

        Francium223 = Material.builder(18, gtnlID("francium_223"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(300))
                .color(0xFFD27F).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(Elements.add(87, 136, 1320.0,
                        Collections.singletonList(new DecayMode(DecayType.BETA_MINUS, 1.0, "Ra-223", 1.149)),
                        null, "Francium-223", "Fr-223", true))
                .build();

        Thorium227 = Material.builder(19, gtnlID("thorium_227"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(2023))
                .color(0x8C8C8C).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(Elements.add(90, 137, 1614432.0,
                        Collections.singletonList(new DecayMode(DecayType.ALPHA, 1.0, "Ra-223", 6.038)),
                        null, "Thorium-227", "Th-227", true))
                .build();

        Radium223 = Material.builder(20, gtnlID("radium_223"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(973))
                .color(0xFFD2D2).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(Elements.add(88, 135, 987552.0,
                        Collections.singletonList(new DecayMode(DecayType.ALPHA, 1.0, "Rn-219", 5.979)),
                        null, "Radium-223", "Ra-223", true))
                .build();

        Radon219 = Material.builder(21, gtnlID("radon_219"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(202))
                .color(0xE664FF).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(Elements.add(86, 133, 3.96,
                        Collections.singletonList(new DecayMode(DecayType.ALPHA, 1.0, "Po-215", 6.946)),
                        null, "Radon-219", "Rn-219", true))
                .build();

        Polonium215 = Material.builder(22, gtnlID("polonium_215"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(527))
                .color(0xC8A878).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(Elements.add(84, 131, 1.781e-3,
                        Collections.singletonList(new DecayMode(DecayType.ALPHA, 1.0, "Pb-211", 7.527)),
                        null, "Polonium-215", "Po-215", true))
                .build();

        Lead211 = Material.builder(23, gtnlID("lead_211"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(601))
                .color(0x5A5A82).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(Elements.add(82, 129, 2166.0,
                        Collections.singletonList(new DecayMode(DecayType.BETA_MINUS, 1.0, "Bi-211", 1.367)),
                        null, "Lead-211", "Pb-211", true))
                .build();

        Bismuth211 = Material.builder(24, gtnlID("bismuth_211"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(545))
                .color(0xB48CFF).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(Elements.add(83, 128, 128.4,
                        Arrays.asList(
                                new DecayMode(DecayType.ALPHA, 0.99724, "Tl-207", 6.751),
                                new DecayMode(DecayType.BETA_MINUS, 0.00276, "Po-211", 0.575)),
                        null, "Bismuth-211", "Bi-211", true))
                .build();

        Polonium211 = Material.builder(25, gtnlID("polonium_211"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(527))
                .color(0xDCC08C).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(Elements.add(84, 127, 0.516,
                        Collections.singletonList(new DecayMode(DecayType.ALPHA, 1.0, "Pb-207", 7.594)),
                        null, "Polonium-211", "Po-211", true))
                .build();

        Thallium207 = Material.builder(26, gtnlID("thallium_207"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(577))
                .color(0xC8D2FF).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(Elements.add(81, 126, 286.2,
                        Collections.singletonList(new DecayMode(DecayType.BETA_MINUS, 1.0, "Pb-207", 1.418)),
                        null, "Thallium-207", "Tl-207", true))
                .build();

        // Stable endpoint of the series.
        Lead207 = Material.builder(27, gtnlID("lead_207"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(601))
                .color(0x6E6E6E).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(Elements.add(82, 125, "Lead-207", "Pb-207", true))
                .build();
    }

    private ActiniumSeriesMaterials() {}
}
