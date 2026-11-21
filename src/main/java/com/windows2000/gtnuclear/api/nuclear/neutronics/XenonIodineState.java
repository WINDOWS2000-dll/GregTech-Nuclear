package com.windows2000.gtnuclear.api.nuclear.neutronics;

import org.jetbrains.annotations.NotNull;

/**
 * I-135/Xe-135 poisoning state: iodine concentration (an intermediate quantity with no direct reactivity effect)
 * and xenon concentration, the latter expressed directly in reactivity-worth units -- see
 * {@link XenonIodineConstants}'s own JavaDoc for why.
 * <p>
 * A record (2026-10-06, Java 25 upgrade pass) -- see {@link NodePosition}'s own JavaDoc for the general
 * reasoning.
 *
 * @param xenonConcentration Reactivity-worth-normalized xenon concentration (always &gt;= 0); see
 *                           {@link #reactivity()}.
 */
public record XenonIodineState(double iodineConcentration, double xenonConcentration) {

    /**
     * @return the reactivity contribution of the current xenon concentration (always &lt;= 0, xenon is a neutron
     *         poison).
     */
    public double reactivity() {
        return -xenonConcentration;
    }

    /**
     * @return the steady-state iodine/xenon concentrations for a reactor held at the given constant relative
     *         neutron population forever -- e.g. {@code equilibrium(1.0, constants)} gives the full-power
     *         equilibrium state, where xenon concentration equals exactly {@code equilibriumXenonWorth}.
     */
    @NotNull
    public static XenonIodineState equilibrium(double relativeNeutronPopulation,
                                               @NotNull XenonIodineConstants constants) {
        double removalRate = constants.xenonDecayConstant +
                constants.xenonBurnupCoefficient * relativeNeutronPopulation;
        double iodine = constants.iodineProductionRate * relativeNeutronPopulation / constants.iodineDecayConstant;
        double xenon = (constants.xenonProductionRate * relativeNeutronPopulation +
                constants.iodineDecayConstant * iodine) / removalRate;
        return new XenonIodineState(iodine, xenon);
    }
}
