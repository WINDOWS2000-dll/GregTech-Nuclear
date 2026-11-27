package com.windows2000.gtnuclear.api.nuclear.operation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * Pure refueling logic (2026-11-27, stage 1-4c of this mod's reactor physics rework, following a real PWR's Mode 6
 * procedures as closely as practical -- see this mod's design memory): the start conditions (1)-(9), the
 * subcritical multiplication used for the count-rate-doubling watch, the automatic hold, and the operating mode
 * (1-6) shown on the console.
 */
public final class RefuelingEvaluator {

    /** The start conditions, in their decided numbering (1)-(9). */
    public enum Condition {
        /** (1) SHUTDOWN (a trip must have been reset). */
        SHUTDOWN,
        /** (2) every control rod drive fully inserted. */
        RODS_INSERTED,
        /** (3) neutron population at or below the shutdown level. */
        SUBCRITICAL,
        /** (4) boron at or above the refueling concentration. */
        BORON,
        /** (5) k_eff <= 0.95 with all rods out -- substituted by (4) until stage 2. */
        SHUTDOWN_MARGIN,
        /** (6) coolant at or below the refueling temperature. */
        TEMPERATURE,
        /** (7) depressurized to atmospheric. */
        PRESSURE,
        /** (8) subcritical long enough (decay heat proxy until stage 2). */
        WAIT_TIME,
        /** (9) coolant inventory at or above rated (cavity flood substitute). */
        COOLANT
    }

    public enum Status {
        MET,
        UNMET,
        /** Not evaluated on its own yet; another condition stands in for it. */
        SUBSTITUTED
    }

    /** Why loading was put on hold automatically. */
    public enum HoldReason {
        COUNT_RATE_DOUBLED,
        TEMPERATURE_HIGH,
        COOLANT_LOW,
        BORON_LOW
    }

    /** Limits, from config. */
    public record Limits(double shutdownPowerLevel, double minBoronPpm, double maxTemperatureKelvin,
                         double maxPressureMPa, double minSubcriticalMinutes) {}

    /**
     * Plant state for the checklist. {@code subcriticalMinutes} is {@link Double#POSITIVE_INFINITY} for a core
     * that has never been critical (fresh fuel has no decay heat to wait out).
     */
    public record Inputs(boolean shutdownStage, boolean allRodsInserted, double relativePower, double boronPpm,
                         double temperatureKelvin, double pressureMPa, double subcriticalMinutes,
                         double coolantMassKg, double ratedCoolantMassKg) {}

    /** The multiplication ratio at or above which a hold is declared ("count rate doubling"). */
    public static final double DOUBLING_RATIO = 2.0;

    private RefuelingEvaluator() {}

    @NotNull
    public static Map<Condition, Status> checklist(@NotNull Inputs in, @NotNull Limits limits) {
        Map<Condition, Status> result = new EnumMap<>(Condition.class);
        result.put(Condition.SHUTDOWN, status(in.shutdownStage()));
        result.put(Condition.RODS_INSERTED, status(in.allRodsInserted()));
        result.put(Condition.SUBCRITICAL, status(in.relativePower() <= limits.shutdownPowerLevel()));
        result.put(Condition.BORON, status(in.boronPpm() >= limits.minBoronPpm()));
        result.put(Condition.SHUTDOWN_MARGIN, Status.SUBSTITUTED);
        result.put(Condition.TEMPERATURE, status(in.temperatureKelvin() <= limits.maxTemperatureKelvin()));
        result.put(Condition.PRESSURE, status(in.pressureMPa() <= limits.maxPressureMPa()));
        result.put(Condition.WAIT_TIME, status(in.subcriticalMinutes() >= limits.minSubcriticalMinutes()));
        result.put(Condition.COOLANT, status(in.coolantMassKg() >= in.ratedCoolantMassKg()));
        return result;
    }

    private static Status status(boolean met) {
        return met ? Status.MET : Status.UNMET;
    }

    /** The first unmet condition, in numbering order; {@code null} if all are met (or substituted). */
    @Nullable
    public static Condition firstUnmet(@NotNull Map<Condition, Status> checklist, boolean includeStage) {
        for (Condition condition : Condition.values()) {
            if (!includeStage && condition == Condition.SHUTDOWN) continue;
            if (checklist.get(condition) == Status.UNMET) return condition;
        }
        return null;
    }

    /**
     * The console's one-line checklist, e.g. {@code "REFUEL: 1o 2o 3o 4x 5- 6x 7o 8x 9o"} ({@code o} met, {@code x}
     * unmet, {@code -} substituted). During refueling condition (1) reads met (the reactor left SHUTDOWN by design).
     * The one-condition-per-line form is kept as an idea for roadmap G's console redesign (2026-11-27 decision).
     */
    @NotNull
    public static String summaryLine(@NotNull Map<Condition, Status> checklist, boolean refueling) {
        StringBuilder line = new StringBuilder("REFUEL:");
        for (Condition condition : Condition.values()) {
            Status status = refueling && condition == Condition.SHUTDOWN ? Status.MET : checklist.get(condition);
            line.append(' ').append(condition.ordinal() + 1).append(switch (status) {
                case MET -> 'o';
                case UNMET -> 'x';
                case SUBSTITUTED -> '-';
            });
        }
        return line.toString();
    }

    /** Whether refueling may begin: every condition (1)-(9) met. */
    public static boolean canStart(@NotNull Map<Condition, Status> checklist) {
        return firstUnmet(checklist, true) == null;
    }

    /** Whether a hold may be lifted: conditions (2)-(9) met (the stage is REFUELING, not SHUTDOWN, by then). */
    public static boolean canResume(@NotNull Map<Condition, Status> checklist) {
        return firstUnmet(checklist, false) == null;
    }

    /**
     * Subcritical multiplication {@code M = 1 / (1 - k)} from the total reactivity {@code rho} (with
     * {@code k = 1 / (1 - rho)}, so {@code M = (1 - rho) / -rho}). The count rate of a source-driven subcritical core
     * is proportional to M, so watching M is watching the count rate. A core with no fuel multiplies nothing (M = 1);
     * a core at or above critical has no finite multiplication ({@link Double#POSITIVE_INFINITY}).
     */
    public static double multiplication(double totalReactivity, boolean hasFuel) {
        if (!hasFuel) return 1.0;
        if (totalReactivity >= 0) return Double.POSITIVE_INFINITY;
        return (1.0 - totalReactivity) / -totalReactivity;
    }

    /** @return the reason loading must be put on hold now, or {@code null} if it may continue. */
    @Nullable
    public static HoldReason holdReason(double multiplication, double baselineMultiplication, double temperatureKelvin,
                                        double coolantMassKg, double ratedCoolantMassKg, double boronPpm,
                                        @NotNull Limits limits) {
        if (multiplication >= DOUBLING_RATIO * baselineMultiplication) return HoldReason.COUNT_RATE_DOUBLED;
        if (temperatureKelvin > limits.maxTemperatureKelvin()) return HoldReason.TEMPERATURE_HIGH;
        if (coolantMassKg < ratedCoolantMassKg) return HoldReason.COOLANT_LOW;
        if (boronPpm < limits.minBoronPpm()) return HoldReason.BORON_LOW;
        return null;
    }

    /** k_eff threshold (0.99) separating Modes 1-2 from 3-5. */
    public static final double CRITICAL_MODE_K = 0.99;
    /** Mode 1/2 split: relative power above 5%. */
    public static final double POWER_MODE_FRACTION = 0.05;
    /** Mode 3/4 split: 350 F. */
    public static final double HOT_STANDBY_KELVIN = 450.0;
    /** Mode 4/5 split: 200 F. */
    public static final double COLD_SHUTDOWN_KELVIN = 366.0;

    /**
     * A real PWR's Technical Specification operating mode: 1 power operation, 2 startup, 3 hot standby, 4 hot
     * shutdown, 5 cold shutdown, 6 refueling.
     */
    public static int operatingMode(boolean refueling, double totalReactivity, boolean hasFuel, double relativePower,
                                    double temperatureKelvin) {
        if (refueling) return 6;
        double k = hasFuel ? 1.0 / (1.0 - totalReactivity) : 0.0;
        if (hasFuel && (totalReactivity >= 1.0 || k >= CRITICAL_MODE_K)) {
            return relativePower > POWER_MODE_FRACTION ? 1 : 2;
        }
        if (temperatureKelvin >= HOT_STANDBY_KELVIN) return 3;
        if (temperatureKelvin > COLD_SHUTDOWN_KELVIN) return 4;
        return 5;
    }
}
