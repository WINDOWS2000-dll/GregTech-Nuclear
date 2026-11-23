package com.windows2000.gtnuclear.api.nuclear.thermal;

import org.jetbrains.annotations.NotNull;

/**
 * Per-category energy bookkeeping (Joules) for one thermal component -- added 2026-11-21 (stage 0-3 of this mod's
 * reactor physics rework, see this mod's design memory) so every later stage can verify energy conservation
 * numerically instead of by inspection. Accumulates over an arbitrary number of ticks (typically one telemetry
 * interval), then {@link #reset()}s.
 * <p>
 * Each category declares its own {@link Kind}: boundary {@link Kind#INFLOW}/{@link Kind#OUTFLOW} terms,
 * the component's own {@link Kind#STORED_CHANGE} (internal energy delta over the same window), and
 * {@link Kind#INFO} terms that are recorded for diagnosis but excluded from the balance (e.g. an internal transfer
 * between two parts of the same component, or a quantization error that happens outside this component's own
 * boundary). {@link #residualJoules()} is then {@code inflow - outflow - storedChange}, which a conserving model
 * keeps at (numerically) zero -- a nonzero residual is exactly the energy this model is silently creating or
 * destroying.
 * <p>
 * Pure Java with no Minecraft/GT dependency, matching every other class in {@code api.nuclear}; the concrete
 * category enums live next to the components that use them.
 *
 * @param <C> the component's own category enum
 */
public final class EnergyLedger<C extends Enum<C> & EnergyLedger.Category> {

    /** How a category participates in {@link #residualJoules()}. */
    public enum Kind {
        INFLOW,
        OUTFLOW,
        STORED_CHANGE,
        INFO
    }

    /** Implemented by each component's own category enum. */
    public interface Category {

        @NotNull
        Kind kind();
    }

    private final double[] joules;
    private int ticks = 0;

    public EnergyLedger(@NotNull Class<C> categoryType) {
        this.joules = new double[categoryType.getEnumConstants().length];
    }

    /** Adds {@code amountJoules} (may be negative, e.g. a stored-energy decrease) to {@code category}. */
    public void add(@NotNull C category, double amountJoules) {
        joules[category.ordinal()] += amountJoules;
    }

    /** Marks one simulation tick as accumulated into this window -- see {@link #ticks()}. */
    public void endTick() {
        ticks++;
    }

    public double get(@NotNull C category) {
        return joules[category.ordinal()];
    }

    /** Number of ticks accumulated since the last {@link #reset()}, for converting totals back into Watts. */
    public int ticks() {
        return ticks;
    }

    /**
     * @param categories every constant of this ledger's own category enum (i.e. {@code C.values()}) -- passed in
     *                   rather than stored, since generic enum arrays cannot be created reflectively without an
     *                   unchecked cast
     * @return {@code inflow - outflow - storedChange} over this window (J); zero for a perfectly conserving model
     */
    public double residualJoules(@NotNull C[] categories) {
        double residual = 0.0;
        for (C category : categories) {
            double value = joules[category.ordinal()];
            switch (category.kind()) {
                case INFLOW -> residual += value;
                case OUTFLOW, STORED_CHANGE -> residual -= value;
                case INFO -> {}
            }
        }
        return residual;
    }

    /** Clears every category and the tick count, starting a new window. */
    public void reset() {
        java.util.Arrays.fill(joules, 0.0);
        ticks = 0;
    }
}
