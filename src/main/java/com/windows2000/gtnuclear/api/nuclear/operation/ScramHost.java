package com.windows2000.gtnuclear.api.nuclear.operation;

/**
 * The narrow set of reactor sensor reads {@link ReactorScramEvaluator} needs from whatever reactor multiblock it
 * is driving (2026-10-06, extracted from {@code MetaTileEntityPressurizedWaterReactor} as the second step of
 * that class's own decomposition -- see this mod's design memory, same {@link AutoControlHost}-style pattern as
 * the first step). Pure sensor reads only, no commands: {@link ReactorScramEvaluator} only ever computes the
 * next {@link ReactorOperatingState} and hands it back for the host to apply, never mutates the host directly.
 * <p>
 * {@link #isShutdownComplete()} is deliberately left as a single opaque host-side query rather than decomposed
 * further (e.g. into raw control-rod insertion data) -- its own internals are host-multiblock-specific detail
 * {@link ReactorScramEvaluator} has no need to see.
 */
public interface ScramHost {

    double getTotalReactivity();

    double getRelativeNeutronPopulation();

    double getTemperatureKelvin();

    double getPressureMPa();

    double getCoolantMassKg();

    /** The reactor's own cached rated coolant mass -- see that field's own JavaDoc on its one-tick-stale freshness. */
    double getRatedCoolantMassKg();

    /** Wraps {@code World#isBlockPowered} -- a manual SCRAM-via-redstone trigger. */
    boolean isWorldBlockPowered();

    boolean hasActiveConsoleLink();

    boolean isPumpPowerUnavailable();

    /** @see ReactorScramEvaluator#checkAutomaticStageTransitions */
    boolean isShutdownComplete();
}
