package com.windows2000.gtnuclear.api.nuclear.operation;

import com.windows2000.gtnuclear.api.nuclear.accident.LOCADetector;
import com.windows2000.gtnuclear.common.ConfigHolder;
import org.jetbrains.annotations.NotNull;

/**
 * The reactor's own operating-state machine: the two automatic (non-operator-command) stage transitions plus
 * every SCRAM trigger except the manual button (2026-10-06, extracted from
 * {@code MetaTileEntityPressurizedWaterReactor} as the second step of that class's own decomposition -- see this
 * mod's design memory). Every sensor read goes through the narrow {@link ScramHost} interface, so this class has
 * no dependency on the concrete reactor multiblock -- and, unlike {@link ReactorAutoControlSequencer}, issues no
 * commands at all: both {@link #checkAutomaticStageTransitions}/{@link #checkScramConditions} are pure functions
 * of {@code (host, current state)} that simply return the next {@link ReactorOperatingState} for the host to
 * apply (same {@code null}-free, always-returns-a-state style as {@link ReactorOperatingEvaluator} itself).
 * <p>
 * Holds only {@link #linkLossTicks}/{@link #powerLossTicks} -- the debounce counters genuinely specific to this
 * evaluator's own pacing, mirroring how {@link ReactorAutoControlSequencer} holds only its own pacing/controller
 * state. {@link ReactorOperatingState} itself stays host-owned (unlike the auto-control loop's own
 * {@code active} flag): plenty of other reactor commands -- {@code requestStartup}/{@code requestShutdown}/
 * {@code requestManualScram}, etc. -- read and write it too, so moving ownership here would ripple far beyond
 * this one evaluator.
 */
public final class ReactorScramEvaluator {

    private int linkLossTicks = 0;
    private int powerLossTicks = 0;

    /**
     * {@code STARTUP} -&gt; {@code OPERATING} once total reactivity is no longer negative (the reactor has gone
     * critical), and {@code SHUTTING_DOWN} -&gt; {@code SHUTDOWN} once {@link ScramHost#isShutdownComplete()}.
     */
    @NotNull
    public ReactorOperatingState checkAutomaticStageTransitions(@NotNull ScramHost host,
                                                                @NotNull ReactorOperatingState current) {
        if (current.stage == ReactorOperatingStage.STARTUP && host.getTotalReactivity() >= 0) {
            return ReactorOperatingEvaluator.criticalityAchieved(current);
        }
        if (current.stage == ReactorOperatingStage.SHUTTING_DOWN && host.isShutdownComplete()) {
            return ReactorOperatingEvaluator.shutdownComplete(current);
        }
        return current;
    }

    /**
     * Checks four of the five SCRAM triggers from this mod's design memory (2026-09-17: "温度/圧力の安全限界超過、
     * 一次冷却水量不足(LOCA相当)、手動ボタン/レッドストーン信号、コンソール接続の喪失") -- the fifth, the manual
     * button, is the host's own {@code requestManualScram()}, called directly from the console rather than
     * checked here. Plus {@link ScramReason#POWER_LIMIT} (2026-09-24, real-playtest-driven -- see that
     * constant's own JavaDoc), the first trigger here that watches relative power itself rather than a
     * downstream consequence of it. Each trigger independently attempts {@link ReactorOperatingEvaluator#scram},
     * which is a no-op once already {@code TRIPPED} (or while {@code SHUTDOWN}) -- so triggers are checked
     * unconditionally every call with no need to short-circuit once one has already fired.
     */
    @NotNull
    public ReactorOperatingState checkScramConditions(@NotNull ScramHost host, @NotNull ReactorOperatingState current) {
        ReactorOperatingState state = current;

        if (host.getTemperatureKelvin() >= ConfigHolder.reactor.maxSafeTemperatureKelvin) {
            state = ReactorOperatingEvaluator.scram(state, ScramReason.TEMPERATURE_LIMIT);
        }
        if (host.getPressureMPa() >= ConfigHolder.reactor.maxSafePressureMPa) {
            state = ReactorOperatingEvaluator.scram(state, ScramReason.PRESSURE_LIMIT);
        }
        if (host.getRelativeNeutronPopulation() >= ConfigHolder.reactor.maxSafeRelativePower) {
            state = ReactorOperatingEvaluator.scram(state, ScramReason.POWER_LIMIT);
        }
        if (LOCADetector.isLOCA(host.getCoolantMassKg(), host.getRatedCoolantMassKg(),
                ConfigHolder.reactor.locaCoolantFraction)) {
            state = ReactorOperatingEvaluator.scram(state, ScramReason.COOLANT_LOSS);
        }
        if (host.isWorldBlockPowered()) {
            state = ReactorOperatingEvaluator.scram(state, ScramReason.REDSTONE_SIGNAL);
        }

        if (host.hasActiveConsoleLink()) {
            linkLossTicks = 0;
        } else {
            linkLossTicks++;
            if (linkLossTicks >= ConfigHolder.reactor.linkLossScramDebounceTicks) {
                state = ReactorOperatingEvaluator.scram(state, ScramReason.CONSOLE_LINK_LOST);
            }
        }

        if (!host.isPumpPowerUnavailable()) {
            powerLossTicks = 0;
        } else {
            powerLossTicks++;
            if (powerLossTicks >= ConfigHolder.reactor.powerLossScramDebounceTicks) {
                state = ReactorOperatingEvaluator.scram(state, ScramReason.LOSS_OF_ELECTRICAL_POWER);
            }
        }

        return state;
    }
}
