package com.windows2000.gtnuclear.api.nuclear.fuel;

import gregtech.api.unification.Element;
import gregtech.api.unification.Elements;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;

class FuelBurnupEngineTest {

    // Elements.U235/U238/Pu239 are GTST-native static fields, safely usable in unit tests with no Forge
    // dependency. Their real capture daughters (U-236/Pu-240) are only registered via this mod's own
    // GTNLMaterials, which is populated by a Forge MaterialEvent that never fires under `./gradlew test` --
    // so, mirroring DecayEngineTest's established pattern, these tests register their own stable stand-in
    // daughter isotopes directly and pass them in as this method's now-explicit captureDaughters parameter.
    private static final Element TEST_U236 = Elements.add(92, 144, "FuelBurnupTestUranium-236", "TU236", true);
    private static final Element TEST_PU240 = Elements.add(94, 146, "FuelBurnupTestPlutonium-240", "TPU240", true);

    private static final Map<Element, Element> CAPTURE_DAUGHTERS = buildCaptureDaughters();

    private static Map<Element, Element> buildCaptureDaughters() {
        Map<Element, Element> daughters = new HashMap<>();
        daughters.put(Elements.U235, TEST_U236);
        daughters.put(Elements.U238, Elements.Pu239);
        daughters.put(Elements.Pu239, TEST_PU240);
        return daughters;
    }

    @Test
    void zeroPowerLeavesCompositionUnchanged() {
        Map<Element, Double> composition = Collections.singletonMap(Elements.U235, 100.0);
        Map<Element, Double> next = FuelBurnupEngine.advance(composition, CAPTURE_DAUGHTERS, 1.0, 1.0, 0.0, 1.0, 20.0);
        assertThat(next.get(Elements.U235), closeTo(100.0, 1e-9));
    }

    @Test
    void zeroFluxLeavesCompositionUnchanged() {
        Map<Element, Double> composition = Collections.singletonMap(Elements.U235, 100.0);
        Map<Element, Double> next = FuelBurnupEngine.advance(composition, CAPTURE_DAUGHTERS, 0.0, 0.0, 1.0, 1.0, 20.0);
        assertThat(next.get(Elements.U235), closeTo(100.0, 1e-9));
    }

    @Test
    void reactingIsotopeDepletesAndProducesDaughter() {
        Map<Element, Double> composition = Collections.singletonMap(Elements.U235, 100.0);
        Map<Element, Double> next = FuelBurnupEngine.advance(composition, CAPTURE_DAUGHTERS, 1.0, 1.0, 1.0, 1.0e-6,
                20.0);
        assertThat(next.get(Elements.U235), is(org.hamcrest.Matchers.lessThan(100.0)));
        assertThat(next.getOrDefault(TEST_U236, 0.0), greaterThan(0.0));
    }

    @Test
    void massIsConservedBetweenParentLossAndDaughterGainForCaptureOnlyIsotope() {
        // U-238 has essentially zero thermal fission cross-section but a small, non-negligible *fast* fission
        // cross-section (real value 0.55b) -- so this only holds to within ~1% under a thermal-flux-dominated
        // spectrum (this reactor design's moderated core, not an equal-flux abstraction); under such a spectrum
        // nearly all reacted mass should become Pu-239 (capture-without-fission), not vanish as "fissioned."
        Map<Element, Double> composition = Collections.singletonMap(Elements.U238, 100.0);
        Map<Element, Double> next = FuelBurnupEngine.advance(composition, CAPTURE_DAUGHTERS, 0.1, 2.0, 1.0, 1.0e-6,
                20.0);
        double lost = 100.0 - next.get(Elements.U238);
        double gained = next.getOrDefault(Elements.Pu239, 0.0);
        assertThat(gained, closeTo(lost, lost * 0.02));
    }

    @Test
    void terminalPoisonIsUnaffectedRegardlessOfFluxOrPower() {
        // TEST_U236 has no entry of its own in CAPTURE_DAUGHTERS, matching the real U-236's terminal-poison
        // status in this mod's burnup chain.
        Map<Element, Double> composition = Collections.singletonMap(TEST_U236, 50.0);
        Map<Element, Double> next = FuelBurnupEngine.advance(composition, CAPTURE_DAUGHTERS, 1.0, 1.0, 1.0, 1.0, 20.0);
        assertThat(next.get(TEST_U236), closeTo(50.0, 1e-9));
    }

    @Test
    void reactedAmountNeverExceedsWhatWasPresent() {
        Map<Element, Double> composition = Collections.singletonMap(Elements.U235, 1.0);
        // Deliberately huge rate coefficient to try to force over-depletion.
        Map<Element, Double> next = FuelBurnupEngine.advance(composition, CAPTURE_DAUGHTERS, 1.0, 1.0, 1.0, 1.0e6,
                20.0);
        assertThat(next.get(Elements.U235), is(org.hamcrest.Matchers.greaterThanOrEqualTo(0.0)));
    }

    @Test
    void higherThermalFluxBurnsThermalHeavyIsotopeFasterThanFastFlux() {
        Map<Element, Double> composition = Collections.singletonMap(Elements.U235, 100.0);
        Map<Element, Double> thermalHeavy = FuelBurnupEngine.advance(composition, CAPTURE_DAUGHTERS, 0.1, 2.0, 1.0,
                1.0e-6, 20.0);
        Map<Element, Double> fastHeavy = FuelBurnupEngine.advance(composition, CAPTURE_DAUGHTERS, 2.0, 0.1, 1.0, 1.0e-6,
                20.0);
        double thermalHeavyDepletion = 100.0 - thermalHeavy.get(Elements.U235);
        double fastHeavyDepletion = 100.0 - fastHeavy.get(Elements.U235);
        // U-235's thermal cross-sections dwarf its fast ones, so thermal-flux-heavy conditions should burn it
        // faster despite both cases having the same total flux magnitude (0.1+2.0 vs 2.0+0.1).
        assertThat(thermalHeavyDepletion, greaterThan(fastHeavyDepletion));
    }

    @Test
    void mixedCompositionEachIsotopeReactsIndependently() {
        Map<Element, Double> composition = new LinkedHashMap<>();
        composition.put(Elements.U235, 10.0);
        composition.put(Elements.U238, 90.0);
        Map<Element, Double> next = FuelBurnupEngine.advance(composition, CAPTURE_DAUGHTERS, 1.0, 1.0, 1.0, 1.0e-6,
                20.0);
        assertThat(next.get(Elements.U235), is(org.hamcrest.Matchers.lessThan(10.0)));
        assertThat(next.get(Elements.U238), is(org.hamcrest.Matchers.lessThan(90.0)));
        assertThat(next.getOrDefault(Elements.Pu239, 0.0), greaterThan(0.0));
    }
}
