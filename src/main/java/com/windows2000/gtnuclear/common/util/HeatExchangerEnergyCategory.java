package com.windows2000.gtnuclear.common.util;

import com.windows2000.gtnuclear.api.nuclear.thermal.EnergyLedger;
import org.jetbrains.annotations.NotNull;

/**
 * {@link EnergyLedger} categories for {@code MetaTileEntityHeatExchanger}'s own primary-side + secondary-side
 * inventory (2026-11-21, stage 0-3 -- see {@link EnergyLedger}'s own JavaDoc). Same {@code h = c_p * (T - 300 K)}
 * liquid convention as {@link ReactorEnergyCategory}; steam additionally carries
 * {@code SteamProperties.LATENT_HEAT_OF_VAPORIZATION_J_PER_KG}.
 */
public enum HeatExchangerEnergyCategory implements EnergyLedger.Category {

    /** Hot primary coolant drained from this heat exchanger's coolant import hatch, at its band temperature. */
    PRIMARY_IMPORT(EnergyLedger.Kind.INFLOW),
    /** Feedwater drained into the secondary side, at its assumed inlet temperature. */
    FEEDWATER(EnergyLedger.Kind.INFLOW),
    /** Cooled primary coolant pumped back out, at the primary side's own actual temperature. */
    PRIMARY_EXPORT(EnergyLedger.Kind.OUTFLOW),
    /** High-pressure steam exported (sensible heat up to saturation plus latent heat). */
    STEAM(EnergyLedger.Kind.OUTFLOW),
    /** Secondary water mass discarded above its rated inventory after feedwater mixing (blowdown abstraction). */
    BLOWDOWN(EnergyLedger.Kind.OUTFLOW),
    /** Change in primary-side + secondary-side internal energy. */
    STORED_CHANGE(EnergyLedger.Kind.STORED_CHANGE),
    /** Primary-to-secondary heat transfer -- internal to this component, recorded for reference only. */
    TRANSFER(EnergyLedger.Kind.INFO),
    /**
     * {@code (T - T_band) * m * c_p} of every exported primary batch -- see
     * {@link ReactorEnergyCategory#BAND_ROUNDING}.
     */
    BAND_ROUNDING(EnergyLedger.Kind.INFO);

    private final EnergyLedger.Kind kind;

    HeatExchangerEnergyCategory(@NotNull EnergyLedger.Kind kind) {
        this.kind = kind;
    }

    @NotNull
    @Override
    public EnergyLedger.Kind kind() {
        return kind;
    }
}
