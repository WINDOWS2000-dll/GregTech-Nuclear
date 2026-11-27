package com.windows2000.gtnuclear.api.nuclear.operation;

import org.jetbrains.annotations.NotNull;

/**
 * The narrow set of reactor state reads and operator-command calls {@link ReactorAutoControlSequencer} needs from
 * whatever reactor multiblock it is driving (2026-10-06, extracted from
 * {@code MetaTileEntityPressurizedWaterReactor} as the first step of that class's own decomposition -- see this
 * mod's design memory). Every method here already exists as a public console-facing accessor/command on that
 * class for the control console's own benefit, so implementing this interface costs that class nothing beyond
 * declaring {@code implements AutoControlHost} plus the handful of small aggregate getters noted below.
 * <p>
 * Deliberately exposes pre-aggregated scalars ({@link #getAverageControlRodInsertionPercent()}/
 * {@link #getBank0AverageInsertionFraction()}/{@link #getBankTotalWorthReactivity(int)}) rather than a raw list of
 * per-rod summaries: the auto-control loop only ever needs these aggregates, and keeping the interface free of
 * any reactor-multiblock-specific types avoids a reverse dependency from this {@code api} package onto
 * {@code common.metatileentities}.
 */
public interface AutoControlHost {

    double getBoronPpm();

    boolean isBoronDilutionActive();

    boolean isBoronConcentrationActive();

    void requestToggleBoronDilution();

    void requestToggleBoronConcentration();

    void requestBeginCoolantInjection();

    void requestBeginPressurization();

    void requestStartup();

    @NotNull
    ReactorOperatingState getOperatingState();

    /** Average insertion depth (0-100) across every control rod in the reactor, regardless of bank. */
    double getAverageControlRodInsertionPercent();

    /** Average insertion fraction (0-1) across bank 0's own control rods only. */
    double getBank0AverageInsertionFraction();

    /** @param bankId the bank to total, e.g. 0 for the auto-control loop's own always-unassigned default bank. */
    double getBankTotalWorthReactivity(int bankId);

    double getTotalReactivity();

    double getRelativeNeutronPopulation();

    void requestNudgeBank(int bankId, double deltaFraction);

    /**
     * Measured reactor period (s): positive while power rises, infinite when steady (stage 2-2 follow-up,
     * 2026-11-28) -- the auto control's startup-rate limiter reads it like an operator reads a period meter.
     */
    double getReactorPeriodSeconds();

    /** Current total delayed neutron fraction beta (0 without fuel), for limits expressed in dollars. */
    double getDelayedNeutronFraction();
}
