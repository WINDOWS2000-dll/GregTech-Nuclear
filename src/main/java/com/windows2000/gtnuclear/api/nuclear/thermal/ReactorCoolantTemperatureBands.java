package com.windows2000.gtnuclear.api.nuclear.thermal;

/**
 * Discrete temperature bands the primary coolant loop's physical fluid transport (reactor &lt;-&gt; heat exchanger,
 * via real GT fluid pipes) is quantized into -- see this mod's design memory (2026-09-22) for why: a continuously
 * -varying temperature carried as {@code FluidStack} NBT is fundamentally incompatible with Forge/GT's fluid
 * merge semantics ({@code FluidTank#fillInternal}/GT's own pipe {@code PipeTankList#findChannel} both require
 * exact NBT-tag equality to merge into an already-occupied tank/channel, and an ordinary single-channel pipe like
 * {@code ReactorCoolantPipeAlloy} has no fallback empty channel once occupied) -- once the reactor's temperature
 * starts changing every tick during real operation, fills/transfers would silently stall.
 * <p>
 * Each band is instead its own registered {@code Fluid} (see {@code GTNLMaterials#ReactorCoolantWaterBands}),
 * so GT's exact-type-equality merge logic works correctly without any special-casing: consecutive ticks usually
 * select the same band (bands are coarse relative to how fast temperature actually moves), and even when a
 * band boundary is crossed, the fluid transport layer just switches to a different (but still perfectly mergeable
 * amongst itself) registered fluid -- no stalling, no GTST changes needed.
 * <p>
 * This is purely a fluid-transport quantization: all of this reactor's actual physics
 * ({@code temperatureKelvin}, SCRAM thresholds, void/boiling, reactivity) continue to use the full-precision
 * continuous {@code double} values as before. A band's registered temperature is only read back out via
 * {@code Fluid#getTemperature()} (a stable, per-type property, not NBT) to feed
 * {@link CoolantThermalSolver#mixTemperatures} on the receiving end.
 */
public final class ReactorCoolantTemperatureBands {

    /** Lowest band's temperature (K) -- ambient/cold-shutdown coolant. */
    private static final double MIN_BAND_TEMPERATURE_KELVIN = 300.0;
    /**
     * Highest band's temperature (K) -- comfortably covers transient overshoot above
     * {@code ConfigHolder.reactor.maxSafeTemperatureKelvin}'s own 620K default before SCRAM catches it.
     */
    private static final double MAX_BAND_TEMPERATURE_KELVIN = 700.0;
    /**
     * Spacing (K) between consecutive bands. Narrowed from an original 50K down to 5K, 2026-09-28 (real-playtest-
     * driven, see this mod's design memory): during pre-criticality heatup, a reactor's coolant pump friction heat
     * alone moves the coolant only a few Kelvin per full primary-circulation round trip. With 50K spacing, that
     * round trip's own export-then-reimport always relabeled the returning coolant back down to the SAME
     * (unchanged) band, so genuine warming below the next band's own midpoint was silently discarded every cycle
     * -- a real playtest reproduced this exactly: primary coolant reached a rock-solid equilibrium at 304.8K,
     * hand-calculated (heating rate versus circulated mass fraction) to match the 50K bands' own band-0 fixed
     * point to within 0.02K, hundreds of Kelvin short of the 565K heatup target. A band spacing W only allows
     * forward progress once the true equilibrium overshoot above a band (heating rate / circulated mass fraction)
     * exceeds W/2; 5K keeps that margin comfortable for this mod's own reference reactor while still keeping the
     * registered-fluid count (81 bands from 300-700K) manageable.
     */
    private static final double BAND_SPACING_KELVIN = 5.0;

    /**
     * Representative temperatures (K) for each band, ascending -- see {@link #MIN_BAND_TEMPERATURE_KELVIN}/
     * {@link #MAX_BAND_TEMPERATURE_KELVIN}/{@link #BAND_SPACING_KELVIN}'s own JavaDoc for the endpoints/spacing
     * rationale. Band 0 (300K) is ambient/cold-shutdown coolant.
     */
    public static final double[] BAND_TEMPERATURES_KELVIN = generateBandTemperatures();

    private static double[] generateBandTemperatures() {
        int bandCount = (int) Math.round(
                (MAX_BAND_TEMPERATURE_KELVIN - MIN_BAND_TEMPERATURE_KELVIN) / BAND_SPACING_KELVIN) + 1;
        double[] bands = new double[bandCount];
        for (int i = 0; i < bandCount; i++) {
            bands[i] = MIN_BAND_TEMPERATURE_KELVIN + i * BAND_SPACING_KELVIN;
        }
        return bands;
    }

    private ReactorCoolantTemperatureBands() {}

    /**
     * @return the index into {@code bandTemperaturesKelvin} whose value is closest to {@code temperatureKelvin}
     *         (ties broken toward the lower band). Pure function, safe for values outside the array's own range
     *         (clamps to the nearest end).
     */
    public static int nearestBandIndex(double[] bandTemperaturesKelvin, double temperatureKelvin) {
        int nearest = 0;
        double nearestDistance = Double.MAX_VALUE;
        for (int i = 0; i < bandTemperaturesKelvin.length; i++) {
            double distance = Math.abs(bandTemperaturesKelvin[i] - temperatureKelvin);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = i;
            }
        }
        return nearest;
    }
}
