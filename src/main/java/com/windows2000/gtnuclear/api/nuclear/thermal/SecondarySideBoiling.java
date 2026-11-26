package com.windows2000.gtnuclear.api.nuclear.thermal;

/**
 * Energy-conserving secondary-side boiling for the heat exchanger (2026-11-26, stage 1-4b of this mod's reactor
 * physics rework, see this mod's design memory). Given the secondary water after this tick's heat input, it boils
 * the excess above the setpoint's saturation temperature and routes the steam, in order:
 * <ol>
 * <li><b>export</b> to the turbines (its capacity is 0 in cooldown mode),</li>
 * <li><b>steam dump</b> to the condenser (limited by the dump valve capacity and the condensate hatch),</li>
 * <li>whatever cannot leave <b>stays in the water as sensible heat</b> -- physically the pressure rising above the
 * setpoint, which also shrinks the primary-to-secondary temperature difference so heat removal falls by itself --
 * until it reaches the main steam safety valves' saturation temperature, above which steam is vented to
 * atmosphere (<b>relief</b>).</li>
 * </ol>
 * Before this class, steam that the export hatch could not take had its energy simply deleted (the water was clamped
 * to saturation regardless).
 * <p>
 * Energies use this mod's ledger convention: liquid {@code c_p (T - 300 K)}, steam that plus the latent heat, with
 * the steam leaving at the saturation temperature it boiled at. If boiling would consume the whole inventory, the
 * energy left over is reported as relief energy rather than lost.
 */
public final class SecondarySideBoiling {

    private static final double REFERENCE_KELVIN = 300.0;

    /**
     * @param waterTemperatureKelvin secondary water temperature after boiling
     * @param waterMassKg            secondary water mass after boiling
     * @param exportedKg             steam sent to the export hatch
     * @param dumpedKg               steam sent to the steam dump (condensate hatch)
     * @param reliefKg               steam vented by the safety valves
     * @param exportJoules           ledger enthalpy of {@code exportedKg}
     * @param dumpJoules             ledger enthalpy of {@code dumpedKg}
     * @param reliefJoules           ledger enthalpy of {@code reliefKg} plus any dry-out remainder
     */
    public record Result(double waterTemperatureKelvin, double waterMassKg, double exportedKg, double dumpedKg,
                         double reliefKg, double exportJoules, double dumpJoules, double reliefJoules) {}

    private SecondarySideBoiling() {}

    /**
     * @param waterMassKg         secondary water mass
     * @param candidateKelvin     water temperature after this tick's heat input, before any boiling
     * @param setpointSaturationK saturation temperature at the effective pressure setpoint
     * @param exportCapacityKg    steam the export hatch(es) can take this tick
     * @param dumpCapacityKg      steam the steam dump can take this tick
     * @param reliefSaturationK   saturation temperature at the safety valve setpoint
     * @param specificHeat        liquid specific heat (J/(kg K))
     * @param latentHeat          latent heat of vaporization (J/kg)
     */
    public static Result boil(double waterMassKg, double candidateKelvin, double setpointSaturationK,
                              double exportCapacityKg, double dumpCapacityKg, double reliefSaturationK,
                              double specificHeat, double latentHeat) {
        if (waterMassKg <= 0 || candidateKelvin <= setpointSaturationK) {
            return new Result(candidateKelvin, waterMassKg, 0, 0, 0, 0, 0, 0);
        }
        double steamJoulesPerKg = specificHeat * (setpointSaturationK - REFERENCE_KELVIN) + latentHeat;

        double excessJoules = (candidateKelvin - setpointSaturationK) * waterMassKg * specificHeat;
        double desiredKg = Math.min(waterMassKg, excessJoules / latentHeat);
        double exportedKg = Math.min(desiredKg, Math.max(0.0, exportCapacityKg));
        double dumpedKg = Math.min(desiredKg - exportedKg, Math.max(0.0, dumpCapacityKg));
        double mass = waterMassKg - exportedKg - dumpedKg;
        double leftoverJoules = excessJoules - (exportedKg + dumpedKg) * latentHeat;

        double reliefKg = 0.0;
        double reliefJoules = 0.0;
        double temperature;
        if (mass <= 0) {
            mass = 0.0;
            temperature = setpointSaturationK;
            reliefJoules = Math.max(0.0, leftoverJoules);
        } else {
            temperature = setpointSaturationK + leftoverJoules / (mass * specificHeat);
            if (temperature > reliefSaturationK) {
                double reliefSteamJoulesPerKg = specificHeat * (reliefSaturationK - REFERENCE_KELVIN) + latentHeat;
                double reliefExcessJoules = (temperature - reliefSaturationK) * mass * specificHeat;
                reliefKg = Math.min(mass, reliefExcessJoules / latentHeat);
                double remaining = mass - reliefKg;
                double reliefLeftoverJoules = reliefExcessJoules - reliefKg * latentHeat;
                reliefJoules = reliefKg * reliefSteamJoulesPerKg;
                if (remaining <= 0) {
                    mass = 0.0;
                    temperature = reliefSaturationK;
                    reliefJoules += Math.max(0.0, reliefLeftoverJoules);
                } else {
                    mass = remaining;
                    temperature = reliefSaturationK + reliefLeftoverJoules / (remaining * specificHeat);
                }
            }
        }
        return new Result(temperature, mass, exportedKg, dumpedKg, reliefKg, exportedKg * steamJoulesPerKg,
                dumpedKg * steamJoulesPerKg, reliefJoules);
    }
}
