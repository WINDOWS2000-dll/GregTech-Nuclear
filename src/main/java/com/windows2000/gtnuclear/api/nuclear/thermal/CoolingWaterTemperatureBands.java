package com.windows2000.gtnuclear.api.nuclear.thermal;

/**
 * Discrete temperature bands of this mod's non-radioactive cooling water (2026-11-24, stage 1-4a of this mod's
 * reactor physics rework, see this mod's design memory) -- the same "one registered fluid per 5 K band" scheme as
 * {@link ReactorCoolantTemperatureBands}, for the same reason (a temperature carried in fluid NBT breaks Forge/GT
 * fluid merging). First used by the reactor's residual heat removal (RHR) heat exchanger's cooling side; meant to be
 * shared by future component cooling water / service water / cooling tower systems, so the name is generic.
 * <p>
 * The range stops at 370 K: the cooling water side runs near atmospheric pressure, where water boils at 373 K (real
 * component cooling water stays around 320-330 K). {@link ResidualHeatRemoval} caps its heat removal so the outlet
 * never exceeds {@link #MAX_BAND_TEMPERATURE_KELVIN}.
 */
public final class CoolingWaterTemperatureBands {

    public static final double MIN_BAND_TEMPERATURE_KELVIN = 300.0;
    public static final double MAX_BAND_TEMPERATURE_KELVIN = 370.0;
    private static final double BAND_SPACING_KELVIN = 5.0;

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

    /** Index of the band nearest {@code temperatureKelvin} (clamped to the range's ends). */
    public static int nearestBandIndex(double temperatureKelvin) {
        return ReactorCoolantTemperatureBands.nearestBandIndex(BAND_TEMPERATURES_KELVIN, temperatureKelvin);
    }

    private CoolingWaterTemperatureBands() {}
}
