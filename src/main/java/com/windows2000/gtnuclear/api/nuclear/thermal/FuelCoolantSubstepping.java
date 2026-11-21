package com.windows2000.gtnuclear.api.nuclear.thermal;

/**
 * How many fuel&lt;-&gt;coolant coupling sub-steps a single tick needs to stay numerically stable (2026-10-07,
 * extracted from {@code MetaTileEntityPressurizedWaterReactor} as part of that class's own decomposition -- see
 * this mod's design memory). Already had no reactor-specific state dependency before the move -- every input is
 * an explicit parameter.
 */
public final class FuelCoolantSubstepping {

    /**
     * Bounds each fuel&lt;-&gt;coolant coupling sub-step so {@code dtSub * UA / minHeatCapacity} stays under this
     * margin -- the same stability bound {@code PointKineticsSolver} uses for its own sub-stepping (see that
     * class's own JavaDoc), applied here for the identical underlying reason: explicit Euler with a heat flux
     * held fixed over a full tick catastrophically overshoots once the fuel-coolant equilibration time constant
     * ({@code heatCapacity/UA}) is much shorter than a tick. Confirmed via a real playtest report, 2026-09-22
     * (diagnostic logging traced fuel temperature swinging 300K -&gt; -1344K -&gt; +97698K within two ticks --
     * clamped to the melting point and instantly reported as 100% melted -- from a UA on the order of
     * 10^7-10^8 W/K against a single fresh fuel rod's ~60000 J/K heat capacity, entirely without any neutron
     * population/fission power involved).
     */
    private static final double MAX_STEP_UA_RATIO = 0.5;

    /**
     * Floor below which a heat capacity (J/K) is treated as zero/absent rather than a genuine physical value --
     * added 2026-09-25 (real-playtest-driven, see this mod's design memory): 1.0 J/K is already many orders of
     * magnitude below any realistic fuel or coolant mass in this mod, but comfortably above the kind of
     * floating-point residue (observed: ~2e-7 J/K) that a long sequence of mass subtractions can leave behind
     * without ever landing on exactly 0.0.
     */
    private static final double MIN_MEANINGFUL_HEAT_CAPACITY = 1.0;

    /**
     * Hard ceiling on the sub-step count regardless of the computed stability bound -- added 2026-09-25 as
     * defense-in-depth alongside {@link #MIN_MEANINGFUL_HEAT_CAPACITY}: a real playtest confirmed this method
     * returning a value that saturated at {@code Integer.MAX_VALUE} (from a near-zero heat capacity not caught
     * by a bare "&gt; 0" check), producing a single tick that took ~12-13 real-world SECONDS. A tick that's
     * merely numerically approximate is far preferable to one that freezes the server, so this stays as a
     * second, independent line of defense.
     */
    private static final int MAX_FUEL_COOLANT_SUBSTEPS = 10_000;

    private FuelCoolantSubstepping() {}

    /**
     * See {@link #MAX_STEP_UA_RATIO}'s own JavaDoc. Whichever side (fuel or coolant) has the smaller heat
     * capacity is the more restrictive/"stiffer" one and determines the required sub-step count; a side with
     * zero (or floating-point-residue-sized, see {@link #MIN_MEANINGFUL_HEAT_CAPACITY}) heat capacity imposes no
     * constraint from that side.
     */
    public static int subStepCount(double ua, double fuelHeatCapacity, double coolantHeatCapacity,
                                   double tickSeconds) {
        if (ua <= 0) return 1;
        double minHeatCapacity = Double.MAX_VALUE;
        if (fuelHeatCapacity > MIN_MEANINGFUL_HEAT_CAPACITY) {
            minHeatCapacity = Math.min(minHeatCapacity, fuelHeatCapacity);
        }
        if (coolantHeatCapacity > MIN_MEANINGFUL_HEAT_CAPACITY) {
            minHeatCapacity = Math.min(minHeatCapacity, coolantHeatCapacity);
        }
        if (minHeatCapacity == Double.MAX_VALUE) return 1;
        double subStepLimitSeconds = MAX_STEP_UA_RATIO * minHeatCapacity / ua;
        int subSteps = Math.max(1, (int) Math.ceil(tickSeconds / subStepLimitSeconds));
        return Math.min(subSteps, MAX_FUEL_COOLANT_SUBSTEPS);
    }
}
