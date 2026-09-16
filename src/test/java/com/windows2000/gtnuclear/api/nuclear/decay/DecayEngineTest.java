package com.windows2000.gtnuclear.api.nuclear.decay;

import gregtech.api.unification.DecayMode;
import gregtech.api.unification.DecayType;
import gregtech.api.unification.Element;
import gregtech.api.unification.Elements;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;

/**
 * Sanity checks for {@link DecayEngine} using synthetic (not real-world) test nuclides registered via
 * {@link Elements#add}, so these don't depend on -- or interfere with -- this mod's real isotope data.
 */
class DecayEngineTest {

    private static final double EPSILON = 1e-6;

    @Test
    void stableNuclideNeverDecays() {
        Element stable = Elements.add(1, 1, "DecayEngineTestStableA", "DTSA", true);
        Map<Element, Double> composition = Collections.singletonMap(stable, 100.0);

        Map<Element, Double> result = DecayEngine.advance(composition, 1e12);

        assertThat(result.get(stable), closeTo(100.0, EPSILON));
    }

    @Test
    void singleStepDecayConservesTotalCountAndMatchesExponentialLaw() {
        Element daughter = Elements.add(2, 2, "DecayEngineTestStableB", "DTSB", true);
        double halfLife = 1000.0;
        Element parent = Elements.add(1, 2, halfLife,
                Collections.singletonList(new DecayMode(DecayType.ALPHA, 1.0, "DTSB", 0.0)),
                null, "DecayEngineTestParentB", "DTPB", true);

        Map<Element, Double> composition = Collections.singletonMap(parent, 100.0);

        // After exactly one half-life, half the parent should remain, half should have become the daughter.
        Map<Element, Double> result = DecayEngine.advance(composition, halfLife);
        assertThat(result.get(parent), closeTo(50.0, EPSILON));
        assertThat(result.get(daughter), closeTo(50.0, EPSILON));
        assertThat(result.get(parent) + result.get(daughter), closeTo(100.0, EPSILON));

        // After a long time, essentially everything should have decayed to the (stable) daughter.
        Map<Element, Double> longResult = DecayEngine.advance(composition, halfLife * 50);
        assertThat(longResult.getOrDefault(parent, 0.0), closeTo(0.0, EPSILON));
        assertThat(longResult.get(daughter), closeTo(100.0, EPSILON));
    }

    @Test
    void threeStepChainConservesTotalCount() {
        double halfLifeA = 500.0;
        double halfLifeB = 2000.0;
        Element stableC = Elements.add(3, 3, "DecayEngineTestStableC", "DTSC", true);
        Element nuclideB = Elements.add(2, 3, halfLifeB,
                Collections.singletonList(new DecayMode(DecayType.BETA_MINUS, 1.0, "DTSC", 0.0)),
                null, "DecayEngineTestNuclideB", "DTNB", true);
        Element nuclideA = Elements.add(1, 3, halfLifeA,
                Collections.singletonList(new DecayMode(DecayType.ALPHA, 1.0, "DTNB", 0.0)),
                null, "DecayEngineTestNuclideA", "DTNA", true);

        Map<Element, Double> composition = Collections.singletonMap(nuclideA, 100.0);

        for (double t : new double[] { 100.0, 500.0, 3000.0, 100000.0 }) {
            Map<Element, Double> result = DecayEngine.advance(composition, t);
            double total = result.getOrDefault(nuclideA, 0.0) + result.getOrDefault(nuclideB, 0.0) +
                    result.getOrDefault(stableC, 0.0);
            assertThat("total count at t=" + t, total, closeTo(100.0, EPSILON));
        }

        // At a very long time, everything should have settled in the stable end of the chain.
        Map<Element, Double> longResult = DecayEngine.advance(composition, 1_000_000.0);
        assertThat(longResult.get(stableC), closeTo(100.0, EPSILON));
    }

    @Test
    void branchingSplitsByBranchingRatioAndConservesTotalCount() {
        Element branchTarget1 = Elements.add(5, 5, "DecayEngineTestBranch-1", "DTB1", true);
        Element branchTarget2 = Elements.add(6, 6, "DecayEngineTestBranch-2", "DTB2", true);
        double halfLife = 1000.0;
        Element root = Elements.add(4, 4, halfLife,
                Arrays.asList(
                        new DecayMode(DecayType.BETA_MINUS, 0.7, "DTB1", 0.0),
                        new DecayMode(DecayType.ALPHA, 0.3, "DTB2", 0.0)),
                null, "DecayEngineTestBranchRoot", "DTBR", true);

        Map<Element, Double> composition = Collections.singletonMap(root, 100.0);

        // Far past the root's half-life, essentially everything should have branched, split 70/30.
        Map<Element, Double> result = DecayEngine.advance(composition, halfLife * 50);
        assertThat(result.getOrDefault(root, 0.0), closeTo(0.0, EPSILON));
        assertThat(result.get(branchTarget1), closeTo(70.0, EPSILON));
        assertThat(result.get(branchTarget2), closeTo(30.0, EPSILON));
    }

    @Test
    void reAdvancingFromASnapshotMatchesAdvancingOnceForTheCombinedTime() {
        // The "memoryless" property this engine's storage design (see DecayEngine's own JavaDoc) depends on:
        // advancing by t1 then by t2 from the resulting snapshot must equal advancing directly by t1+t2.
        Element daughter = Elements.add(8, 8, "DecayEngineTestStableD", "DTSD", true);
        double halfLife = 750.0;
        Element parent = Elements.add(7, 7, halfLife,
                Collections.singletonList(new DecayMode(DecayType.ALPHA, 1.0, "DTSD", 0.0)),
                null, "DecayEngineTestParentD", "DTPD", true);

        Map<Element, Double> initial = Collections.singletonMap(parent, 100.0);
        double t1 = 300.0;
        double t2 = 900.0;

        Map<Element, Double> direct = DecayEngine.advance(initial, t1 + t2);

        Map<Element, Double> intermediate = DecayEngine.advance(initial, t1);
        Map<Element, Double> stepwise = DecayEngine.advance(new HashMap<>(intermediate), t2);

        assertThat(stepwise.get(parent), closeTo(direct.get(parent), EPSILON));
        assertThat(stepwise.get(daughter), closeTo(direct.get(daughter), EPSILON));
    }
}
