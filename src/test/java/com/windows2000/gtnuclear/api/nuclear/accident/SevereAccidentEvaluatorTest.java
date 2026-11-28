package com.windows2000.gtnuclear.api.nuclear.accident;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.is;

/** {@link SevereAccidentEvaluator} -- stages from the fuel's own temperature and melting (stage 2-3b, O2). */
class SevereAccidentEvaluatorTest {

    private static final SevereAccidentConstants CONSTANTS = new SevereAccidentConstants(0.5, 1.0);
    private static final double DAMAGE = SevereAccidentConstants.FUEL_DAMAGE_TEMPERATURE_KELVIN;
    private static final double HYSTERESIS = SevereAccidentConstants.FUEL_DAMAGE_RECOVERY_HYSTERESIS_KELVIN;
    private static final double ABOVE_MELTING = SevereAccidentConstants.FUEL_MELTING_TEMPERATURE_KELVIN + 100.0;

    private static SevereAccidentState advance(SevereAccidentStage stage, double fuelKelvin, double meltedKg) {
        return SevereAccidentEvaluator.advance(new SevereAccidentState(stage, 0.0), fuelKelvin, meltedKg, 1000.0,
                false, CONSTANTS);
    }

    @Test
    void normalOperatingFuelStaysNormal() {
        assertThat(advance(SevereAccidentStage.NORMAL, 750.0, 0.0).stage, is(SevereAccidentStage.NORMAL));
    }

    @Test
    void fuelAtTheDamageTemperatureStartsUncontrolledHeating() {
        assertThat(advance(SevereAccidentStage.NORMAL, DAMAGE, 0.0).stage,
                is(SevereAccidentStage.UNCONTROLLED_HEATING));
        assertThat(advance(SevereAccidentStage.NORMAL, DAMAGE - 1.0, 0.0).stage, is(SevereAccidentStage.NORMAL));
    }

    @Test
    void uncontrolledHeatingRecoversOnlyBelowTheHysteresisBand() {
        assertThat(advance(SevereAccidentStage.UNCONTROLLED_HEATING, DAMAGE - HYSTERESIS + 1.0, 0.0).stage,
                is(SevereAccidentStage.UNCONTROLLED_HEATING));
        assertThat(advance(SevereAccidentStage.UNCONTROLLED_HEATING, DAMAGE - HYSTERESIS - 1.0, 0.0).stage,
                is(SevereAccidentStage.NORMAL));
    }

    @Test
    void meltingStartsMeltdownFromAnyEarlierStage() {
        assertThat(advance(SevereAccidentStage.UNCONTROLLED_HEATING, ABOVE_MELTING, 0.0).stage,
                is(SevereAccidentStage.MELTDOWN));
        // Physically molten fuel can no longer hide behind a NORMAL label (the pre-O2 inconsistency).
        assertThat(advance(SevereAccidentStage.NORMAL, 1200.0, 10.0).stage, is(SevereAccidentStage.MELTDOWN));
    }

    @Test
    void meltdownIsIrreversibleAndTracksTheMeltFraction() {
        SevereAccidentState next = advance(SevereAccidentStage.MELTDOWN, 600.0, 300.0);
        assertThat(next.stage, is(SevereAccidentStage.MELTDOWN));
        assertThat(next.coreMeltFraction, closeTo(0.3, 1e-12));
    }

    @Test
    void meltdownEscalatesToExplosionAtMeltFractionThreshold() {
        assertThat(advance(SevereAccidentStage.MELTDOWN, ABOVE_MELTING, 500.0).stage,
                is(SevereAccidentStage.EXPLOSION));
    }

    @Test
    void meltdownEscalatesToExplosionOnDirectCoolantContactRegardlessOfMeltFraction() {
        SevereAccidentState next = SevereAccidentEvaluator.advance(
                new SevereAccidentState(SevereAccidentStage.MELTDOWN, 0.0), ABOVE_MELTING, 50.0, 1000.0, true,
                CONSTANTS);
        assertThat(next.stage, is(SevereAccidentStage.EXPLOSION));
    }

    @Test
    void explosionIsTerminal() {
        assertThat(advance(SevereAccidentStage.EXPLOSION, 300.0, 0.0).stage, is(SevereAccidentStage.EXPLOSION));
    }

    @Test
    void explosionMagnitudeScalesLinearlyWithMeltedMassAndFraction() {
        SevereAccidentConstants constants = new SevereAccidentConstants(0.5, 2.0);
        SevereAccidentState state = new SevereAccidentState(SevereAccidentStage.EXPLOSION, 0.5);
        double magnitude = state.explosionMagnitude(300.0, constants);
        assertThat(magnitude, closeTo(2.0 * 300.0 * 0.5, 1e-9));
    }
}
