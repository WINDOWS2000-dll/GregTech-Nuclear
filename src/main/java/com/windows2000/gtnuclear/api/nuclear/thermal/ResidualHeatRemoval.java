package com.windows2000.gtnuclear.api.nuclear.thermal;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Pure calculations of the reactor's built-in residual heat removal (RHR) system (2026-11-24, stage 1-4a of this
 * mod's reactor physics rework, design decision 8-7 "r1": built into the reactor, see this mod's design memory).
 * The RHR heat exchanger's primary (tube) side carries radioactive reactor coolant and exists only inside the
 * reactor; its cooling (shell) side is the non-radioactive cooling water piped through the reactor's two cooling
 * water hatches.
 * <ul>
 * <li><b>Interlocks</b> (real PWR practice: the RHR suction valves protect the system's low-pressure piping):
 * placing RHR in service needs the primary coolant at or below a temperature and pressure limit; exceeding a
 * higher isolation pressure while in service isolates it automatically.</li>
 * <li><b>Heat transfer</b>: a counterflow heat exchanger by the effectiveness-NTU method, so the heat removed can
 * never exceed what either stream can physically carry -- {@code Q <= C_min * (T_primary - T_cooling,in)}, which
 * also keeps the reactor's forward-Euler coolant update from overshooting below the cooling water temperature.</li>
 * <li><b>Outlet limit</b>: heat removal is capped so the cooling water leaves at no more than
 * {@code maxOutletKelvin} (it would boil above {@link CoolingWaterTemperatureBands#MAX_BAND_TEMPERATURE_KELVIN}).</li>
 * </ul>
 */
public final class ResidualHeatRemoval {

    /** Liquid water near room temperature (J/(kg K)) -- the cooling water side. */
    public static final double COOLING_WATER_SPECIFIC_HEAT = 4180.0;

    /** Why RHR cannot be placed in service right now. */
    public enum StartBlocker {
        TEMPERATURE_TOO_HIGH,
        PRESSURE_TOO_HIGH
    }

    /**
     * One tick's exchange result: heat removed from the primary coolant (W), the cooling water's outlet
     * temperature (K), and whether the outlet-temperature cap limited the heat removal.
     */
    public record Exchange(double heatWatts, double coolingOutletKelvin, boolean outletCapped) {

        public static Exchange none(double coolingInletKelvin) {
            return new Exchange(0.0, coolingInletKelvin, false);
        }
    }

    private ResidualHeatRemoval() {}

    /** @return the reason RHR cannot be started, or {@code null} if both interlocks are satisfied. */
    @Nullable
    public static StartBlocker startBlocker(double primaryKelvin, double pressureMPa, double maxStartKelvin,
                                            double maxStartPressureMPa) {
        if (primaryKelvin > maxStartKelvin) return StartBlocker.TEMPERATURE_TOO_HIGH;
        if (pressureMPa > maxStartPressureMPa) return StartBlocker.PRESSURE_TOO_HIGH;
        return null;
    }

    /** Whether an in-service RHR must isolate itself because the primary pressure exceeds its piping limit. */
    public static boolean mustIsolate(double pressureMPa, double isolationPressureMPa) {
        return pressureMPa > isolationPressureMPa;
    }

    /**
     * Counterflow heat exchanger effectiveness for {@code ntu = UA / C_min} and {@code capacityRatio = C_min /
     * C_max} (in {@code [0, 1]}).
     */
    public static double counterflowEffectiveness(double ntu, double capacityRatio) {
        if (ntu <= 0) return 0.0;
        if (Math.abs(1.0 - capacityRatio) < 1e-9) return ntu / (1.0 + ntu);
        double e = Math.exp(-ntu * (1.0 - capacityRatio));
        return (1.0 - e) / (1.0 - capacityRatio * e);
    }

    /**
     * @param primaryKelvin       primary coolant temperature entering the RHR heat exchanger
     * @param coolingInletKelvin  cooling water inlet temperature
     * @param primaryFlowKgPerS   primary-side flow through the heat exchanger
     * @param coolingFlowKgPerS   cooling water flow actually available this tick
     * @param uaWattsPerKelvin    heat exchanger UA
     * @param primarySpecificHeat primary coolant specific heat (J/(kg K))
     * @param coolingSpecificHeat cooling water specific heat (J/(kg K))
     * @param maxOutletKelvin     highest allowed cooling water outlet temperature
     */
    @NotNull
    public static Exchange exchange(double primaryKelvin, double coolingInletKelvin, double primaryFlowKgPerS,
                                    double coolingFlowKgPerS, double uaWattsPerKelvin,
                                    double primarySpecificHeat, double coolingSpecificHeat,
                                    double maxOutletKelvin) {
        double drivingKelvin = primaryKelvin - coolingInletKelvin;
        if (drivingKelvin <= 0 || primaryFlowKgPerS <= 0 || coolingFlowKgPerS <= 0 || uaWattsPerKelvin <= 0 ||
                primarySpecificHeat <= 0 || coolingSpecificHeat <= 0) {
            return Exchange.none(coolingInletKelvin);
        }
        double primaryCapacity = primaryFlowKgPerS * primarySpecificHeat;
        double coolingCapacity = coolingFlowKgPerS * coolingSpecificHeat;
        double minCapacity = Math.min(primaryCapacity, coolingCapacity);
        double maxCapacity = Math.max(primaryCapacity, coolingCapacity);
        double effectiveness = counterflowEffectiveness(uaWattsPerKelvin / minCapacity, minCapacity / maxCapacity);
        double heatWatts = effectiveness * minCapacity * drivingKelvin;

        double outletCapWatts = Math.max(0.0, (maxOutletKelvin - coolingInletKelvin) * coolingCapacity);
        boolean capped = heatWatts > outletCapWatts;
        if (capped) heatWatts = outletCapWatts;
        return new Exchange(heatWatts, coolingInletKelvin + heatWatts / coolingCapacity, capped);
    }
}
