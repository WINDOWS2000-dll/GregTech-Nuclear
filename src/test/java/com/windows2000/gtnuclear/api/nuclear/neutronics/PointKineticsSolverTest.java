package com.windows2000.gtnuclear.api.nuclear.neutronics;

import gregtech.api.unification.Elements;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;

class PointKineticsSolverTest {

    private static final double EPSILON = 1e-6;

    private static PointKineticsConstants pureUranium235() {
        return PointKineticsConstants.fissionWeighted(Collections.singletonMap(Elements.U235, 1.0),
                PointKineticsConstants.STANDARD_PROMPT_NEUTRON_LIFETIME);
    }

    @Test
    void criticalReactorAtEquilibriumStaysConstant() {
        PointKineticsConstants constants = pureUranium235();
        PointKineticsState state = PointKineticsState.equilibrium(1.0, constants);

        PointKineticsState next = PointKineticsSolver.advance(state, 0.0, 1.0, constants);

        assertThat(next.relativeNeutronPopulation, closeTo(1.0, EPSILON));
        for (int i = 0; i < constants.groupCount(); i++) {
            double expected = state.precursorConcentrations[i];
            assertThat(next.precursorConcentrations[i], closeTo(expected, Math.abs(expected) * 1e-6));
        }
    }

    @Test
    void positiveReactivityBelowPromptCriticalIncreasesPopulation() {
        PointKineticsConstants constants = pureUranium235();
        PointKineticsState state = PointKineticsState.equilibrium(1.0, constants);

        for (int i = 0; i < 100; i++) {
            state = PointKineticsSolver.advance(state, 0.001, 1.0, constants);
        }

        assertThat(state.relativeNeutronPopulation, greaterThan(1.0));
    }

    @Test
    void negativeReactivityDecreasesPopulation() {
        PointKineticsConstants constants = pureUranium235();
        PointKineticsState state = PointKineticsState.equilibrium(1.0, constants);

        for (int i = 0; i < 100; i++) {
            state = PointKineticsSolver.advance(state, -0.001, 1.0, constants);
        }

        assertThat(state.relativeNeutronPopulation, lessThan(1.0));
    }

    @Test
    void staysNonNegativeAndFiniteUnderDeepScram() {
        PointKineticsConstants constants = pureUranium235();
        PointKineticsState state = PointKineticsState.equilibrium(1.0, constants);

        for (int i = 0; i < 50; i++) {
            state = PointKineticsSolver.advance(state, -1.0, 1.0, constants);
            assertThat(state.relativeNeutronPopulation, greaterThanOrEqualTo(0.0));
            assertThat(Double.isFinite(state.relativeNeutronPopulation), is(true));
        }

        assertThat(state.relativeNeutronPopulation, lessThan(1.0));
    }

    @Test
    void stronglyPromptSupercriticalReactivityGrowsInsteadOfClampingToZero() {
        // Reproduces a real playtest crash scenario (2026-09-22): a minimum-size HEU core with all rods withdrawn
        // computed +26795 pcm reactivity, and even this far more modest +1000 pcm is already enough to flip
        // un-subdivided backward Euler's coefficient negative over a single 0.05s Minecraft tick (many orders of
        // magnitude longer than the ~2e-5s prompt neutron lifetime): 1 - 0.05*(0.01-0.0065)/2e-5 = -7.75. The old
        // single-step code floored that negative result to exactly zero via the population's own non-negativity
        // clamp, silently freezing power at zero forever regardless of how supercritical the core was. (26795 pcm
        // itself is deliberately not used here -- integrated for a full untempered tick it legitimately overflows
        // a double, which is a separate, expected consequence for this mod's later reactivity-feedback work to
        // address, not something this solver should mask.)
        PointKineticsConstants constants = pureUranium235();
        PointKineticsState seed = PointKineticsState.equilibrium(1e-6, constants);

        PointKineticsState next = PointKineticsSolver.advance(seed, 0.01, 1.0 / 20.0, constants);

        assertThat(Double.isFinite(next.relativeNeutronPopulation), is(true));
        assertThat(next.relativeNeutronPopulation, greaterThan(seed.relativeNeutronPopulation));
    }

    @Test
    void plutonium239FuelHasLowerTotalDelayedNeutronFractionThanUranium235() {
        // Real, well-known reactor physics fact this design must reproduce: Pu-239's delayed neutron fraction is
        // roughly a third of U-235's, which is exactly why the class no longer offers a single fuel-agnostic
        // default -- fuel composition materially changes the reactor's kinetic behavior.
        PointKineticsConstants uranium = pureUranium235();
        PointKineticsConstants plutonium = PointKineticsConstants.fissionWeighted(
                Collections.singletonMap(Elements.Pu239, 1.0), PointKineticsConstants.STANDARD_PROMPT_NEUTRON_LIFETIME);

        assertThat(plutonium.totalDelayedNeutronFraction, lessThan(uranium.totalDelayedNeutronFraction));
    }

    @Test
    void mixedFuelBetaIsBetweenPureIsotopeValues() {
        PointKineticsConstants uranium = pureUranium235();
        PointKineticsConstants plutonium = PointKineticsConstants.fissionWeighted(
                Collections.singletonMap(Elements.Pu239, 1.0), PointKineticsConstants.STANDARD_PROMPT_NEUTRON_LIFETIME);

        Map<gregtech.api.unification.Element, Double> mix = new HashMap<>();
        mix.put(Elements.U235, 0.5);
        mix.put(Elements.Pu239, 0.5);
        PointKineticsConstants mixed = PointKineticsConstants.fissionWeighted(mix,
                PointKineticsConstants.STANDARD_PROMPT_NEUTRON_LIFETIME);

        assertThat(mixed.totalDelayedNeutronFraction, lessThan(uranium.totalDelayedNeutronFraction));
        assertThat(mixed.totalDelayedNeutronFraction, greaterThan(plutonium.totalDelayedNeutronFraction));
    }
}
