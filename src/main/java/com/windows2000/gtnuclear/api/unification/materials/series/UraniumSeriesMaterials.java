package com.windows2000.gtnuclear.api.unification.materials.series;

import gregtech.api.fluids.FluidBuilder;
import gregtech.api.unification.DecayMode;
import gregtech.api.unification.DecayType;
import gregtech.api.unification.Elements;
import gregtech.api.unification.material.Material;

import java.util.Collections;

import static com.windows2000.gtnuclear.api.unification.materials.GTNLMaterials.*;
import static com.windows2000.gtnuclear.api.util.GTNLUtils.gtnlID;
import static gregtech.api.unification.material.info.MaterialFlags.*;
import static gregtech.api.unification.material.info.MaterialIconSet.SHINY;

/**
 * The uranium series (4n+2 natural decay chain), headed by {@code Uranium-238} (already defined natively in GTST,
 * see {@code Elements.U238} and its {@code decayModes}). Each {@link Material} here corresponds 1:1 to an
 * {@link gregtech.api.unification.Element} isotope registered via {@link Elements#add}; {@code daughterElementKey}
 * references resolve lazily, so declaration order here doesn't matter for chain correctness.
 * <p>
 * Branches below ~0.1% are rounded to the dominant path (project convention, e.g. Po-218's ~0.02% beta branch,
 * Bi-214's ~0.02% alpha branch, Bi-210's ~1.3e-4% alpha branch are all omitted). Pa-234's short-lived metastable
 * state ("Pa-234m", 99.84% branch) is what's modeled here as plain "Pa-234" -- the separate long-lived Pa-234
 * ground state (0.16% branch, itself beta-decaying to U-234 regardless) is not modeled, both paths converge on
 * U-234 immediately either way. Half-lives are real-world values in seconds; the in-game time-compression function
 * is applied elsewhere (not yet implemented) when actually simulating decay, not baked into this data.
 */
public class UraniumSeriesMaterials {

    public static void register() {
        Thorium234 = Material.builder(1, gtnlID("thorium_234"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(2023))
                .color(0xB4B4B4).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(Elements.add(90, 144, 2082240.0,
                        Collections.singletonList(new DecayMode(DecayType.BETA_MINUS, 1.0, "Pa-234", 0.273)),
                        null, "Thorium-234", "Th-234", true))
                .build();

        Protactinium234 = Material.builder(2, gtnlID("protactinium_234"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(1841))
                .color(0xB498FF).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(Elements.add(91, 143, 69.54,
                        Collections.singletonList(new DecayMode(DecayType.BETA_MINUS, 1.0, "U-234", 2.269)),
                        null, "Protactinium-234", "Pa-234", true))
                .build();

        Uranium234 = Material.builder(3, gtnlID("uranium_234"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(1405))
                .color(0x3CFA3C).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(Elements.add(92, 142, 7.74739e12,
                        Collections.singletonList(new DecayMode(DecayType.ALPHA, 1.0, "Th-230", 4.859)),
                        null, "Uranium-234", "U-234", true))
                .build();

        Thorium230 = Material.builder(4, gtnlID("thorium_230"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(2023))
                .color(0x9E9E9E).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(Elements.add(90, 140, 2.379443e12,
                        Collections.singletonList(new DecayMode(DecayType.ALPHA, 1.0, "Ra-226", 4.770)),
                        null, "Thorium-230", "Th-230", true))
                .build();

        Radium226 = Material.builder(5, gtnlID("radium_226"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(973))
                .color(0xFFE1E1).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(Elements.add(88, 138, 5.04922e10,
                        Collections.singletonList(new DecayMode(DecayType.ALPHA, 1.0, "Rn-222", 4.871)),
                        null, "Radium-226", "Ra-226", true))
                .build();

        Radon222 = Material.builder(6, gtnlID("radon_222"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(202))
                .color(0xFF64FF).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(Elements.add(86, 136, 330350.4,
                        Collections.singletonList(new DecayMode(DecayType.ALPHA, 1.0, "Po-218", 5.590)),
                        null, "Radon-222", "Rn-222", true))
                .build();

        Polonium218 = Material.builder(7, gtnlID("polonium_218"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(527))
                .color(0xD2B48C).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(Elements.add(84, 134, 186.0,
                        Collections.singletonList(new DecayMode(DecayType.ALPHA, 1.0, "Pb-214", 6.115)),
                        null, "Polonium-218", "Po-218", true))
                .build();

        Lead214 = Material.builder(8, gtnlID("lead_214"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(601))
                .color(0x6E6E96).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(Elements.add(82, 132, 1608.0,
                        Collections.singletonList(new DecayMode(DecayType.BETA_MINUS, 1.0, "Bi-214", 1.024)),
                        null, "Lead-214", "Pb-214", true))
                .build();

        Bismuth214 = Material.builder(9, gtnlID("bismuth_214"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(545))
                .color(0xC8AAFF).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(Elements.add(83, 131, 1194.0,
                        Collections.singletonList(new DecayMode(DecayType.BETA_MINUS, 1.0, "Po-214", 3.270)),
                        null, "Bismuth-214", "Bi-214", true))
                .build();

        Polonium214 = Material.builder(10, gtnlID("polonium_214"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(527))
                .color(0xE0C8A0).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(Elements.add(84, 130, 1.643e-4,
                        Collections.singletonList(new DecayMode(DecayType.ALPHA, 1.0, "Pb-210", 7.833)),
                        null, "Polonium-214", "Po-214", true))
                .build();

        Lead210 = Material.builder(11, gtnlID("lead_210"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(601))
                .color(0x7878A0).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(Elements.add(82, 128, 7.0373e8,
                        Collections.singletonList(new DecayMode(DecayType.BETA_MINUS, 1.0, "Bi-210", 0.064)),
                        null, "Lead-210", "Pb-210", true))
                .build();

        Bismuth210 = Material.builder(12, gtnlID("bismuth_210"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(545))
                .color(0xD2B4FF).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(Elements.add(83, 127, 433036.8,
                        Collections.singletonList(new DecayMode(DecayType.BETA_MINUS, 1.0, "Po-210", 1.163)),
                        null, "Bismuth-210", "Bi-210", true))
                .build();

        Polonium210 = Material.builder(13, gtnlID("polonium_210"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(527))
                .color(0xEED8B4).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(Elements.add(84, 126, 1.1955686e7,
                        Collections.singletonList(new DecayMode(DecayType.ALPHA, 1.0, "Pb-206", 5.407)),
                        null, "Polonium-210", "Po-210", true))
                .build();

        // Stable endpoint of the series.
        Lead206 = Material.builder(14, gtnlID("lead_206"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(601))
                .color(0x828282).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(Elements.add(82, 124, "Lead-206", "Pb-206", true))
                .build();
    }

    private UraniumSeriesMaterials() {}
}
