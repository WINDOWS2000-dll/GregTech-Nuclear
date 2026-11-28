package com.windows2000.gtnuclear.api.nuclear.accident;

/**
 * Thresholds for {@link SevereAccidentEvaluator}. {@link #FUEL_MELTING_TEMPERATURE_KELVIN} is a real, confident
 * physical constant (UO2 ceramic fuel's actual melting point); the explosion trigger/magnitude parameters below
 * are gameplay-balance choices with no real-world physical derivation (real severe-accident explosion energetics
 * -- steam/hydrogen explosions -- are far too complex to reduce to a single formula), so they are Config-facing
 * and expected to need tuning once the multiblock reactor itself is built and playtested.
 */
public final class SevereAccidentConstants {

    /** UO2 fuel's real melting point (K): ~2865 degC, the standard value cited in nuclear engineering references. */
    public static final double FUEL_MELTING_TEMPERATURE_KELVIN = 3138.0;

    /**
     * Fuel temperature (K) at which {@link SevereAccidentStage#UNCONTROLLED_HEATING} begins (2026-11-28, stage 2-3b,
     * decision O2): 1477 K = 1204 degC (2200 F), the peak cladding temperature limit of the US ECCS acceptance
     * criteria (10 CFR 50.46), where cladding oxidation and damage start. Compared against this mod's single lumped
     * fuel temperature until stage 5 resolves it per node (then the hottest node should be used).
     */
    public static final double FUEL_DAMAGE_TEMPERATURE_KELVIN = 1477.0;
    /** Fuel must cool this far below {@link #FUEL_DAMAGE_TEMPERATURE_KELVIN} to return to NORMAL (no melting yet). */
    public static final double FUEL_DAMAGE_RECOVERY_HYSTERESIS_KELVIN = 100.0;

    /**
     * Core melt fraction (0-1) at or above which {@link SevereAccidentStage#MELTDOWN} escalates to
     * {@link SevereAccidentStage#EXPLOSION} even without direct coolant contact.
     */
    public final double explosionMeltFractionThreshold;
    /** Scales {@link SevereAccidentState#explosionMagnitude} -- linear in melted fuel mass and melt fraction. */
    public final double explosionMagnitudeCoefficient;

    public SevereAccidentConstants(double explosionMeltFractionThreshold, double explosionMagnitudeCoefficient) {
        this.explosionMeltFractionThreshold = explosionMeltFractionThreshold;
        this.explosionMagnitudeCoefficient = explosionMagnitudeCoefficient;
    }
}
