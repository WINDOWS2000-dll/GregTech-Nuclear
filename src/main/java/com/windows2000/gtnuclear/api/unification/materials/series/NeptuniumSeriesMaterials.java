package com.windows2000.gtnuclear.api.unification.materials.series;

import gregtech.api.fluids.FluidBuilder;
import gregtech.api.unification.material.Material;

import static com.windows2000.gtnuclear.api.unification.materials.GTNLElements.*;
import static com.windows2000.gtnuclear.api.unification.materials.GTNLMaterials.*;
import static com.windows2000.gtnuclear.api.util.GTNLUtils.gtnlID;
import static gregtech.api.unification.material.info.MaterialFlags.*;
import static gregtech.api.unification.material.info.MaterialIconSet.SHINY;

/**
 * The neptunium series (4n+1 chain) -- mostly extinct in nature (its longest-lived member, Np-237, has too short a
 * half-life to have survived since Earth's formation) but very much alive in a reactor: reached from
 * {@code Pu-241}'s beta-minus decay to {@code Am-241}, already defined natively in GTST (see
 * {@code Elements.Pu241}'s {@code decayModes}). See {@link UraniumSeriesMaterials} for the shared conventions
 * (branch-rounding threshold, lazy daughter resolution, real-world seconds not yet time-compressed); isotope data
 * itself lives in {@link com.windows2000.gtnuclear.api.unification.materials.GTNLElements}.
 * <p>
 * One branch sits at/above the 0.1% inclusion threshold: Bismuth-213 (97.80% beta-minus -> Po-213, 2.20% alpha ->
 * Tl-209); both sides reconverge on Pb-209 one step later.
 * <p>
 * {@code Bismuth-209}, this chain's endpoint, is technically alpha-radioactive in reality (half-life
 * ~2.01e19 years -- over a billion times the age of the universe) but is modeled here as stable
 * ({@code halfLifeSeconds = -1}, no {@link gregtech.api.unification.DecayMode}s): at that timescale it is
 * unconditionally irrelevant to any in-game decay simulation, real-world seconds or compressed.
 */
public class NeptuniumSeriesMaterials {

    public static void register() {
        Americium241 = Material.builder(40, gtnlID("americium_241"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(1449))
                .color(0xFF6478).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(Am241)
                .build();

        Neptunium237 = Material.builder(41, gtnlID("neptunium_237"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(917))
                .color(0x64A0FF).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(Np237)
                .build();

        Protactinium233 = Material.builder(42, gtnlID("protactinium_233"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(1841))
                .color(0xDCC8FF).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(Pa233)
                .build();

        Uranium233 = Material.builder(43, gtnlID("uranium_233"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(1405))
                .color(0x28E028).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(U233)
                .build();

        Thorium229 = Material.builder(44, gtnlID("thorium_229"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(2023))
                .color(0xB0B0B0).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(Th229)
                .build();

        Radium225 = Material.builder(45, gtnlID("radium_225"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(973))
                .color(0xFFE6E6).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(Ra225)
                .build();

        Actinium225 = Material.builder(46, gtnlID("actinium_225"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(1323))
                .color(0xB4FFE0).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(Ac225)
                .build();

        Francium221 = Material.builder(47, gtnlID("francium_221"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(300))
                .color(0xFFE0A0).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(Fr221)
                .build();

        Astatine217 = Material.builder(48, gtnlID("astatine_217"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(575))
                .color(0x785A46).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(At217)
                .build();

        Bismuth213 = Material.builder(49, gtnlID("bismuth_213"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(545))
                .color(0xDCC0FF).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(Bi213)
                .build();

        Polonium213 = Material.builder(50, gtnlID("polonium_213"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(527))
                .color(0xEED0A0).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(Po213)
                .build();

        Thallium209 = Material.builder(51, gtnlID("thallium_209"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(577))
                .color(0xDCE6FF).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(Tl209)
                .build();

        Lead209 = Material.builder(52, gtnlID("lead_209"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(601))
                .color(0x8888B0).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(Pb209)
                .build();

        // Endpoint of the series -- modeled as stable, see this class's own JavaDoc.
        Bismuth209 = Material.builder(53, gtnlID("bismuth_209"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(545))
                .color(0xC8AAEE).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(Bi209)
                .build();
    }

    private NeptuniumSeriesMaterials() {}
}
