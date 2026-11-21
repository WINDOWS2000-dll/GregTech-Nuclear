package com.windows2000.gtnuclear.api.nuclear.operation;

import org.jetbrains.annotations.NotNull;

/**
 * Transition logic for {@link ReactorOperatingState}, structure-independent like the rest of this mod's physics
 * classes (see {@link com.windows2000.gtnuclear.api.nuclear.accident.SevereAccidentEvaluator} for the same
 * immutable-state-plus-stateless-evaluator split applied to the deeper accident-progression state machine this
 * one hands off to indirectly -- a {@link ReactorOperatingStage#TRIPPED} reactor whose coolant loss is not
 * resolved is exactly the condition the caller feeds into that separate evaluator as
 * {@code abnormalCoolingOrControl}).
 * <p>
 * Every method here is a single attempted transition; called from a stage that cannot make that transition, it
 * returns {@code previous} unchanged (a no-op) rather than throwing -- transitions are driven by a mix of
 * operator commands (console buttons) and automatic physics conditions evaluated every tick, and neither side
 * should need to pre-check the current stage before calling.
 */
public final class ReactorOperatingEvaluator {

    private ReactorOperatingEvaluator() {}

    /** Operator command: begin bringing the reactor up from a full stop. Only valid from {@code SHUTDOWN}. */
    @NotNull
    public static ReactorOperatingState beginStartup(@NotNull ReactorOperatingState previous) {
        return previous.stage == ReactorOperatingStage.SHUTDOWN ?
                new ReactorOperatingState(ReactorOperatingStage.STARTUP, null) : previous;
    }

    /**
     * Automatic: the caller determined the reactor has gone critical (e.g. total reactivity from
     * {@link com.windows2000.gtnuclear.api.nuclear.neutronics.CMFDResult}, control rods, xenon, and boron is no
     * longer negative). Only valid from {@code STARTUP}; this deliberately does not model "rated power" as a
     * separate stage -- see {@link ReactorOperatingStage}'s own JavaDoc.
     */
    @NotNull
    public static ReactorOperatingState criticalityAchieved(@NotNull ReactorOperatingState previous) {
        return previous.stage == ReactorOperatingStage.STARTUP ?
                new ReactorOperatingState(ReactorOperatingStage.OPERATING, null) : previous;
    }

    /** Operator command: begin a planned shutdown. Only valid from {@code OPERATING}. */
    @NotNull
    public static ReactorOperatingState beginShutdown(@NotNull ReactorOperatingState previous) {
        return previous.stage == ReactorOperatingStage.OPERATING ?
                new ReactorOperatingState(ReactorOperatingStage.SHUTTING_DOWN, null) : previous;
    }

    /**
     * Automatic: the caller determined the planned shutdown has finished (power negligible, rods fully
     * inserted). Only valid from {@code SHUTTING_DOWN}.
     */
    @NotNull
    public static ReactorOperatingState shutdownComplete(@NotNull ReactorOperatingState previous) {
        return previous.stage == ReactorOperatingStage.SHUTTING_DOWN ?
                new ReactorOperatingState(ReactorOperatingStage.SHUTDOWN, null) : previous;
    }

    /**
     * SCRAM, from any trigger listed on {@link ScramReason}. Valid from any stage except {@code SHUTDOWN} (already
     * safe) and {@code TRIPPED} itself (idempotent -- re-tripping an already-tripped reactor does not overwrite
     * the original {@link ReactorOperatingState#lastScramReason}, matching a real annunciator latching the first
     * cause).
     */
    @NotNull
    public static ReactorOperatingState scram(@NotNull ReactorOperatingState previous, @NotNull ScramReason reason) {
        if (previous.stage == ReactorOperatingStage.SHUTDOWN || previous.stage == ReactorOperatingStage.TRIPPED) {
            return previous;
        }
        return new ReactorOperatingState(ReactorOperatingStage.TRIPPED, reason);
    }

    /**
     * Operator command from the console: the only way out of {@code TRIPPED}, deliberately never automatic (see
     * this mod's design memory: "安全条件が回復しても自動復旧はしない"). Only valid from {@code TRIPPED}. If the
     * condition that caused the SCRAM is still active, the caller's own per-tick SCRAM check will simply trip it
     * again on the next tick -- this method does not need to (and does not) re-check safety conditions itself.
     */
    @NotNull
    public static ReactorOperatingState manualReset(@NotNull ReactorOperatingState previous) {
        return previous.stage == ReactorOperatingStage.TRIPPED ?
                new ReactorOperatingState(ReactorOperatingStage.SHUTDOWN, null) : previous;
    }
}
