package com.windows2000.gtnuclear.api.nuclear.accident;

/**
 * Detects a loss-of-coolant accident (LOCA): the current primary coolant mass falling below a configurable
 * fraction of the reactor's rated ("full tank") mass -- see
 * {@link com.windows2000.gtnuclear.api.nuclear.thermal.RatedCoolantMass}. The result is meant to feed into
 * {@link SevereAccidentEvaluator#advance}'s {@code abnormalCoolingOrControl} flag alongside structural damage
 * (e.g. a formed-but-invalid fuel/control-rod housing column), not to drive severe-accident staging on its own --
 * combining existing signals rather than introducing a separate damage-detection system.
 */
public final class LOCADetector {

    private LOCADetector() {}

    /**
     * @param currentCoolantMassKg  current primary coolant mass
     * @param ratedCoolantMassKg    the reactor's rated ("full tank") primary coolant mass, always &gt;= 0
     * @param locaThresholdFraction fraction (0-1) of rated mass below which a LOCA is declared
     * @return true if the current coolant mass is below the threshold fraction of rated mass
     */

    public static boolean isLOCA(double currentCoolantMassKg, double ratedCoolantMassKg, double locaThresholdFraction) {
        if (ratedCoolantMassKg <= 0) return false;
        return currentCoolantMassKg < locaThresholdFraction * ratedCoolantMassKg;
    }
}
