package com.windows2000.gtnuclear.api.nuclear.thermal;

import com.windows2000.gtnuclear.api.nuclear.accident.SevereAccidentConstants;

/**
 * Advances fuel temperature by one fuel-coolant coupling sub-step, clamping into melted fuel mass instead of
 * exceeding the melting point (2026-10-07, extracted from
 * {@code MetaTileEntityPressurizedWaterReactor#updateFuelTemperatureStep} as part of that class's own
 * decomposition -- see this mod's design memory). Already amounted to a pure function of its own three
 * "current value" fields plus its method parameters before the move; {@link #advance} makes that explicit by
 * taking those three current values in and returning the next three as a {@link Result}, rather than mutating
 * fields directly.
 */
public final class FuelTemperatureStep {

    private FuelTemperatureStep() {}

    /**
     * @param fuelTemperatureKelvin         next fuel temperature (clamped at the melting point if exceeded)
     * @param meltedFuelMassKg              next melted fuel mass, unchanged unless melting occurred this step
     * @param coreFuelMassAtMeltdownStartKg unchanged unless melting newly began this step
     */
    public record Result(double fuelTemperatureKelvin, double meltedFuelMassKg,
                         double coreFuelMassAtMeltdownStartKg) {}

    /**
     * Once the candidate next temperature would exceed {@code SevereAccidentConstants.FUEL_MELTING_TEMPERATURE_KELVIN},
     * clamps temperature there instead and converts the excess energy to melted fuel mass via UO2's real latent
     * heat of fusion -- a phase-change plateau, the same pattern
     * {@code MetaTileEntityPressurizedWaterReactor#applyCoolantSubcoolingClamp} uses for boiling. Since
     * {@link CoolantThermalSolver#advance} is affine in net heat, this one clamp-and-convert step is correct
     * whether the fuel just now crossed the melting point this sub-step or was already sitting at it.
     * <p>
     * {@code meltedFuelMassKg} is capped against {@code coreFuelMassAtMeltdownStartKg} (captured once, the
     * instant melting actually begins), not a freshly recomputed live total fuel mass -- see that parameter's
     * own call site for why capping against a live value was a real bug (reloading a small amount of fresh fuel
     * after an earlier meltdown could look like the new fuel was already almost entirely melted).
     *
     * @param currentFuelTemperatureKelvin         fuel temperature at the start of this sub-step
     * @param currentMeltedFuelMassKg              melted fuel mass at the start of this sub-step
     * @param currentCoreFuelMassAtMeltdownStartKg the stable denominator captured when melting first began (0 if
     *                                             melting has never occurred)
     */
    public static Result advance(double currentFuelTemperatureKelvin, double currentMeltedFuelMassKg,
                                 double currentCoreFuelMassAtMeltdownStartKg, double fissionPowerWatts,
                                 double transferWatts, double totalFuelMassKg, double fuelHeatCapacity,
                                 double dtSeconds) {
        double candidateKelvin = CoolantThermalSolver.advance(currentFuelTemperatureKelvin,
                fissionPowerWatts - transferWatts, totalFuelMassKg,
                FuelThermalConstants.SPECIFIC_HEAT_CAPACITY_J_PER_KG_KELVIN, dtSeconds);
        if (candidateKelvin <= SevereAccidentConstants.FUEL_MELTING_TEMPERATURE_KELVIN || fuelHeatCapacity <= 0) {
            return new Result(candidateKelvin, currentMeltedFuelMassKg, currentCoreFuelMassAtMeltdownStartKg);
        }

        double coreFuelMassAtMeltdownStartKg = currentMeltedFuelMassKg <= 0 ? totalFuelMassKg :
                currentCoreFuelMassAtMeltdownStartKg;
        double excessJoules = (candidateKelvin - SevereAccidentConstants.FUEL_MELTING_TEMPERATURE_KELVIN) *
                fuelHeatCapacity;
        double meltedFuelMassKg = Math.min(coreFuelMassAtMeltdownStartKg,
                currentMeltedFuelMassKg + excessJoules / FuelThermalConstants.LATENT_HEAT_OF_FUSION_J_PER_KG);
        return new Result(SevereAccidentConstants.FUEL_MELTING_TEMPERATURE_KELVIN, meltedFuelMassKg,
                coreFuelMassAtMeltdownStartKg);
    }
}
