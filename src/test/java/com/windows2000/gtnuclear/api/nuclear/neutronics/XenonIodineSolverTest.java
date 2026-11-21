package com.windows2000.gtnuclear.api.nuclear.neutronics;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;

class XenonIodineSolverTest {

    private static final double EPSILON = 1e-6;
    private static final double DEAD_TIME_SECONDS = 1800.0;
    private static final double EQUILIBRIUM_WORTH = 0.028;

    @Test
    void fullPowerEquilibriumIsAFixedPoint() {
        XenonIodineConstants constants = new XenonIodineConstants(DEAD_TIME_SECONDS, EQUILIBRIUM_WORTH);
        XenonIodineState equilibrium = XenonIodineState.equilibrium(1.0, constants);

        XenonIodineState next = XenonIodineSolver.advance(equilibrium, 1.0, 10.0, constants);

        assertThat(next.iodineConcentration(),
                closeTo(equilibrium.iodineConcentration(), Math.abs(equilibrium.iodineConcentration()) * 1e-6));
        assertThat(next.xenonConcentration(),
                closeTo(equilibrium.xenonConcentration(), Math.abs(equilibrium.xenonConcentration()) * 1e-6));
        assertThat(equilibrium.xenonConcentration(), closeTo(EQUILIBRIUM_WORTH, EPSILON));
    }

    @Test
    void zeroPowerFromZeroStartStaysZero() {
        XenonIodineConstants constants = new XenonIodineConstants(DEAD_TIME_SECONDS, EQUILIBRIUM_WORTH);
        XenonIodineState state = new XenonIodineState(0.0, 0.0);

        for (int i = 0; i < 100; i++) {
            state = XenonIodineSolver.advance(state, 0.0, 10.0, constants);
        }

        assertThat(state.iodineConcentration(), closeTo(0.0, EPSILON));
        assertThat(state.xenonConcentration(), closeTo(0.0, EPSILON));
    }

    @Test
    void xenonRisesAfterShutdownThenFalls() {
        // The classic "iodine pit": after an instantaneous shutdown from full-power equilibrium, xenon (no longer
        // burned up by neutron absorption) first rises above its equilibrium value before eventually decaying.
        XenonIodineConstants constants = new XenonIodineConstants(DEAD_TIME_SECONDS, EQUILIBRIUM_WORTH);
        XenonIodineState state = XenonIodineState.equilibrium(1.0, constants);
        double equilibriumXenon = state.xenonConcentration();

        double dt = 5.0;
        double peak = state.xenonConcentration();
        for (int i = 0; i < 2000; i++) {
            state = XenonIodineSolver.advance(state, 0.0, dt, constants);
            peak = Math.max(peak, state.xenonConcentration());
        }

        assertThat(peak, greaterThan(equilibriumXenon));
        // Far past the dead time (2000*5s = 10000s >> 1800s), xenon should have decayed back below its
        // pre-shutdown equilibrium value.
        assertThat(state.xenonConcentration(), lessThan(equilibriumXenon));
    }

    @Test
    void higherNeutronPopulationProducesMoreIodineAndXenon() {
        XenonIodineConstants constants = new XenonIodineConstants(DEAD_TIME_SECONDS, EQUILIBRIUM_WORTH);
        XenonIodineState lowPower = XenonIodineState.equilibrium(0.5, constants);
        XenonIodineState highPower = XenonIodineState.equilibrium(1.0, constants);

        assertThat(highPower.iodineConcentration(), greaterThan(lowPower.iodineConcentration()));
        assertThat(highPower.xenonConcentration(), greaterThan(lowPower.xenonConcentration()));
    }
}
