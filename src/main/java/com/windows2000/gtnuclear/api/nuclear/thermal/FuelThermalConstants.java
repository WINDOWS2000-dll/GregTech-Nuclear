package com.windows2000.gtnuclear.api.nuclear.thermal;

/**
 * Real, confident physical constants for UO2 fuel's own thermal behavior -- as with
 * {@code com.windows2000.gtnuclear.api.nuclear.accident.SevereAccidentConstants#FUEL_MELTING_TEMPERATURE_KELVIN},
 * these are standard nuclear engineering reference values, not gameplay-balance knobs, so they are hardcoded here
 * rather than Config-facing.
 */
public final class FuelThermalConstants {

    /** UO2's real specific heat capacity (J/(kg*K)) at typical operating temperature. */
    public static final double SPECIFIC_HEAT_CAPACITY_J_PER_KG_KELVIN = 300.0;

    /** UO2's real latent heat of fusion (J/kg) -- energy absorbed per kg converting solid fuel to corium. */
    public static final double LATENT_HEAT_OF_FUSION_J_PER_KG = 277000.0;

    private FuelThermalConstants() {}
}
