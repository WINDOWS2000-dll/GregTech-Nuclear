package com.windows2000.gtnuclear.common.util;

import com.windows2000.gtnuclear.api.nuclear.thermal.EnergyLedger;
import org.jetbrains.annotations.NotNull;

/**
 * {@link EnergyLedger} categories for {@code MetaTileEntityPressurizedWaterReactor}'s own fuel + primary coolant
 * inventory (2026-11-21, stage 0-3 -- see {@link EnergyLedger}'s own JavaDoc). Liquid enthalpies use this mod's
 * current constant-specific-heat convention, {@code h = c_p * (T - 300 K)}, matching
 * {@code CoolantThermalSolver}.
 */
public enum ReactorEnergyCategory implements EnergyLedger.Category {

    /** Fission heat deposited in the fuel. */
    FISSION(EnergyLedger.Kind.INFLOW),
    /** Reactor coolant pump heat deposited in the coolant. */
    RCP(EnergyLedger.Kind.INFLOW),
    /** Returning primary coolant drained from the coolant import hatch(es), at its fluid band temperature. */
    IMPORT(EnergyLedger.Kind.INFLOW),
    /** Coolant injection / boron adjustment input fluid, at its own fluid temperature. */
    MAKEUP_IN(EnergyLedger.Kind.INFLOW),
    /** Primary coolant pumped out to the coolant export hatch(es), at this reactor's own actual temperature. */
    EXPORT(EnergyLedger.Kind.OUTFLOW),
    /** Boron-adjustment letdown (radioactive wastewater) removed, at this reactor's own actual temperature. */
    MAKEUP_OUT(EnergyLedger.Kind.OUTFLOW),
    /** Heat removed by the residual heat removal system into its cooling water (stage 1-4a). */
    RHR(EnergyLedger.Kind.OUTFLOW),
    /** Residual heat removal pump heat deposited in the coolant (stage 1-4a). */
    RHR_PUMP(EnergyLedger.Kind.INFLOW),
    /** Change in fuel + coolant internal energy (including void latent heat and fuel melting latent heat). */
    STORED_CHANGE(EnergyLedger.Kind.STORED_CHANGE),
    /**
     * {@code (T - T_band) * m * c_p} of every exported batch -- the energy lost (or gained) in transit because the
     * exported fluid is labeled with its nearest temperature band rather than its exact temperature. Happens
     * outside this reactor's own boundary, so it is informational here (excluded from the residual).
     */
    BAND_ROUNDING(EnergyLedger.Kind.INFO);

    private final EnergyLedger.Kind kind;

    ReactorEnergyCategory(@NotNull EnergyLedger.Kind kind) {
        this.kind = kind;
    }

    @NotNull
    @Override
    public EnergyLedger.Kind kind() {
        return kind;
    }
}
