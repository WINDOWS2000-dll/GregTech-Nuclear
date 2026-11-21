package com.windows2000.gtnuclear.api.nuclear.power;

import gregtech.api.capability.IEnergyContainer;

import org.jetbrains.annotations.NotNull;

/**
 * Reactor power consumption (EU/t), a two-term formula:
 * 
 * <pre>
 *   EU/t = baseCoefficientPerChannel * channelCount + pumpCoefficient * (currentThermalOutput / ratedThermalOutput)
 * </pre>
 * 
 * The baseline term represents instrumentation/control load -- consumed whenever the console is connected,
 * regardless of whether the reactor is running or stopped, scaling with reactor size (channel count). The pump
 * term represents primary coolant pump power, which in a real plant scales with coolant flow rate; since flow
 * rate is set to remove however much heat is currently being generated, that scales with current thermal output
 * relative to rated -- see this mod's design memory for the full derivation.
 */
public final class ReactorPowerConsumption {

    private ReactorPowerConsumption() {}

    /** The baseline (instrumentation/control) term alone -- see this class's own JavaDoc. */
    public static double basePowerEuPerTick(int channelCount, double baseCoefficientPerChannel) {
        return baseCoefficientPerChannel * channelCount;
    }

    /**
     * The pump term alone -- see this class's own JavaDoc. Split out from {@link #euPerTick} (2026-09-28) so a
     * caller that needs to convert only the pump's own draw into physical heat (e.g. reactor coolant pump
     * friction heat entering the coolant) is not stuck with the combined total, which also includes the
     * baseline's non-heat-generating instrumentation load.
     */
    public static double pumpPowerEuPerTick(double currentThermalOutput, double ratedThermalOutput,
                                            double pumpCoefficient) {
        double pumpLoadFraction = ratedThermalOutput > 0 ? currentThermalOutput / ratedThermalOutput : 0.0;
        return pumpCoefficient * pumpLoadFraction;
    }

    public static double euPerTick(int channelCount, double currentThermalOutput, double ratedThermalOutput,
                                   double baseCoefficientPerChannel, double pumpCoefficient) {
        return basePowerEuPerTick(channelCount, baseCoefficientPerChannel) +
                pumpPowerEuPerTick(currentThermalOutput, ratedThermalOutput, pumpCoefficient);
    }

    /**
     * Draws up to {@code desiredEuPerTick} from {@code energyContainer} (2026-10-07, extracted from
     * {@code MetaTileEntityPressurizedWaterReactor} as part of that class's own decomposition -- see this mod's
     * design memory). Already took only GT's own {@link IEnergyContainer} as a parameter before the move, with
     * no reactor-specific state involved at all.
     *
     * @return the amount actually removed, as a positive magnitude (0 if none desired or none available) --
     *         never more than what exists. {@link IEnergyContainer#removeEnergy(long)}'s own default
     *         implementation is {@code changeEnergy(-energyToRemove)}, and {@code changeEnergy} returns the
     *         signed delta actually applied (new stored minus old stored) -- i.e. <b>negative</b> for a
     *         successful removal, despite that method's own JavaDoc describing the return value as a plain
     *         "amount removed" (confirmed via a real playtest, 2026-09-28, see this mod's design memory: trusting
     *         that raw negative value directly made downstream EU/t and derived heat both negative). Negating
     *         the raw delta here is the single fix point.
     */
    public static long drawEnergy(@NotNull IEnergyContainer energyContainer, long desiredEuPerTick) {
        if (desiredEuPerTick <= 0) return 0L;
        long availableEuPerTick = Math.min(desiredEuPerTick, energyContainer.getEnergyStored());
        if (availableEuPerTick <= 0) return 0L;
        return -energyContainer.removeEnergy(availableEuPerTick);
    }
}
