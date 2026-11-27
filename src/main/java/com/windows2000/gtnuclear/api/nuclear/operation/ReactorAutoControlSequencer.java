package com.windows2000.gtnuclear.api.nuclear.operation;

import com.windows2000.gtnuclear.common.ConfigHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The reactor's optional automatic startup/boron/control-rod sequencing loop (2026-10-06, extracted from
 * {@code MetaTileEntityPressurizedWaterReactor} as the first step of that class's own decomposition -- see this
 * mod's design memory). Holds only the state genuinely specific to this loop's own pacing and proportional
 * controllers; every other reactor state it reads or command it issues goes through the narrow
 * {@link AutoControlHost} interface, so this class has no dependency on the concrete reactor multiblock at all
 * and can be unit-tested against a fake host.
 * <p>
 * Deliberately does not own reactor-wide telemetry logging: {@code ReactorTelemetryLogger.logRow(...)} needs
 * dozens of getters spanning every reactor subsystem, not just auto-control state, so that stays the host's own
 * responsibility. {@link #update} instead returns the human-readable action label for the tick it actually made
 * a rod-nudging decision on (or {@code null} on every other tick, once per
 * {@code ConfigHolder.reactor.autoControlIntervalTicks}), letting the host decide whether/how to log it.
 */
public final class ReactorAutoControlSequencer {

    private static final double TICK_SECONDS = 1.0 / 20.0;

    private boolean active = false;
    private int ticksUntilNextAction = 0;
    private double elapsedSeconds = 0.0;
    /**
     * Same-tick scratch value {@link #updateAutoBoronControl}/{@link #updateStartupBoronAssist}/
     * {@link #updateAutoStartupSequence} set before the host's own boron-adjustment step next reads it via
     * {@link #getBoronAdjustmentRateFraction()} -- not persisted, matching {@link #active}'s own transient status.
     */
    private double boronAdjustmentRateFraction = 1.0;

    public void toggleActive() {
        active = !active;
    }

    /** Sets {@link #active} directly, for the host's own network-sync read path (as opposed to a toggle command). */
    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isActive() {
        return active;
    }

    /** Read by the host's own boron-adjustment step to scale its rate while the auto loop is correcting insertion. */
    public double getBoronAdjustmentRateFraction() {
        return boronAdjustmentRateFraction;
    }

    /** Elapsed in-loop seconds at the last rod-nudging decision -- the timestamp for that same telemetry row. */
    public double getElapsedSeconds() {
        return elapsedSeconds;
    }

    /**
     * Drives one tick of the whole auto-control loop against {@code host}. The startup-sequence and boron-control
     * steps run every call (each is its own no-op while {@link #active} is false or the reactor is in the wrong
     * stage); the rod-nudging decision only runs once every {@code ConfigHolder.reactor.autoControlIntervalTicks}
     * calls.
     *
     * @return the action label for this interval's rod-nudging decision, or {@code null} if this call fell
     *         within the current interval's countdown (no decision was made this tick).
     */
    @Nullable
    public String update(@NotNull AutoControlHost host) {
        updateAutoStartupSequence(host);
        updateAutoBoronControl(host);
        return updateAutoRodControl(host);
    }

    /**
     * Drives the reactor from {@code SHUTDOWN} through the same console-facing startup commands a player would
     * press by hand -- {@code requestBeginCoolantInjection}, {@code requestBeginPressurization}, then
     * {@code requestStartup} -- in that order, every tick while active. Coolant injection and pressurization run
     * unconditionally (each is already its own idle-no-op guard on the host side), but {@code requestStartup} is
     * deliberately withheld until {@link AutoControlHost#getBoronPpm()} reaches
     * {@code ConfigHolder.reactor.autoControlInitialBoronChargePpm} -- see that field's own JavaDoc for why a
     * boron-free startup leaves the rod loop with no reserve to fall back on later.
     */
    private void updateAutoStartupSequence(@NotNull AutoControlHost host) {
        if (!active) return;
        if (host.getOperatingState().stage != ReactorOperatingStage.SHUTDOWN) return;
        host.requestBeginCoolantInjection();
        host.requestBeginPressurization();

        if (host.getBoronPpm() < ConfigHolder.reactor.autoControlInitialBoronChargePpm) {
            boronAdjustmentRateFraction = 1.0;
            if (!host.isBoronConcentrationActive()) host.requestToggleBoronConcentration();
            return;
        }
        if (host.isBoronConcentrationActive()) host.requestToggleBoronConcentration();

        host.requestStartup();
    }

    /**
     * Keeps bank 0's own average rod insertion near {@code ConfigHolder.reactor.autoControlRodInsertionTargetPercent}
     * via a continuous proportional boron controller. Deliberately a no-op outside {@code OPERATING} --
     * {@code SHUTDOWN} is {@link #updateAutoStartupSequence}'s own exclusive boron territory, and
     * {@code STARTUP} defers to {@link #updateStartupBoronAssist}.
     */
    private void updateAutoBoronControl(@NotNull AutoControlHost host) {
        if (!active) return;
        if (host.getOperatingState().stage == ReactorOperatingStage.STARTUP) {
            updateStartupBoronAssist(host);
            return;
        }
        if (host.getOperatingState().stage != ReactorOperatingStage.OPERATING) return;
        double avgInsertionPercent = host.getAverageControlRodInsertionPercent();

        double signedErrorPercent = avgInsertionPercent - ConfigHolder.reactor.autoControlRodInsertionTargetPercent;
        double deadbandPercent = ConfigHolder.reactor.autoControlRodInsertionDeadbandPercent;
        double absErrorPercent = Math.abs(signedErrorPercent);

        if (absErrorPercent <= deadbandPercent) {
            boronAdjustmentRateFraction = 1.0;
            if (host.isBoronDilutionActive()) host.requestToggleBoronDilution();
            if (host.isBoronConcentrationActive()) host.requestToggleBoronConcentration();
            return;
        }

        double proportionalBandPercent = ConfigHolder.reactor.autoControlRodInsertionProportionalBandPercent;
        boronAdjustmentRateFraction = Math.min(1.0,
                (absErrorPercent - deadbandPercent) / (proportionalBandPercent - deadbandPercent));

        if (signedErrorPercent < 0) {
            if (host.isBoronConcentrationActive()) host.requestToggleBoronConcentration();
            if (periodBlocksWithdrawal(host)) {
                // Startup-rate limiter: dilution adds reactivity just like a withdrawal.
                if (host.isBoronDilutionActive()) host.requestToggleBoronDilution();
            } else if (!host.isBoronDilutionActive()) {
                host.requestToggleBoronDilution();
            }
        } else {
            if (host.isBoronDilutionActive()) host.requestToggleBoronDilution();
            if (!host.isBoronConcentrationActive()) host.requestToggleBoronConcentration();
        }
    }

    /**
     * Narrow {@code STARTUP}-only safety valve: withdraws bank 0 via boron dilution whenever rods are already
     * pinned near their 0% floor and the reactor is still subcritical -- mirrors a real PWR's own
     * approach-to-criticality procedure, which alternates rod withdrawal and boron dilution rather than relying on
     * rods alone.
     */
    private void updateStartupBoronAssist(@NotNull AutoControlHost host) {
        double avgInsertionPercent = host.getAverageControlRodInsertionPercent();
        boolean rodsNearlyExhausted = avgInsertionPercent <= ConfigHolder.reactor.startupBoronAssistRodThresholdPercent;
        boolean stillSubcritical = host.getTotalReactivity() < 0.0;

        if (rodsNearlyExhausted && stillSubcritical && !periodBlocksWithdrawal(host)) {
            boronAdjustmentRateFraction = 1.0;
            if (host.isBoronConcentrationActive()) host.requestToggleBoronConcentration();
            if (!host.isBoronDilutionActive()) host.requestToggleBoronDilution();
        } else if (host.isBoronDilutionActive()) {
            host.requestToggleBoronDilution();
        }
    }

    @Nullable
    private String updateAutoRodControl(@NotNull AutoControlHost host) {
        if (ticksUntilNextAction-- > 0) return null;
        int intervalTicks = ConfigHolder.reactor.autoControlIntervalTicks;
        // "- 1": the post-decrement check above fires once the counter has already counted down through 0, so
        // resetting it to intervalTicks made the real period intervalTicks + 1 while elapsedSeconds advanced by
        // only intervalTicks -- every reactor telemetry CSV ran ~5% slow at the default 20 (fixed 2026-11-26).
        ticksUntilNextAction = Math.max(0, intervalTicks - 1);
        elapsedSeconds += intervalTicks * TICK_SECONDS;

        if (!active) {
            return "manual";
        }
        if (host.getOperatingState().stage == ReactorOperatingStage.SHUTDOWN) {
            return "shutdown";
        }
        // Stage 1-1 (2026-11-21, see this mod's design memory): a tripped reactor's rods are dropping in and
        // latched until a manual reset -- the loop must never fight that. Before this guard, real playtest logs
        // (2026-09-24/25) showed this loop withdrawing rods after a trip, driving a power excursion and meltdown.
        if (host.getOperatingState().stage == ReactorOperatingStage.TRIPPED) {
            return "tripped";
        }
        // Stage 1-4c: rod drives are disconnected during refueling.
        if (host.getOperatingState().stage == ReactorOperatingStage.REFUELING) {
            return "refueling";
        }
        return updateAutoRodControlAscensionOrHold(host);
    }

    /**
     * Derives the rod loop's own reactivity target from bank 0's currently <i>remaining</i> withdrawal margin:
     * {@code (bank 0's own total worth / 2) * (1 - cos(pi * currentInsertionFraction))} is exactly the reactivity
     * {@code ControlRodWorth.reactivity} would still release if bank 0 withdrew the rest of the way to 0% right
     * now. Targeting {@code currentReactivityPcm + marginFraction * thatCeiling} keeps the target strictly
     * between "no further ask" and "the absolute physical maximum," so the loop naturally settles into "hold"
     * instead of spinning a permanently-unreachable withdraw command as insertion approaches 0%. Clamped to
     * {@code ConfigHolder.reactor.autoControlMaxTargetReactivityDollars} (times beta) so the uncapped formula never
     * asks for
     * anywhere near the delayed-neutron fraction beta while bank 0 still has withdrawal margin to work with.
     */
    private double computeAutoRodControlTargetPcm(@NotNull AutoControlHost host, boolean ascending,
                                                  double reactivityPcm) {
        double avgInsertionFraction = host.getBank0AverageInsertionFraction();
        double bank0WorthPcm = host.getBankTotalWorthReactivity(0) * 1.0e5;
        double remainingWithdrawalCeilingPcm = (bank0WorthPcm / 2.0) * (1.0 - Math.cos(Math.PI * avgInsertionFraction));
        double marginFraction = ascending ? ConfigHolder.reactor.autoControlAscensionMarginFraction :
                ConfigHolder.reactor.autoControlHoldMarginFraction;
        double uncappedTargetPcm = reactivityPcm + marginFraction * remainingWithdrawalCeilingPcm;
        double capPcm = ConfigHolder.reactor.autoControlMaxTargetReactivityDollars *
                host.getDelayedNeutronFraction() * 1.0e5;
        return Math.min(uncappedTargetPcm, capPcm);
    }

    /** Startup-rate limiter: power rising with a period shorter than the withdrawal limit (2026-11-28). */
    private static boolean periodBlocksWithdrawal(@NotNull AutoControlHost host) {
        double period = host.getReactorPeriodSeconds();
        return period > 0 && period < ConfigHolder.reactor.autoControlMinWithdrawalPeriodSeconds;
    }

    /** Startup-rate limiter: power rising faster than the insertion limit -- insert regardless of target. */
    private static boolean periodDemandsInsertion(@NotNull AutoControlHost host) {
        double period = host.getReactorPeriodSeconds();
        return period > 0 && period < ConfigHolder.reactor.autoControlInsertPeriodSeconds;
    }

    /**
     * The actual ascension/hold rod-nudging logic {@link #updateAutoRodControl} runs once past {@code SHUTDOWN}.
     *
     * @return the human-readable action label for this interval's telemetry row.
     */
    /**
     * Bank 0 nudge sizes (insertion fraction). Scaled down 5x on 2026-11-27 (stage 2-2, decision B): fixing beta's
     * weighting roughly halved it (prompt critical at ~650 pcm instead of ~1370), and with the current oversized
     * bank worth a single old 1% step near mid-insertion was ~240 pcm. Interim until stage 4 rebuilds rod worth
     * from CMFD absorbers.
     */
    private static final double LARGE_STEP = 0.002;
    private static final double MEDIUM_STEP = 0.0002;
    private static final double SMALL_STEP = 0.00002;

    @NotNull
    private String updateAutoRodControlAscensionOrHold(@NotNull AutoControlHost host) {
        // Startup-rate limiter first (2026-11-28): a real 2026-11-27 playtest rose five decades in ~50 s (0.64 $,
        // 2.6 s period) because the reactivity target alone cannot see how fast power is already climbing.
        if (periodDemandsInsertion(host)) {
            host.requestNudgeBank(0, LARGE_STEP);
            return "insert 0.2% (period)";
        }
        return decideAscensionOrHold(host);
    }

    /** A withdrawal nudge, unless the startup-rate limiter blocks it (checked before acting, not after). */
    @NotNull
    private static String withdraw(@NotNull AutoControlHost host, double step, @NotNull String label) {
        if (periodBlocksWithdrawal(host)) return "hold (period)";
        host.requestNudgeBank(0, -step);
        return label;
    }

    @NotNull
    private String decideAscensionOrHold(@NotNull AutoControlHost host) {
        boolean ascending = host.getRelativeNeutronPopulation() <
                ConfigHolder.reactor.autoControlHoldModePowerThreshold;
        double reactivityPcm = host.getTotalReactivity() * 1.0e5;
        double targetPcm = computeAutoRodControlTargetPcm(host, ascending, reactivityPcm);
        // Power ceiling override: subtracts a penalty proportional to how far power already sits above
        // autoControlPowerCeilingThreshold, overriding whatever the nominal target was so the loop actively
        // corrects power back down instead of continuing to chase a fixed positive cushion regardless of power
        // level -- see this mod's design memory for the real-playtest overshoot this was added to fix.
        double overPowerFraction = Math.max(0.0,
                host.getRelativeNeutronPopulation() - ConfigHolder.reactor.autoControlPowerCeilingThreshold);
        targetPcm -= overPowerFraction * ConfigHolder.reactor.autoControlPowerCeilingPenaltyPcmPerUnit;
        double errorPcm = reactivityPcm - targetPcm;
        double largeStepThresholdPcm = ConfigHolder.reactor.autoControlLargeStepThresholdPcm;

        if (ascending) {
            double deadbandPcm = ConfigHolder.reactor.autoControlDeadbandPcm;
            if (errorPcm > largeStepThresholdPcm) {
                host.requestNudgeBank(0, LARGE_STEP);
                return "insert 0.2%";
            } else if (errorPcm > deadbandPcm) {
                host.requestNudgeBank(0, MEDIUM_STEP);
                return "insert 0.02%";
            } else if (errorPcm < -largeStepThresholdPcm) {
                return withdraw(host, LARGE_STEP, "withdraw 0.2%");
            } else if (errorPcm < -deadbandPcm) {
                return withdraw(host, MEDIUM_STEP, "withdraw 0.02%");
            } else {
                return "hold";
            }
        }

        double deadbandPcm = ConfigHolder.reactor.autoControlHoldDeadbandPcm;
        double mediumStepThresholdPcm = ConfigHolder.reactor.autoControlHoldMediumStepThresholdPcm;
        if (errorPcm > largeStepThresholdPcm) {
            host.requestNudgeBank(0, LARGE_STEP);
            return "insert 0.2%";
        } else if (errorPcm > mediumStepThresholdPcm) {
            host.requestNudgeBank(0, MEDIUM_STEP);
            return "insert 0.02%";
        } else if (errorPcm > deadbandPcm) {
            host.requestNudgeBank(0, SMALL_STEP);
            return "insert 0.002%";
        } else if (errorPcm < -largeStepThresholdPcm) {
            return withdraw(host, LARGE_STEP, "withdraw 0.2%");
        } else if (errorPcm < -mediumStepThresholdPcm) {
            return withdraw(host, MEDIUM_STEP, "withdraw 0.02%");
        } else if (errorPcm < -deadbandPcm) {
            return withdraw(host, SMALL_STEP, "withdraw 0.002%");
        } else {
            return "hold";
        }
    }
}
