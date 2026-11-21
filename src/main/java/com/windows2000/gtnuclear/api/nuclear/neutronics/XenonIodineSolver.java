package com.windows2000.gtnuclear.api.nuclear.neutronics;

import org.jetbrains.annotations.NotNull;

/**
 * Solves the coupled I-135/Xe-135 poisoning ODEs:
 * 
 * <pre>
 *   dI/dt = P_I*n - lambda_I*I
 *   dX/dt = P_X*n + lambda_I*I - (lambda_X + sigma*n)*X
 * </pre>
 * 
 * where {@code n} is the relative neutron population (assumed constant over one {@link #advance} call, mirroring
 * how {@link PointKineticsSolver} treats reactivity as constant per step), {@code P_I}/{@code P_X} the production
 * rates and {@code lambda_I}/{@code lambda_X}/{@code sigma} the decay/burnup rates from {@link XenonIodineConstants}.
 * <p>
 * <b>Integration method:</b> classical 4th-order Runge-Kutta. Unlike {@link PointKineticsSolver}'s prompt-neutron
 * term (~1e-5 s, extremely stiff against tick-scale steps), these rate constants are all of comparable, moderate
 * magnitude (after {@link XenonDeadTimeCalibration}'s scaling, typically ~1e-3 s^-1) -- not stiff enough to need an
 * implicit method, so a straightforward explicit RK4 is used instead of deriving and special-casing an exact
 * closed-form solution.
 */
public final class XenonIodineSolver {

    private XenonIodineSolver() {}

    @NotNull
    public static XenonIodineState advance(@NotNull XenonIodineState state, double relativeNeutronPopulation,
                                           double dtSeconds, @NotNull XenonIodineConstants constants) {
        double i0 = state.iodineConcentration();
        double x0 = state.xenonConcentration();

        double[] k1 = derivatives(i0, x0, relativeNeutronPopulation, constants);
        double[] k2 = derivatives(i0 + dtSeconds / 2 * k1[0], x0 + dtSeconds / 2 * k1[1], relativeNeutronPopulation,
                constants);
        double[] k3 = derivatives(i0 + dtSeconds / 2 * k2[0], x0 + dtSeconds / 2 * k2[1], relativeNeutronPopulation,
                constants);
        double[] k4 = derivatives(i0 + dtSeconds * k3[0], x0 + dtSeconds * k3[1], relativeNeutronPopulation, constants);

        double newIodine = i0 + dtSeconds / 6.0 * (k1[0] + 2 * k2[0] + 2 * k3[0] + k4[0]);
        double newXenon = x0 + dtSeconds / 6.0 * (k1[1] + 2 * k2[1] + 2 * k3[1] + k4[1]);

        return new XenonIodineState(Math.max(newIodine, 0.0), Math.max(newXenon, 0.0));
    }

    @NotNull
    private static double[] derivatives(double iodine, double xenon, double relativeNeutronPopulation,
                                        @NotNull XenonIodineConstants constants) {
        double dIodine = constants.iodineProductionRate * relativeNeutronPopulation -
                constants.iodineDecayConstant * iodine;
        double removalRate = constants.xenonDecayConstant +
                constants.xenonBurnupCoefficient * relativeNeutronPopulation;
        double dXenon = constants.xenonProductionRate * relativeNeutronPopulation +
                constants.iodineDecayConstant * iodine - removalRate * xenon;
        return new double[] { dIodine, dXenon };
    }
}
