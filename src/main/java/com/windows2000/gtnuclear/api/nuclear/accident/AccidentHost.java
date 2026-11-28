package com.windows2000.gtnuclear.api.nuclear.accident;

/**
 * The narrow set of reactor sensor reads {@link SevereAccidentProgressionTracker} needs from whatever reactor
 * multiblock it is driving (2026-10-06, extracted from {@code MetaTileEntityPressurizedWaterReactor} as the
 * second step of that class's own decomposition -- see this mod's design memory, same host-interface pattern as
 * {@code AutoControlHost}/{@code ScramHost}). Pure sensor reads only: the actual in-world explosion that fires
 * when the reactor's severe-accident stage crosses into {@link SevereAccidentStage#EXPLOSION} stays the host's
 * own responsibility (it cannot be made pure), so this interface only needs to supply
 * {@link SevereAccidentProgressionTracker#advance}'s inputs -- the host itself detects that stage crossing by
 * comparing before/after {@link SevereAccidentState#stage} and decides whether/how to fire the explosion.
 */
public interface AccidentHost {

    double getCoolantMassKg();

    /** Freshly recomputed each call -- unlike {@code ScramHost#getRatedCoolantMassKg()}, never cached. */
    double computeRatedCoolantMassKg();

    double getPrimaryVoidFraction();

    boolean isHousingMismatch();

    double getFuelTemperatureKelvin();

    double getMeltedFuelMassKg();

    /**
     * @see SevereAccidentProgressionTracker#advance for why this (not a live recompute) is used once melting starts.
     */
    double getCoreFuelMassAtMeltdownStartKg();

    double computeTotalCoreFuelMassKg();
}
