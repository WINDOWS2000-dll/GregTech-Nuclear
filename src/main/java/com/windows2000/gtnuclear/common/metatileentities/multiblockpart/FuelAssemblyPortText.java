package com.windows2000.gtnuclear.common.metatileentities.multiblockpart;

import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * Number formatting for {@link MetaTileEntityFuelAssemblyPort}'s provisional UI (2026-11-21, stage 1-3 (d)), kept
 * pure so the formats are unit-testable. Labels themselves come from the lang file; this only formats the values
 * substituted into them.
 */
public final class FuelAssemblyPortText {

    /** Physical height of one channel layer (one 21.5 cm assembly-pitch cube, see this mod's design memory). */
    public static final double LAYER_HEIGHT_METERS = 0.215;

    private FuelAssemblyPortText() {}

    /** e.g. {@code 17 -> "17 (3.66 m)"}. */
    @NotNull
    public static String length(int layers) {
        return String.format(Locale.ROOT, "%d (%.2f m)", layers, layers * LAYER_HEIGHT_METERS);
    }

    /** e.g. {@code 0.045 -> "4.50%"}. */
    @NotNull
    public static String enrichment(double u235Fraction) {
        return String.format(Locale.ROOT, "%.2f%%", u235Fraction * 100.0);
    }

    /** e.g. {@code 0.823 -> "82%"}. */
    @NotNull
    public static String percent(double fraction) {
        return String.format(Locale.ROOT, "%.0f%%", fraction * 100.0);
    }

    /** e.g. {@code 7.43 -> "7.4 kg"}, {@code 0.0123 -> "0.012 kg"}. */
    @NotNull
    public static String kilograms(double kg) {
        if (kg >= 10.0) return String.format(Locale.ROOT, "%.1f kg", kg);
        if (kg >= 1.0) return String.format(Locale.ROOT, "%.2f kg", kg);
        return String.format(Locale.ROOT, "%.3f kg", kg);
    }

    /** e.g. {@code -2850.4 -> "-2850 pcm"}. */
    @NotNull
    public static String pcm(double value) {
        if (Double.isNaN(value)) return "---";
        return String.format(Locale.ROOT, "%.0f pcm", value);
    }

    /** Relative local power, e.g. {@code 1.1234 -> "1.12"}. */
    @NotNull
    public static String relativePower(double value) {
        if (Double.isNaN(value)) return "---";
        return String.format(Locale.ROOT, "%.2f", value);
    }

    /** Fuel temperature, or a placeholder until per-node fuel temperature exists (stage 5). */
    @NotNull
    public static String temperature(double kelvin) {
        if (Double.isNaN(kelvin)) return "---";
        return String.format(Locale.ROOT, "%.0f K", kelvin);
    }
}
