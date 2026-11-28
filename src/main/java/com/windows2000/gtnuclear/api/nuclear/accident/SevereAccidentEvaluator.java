package com.windows2000.gtnuclear.api.nuclear.accident;

import org.jetbrains.annotations.NotNull;

/**
 * Advances {@link SevereAccidentState} by one step from the physical state of the fuel (2026-11-28, stage 2-3b of
 * this mod's reactor physics rework, decision O2): the stage is a label of what the fuel is actually doing, not of
 * what caused it.
 * <ul>
 * <li>{@link SevereAccidentStage#UNCONTROLLED_HEATING} while the fuel is at or above
 * {@link SevereAccidentConstants#FUEL_DAMAGE_TEMPERATURE_KELVIN}; back to {@link SevereAccidentStage#NORMAL} once it
 * cools {@link SevereAccidentConstants#FUEL_DAMAGE_RECOVERY_HYSTERESIS_KELVIN} below that, if nothing melted.</li>
 * <li>{@link SevereAccidentStage#MELTDOWN} as soon as any fuel has melted (or the melting point is reached) -- from
 * any earlier stage, irreversible.</li>
 * <li>{@link SevereAccidentStage#EXPLOSION} from MELTDOWN at the melt-fraction threshold (or the coolant-contact
 * branch, currently always false -- see {@code SevereAccidentProgressionTracker}), terminal.</li>
 * </ul>
 * Before O2 the stage needed a cause ("abnormal cooling or control": LOCA, void, housing mismatch) and a heat-source
 * flag: fuel could then melt physically while the stage stayed NORMAL (a LOCA after a SCRAM had "no heat source"),
 * and a hot core fell back to NORMAL the moment the cause cleared. Causes are now console alarms instead.
 */
public final class SevereAccidentEvaluator {

    private SevereAccidentEvaluator() {}

    @NotNull
    public static SevereAccidentState advance(@NotNull SevereAccidentState previous, double fuelTemperatureKelvin,
                                              double meltedFuelMassKg, double totalCoreFuelMassKg,
                                              boolean coolantContactWithCorium,
                                              @NotNull SevereAccidentConstants constants) {
        boolean melting = meltedFuelMassKg > 0 ||
                fuelTemperatureKelvin >= SevereAccidentConstants.FUEL_MELTING_TEMPERATURE_KELVIN;
        switch (previous.stage) {
            case NORMAL:
                if (melting) return meltdown(meltedFuelMassKg, totalCoreFuelMassKg);
                return fuelTemperatureKelvin >= SevereAccidentConstants.FUEL_DAMAGE_TEMPERATURE_KELVIN ?
                        new SevereAccidentState(SevereAccidentStage.UNCONTROLLED_HEATING, 0.0) : previous;
            case UNCONTROLLED_HEATING:
                if (melting) return meltdown(meltedFuelMassKg, totalCoreFuelMassKg);
                if (fuelTemperatureKelvin < SevereAccidentConstants.FUEL_DAMAGE_TEMPERATURE_KELVIN -
                        SevereAccidentConstants.FUEL_DAMAGE_RECOVERY_HYSTERESIS_KELVIN) {
                    return SevereAccidentState.normal();
                }
                return previous;
            case MELTDOWN: {
                SevereAccidentState state = meltdown(meltedFuelMassKg, totalCoreFuelMassKg);
                if (state.coreMeltFraction >= constants.explosionMeltFractionThreshold || coolantContactWithCorium) {
                    return new SevereAccidentState(SevereAccidentStage.EXPLOSION, state.coreMeltFraction);
                }
                return state;
            }
            case EXPLOSION:
            default:
                return previous;
        }
    }

    @NotNull
    private static SevereAccidentState meltdown(double meltedFuelMassKg, double totalCoreFuelMassKg) {
        double meltFraction = totalCoreFuelMassKg > 0 ? Math.min(meltedFuelMassKg / totalCoreFuelMassKg, 1.0) : 0.0;
        return new SevereAccidentState(SevereAccidentStage.MELTDOWN, meltFraction);
    }
}
