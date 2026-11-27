package com.windows2000.gtnuclear.api.nuclear.neutronics;

import gregtech.api.unification.Element;
import gregtech.api.unification.Elements;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Stage 2-1 (source term, {@link NeutronSourceStrength}) and stage 2-2 (fission-rate weighted beta). */
class NeutronSourceAndBetaTest {

    private static final double LAMBDA = PointKineticsConstants.STANDARD_PROMPT_NEUTRON_LIFETIME;

    @Test
    void cf252StrengthIsPhysical() {
        // ~2.3E12 n/s per gram (2.3E9 per milligram) is the well-known Cf-252 specific neutron emission.
        assertEquals(2.31e12, NeutronSourceStrength.cf252NeutronsPerSecond(1.0), 0.03e12);
        assertEquals(2.31e9, NeutronSourceStrength.cf252NeutronsPerSecond(1.0e-3), 0.03e9);
        assertEquals(0.0, NeutronSourceStrength.cf252NeutronsPerSecond(0.0));
    }

    @Test
    void intrinsicSourceSumsKnownYieldsOnly() {
        Map<String, Double> grams = new HashMap<>();
        grams.put("U-238", 1.0e6);
        grams.put("Pu-240", 10.0);
        grams.put("Xx-999", 1.0e9);
        assertEquals(1.36e4 + 1.02e4, NeutronSourceStrength.intrinsicNeutronsPerSecond(grams), 1.0);
    }

    @Test
    void relativeRateUsesRatedPopulation() {
        // N0 = nu * (P / E_f) * Lambda.
        double power = 5.05e9;
        double ratedPopulation = NeutronSourceStrength.NEUTRONS_PER_FISSION * power /
                NeutronSourceStrength.ENERGY_PER_FISSION_JOULES * LAMBDA;
        assertEquals(1.0 / ratedPopulation, NeutronSourceStrength.relativeRate(1.0, power, LAMBDA), 1e-30);
        assertEquals(0.0, NeutronSourceStrength.relativeRate(1.0, 0.0, LAMBDA));
    }

    @Test
    void sourceHoldsASubcriticalCoreAtItsEquilibriumLevel() {
        PointKineticsConstants constants = PointKineticsConstants.fissionWeighted(
                Collections.singletonMap(Elements.U235, 1.0), LAMBDA);
        double source = 3.0e-7;
        double rho = -0.05;
        PointKineticsState state = new PointKineticsState(0.0, new double[constants.groupCount()]);
        for (int i = 0; i < 20 * 3600; i++) {
            state = PointKineticsSolver.advance(state, rho, 0.05, constants, source);
        }
        // n = S * Lambda / -rho once the precursors have settled.
        assertEquals(source * LAMBDA / -rho, state.relativeNeutronPopulation, 1e-3 * source * LAMBDA / -rho);
    }

    @Test
    void sourceStartsATrueZeroCoreWithoutSeeding() {
        PointKineticsConstants constants = PointKineticsConstants.fissionWeighted(
                Collections.singletonMap(Elements.U235, 1.0), LAMBDA);
        PointKineticsState state = new PointKineticsState(0.0, new double[constants.groupCount()]);
        state = PointKineticsSolver.advance(state, -0.01, 0.05, constants, 1.0e-6);
        assertTrue(state.relativeNeutronPopulation > 0);
    }

    @Test
    void betaIsWeightedByFissionRateNotAmount() {
        // LEU-like: U-238 is 95% of the atoms but (here) only ~1% of the fissions.
        Map<Element, Double> rates = new HashMap<>();
        rates.put(Elements.U235, 99.0);
        rates.put(Elements.U238, 1.0);
        PointKineticsConstants constants = PointKineticsConstants.fissionWeighted(rates, LAMBDA);
        double betaU235 = 0.0065;
        double betaU238 = 0.0140;
        assertEquals(0.99 * betaU235 + 0.01 * betaU238, constants.totalDelayedNeutronFraction, 2e-5);
    }

    @Test
    void accumulatedFissionRatesSkipNonFissileAndUseBothGroups() {
        Map<Element, Double> rates = new HashMap<>();
        Map<Element, Double> amounts = new HashMap<>();
        amounts.put(Elements.U235, 0.045);
        amounts.put(Elements.U238, 0.955);
        PointKineticsConstants.accumulateFissionRates(rates, amounts, 2.0, 3.0);
        double u235 = 0.045 * (Elements.U235.crossSections.fastFissionBarns * 2.0 +
                Elements.U235.crossSections.thermalFissionBarns * 3.0);
        assertEquals(u235, rates.get(Elements.U235), 1e-9);
        assertTrue(rates.get(Elements.U235) > 10 * rates.get(Elements.U238));

        // A second region adds on top.
        PointKineticsConstants.accumulateFissionRates(rates, amounts, 2.0, 3.0);
        assertEquals(2 * u235, rates.get(Elements.U235), 1e-9);
    }
}
