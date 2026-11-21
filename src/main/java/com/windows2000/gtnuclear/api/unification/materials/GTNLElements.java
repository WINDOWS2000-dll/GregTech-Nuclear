package com.windows2000.gtnuclear.api.unification.materials;

import gregtech.api.unification.DecayMode;
import gregtech.api.unification.DecayType;
import gregtech.api.unification.Element;
import gregtech.api.unification.NeutronCrossSections;

import java.util.Arrays;
import java.util.Collections;

import static gregtech.api.unification.Elements.add;

/**
 * Isotope {@link Element} registrations for this mod's four natural decay chains and its in-reactor burnup
 * products, following the same pattern as GTST's own {@code Elements} class: {@code public static final} fields
 * populated at class-load time (no Forge lifecycle dependency), rather than being registered inline inside each
 * series' {@code Material.builder(...)} chain. The corresponding {@code *SeriesMaterials}/
 * {@code ReactorBurnupProductMaterials} classes reference these fields directly (e.g.
 * {@code .element(GTNLElements.Th234)}), keeping nuclide data separate from Material/texture/recipe bookkeeping.
 * <p>
 * Field names follow GTST's own convention (see {@code Elements.U238}/{@code Elements.He3}): element symbol +
 * mass number, e.g. {@code Th234} for Thorium-234. {@code daughterElementKey} references inside {@link DecayMode}
 * resolve lazily by symbol, so declaration order here doesn't matter for chain correctness. Branches below ~0.1%
 * are rounded to the dominant path (project convention); half-lives are real-world values in seconds, with
 * in-game time-compression applied elsewhere when actually simulating decay.
 */
public final class GTNLElements {

    private GTNLElements() {}

    // Uranium series (4n+2, headed by the native Elements.U238), IDs 1-14 in GTNLMaterials. See
    // UraniumSeriesMaterials.
    public static final Element Th234 = add(90, 144, 2082240.0,
            Collections.singletonList(new DecayMode(DecayType.BETA_MINUS, 1.0, "Pa-234", 0.273)),
            "Thorium-234", "Th-234", true);
    public static final Element Pa234 = add(91, 143, 69.54,
            Collections.singletonList(new DecayMode(DecayType.BETA_MINUS, 1.0, "U-234", 2.269)),
            "Protactinium-234", "Pa-234", true);
    public static final Element U234 = add(92, 142, 7.74739e12,
            Collections.singletonList(new DecayMode(DecayType.ALPHA, 1.0, "Th-230", 4.859)),
            "Uranium-234", "U-234", true);
    public static final Element Th230 = add(90, 140, 2.379443e12,
            Collections.singletonList(new DecayMode(DecayType.ALPHA, 1.0, "Ra-226", 4.770)),
            "Thorium-230", "Th-230", true);
    public static final Element Ra226 = add(88, 138, 5.04922e10,
            Collections.singletonList(new DecayMode(DecayType.ALPHA, 1.0, "Rn-222", 4.871)),
            "Radium-226", "Ra-226", true);
    public static final Element Rn222 = add(86, 136, 330350.4,
            Collections.singletonList(new DecayMode(DecayType.ALPHA, 1.0, "Po-218", 5.590)),
            "Radon-222", "Rn-222", true);
    public static final Element Po218 = add(84, 134, 186.0,
            Collections.singletonList(new DecayMode(DecayType.ALPHA, 1.0, "Pb-214", 6.115)),
            "Polonium-218", "Po-218", true);
    public static final Element Pb214 = add(82, 132, 1608.0,
            Collections.singletonList(new DecayMode(DecayType.BETA_MINUS, 1.0, "Bi-214", 1.024)),
            "Lead-214", "Pb-214", true);
    public static final Element Bi214 = add(83, 131, 1194.0,
            Collections.singletonList(new DecayMode(DecayType.BETA_MINUS, 1.0, "Po-214", 3.270)),
            "Bismuth-214", "Bi-214", true);
    public static final Element Po214 = add(84, 130, 1.643e-4,
            Collections.singletonList(new DecayMode(DecayType.ALPHA, 1.0, "Pb-210", 7.833)),
            "Polonium-214", "Po-214", true);
    public static final Element Pb210 = add(82, 128, 7.0373e8,
            Collections.singletonList(new DecayMode(DecayType.BETA_MINUS, 1.0, "Bi-210", 0.064)),
            "Lead-210", "Pb-210", true);
    public static final Element Bi210 = add(83, 127, 433036.8,
            Collections.singletonList(new DecayMode(DecayType.BETA_MINUS, 1.0, "Po-210", 1.163)),
            "Bismuth-210", "Bi-210", true);
    public static final Element Po210 = add(84, 126, 1.1955686e7,
            Collections.singletonList(new DecayMode(DecayType.ALPHA, 1.0, "Pb-206", 5.407)),
            "Polonium-210", "Po-210", true);
    // Stable endpoint of the series.
    public static final Element Pb206 = add(82, 124, "Lead-206", "Pb-206", true);

    // Actinium series (4n+3, headed by the native Elements.U235), IDs 15-27. See ActiniumSeriesMaterials. Two
    // branches sit at/above the 0.1% inclusion threshold and are modeled with two DecayModes each rather than
    // rounded away.
    public static final Element Th231 = add(90, 141, 91872.0,
            Collections.singletonList(new DecayMode(DecayType.BETA_MINUS, 1.0, "Pa-231", 0.389)),
            "Thorium-231", "Th-231", true);
    public static final Element Pa231 = add(91, 140, 1.033827e12,
            Collections.singletonList(new DecayMode(DecayType.ALPHA, 1.0, "Ac-227", 5.150)),
            "Protactinium-231", "Pa-231", true);
    public static final Element Ac227 = add(89, 138, 6.8707e8,
            Arrays.asList(
                    new DecayMode(DecayType.BETA_MINUS, 0.9862, "Th-227", 0.045),
                    new DecayMode(DecayType.ALPHA, 0.0138, "Fr-223", 5.042)),
            "Actinium-227", "Ac-227", true);
    public static final Element Fr223 = add(87, 136, 1320.0,
            Collections.singletonList(new DecayMode(DecayType.BETA_MINUS, 1.0, "Ra-223", 1.149)),
            "Francium-223", "Fr-223", true);
    public static final Element Th227 = add(90, 137, 1614432.0,
            Collections.singletonList(new DecayMode(DecayType.ALPHA, 1.0, "Ra-223", 6.038)),
            "Thorium-227", "Th-227", true);
    public static final Element Ra223 = add(88, 135, 987552.0,
            Collections.singletonList(new DecayMode(DecayType.ALPHA, 1.0, "Rn-219", 5.979)),
            "Radium-223", "Ra-223", true);
    public static final Element Rn219 = add(86, 133, 3.96,
            Collections.singletonList(new DecayMode(DecayType.ALPHA, 1.0, "Po-215", 6.946)),
            "Radon-219", "Rn-219", true);
    public static final Element Po215 = add(84, 131, 1.781e-3,
            Collections.singletonList(new DecayMode(DecayType.ALPHA, 1.0, "Pb-211", 7.527)),
            "Polonium-215", "Po-215", true);
    public static final Element Pb211 = add(82, 129, 2166.0,
            Collections.singletonList(new DecayMode(DecayType.BETA_MINUS, 1.0, "Bi-211", 1.367)),
            "Lead-211", "Pb-211", true);
    public static final Element Bi211 = add(83, 128, 128.4,
            Arrays.asList(
                    new DecayMode(DecayType.ALPHA, 0.99724, "Tl-207", 6.751),
                    new DecayMode(DecayType.BETA_MINUS, 0.00276, "Po-211", 0.575)),
            "Bismuth-211", "Bi-211", true);
    public static final Element Po211 = add(84, 127, 0.516,
            Collections.singletonList(new DecayMode(DecayType.ALPHA, 1.0, "Pb-207", 7.594)),
            "Polonium-211", "Po-211", true);
    public static final Element Tl207 = add(81, 126, 286.2,
            Collections.singletonList(new DecayMode(DecayType.BETA_MINUS, 1.0, "Pb-207", 1.418)),
            "Thallium-207", "Tl-207", true);
    // Stable endpoint of the series.
    public static final Element Pb207 = add(82, 125, "Lead-207", "Pb-207", true);

    // Thorium series (4n, headed by Th232 itself -- GTST has no native isotope-flagged "Th-232" to extend), IDs
    // 28-39. See ThoriumSeriesMaterials. One branch sits at/above the 0.1% inclusion threshold.
    public static final Element Th232 = add(90, 142, 4.4339e17,
            Collections.singletonList(new DecayMode(DecayType.ALPHA, 1.0, "Ra-228", 4.083)),
            "Thorium-232", "Th-232", true);
    public static final Element Ra228 = add(88, 140, 1.81456e8,
            Collections.singletonList(new DecayMode(DecayType.BETA_MINUS, 1.0, "Ac-228", 0.046)),
            "Radium-228", "Ra-228", true);
    public static final Element Ac228 = add(89, 139, 22140.0,
            Collections.singletonList(new DecayMode(DecayType.BETA_MINUS, 1.0, "Th-228", 2.124)),
            "Actinium-228", "Ac-228", true);
    public static final Element Th228 = add(90, 138, 6.0330e7,
            Collections.singletonList(new DecayMode(DecayType.ALPHA, 1.0, "Ra-224", 5.520)),
            "Thorium-228", "Th-228", true);
    public static final Element Ra224 = add(88, 136, 313876.16,
            Collections.singletonList(new DecayMode(DecayType.ALPHA, 1.0, "Rn-220", 5.789)),
            "Radium-224", "Ra-224", true);
    public static final Element Rn220 = add(86, 134, 55.6,
            Collections.singletonList(new DecayMode(DecayType.ALPHA, 1.0, "Po-216", 6.405)),
            "Radon-220", "Rn-220", true);
    public static final Element Po216 = add(84, 132, 0.145,
            Collections.singletonList(new DecayMode(DecayType.ALPHA, 1.0, "Pb-212", 6.906)),
            "Polonium-216", "Po-216", true);
    public static final Element Pb212 = add(82, 130, 38304.0,
            Collections.singletonList(new DecayMode(DecayType.BETA_MINUS, 1.0, "Bi-212", 0.570)),
            "Lead-212", "Pb-212", true);
    public static final Element Bi212 = add(83, 129, 3633.0,
            Arrays.asList(
                    new DecayMode(DecayType.BETA_MINUS, 0.6406, "Po-212", 2.254),
                    new DecayMode(DecayType.ALPHA, 0.3594, "Tl-208", 6.207)),
            "Bismuth-212", "Bi-212", true);
    public static final Element Po212 = add(84, 128, 2.99e-7,
            Collections.singletonList(new DecayMode(DecayType.ALPHA, 1.0, "Pb-208", 8.954)),
            "Polonium-212", "Po-212", true);
    public static final Element Tl208 = add(81, 127, 183.18,
            Collections.singletonList(new DecayMode(DecayType.BETA_MINUS, 1.0, "Pb-208", 4.999)),
            "Thallium-208", "Tl-208", true);
    // Stable endpoint of the series.
    public static final Element Pb208 = add(82, 126, "Lead-208", "Pb-208", true);

    // Neptunium series (4n+1, reached from the native Elements.Pu241 -> Am241 -> Np237), IDs 40-53. See
    // NeptuniumSeriesMaterials. One branch sits at/above the 0.1% inclusion threshold.
    public static final Element Am241 = add(95, 146, 1.36392e10,
            Collections.singletonList(new DecayMode(DecayType.ALPHA, 1.0, "Np-237", 5.638)),
            "Americium-241", "Am-241", true);
    public static final Element Np237 = add(93, 144, 6.76595e13,
            Collections.singletonList(new DecayMode(DecayType.ALPHA, 1.0, "Pa-233", 4.959)),
            "Neptunium-237", "Np-237", true);
    public static final Element Pa233 = add(91, 142, 2330640.0,
            Collections.singletonList(new DecayMode(DecayType.BETA_MINUS, 1.0, "U-233", 0.571)),
            "Protactinium-233", "Pa-233", true);
    public static final Element U233 = add(92, 141, 5.02397e12,
            Collections.singletonList(new DecayMode(DecayType.ALPHA, 1.0, "Th-229", 4.909)),
            "Uranium-233", "U-233", true);
    public static final Element Th229 = add(90, 139, 2.31633e11,
            Collections.singletonList(new DecayMode(DecayType.ALPHA, 1.0, "Ra-225", 5.168)),
            "Thorium-229", "Th-229", true);
    public static final Element Ra225 = add(88, 137, 1287360.0,
            Collections.singletonList(new DecayMode(DecayType.BETA_MINUS, 1.0, "Ac-225", 0.36)),
            "Radium-225", "Ra-225", true);
    public static final Element Ac225 = add(89, 136, 857088.0,
            Collections.singletonList(new DecayMode(DecayType.ALPHA, 1.0, "Fr-221", 5.935)),
            "Actinium-225", "Ac-225", true);
    public static final Element Fr221 = add(87, 134, 294.0,
            Collections.singletonList(new DecayMode(DecayType.ALPHA, 1.0, "At-217", 6.457)),
            "Francium-221", "Fr-221", true);
    public static final Element At217 = add(85, 132, 0.0323,
            Collections.singletonList(new DecayMode(DecayType.ALPHA, 1.0, "Bi-213", 7.202)),
            "Astatine-217", "At-217", true);
    public static final Element Bi213 = add(83, 130, 2735.4,
            Arrays.asList(
                    new DecayMode(DecayType.BETA_MINUS, 0.9780, "Po-213", 1.423),
                    new DecayMode(DecayType.ALPHA, 0.0220, "Tl-209", 5.87)),
            "Bismuth-213", "Bi-213", true);
    public static final Element Po213 = add(84, 129, 3.72e-6,
            Collections.singletonList(new DecayMode(DecayType.ALPHA, 1.0, "Pb-209", 8.536)),
            "Polonium-213", "Po-213", true);
    public static final Element Tl209 = add(81, 128, 129.66,
            Collections.singletonList(new DecayMode(DecayType.BETA_MINUS, 1.0, "Pb-209", 3.99)),
            "Thallium-209", "Tl-209", true);
    public static final Element Pb209 = add(82, 127, 11642.4,
            Collections.singletonList(new DecayMode(DecayType.BETA_MINUS, 1.0, "Bi-209", 0.644)),
            "Lead-209", "Pb-209", true);
    // Endpoint of the series -- modeled as stable, see NeptuniumSeriesMaterials' own JavaDoc (real half-life
    // ~2.01e19 years, unconditionally irrelevant to any in-game decay simulation).
    public static final Element Bi209 = add(83, 126, "Bismuth-209", "Bi-209", true);

    // In-reactor burnup products (neutron-capture-without-fission terminal poisons in this mod's fuel burnup
    // simulation), IDs 58-59. See ReactorBurnupProductMaterials.
    public static final Element U236 = add(92, 144, 7.391e14,
            Collections.singletonList(new DecayMode(DecayType.ALPHA, 1.0, "Th-232", 4.572)),
            new NeutronCrossSections(0.0, 5.1, 0.0, 0.06, 9.0, new double[0], new double[0]),
            "Uranium-236", "U-236", true);
    public static final Element Pu240 = add(94, 146, 2.0704e11,
            Collections.singletonList(new DecayMode(DecayType.ALPHA, 1.0, "U-236", 5.256)),
            new NeutronCrossSections(0.0, 289.5, 0.0, 0.3, 9.9, new double[0], new double[0]),
            "Plutonium-240", "Pu-240", true);

    // Startup neutron source isotopes, IDs 60-61. See NeutronSourceMaterials.
    public static final Element Cf252 = add(98, 154, 8.3472e7,
            Arrays.asList(
                    new DecayMode(DecayType.ALPHA, 0.9691, "Cm-248", 6.217),
                    new DecayMode(DecayType.SPONTANEOUS_FISSION, 0.0309, null, 200.0)),
            "Californium-252", "Cf-252", true);
    // Real Cm-248 continues decaying (91.61% alpha -> Pu-244, 8.39% spontaneous fission, half-life ~348,000
    // years) but that chain does not reconnect to any of this mod's four registered primordial series within a
    // useful number of steps -- modeled as stable here, the same "real half-life exists but is out of this mod's
    // simulated scope" treatment already used for Bi-209 (see NeptuniumSeriesMaterials's own JavaDoc).
    public static final Element Cm248 = add(96, 152, "Curium-248", "Cm-248", true);
}
