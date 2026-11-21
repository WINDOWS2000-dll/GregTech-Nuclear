package com.windows2000.gtnuclear.api.nuclear.decay;

import gregtech.api.unification.Element;
import gregtech.api.unification.Elements;

import com.windows2000.gtnuclear.common.ConfigHolder;

/**
 * Power-law time compression for {@link DecayEngine}: maps a real-world half-life (seconds) to the "compressed"
 * half-life actually used for in-game decay, via {@code compressed = k * real^p}. A pure power law preserves
 * relative ordering/ratios between isotopes (self-similar across scales) while still compressing the real-world
 * range -- roughly microseconds to tens of billions of years, ~21 orders of magnitude -- down to a
 * gameplay-reasonable one (fractions of a second up to a few days), unlike a flat linear/logarithmic scale which
 * would either barely compress the long end or collapse most of the short end together.
 * <p>
 * The two knobs ({@link ConfigHolder.DecayOptions#daysForLongestIsotopeHalfLife},
 * {@link ConfigHolder.DecayOptions#compressionExponent}) are read fresh on every call (not cached), so config
 * changes take effect immediately; only the scan for the single longest real half-life among all registered
 * {@link Element}s is cached, since that's a fixed property of the registered dataset, not something that changes
 * at runtime.
 */
public final class TimeCompression {

    private static final double SECONDS_PER_DAY = 86400.0;

    private static double cachedLongestRealHalfLifeSeconds = -1;

    private TimeCompression() {}

    /**
     * @param realHalfLifeSeconds a real-world half-life in seconds; non-positive (stable/unmodeled) values are
     *                            returned unchanged, since {@link DecayEngine} never uses them as a decay constant
     *                            anyway.
     * @return the compressed half-life (seconds) to actually use for in-game decay.
     */
    public static double compress(double realHalfLifeSeconds) {
        if (realHalfLifeSeconds <= 0) return realHalfLifeSeconds;
        double p = ConfigHolder.decay.compressionExponent;
        return scaleFactor(p) * Math.pow(realHalfLifeSeconds, p);
    }

    private static double scaleFactor(double p) {
        double targetCompressedSeconds = ConfigHolder.decay.daysForLongestIsotopeHalfLife * SECONDS_PER_DAY;
        return targetCompressedSeconds / Math.pow(longestRealHalfLifeSeconds(), p);
    }

    private static double longestRealHalfLifeSeconds() {
        if (cachedLongestRealHalfLifeSeconds > 0) return cachedLongestRealHalfLifeSeconds;
        double max = 0;
        for (Element element : Elements.getAllElements()) {
            if (element.halfLifeSeconds > max) max = element.halfLifeSeconds;
        }
        cachedLongestRealHalfLifeSeconds = max > 0 ? max : 1.0;
        return cachedLongestRealHalfLifeSeconds;
    }
}
