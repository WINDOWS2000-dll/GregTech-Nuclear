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
 * The neptunium series (4n+1 chain) -- mostly extinct in nature (its longest-lived member, Np-237, has too short a
 * half-life to have survived since Earth's formation) but very much alive in a reactor: reached from
 * {@code Pu-241}'s beta-minus decay to {@code Am-241}, already defined natively in GTST (see
 * {@code Elements.Pu241}'s {@code decayModes}). See {@link UraniumSeriesMaterials} for the shared conventions
 * (branch-rounding threshold, lazy daughter resolution, real-world seconds not yet time-compressed).
 * <p>
 * One branch sits at/above the 0.1% inclusion threshold: Bismuth-213 (97.80% beta-minus -> Po-213, 2.20% alpha ->
 * Tl-209); both sides reconverge on Pb-209 one step later.
 * <p>
 * {@code Bismuth-209}, this chain's endpoint, is technically alpha-radioactive in reality (half-life
 * ~2.01e19 years -- over a billion times the age of the universe) but is modeled here as stable
 * ({@code halfLifeSeconds = -1}, no {@link DecayMode}s): at that timescale it is unconditionally irrelevant to any
 * in-game decay simulation, real-world seconds or compressed.
 */
public class NeptuniumSeriesMaterials {

    public static void register() {
        Americium241 = Material.builder(40, gtnlID("americium_241"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(1449))
                .color(0xFF6478).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(Elements.add(95, 146, 1.36392e10,
                        Collections.singletonList(new DecayMode(DecayType.ALPHA, 1.0, "Np-237", 5.638)),
                        null, "Americium-241", "Am-241", true))
                .build();

        Neptunium237 = Material.builder(41, gtnlID("neptunium_237"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(917))
                .color(0x64A0FF).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(Elements.add(93, 144, 6.76595e13,
                        Collections.singletonList(new DecayMode(DecayType.ALPHA, 1.0, "Pa-233", 4.959)),
                        null, "Neptunium-237", "Np-237", true))
                .build();

        Protactinium233 = Material.builder(42, gtnlID("protactinium_233"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(1841))
                .color(0xDCC8FF).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(Elements.add(91, 142, 2330640.0,
                        Collections.singletonList(new DecayMode(DecayType.BETA_MINUS, 1.0, "U-233", 0.571)),
                        null, "Protactinium-233", "Pa-233", true))
                .build();

        Uranium233 = Material.builder(43, gtnlID("uranium_233"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(1405))
                .color(0x28E028).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(Elements.add(92, 141, 5.02397e12,
                        Collections.singletonList(new DecayMode(DecayType.ALPHA, 1.0, "Th-229", 4.909)),
                        null, "Uranium-233", "U-233", true))
                .build();

        Thorium229 = Material.builder(44, gtnlID("thorium_229"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(2023))
                .color(0xB0B0B0).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(Elements.add(90, 139, 2.31633e11,
                        Collections.singletonList(new DecayMode(DecayType.ALPHA, 1.0, "Ra-225", 5.168)),
                        null, "Thorium-229", "Th-229", true))
                .build();

        Radium225 = Material.builder(45, gtnlID("radium_225"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(973))
                .color(0xFFE6E6).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(Elements.add(88, 137, 1287360.0,
                        Collections.singletonList(new DecayMode(DecayType.BETA_MINUS, 1.0, "Ac-225", 0.36)),
                        null, "Radium-225", "Ra-225", true))
                .build();

        Actinium225 = Material.builder(46, gtnlID("actinium_225"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(1323))
                .color(0xB4FFE0).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(Elements.add(89, 136, 857088.0,
                        Collections.singletonList(new DecayMode(DecayType.ALPHA, 1.0, "Fr-221", 5.935)),
                        null, "Actinium-225", "Ac-225", true))
                .build();

        Francium221 = Material.builder(47, gtnlID("francium_221"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(300))
                .color(0xFFE0A0).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(Elements.add(87, 134, 294.0,
                        Collections.singletonList(new DecayMode(DecayType.ALPHA, 1.0, "At-217", 6.457)),
                        null, "Francium-221", "Fr-221", true))
                .build();

        Astatine217 = Material.builder(48, gtnlID("astatine_217"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(575))
                .color(0x785A46).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(Elements.add(85, 132, 0.0323,
                        Collections.singletonList(new DecayMode(DecayType.ALPHA, 1.0, "Bi-213", 7.202)),
                        null, "Astatine-217", "At-217", true))
                .build();

        Bismuth213 = Material.builder(49, gtnlID("bismuth_213"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(545))
                .color(0xDCC0FF).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(Elements.add(83, 130, 2735.4,
                        Arrays.asList(
                                new DecayMode(DecayType.BETA_MINUS, 0.9780, "Po-213", 1.423),
                                new DecayMode(DecayType.ALPHA, 0.0220, "Tl-209", 5.87)),
                        null, "Bismuth-213", "Bi-213", true))
                .build();

        Polonium213 = Material.builder(50, gtnlID("polonium_213"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(527))
                .color(0xEED0A0).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(Elements.add(84, 129, 3.72e-6,
                        Collections.singletonList(new DecayMode(DecayType.ALPHA, 1.0, "Pb-209", 8.536)),
                        null, "Polonium-213", "Po-213", true))
                .build();

        Thallium209 = Material.builder(51, gtnlID("thallium_209"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(577))
                .color(0xDCE6FF).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(Elements.add(81, 128, 129.66,
                        Collections.singletonList(new DecayMode(DecayType.BETA_MINUS, 1.0, "Pb-209", 3.99)),
                        null, "Thallium-209", "Tl-209", true))
                .build();

        Lead209 = Material.builder(52, gtnlID("lead_209"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(601))
                .color(0x8888B0).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(Elements.add(82, 127, 11642.4,
                        Collections.singletonList(new DecayMode(DecayType.BETA_MINUS, 1.0, "Bi-209", 0.644)),
                        null, "Lead-209", "Pb-209", true))
                .build();

        // Endpoint of the series -- modeled as stable, see this class's own JavaDoc.
        Bismuth209 = Material.builder(53, gtnlID("bismuth_209"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(545))
                .color(0xC8AAEE).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(Elements.add(83, 126, "Bismuth-209", "Bi-209", true))
                .build();
    }

    private NeptuniumSeriesMaterials() {}
}
