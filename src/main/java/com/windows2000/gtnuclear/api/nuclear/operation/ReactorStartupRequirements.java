package com.windows2000.gtnuclear.api.nuclear.operation;

import org.jetbrains.annotations.Nullable;

/**
 * Every precondition the console's "startup" command checks, in display order, as a pure function (2026-11-21,
 * stage 1-3 (b) of this mod's reactor physics rework, see this mod's design memory). Splits into the plant being
 * physically prepared (coolant, pressure, heatup -- stage 6 of the 2026-09 startup sequence work) and the core
 * being operable (housing columns, assembly lengths, fuel, control rods -- the "operational requirements" moved
 * out of the structure pattern in stage 1-3, since GT's structure check only sees blocks, not slot contents).
 */
public final class ReactorStartupRequirements {

    /** Minimum number of intact control rod clusters a startup requires. */
    public static final int MIN_CONTROL_ROD_CLUSTERS = 4;

    /** The first unmet precondition, in display order. */
    public enum Blocker {
        HOUSING_MISMATCH,
        ASSEMBLY_TOO_LONG,
        NO_FUEL,
        INSUFFICIENT_CONTROL_RODS,
        RCP_NOT_RUNNING,
        COOLANT_NOT_FILLED,
        NOT_PRESSURIZED,
        NOT_HEATED_UP
    }

    /** Snapshot of every input the check needs. */
    public record Inputs(boolean housingMismatch, boolean assemblyTooLong, int loadedFuelAssemblies,
                         int intactControlRodClusters, boolean rcpRunning, double coolantMassKg,
                         double requiredCoolantMassKg,
                         double pressureMPa, double requiredPressureMPa, double temperatureKelvin,
                         double requiredTemperatureKelvin) {}

    private ReactorStartupRequirements() {}

    /** @return the first unmet precondition, or {@code null} if startup is allowed. */
    @Nullable
    public static Blocker firstBlocker(Inputs in) {
        if (in.housingMismatch()) return Blocker.HOUSING_MISMATCH;
        if (in.assemblyTooLong()) return Blocker.ASSEMBLY_TOO_LONG;
        if (in.loadedFuelAssemblies() <= 0) return Blocker.NO_FUEL;
        if (in.intactControlRodClusters() < MIN_CONTROL_ROD_CLUSTERS) return Blocker.INSUFFICIENT_CONTROL_RODS;
        if (!in.rcpRunning()) return Blocker.RCP_NOT_RUNNING;
        if (in.coolantMassKg() < in.requiredCoolantMassKg()) return Blocker.COOLANT_NOT_FILLED;
        if (in.pressureMPa() < in.requiredPressureMPa()) return Blocker.NOT_PRESSURIZED;
        if (in.temperatureKelvin() < in.requiredTemperatureKelvin()) return Blocker.NOT_HEATED_UP;
        return null;
    }
}
