package com.windows2000.gtnuclear.api.nuclear.neutronics;

import org.jetbrains.annotations.NotNull;

/**
 * Point kinetics state: relative neutron population (n/n0, proportional to reactor power) and the delayed neutron
 * precursor concentrations for each group, in the same relative units as the neutron population.
 */
public final class PointKineticsState {

    public final double relativeNeutronPopulation;
    @NotNull
    public final double[] precursorConcentrations;

    public PointKineticsState(double relativeNeutronPopulation, @NotNull double[] precursorConcentrations) {
        this.relativeNeutronPopulation = relativeNeutronPopulation;
        this.precursorConcentrations = precursorConcentrations;
    }

    /**
     * @return the steady-state (critical, constant-power) precursor concentrations for the given neutron
     *         population: {@code C_i = (beta_i / (Lambda * lambda_i)) * n}.
     */
    @NotNull
    public static PointKineticsState equilibrium(double relativeNeutronPopulation,
                                                 @NotNull PointKineticsConstants constants) {
        double[] concentrations = new double[constants.groupCount()];
        for (int i = 0; i < concentrations.length; i++) {
            concentrations[i] = constants.delayedNeutronFractions[i] * relativeNeutronPopulation /
                    (constants.promptNeutronLifetime * constants.precursorDecayConstants[i]);
        }
        return new PointKineticsState(relativeNeutronPopulation, concentrations);
    }
}
