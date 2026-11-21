package com.windows2000.gtnuclear.api.nuclear.power;

import com.windows2000.gtnuclear.api.nuclear.thermal.SteamProperties;

/**
 * Converts steam mass flow into electrical output via real thermodynamics (mass flow x latent heat x stage
 * efficiency x rotor performance), rather than a static authored fuel recipe -- see this mod's design memory
 * (2026-10) for why GT's own {@code RecipeMaps.STEAM_TURBINE_FUELS}/{@code MetaTileEntityLargeTurbine} were
 * deliberately not reused: this mod's turbines derive their output from the same real latent-heat/saturation-
 * curve data already driving {@code MetaTileEntityHeatExchanger}, consistent with this mod's physics-first
 * design throughout (Bateman decay, CMFD, point kinetics, IAPWS saturation curve), rather than a flat EU/t-per-
 * mB recipe value with no connection to the fluid's own temperature/pressure.
 * <p>
 * GT's own rotor item data ({@code TurbineRotorBehavior}/{@code RotorProperty}) is read directly by the host MTE
 * (see {@code MetaTileEntityTurbineRotorHolder}) and passed in here as plain percentages -- this class itself
 * has no GT/Minecraft dependency, matching every other pure engine class in {@code api.nuclear.*}.
 */
public final class SteamTurbineOutput {

    private SteamTurbineOutput() {}

    /**
     * A single turbine unit's own steam throughput ceiling this tick -- GT's rotor "power" stat caps how much
     * working fluid one physical turbine can pass, independent of how efficiently it converts that flow to
     * electricity (see {@link #electricalOutputWatts}'s own {@code rotorEfficiencyPercent} for that side).
     *
     * @param baseThroughputMbPerTick this turbine stage's own throughput at a nominal (100%) rotor
     * @param rotorPowerPercent       {@code TurbineRotorBehavior.getRotorPower(stack)}, or 0 with no rotor loaded
     */
    public static double maxThroughputMbPerTick(double baseThroughputMbPerTick, double rotorPowerPercent) {
        return baseThroughputMbPerTick * (rotorPowerPercent / 100.0);
    }

    /**
     * This stage's own share of the overall HP+LP turbine thermal efficiency, proportional to the temperature
     * drop this stage is responsible for out of the total drop from throttle to condenser -- see this mod's
     * design memory for why this (rather than an arbitrary fixed fraction) is used: it ties the HP/LP split to
     * the actual temperatures of {@code HighPressureSteam}/{@code LowPressureSteam}/ambient condenser
     * conditions, consistent with this mod's real-saturation-curve-driven design elsewhere.
     *
     * @return 0.0 if {@code overallInletTemperatureKelvin <= overallOutletTemperatureKelvin} (no real drop to
     *         divide by, rather than dividing by zero/going negative)
     */
    public static double stageEfficiencyFraction(double totalEfficiencyFraction, double stageInletTemperatureKelvin,
                                                 double stageOutletTemperatureKelvin,
                                                 double overallInletTemperatureKelvin,
                                                 double overallOutletTemperatureKelvin) {
        double overallDropKelvin = overallInletTemperatureKelvin - overallOutletTemperatureKelvin;
        if (overallDropKelvin <= 0.0) return 0.0;
        double stageDropKelvin = stageInletTemperatureKelvin - stageOutletTemperatureKelvin;
        return totalEfficiencyFraction * (stageDropKelvin / overallDropKelvin);
    }

    /**
     * @param consumedSteamMbPerTick  actual steam consumed this tick (already capped by
     *                                {@link #maxThroughputMbPerTick} and by what the input hatch actually had)
     * @param stageEfficiencyFraction this stage's own share of total turbine efficiency, see
     *                                {@link #stageEfficiencyFraction}
     * @param rotorEfficiencyPercent  {@code TurbineRotorBehavior.getRotorEfficiency(stack)}, or 0 with no rotor
     * @return electrical power (W) generated this tick
     */
    public static double electricalOutputWatts(double consumedSteamMbPerTick, double stageEfficiencyFraction,
                                               double rotorEfficiencyPercent, double tickSeconds) {
        if (consumedSteamMbPerTick <= 0.0 || tickSeconds <= 0.0) return 0.0;
        double massFlowKgPerSecond = (consumedSteamMbPerTick / 1000.0) / tickSeconds;
        return massFlowKgPerSecond * SteamProperties.LATENT_HEAT_OF_VAPORIZATION_J_PER_KG * stageEfficiencyFraction *
                (rotorEfficiencyPercent / 100.0);
    }

    /**
     * @param wattsPerEuPerTick this mod's own generation-side EU-to-Watts gameplay convention, never a GT lore value.
     */
    public static double euPerTick(double electricalOutputWatts, double wattsPerEuPerTick) {
        return wattsPerEuPerTick > 0.0 ? electricalOutputWatts / wattsPerEuPerTick : 0.0;
    }
}
