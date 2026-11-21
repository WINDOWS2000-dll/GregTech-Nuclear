package com.windows2000.gtnuclear.api.nuclear.accident;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.is;

class SevereAccidentEvaluatorTest {

    private static final SevereAccidentConstants CONSTANTS = new SevereAccidentConstants(0.5, 1.0);
    private static final double BELOW_MELTING = SevereAccidentConstants.FUEL_MELTING_TEMPERATURE_KELVIN - 100.0;
    private static final double ABOVE_MELTING = SevereAccidentConstants.FUEL_MELTING_TEMPERATURE_KELVIN + 100.0;

    @Test
    void staysNormalWhenCoolingAndControlAreFine() {
        SevereAccidentState state = SevereAccidentState.normal();
        SevereAccidentState next = SevereAccidentEvaluator.advance(state, false, 600.0, 0.0, 1000.0, false, CONSTANTS);
        assertThat(next.stage, is(SevereAccidentStage.NORMAL));
    }

    @Test
    void abnormalCoolingOrControlEscalatesToUncontrolledHeating() {
        SevereAccidentState state = SevereAccidentState.normal();
        SevereAccidentState next = SevereAccidentEvaluator.advance(state, true, 600.0, 0.0, 1000.0, false, CONSTANTS);
        assertThat(next.stage, is(SevereAccidentStage.UNCONTROLLED_HEATING));
    }

    @Test
    void uncontrolledHeatingRecoversToNormalIfCoolingRestoredBeforeMelting() {
        SevereAccidentState state = new SevereAccidentState(SevereAccidentStage.UNCONTROLLED_HEATING, 0.0);
        SevereAccidentState next = SevereAccidentEvaluator.advance(state, false, BELOW_MELTING, 0.0, 1000.0, false,
                CONSTANTS);
        assertThat(next.stage, is(SevereAccidentStage.NORMAL));
    }

    @Test
    void uncontrolledHeatingEscalatesToMeltdownAboveMeltingPoint() {
        SevereAccidentState state = new SevereAccidentState(SevereAccidentStage.UNCONTROLLED_HEATING, 0.0);
        SevereAccidentState next = SevereAccidentEvaluator.advance(state, true, ABOVE_MELTING, 0.0, 1000.0, false,
                CONSTANTS);
        assertThat(next.stage, is(SevereAccidentStage.MELTDOWN));
    }

    @Test
    void uncontrolledHeatingStaysPutBelowMeltingPointWhileStillAbnormal() {
        SevereAccidentState state = new SevereAccidentState(SevereAccidentStage.UNCONTROLLED_HEATING, 0.0);
        SevereAccidentState next = SevereAccidentEvaluator.advance(state, true, BELOW_MELTING, 0.0, 1000.0, false,
                CONSTANTS);
        assertThat(next.stage, is(SevereAccidentStage.UNCONTROLLED_HEATING));
    }

    @Test
    void meltdownStaysMeltdownBelowExplosionThresholdWithoutCoolantContact() {
        SevereAccidentState state = new SevereAccidentState(SevereAccidentStage.MELTDOWN, 0.0);
        SevereAccidentState next = SevereAccidentEvaluator.advance(state, true, ABOVE_MELTING, 300.0, 1000.0, false,
                CONSTANTS);
        assertThat(next.stage, is(SevereAccidentStage.MELTDOWN));
        assertThat(next.coreMeltFraction, closeTo(0.3, 1e-9));
    }

    @Test
    void meltdownEscalatesToExplosionAtMeltFractionThreshold() {
        SevereAccidentState state = new SevereAccidentState(SevereAccidentStage.MELTDOWN, 0.0);
        SevereAccidentState next = SevereAccidentEvaluator.advance(state, true, ABOVE_MELTING, 500.0, 1000.0, false,
                CONSTANTS);
        assertThat(next.stage, is(SevereAccidentStage.EXPLOSION));
        assertThat(next.coreMeltFraction, closeTo(0.5, 1e-9));
    }

    @Test
    void meltdownEscalatesToExplosionOnDirectCoolantContactRegardlessOfMeltFraction() {
        SevereAccidentState state = new SevereAccidentState(SevereAccidentStage.MELTDOWN, 0.0);
        SevereAccidentState next = SevereAccidentEvaluator.advance(state, true, ABOVE_MELTING, 50.0, 1000.0, true,
                CONSTANTS);
        assertThat(next.stage, is(SevereAccidentStage.EXPLOSION));
    }

    @Test
    void explosionIsTerminal() {
        SevereAccidentState state = new SevereAccidentState(SevereAccidentStage.EXPLOSION, 0.8);
        SevereAccidentState next = SevereAccidentEvaluator.advance(state, false, 300.0, 0.0, 1000.0, false, CONSTANTS);
        assertThat(next.stage, is(SevereAccidentStage.EXPLOSION));
        assertThat(next.coreMeltFraction, closeTo(0.8, 1e-9));
    }

    @Test
    void explosionMagnitudeScalesLinearlyWithMeltedMassAndFraction() {
        SevereAccidentConstants constants = new SevereAccidentConstants(0.5, 2.0);
        SevereAccidentState state = new SevereAccidentState(SevereAccidentStage.EXPLOSION, 0.5);
        double magnitude = state.explosionMagnitude(300.0, constants);
        assertThat(magnitude, closeTo(2.0 * 300.0 * 0.5, 1e-9));
    }
}
