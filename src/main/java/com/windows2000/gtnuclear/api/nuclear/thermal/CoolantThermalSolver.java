package com.windows2000.gtnuclear.api.nuclear.thermal;

/**
 * Advances primary coolant temperature via a simple lumped (single-node, well-mixed) energy balance:
 * {@code dT/dt = netHeatInputWatts / (coolantMassKg * specificHeatCapacity)}. Reactor thermal power generation,
 * heat removal via the (not yet designed) heat exchanger, and any other heat sources/sinks are all the caller's
 * responsibility to net together into {@code netHeatInputWatts} -- this class deliberately does not model the
 * heat exchanger itself (see this mod's design memory: building that model before the heat exchanger multiblock
 * itself is designed would mean guessing at its interface).
 * <p>
 * <b>Integration method:</b> explicit (forward) Euler. Unlike
 * {@link com.windows2000.gtnuclear.api.nuclear.neutronics.PointKineticsSolver}'s prompt-neutron term, this
 * system is not stiff at the tick-scale time steps this mod uses elsewhere, so no implicit scheme is needed.
 */
public final class CoolantThermalSolver {

    /**
     * Representative specific heat capacity of saturated liquid water (J/(kg*K)) at typical PWR primary coolant
     * conditions (~300 degC, ~15.5 MPa) -- notably higher than water's textbook room-temperature value
     * (~4186 J/(kg*K)) because water's specific heat rises markedly as the critical point is approached. A
     * temperature-dependent correlation would be more precise but is not implemented in this first version.
     */
    public static final double REPRESENTATIVE_SPECIFIC_HEAT_CAPACITY = 5300.0;

    private CoolantThermalSolver() {}

    public static double advance(double temperatureKelvin, double netHeatInputWatts, double coolantMassKg,
                                 double specificHeatCapacity, double dtSeconds) {
        double heatCapacity = coolantMassKg * specificHeatCapacity;
        if (heatCapacity <= 0) return temperatureKelvin;
        return temperatureKelvin + dtSeconds * netHeatInputWatts / heatCapacity;
    }

    /**
     * Mass-weighted average temperature (K) of two mixed masses of the same fluid -- the same explicit
     * mass-balance formula this mod already uses for boron concentration mixing (see
     * {@code MetaTileEntityPressurizedWaterReactor#updateBoronAdjustment}), applied to temperature instead of
     * concentration. Used wherever incoming coolant of a known mass/temperature (e.g. drained from a fluid hatch,
     * read off a {@code FluidStack}'s own NBT tag) merges into an existing lumped-node mass/temperature.
     *
     * @return {@code temperatureA} unchanged if both masses are zero (nothing to mix).
     */
    public static double mixTemperatures(double massA, double temperatureA, double massB, double temperatureB) {
        double totalMass = massA + massB;
        if (totalMass <= 0) return temperatureA;
        return (massA * temperatureA + massB * temperatureB) / totalMass;
    }
}
