package com.windows2000.gtnuclear.api.nuclear.neutronics;

/**
 * Boron reactivity feedback: a simple linear worth model, {@code delta-rho = -worthPerPpm * concentrationPpm}.
 * Unlike xenon/iodine, boron concentration is not solved via an ODE here -- per this mod's design memory, boron
 * is tracked as an abstract reactor-wide ppm status value updated directly by dilution/concentration operations
 * (mass-balance on injection), not simulated as a continuous process. This class only converts that status value
 * into its reactivity contribution.
 */
public final class BoronReactivity {

    private BoronReactivity() {}

    /**
     * @return the reactivity contribution of the given boron concentration (always &lt;= 0 for positive inputs, boron
     *         is a neutron absorber).
     */
    public static double reactivity(double concentrationPpm, double worthPerPpm) {
        return -worthPerPpm * concentrationPpm;
    }
}
