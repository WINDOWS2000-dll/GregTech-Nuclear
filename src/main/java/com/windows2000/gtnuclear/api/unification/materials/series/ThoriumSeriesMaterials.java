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
 * The thorium series (4n natural decay chain), headed by {@code Thorium-232}. Unlike the uranium/actinium series,
 * GTST has no existing isotope-flagged "Th-232" {@link gregtech.api.unification.Element} to extend -- GTST's own
 * {@code Materials.Thorium} uses the generic, non-isotope {@code Elements.Th} (no mass number, no decay data),
 * exactly like {@code Elements.U}/{@code Elements.Pu} coexist with their specific {@code U238}/{@code Pu239}
 * isotopes. {@code Thorium232} here is a brand-new isotope Material registered entirely from this mod, following
 * that same established GTST convention; it does not replace or alias GTST's plain Thorium. See
 * {@link UraniumSeriesMaterials} for the shared conventions (branch-rounding threshold, lazy daughter resolution,
 * real-world seconds not yet time-compressed).
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
                .element(Elements.add(90, 142, 4.4339e17,
                        Collections.singletonList(new DecayMode(DecayType.ALPHA, 1.0, "Ra-228", 4.083)),
                        null, "Thorium-232", "Th-232", true))
                .build();

        Radium228 = Material.builder(29, gtnlID("radium_228"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(973))
                .color(0xFFC8C8).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(Elements.add(88, 140, 1.81456e8,
                        Collections.singletonList(new DecayMode(DecayType.BETA_MINUS, 1.0, "Ac-228", 0.046)),
                        null, "Radium-228", "Ra-228", true))
                .build();

        Actinium228 = Material.builder(30, gtnlID("actinium_228"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(1323))
                .color(0x82FFC3).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(Elements.add(89, 139, 22140.0,
                        Collections.singletonList(new DecayMode(DecayType.BETA_MINUS, 1.0, "Th-228", 2.124)),
                        null, "Actinium-228", "Ac-228", true))
                .build();

        Thorium228 = Material.builder(31, gtnlID("thorium_228"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(2023))
                .color(0x969696).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(Elements.add(90, 138, 6.0330e7,
                        Collections.singletonList(new DecayMode(DecayType.ALPHA, 1.0, "Ra-224", 5.520)),
                        null, "Thorium-228", "Th-228", true))
                .build();

        Radium224 = Material.builder(32, gtnlID("radium_224"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(973))
                .color(0xFFB4B4).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(Elements.add(88, 136, 313876.16,
                        Collections.singletonList(new DecayMode(DecayType.ALPHA, 1.0, "Rn-220", 5.789)),
                        null, "Radium-224", "Ra-224", true))
                .build();

        Radon220 = Material.builder(33, gtnlID("radon_220"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(202))
                .color(0xD264FF).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(Elements.add(86, 134, 55.6,
                        Collections.singletonList(new DecayMode(DecayType.ALPHA, 1.0, "Po-216", 6.405)),
                        null, "Radon-220", "Rn-220", true))
                .build();

        Polonium216 = Material.builder(34, gtnlID("polonium_216"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(527))
                .color(0xBE9860).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(Elements.add(84, 132, 0.145,
                        Collections.singletonList(new DecayMode(DecayType.ALPHA, 1.0, "Pb-212", 6.906)),
                        null, "Polonium-216", "Po-216", true))
                .build();

        Lead212 = Material.builder(35, gtnlID("lead_212"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(601))
                .color(0x505078).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(Elements.add(82, 130, 38304.0,
                        Collections.singletonList(new DecayMode(DecayType.BETA_MINUS, 1.0, "Bi-212", 0.570)),
                        null, "Lead-212", "Pb-212", true))
                .build();

        Bismuth212 = Material.builder(36, gtnlID("bismuth_212"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(545))
                .color(0x9678E6).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(Elements.add(83, 129, 3633.0,
                        Arrays.asList(
                                new DecayMode(DecayType.BETA_MINUS, 0.6406, "Po-212", 2.254),
                                new DecayMode(DecayType.ALPHA, 0.3594, "Tl-208", 6.207)),
                        null, "Bismuth-212", "Bi-212", true))
                .build();

        Polonium212 = Material.builder(37, gtnlID("polonium_212"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(527))
                .color(0xD4B080).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(Elements.add(84, 128, 2.99e-7,
                        Collections.singletonList(new DecayMode(DecayType.ALPHA, 1.0, "Pb-208", 8.954)),
                        null, "Polonium-212", "Po-212", true))
                .build();

        Thallium208 = Material.builder(38, gtnlID("thallium_208"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(577))
                .color(0xAAB4FF).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(Elements.add(81, 127, 183.18,
                        Collections.singletonList(new DecayMode(DecayType.BETA_MINUS, 1.0, "Pb-208", 4.999)),
                        null, "Thallium-208", "Tl-208", true))
                .build();

        // Stable endpoint of the series.
        Lead208 = Material.builder(39, gtnlID("lead_208"))
                .dust(3).ingot(3)
                .liquid(new FluidBuilder().temperature(601))
                .color(0x606060).iconSet(SHINY)
                .flags(GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_BOLT_SCREW, GENERATE_ROUND, GENERATE_PLATE,
                        GENERATE_DENSE, GENERATE_FOIL)
                .element(Elements.add(82, 126, "Lead-208", "Pb-208", true))
                .build();
    }

    private ThoriumSeriesMaterials() {}
}
