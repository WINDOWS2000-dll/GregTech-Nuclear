package com.windows2000.gtnuclear.api.nuclear.neutronics;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;

class XenonDeadTimeCalibrationTest {

    @Test
    void realDeadTimeIsAPhysicallyReasonableNumberOfHours() {
        double hours = XenonDeadTimeCalibration.realDeadTimeSeconds() / 3600.0;
        // Real-world PWR "poison-out" dead time after a full-power trip is commonly cited as roughly 1-2 days.
        assertThat(hours, greaterThan(10.0));
        assertThat(hours, lessThan(72.0));
    }

    @Test
    void scalingFactorForRealDeadTimeIsOne() {
        double factor = XenonDeadTimeCalibration.scalingFactorFor(XenonDeadTimeCalibration.realDeadTimeSeconds());
        assertThat(factor, closeTo(1.0, 1e-9));
    }

    @Test
    void scalingFactorIsInverselyProportionalToTarget() {
        double target = 1800.0;
        double factor = XenonDeadTimeCalibration.scalingFactorFor(target);
        double factorAtDoubleTarget = XenonDeadTimeCalibration.scalingFactorFor(target * 2);
        assertThat(factorAtDoubleTarget, closeTo(factor / 2.0, factor * 1e-9));
    }

    @Test
    void configuredDeadTimeMatchesSimulatedXenonRecoveryTime() {
        double configuredDeadTimeSeconds = 1800.0;
        double equilibriumWorth = 0.028;
        XenonIodineConstants constants = new XenonIodineConstants(configuredDeadTimeSeconds, equilibriumWorth);

        XenonIodineState state = XenonIodineState.equilibrium(1.0, constants);
        double equilibriumXenon = state.xenonConcentration();

        double dt = 1.0;
        double elapsed = 0.0;
        double previousXenon = state.xenonConcentration();
        double crossingTime = -1.0;
        while (elapsed < configuredDeadTimeSeconds * 3) {
            state = XenonIodineSolver.advance(state, 0.0, dt, constants);
            elapsed += dt;
            if (previousXenon >= equilibriumXenon && state.xenonConcentration() < equilibriumXenon) {
                crossingTime = elapsed;
                break;
            }
            previousXenon = state.xenonConcentration();
        }

        assertThat(crossingTime, greaterThan(0.0));
        // RK4 numerical integration vs. the calibration's own bisection root-find should agree closely.
        assertThat(crossingTime, closeTo(configuredDeadTimeSeconds, configuredDeadTimeSeconds * 0.05));
    }
}
