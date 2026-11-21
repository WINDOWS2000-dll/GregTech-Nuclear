package com.windows2000.gtnuclear.api.nuclear.thermal;

/**
 * Representative water/steam phase-change constants shared by the primary loop's own subcooling-margin (voiding)
 * calculation and the secondary loop's boiling calculation -- both driven off {@link WaterSaturationCurve}, but
 * needing an energy-per-mass conversion for the phase change itself that the Wagner-equation saturation curve
 * does not provide. As with {@link CoolantThermalSolver#REPRESENTATIVE_SPECIFIC_HEAT_CAPACITY}, this is a single
 * representative value at typical PWR conditions, not a full pressure-dependent correlation.
 */
public final class SteamProperties {

    /**
     * Representative latent heat of vaporization (J/kg) for water flashing to steam at typical PWR primary/
     * secondary pressures (order of magnitude at ~7-15 MPa) -- notably lower than water's textbook atmospheric-
     * pressure value (~2257000 J/kg) because latent heat drops as pressure rises toward the critical point.
     */
    public static final double LATENT_HEAT_OF_VAPORIZATION_J_PER_KG = 1500000.0;

    private SteamProperties() {}
}
