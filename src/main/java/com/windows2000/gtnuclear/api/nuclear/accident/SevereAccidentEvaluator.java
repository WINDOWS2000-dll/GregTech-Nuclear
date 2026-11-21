package com.windows2000.gtnuclear.api.nuclear.accident;

import org.jetbrains.annotations.NotNull;

/**
 * Advances {@link SevereAccidentState} by one step. Determining <i>whether</i> cooling/control is currently
 * abnormal (structural damage, or an unresolved post-SCRAM LOCA -- see this mod's design memory) and how much
 * fuel has melted are both the caller's responsibility (reactor-structure-specific concerns not modeled here);
 * this class only encodes the stage transition logic itself, structure-independent like the rest of this mod's
 * physics classes.
 * <p>
 * {@link SevereAccidentStage#UNCONTROLLED_HEATING} can recover back to {@link SevereAccidentStage#NORMAL} if
 * cooling/control is restored before fuel temperature reaches the melting point -- a contained near-miss.
 * {@link SevereAccidentStage#MELTDOWN} and {@link SevereAccidentStage#EXPLOSION} are terminal/irreversible once
 * reached, matching real accident progression (melted fuel does not un-melt on its own).
 */
public final class SevereAccidentEvaluator {

    private SevereAccidentEvaluator() {}

    @NotNull
    public static SevereAccidentState advance(@NotNull SevereAccidentState previous, boolean abnormalCoolingOrControl,
                                              double fuelTemperatureKelvin, double meltedFuelMassKg,
                                              double totalCoreFuelMassKg, boolean coolantContactWithCorium,
                                              @NotNull SevereAccidentConstants constants) {
        switch (previous.stage) {
            case NORMAL:
                return abnormalCoolingOrControl ?
                        new SevereAccidentState(SevereAccidentStage.UNCONTROLLED_HEATING, 0.0) : previous;
            case UNCONTROLLED_HEATING:
                if (!abnormalCoolingOrControl) {
                    return SevereAccidentState.normal();
                }
                if (fuelTemperatureKelvin >= SevereAccidentConstants.FUEL_MELTING_TEMPERATURE_KELVIN) {
                    return new SevereAccidentState(SevereAccidentStage.MELTDOWN, 0.0);
                }
                return previous;
            case MELTDOWN: {
                double meltFraction = totalCoreFuelMassKg > 0 ? Math.min(meltedFuelMassKg / totalCoreFuelMassKg, 1.0) :
                        0.0;
                if (meltFraction >= constants.explosionMeltFractionThreshold || coolantContactWithCorium) {
                    return new SevereAccidentState(SevereAccidentStage.EXPLOSION, meltFraction);
                }
                return new SevereAccidentState(SevereAccidentStage.MELTDOWN, meltFraction);
            }
            case EXPLOSION:
            default:
                return previous;
        }
    }
}
