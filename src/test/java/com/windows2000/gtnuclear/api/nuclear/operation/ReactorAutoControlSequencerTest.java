package com.windows2000.gtnuclear.api.nuclear.operation;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link ReactorAutoControlSequencer} has no dependency on the concrete reactor multiblock -- every reactor
 * state read or command call goes through {@link AutoControlHost} -- so it is exercised here against
 * {@link FakeAutoControlHost} instead of a real {@code MetaTileEntityPressurizedWaterReactor} (which cannot be
 * constructed outside a running Minecraft/GT environment). Expected numbers throughout are derived from this
 * mod's own default {@code ConfigHolder.reactor} values (not hardcoded independently of them), so a future
 * balance-tuning change to those defaults is expected to require updating these tests too.
 */
class ReactorAutoControlSequencerTest {

    @Test
    void togglingActiveFlipsIsActive() {
        ReactorAutoControlSequencer sequencer = new ReactorAutoControlSequencer();
        assertFalse(sequencer.isActive());
        sequencer.toggleActive();
        assertTrue(sequencer.isActive());
        sequencer.toggleActive();
        assertFalse(sequencer.isActive());
    }

    @Test
    void setActiveAssignsDirectlyForNetworkSyncReadPath() {
        ReactorAutoControlSequencer sequencer = new ReactorAutoControlSequencer();
        sequencer.setActive(true);
        assertTrue(sequencer.isActive());
        sequencer.setActive(false);
        assertFalse(sequencer.isActive());
    }

    @Test
    void firstUpdateFiresImmediatelyThenCountsDownFullInterval() {
        ReactorAutoControlSequencer sequencer = new ReactorAutoControlSequencer();
        FakeAutoControlHost host = new FakeAutoControlHost();
        host.operatingState = new ReactorOperatingState(ReactorOperatingStage.OPERATING, null);

        // A 20-tick interval means the next action fires 20 ticks after the first (19 silent ticks in between) --
        // the pre-2026-11-26 code (and this test) used 21, which made every telemetry CSV run ~5% slow.
        assertEquals("manual", sequencer.update(host));
        for (int i = 0; i < 19; i++) {
            assertNull(sequencer.update(host), "tick " + i + " should still be within the same interval");
        }
        assertEquals("manual", sequencer.update(host));
    }

    @Test
    void inactiveDoesNothingDuringShutdown() {
        ReactorAutoControlSequencer sequencer = new ReactorAutoControlSequencer();
        FakeAutoControlHost host = new FakeAutoControlHost();
        host.operatingState = new ReactorOperatingState(ReactorOperatingStage.SHUTDOWN, null);

        sequencer.update(host);

        assertFalse(host.coolantInjectionRequested);
        assertFalse(host.pressurizationRequested);
        assertFalse(host.startupRequested);
        assertFalse(host.boronConcentrationActive);
    }

    @Test
    void activeDuringShutdownBelowBoronThresholdRequestsInjectionPressurizationAndConcentratesButNotStartup() {
        ReactorAutoControlSequencer sequencer = new ReactorAutoControlSequencer();
        sequencer.toggleActive();
        FakeAutoControlHost host = new FakeAutoControlHost();
        host.operatingState = new ReactorOperatingState(ReactorOperatingStage.SHUTDOWN, null);
        host.boronPpm = 500.0;

        String action = sequencer.update(host);

        assertTrue(host.coolantInjectionRequested);
        assertTrue(host.pressurizationRequested);
        assertTrue(host.boronConcentrationActive);
        assertFalse(host.startupRequested);
        assertEquals("shutdown", action);
    }

    @Test
    void activeDuringShutdownAtBoronThresholdStopsConcentratingAndRequestsStartup() {
        ReactorAutoControlSequencer sequencer = new ReactorAutoControlSequencer();
        sequencer.toggleActive();
        FakeAutoControlHost host = new FakeAutoControlHost();
        host.operatingState = new ReactorOperatingState(ReactorOperatingStage.SHUTDOWN, null);
        host.boronPpm = 800.0;
        host.boronConcentrationActive = true;

        sequencer.update(host);

        assertFalse(host.boronConcentrationActive);
        assertTrue(host.startupRequested);
    }

    @Test
    void activeOutsideShutdownDoesNothingInStartupSequence() {
        ReactorAutoControlSequencer sequencer = new ReactorAutoControlSequencer();
        sequencer.toggleActive();
        FakeAutoControlHost host = new FakeAutoControlHost();
        host.operatingState = new ReactorOperatingState(ReactorOperatingStage.OPERATING, null);
        host.averageControlRodInsertionPercent = 50.0;

        sequencer.update(host);

        assertFalse(host.coolantInjectionRequested);
        assertFalse(host.pressurizationRequested);
        assertFalse(host.startupRequested);
    }

    @Test
    void operatingWithinDeadbandClearsBoronAndResetsRateToFull() {
        ReactorAutoControlSequencer sequencer = new ReactorAutoControlSequencer();
        sequencer.toggleActive();
        FakeAutoControlHost host = new FakeAutoControlHost();
        host.operatingState = new ReactorOperatingState(ReactorOperatingStage.OPERATING, null);
        host.averageControlRodInsertionPercent = 55.0;
        host.boronDilutionActive = true;

        sequencer.update(host);

        assertFalse(host.boronDilutionActive);
        assertFalse(host.boronConcentrationActive);
        assertEquals(1.0, sequencer.getBoronAdjustmentRateFraction(), 1e-9);
    }

    @Test
    void operatingAboveTargetPlusDeadbandConcentratesWithProportionalRate() {
        ReactorAutoControlSequencer sequencer = new ReactorAutoControlSequencer();
        sequencer.toggleActive();
        FakeAutoControlHost host = new FakeAutoControlHost();
        host.operatingState = new ReactorOperatingState(ReactorOperatingStage.OPERATING, null);
        host.averageControlRodInsertionPercent = 70.0;

        sequencer.update(host);

        assertTrue(host.boronConcentrationActive);
        assertFalse(host.boronDilutionActive);
        assertEquals(10.0 / 15.0, sequencer.getBoronAdjustmentRateFraction(), 1e-9);
    }

    @Test
    void operatingBelowTargetMinusDeadbandDilutesWithProportionalRate() {
        ReactorAutoControlSequencer sequencer = new ReactorAutoControlSequencer();
        sequencer.toggleActive();
        FakeAutoControlHost host = new FakeAutoControlHost();
        host.operatingState = new ReactorOperatingState(ReactorOperatingStage.OPERATING, null);
        host.averageControlRodInsertionPercent = 30.0;

        sequencer.update(host);

        assertTrue(host.boronDilutionActive);
        assertFalse(host.boronConcentrationActive);
        assertEquals(10.0 / 15.0, sequencer.getBoronAdjustmentRateFraction(), 1e-9);
    }

    @Test
    void inactiveDoesNothingInBoronControl() {
        ReactorAutoControlSequencer sequencer = new ReactorAutoControlSequencer();
        FakeAutoControlHost host = new FakeAutoControlHost();
        host.operatingState = new ReactorOperatingState(ReactorOperatingStage.OPERATING, null);
        host.averageControlRodInsertionPercent = 90.0;

        sequencer.update(host);

        assertFalse(host.boronDilutionActive);
        assertFalse(host.boronConcentrationActive);
    }

    @Test
    void startupStageDelegatesToBoronAssistWhenRodsExhaustedAndSubcritical() {
        ReactorAutoControlSequencer sequencer = new ReactorAutoControlSequencer();
        sequencer.toggleActive();
        FakeAutoControlHost host = new FakeAutoControlHost();
        host.operatingState = new ReactorOperatingState(ReactorOperatingStage.STARTUP, null);
        host.averageControlRodInsertionPercent = 10.0;
        host.totalReactivity = -0.01;

        sequencer.update(host);

        assertTrue(host.boronDilutionActive);
        assertFalse(host.boronConcentrationActive);
        assertEquals(1.0, sequencer.getBoronAdjustmentRateFraction(), 1e-9);
    }

    @Test
    void startupStageStopsDilutionWhenNoLongerNeeded() {
        ReactorAutoControlSequencer sequencer = new ReactorAutoControlSequencer();
        sequencer.toggleActive();
        FakeAutoControlHost host = new FakeAutoControlHost();
        host.operatingState = new ReactorOperatingState(ReactorOperatingStage.STARTUP, null);
        host.averageControlRodInsertionPercent = 50.0;
        host.totalReactivity = 0.01;
        host.boronDilutionActive = true;

        sequencer.update(host);

        assertFalse(host.boronDilutionActive);
    }

    private static FakeAutoControlHost rodControlHost(double relativeNeutronPopulation, double totalReactivity,
                                                      double bankTotalWorthReactivity,
                                                      double bank0AverageInsertionFraction) {
        FakeAutoControlHost host = new FakeAutoControlHost();
        host.operatingState = new ReactorOperatingState(ReactorOperatingStage.OPERATING, null);
        host.averageControlRodInsertionPercent = 50.0;
        host.relativeNeutronPopulation = relativeNeutronPopulation;
        host.totalReactivity = totalReactivity;
        host.bankTotalWorthReactivity = bankTotalWorthReactivity;
        host.bank0AverageInsertionFraction = bank0AverageInsertionFraction;
        return host;
    }

    @Test
    void trippedReactorIsNeverNudgedEvenWithAStrongWithdrawSignal() {
        ReactorAutoControlSequencer sequencer = new ReactorAutoControlSequencer();
        sequencer.toggleActive();
        // Same host state that drives "withdraw 0.2%" while OPERATING (see the ascending tests below), but tripped.
        FakeAutoControlHost host = rodControlHost(0.5, -0.009, 0.0, 0.0);
        host.operatingState = new ReactorOperatingState(ReactorOperatingStage.TRIPPED, ScramReason.MANUAL);

        assertEquals("tripped", sequencer.update(host));
        assertTrue(host.nudges.isEmpty());
    }

    @Test
    void ascendingLargePositiveErrorInsertsOnePercent() {
        ReactorAutoControlSequencer sequencer = new ReactorAutoControlSequencer();
        sequencer.toggleActive();
        FakeAutoControlHost host = rodControlHost(0.5, 0.009, 0.0, 0.0);

        assertEquals("insert 0.2%", sequencer.update(host));
        assertEquals(1, host.nudges.size());
        assertEquals(0, host.nudges.get(0).bankId);
        assertEquals(0.002, host.nudges.get(0).deltaFraction, 1e-9);
    }

    @Test
    void ascendingModerateErrorInsertsPointOnePercent() {
        ReactorAutoControlSequencer sequencer = new ReactorAutoControlSequencer();
        sequencer.toggleActive();
        FakeAutoControlHost host = rodControlHost(0.5, 0.005, 0.0, 0.0);

        assertEquals("insert 0.02%", sequencer.update(host));
        assertEquals(0.0002, host.nudges.get(0).deltaFraction, 1e-9);
    }

    @Test
    void ascendingWithinDeadbandHolds() {
        ReactorAutoControlSequencer sequencer = new ReactorAutoControlSequencer();
        sequencer.toggleActive();
        FakeAutoControlHost host = rodControlHost(0.5, 0.003, 0.0, 0.0);

        assertEquals("hold", sequencer.update(host));
        assertTrue(host.nudges.isEmpty());
    }

    @Test
    void ascendingModerateNegativeErrorWithdrawsPointOnePercent() {
        ReactorAutoControlSequencer sequencer = new ReactorAutoControlSequencer();
        sequencer.toggleActive();
        FakeAutoControlHost host = rodControlHost(0.5, -0.004, 0.004, 1.0);

        assertEquals("withdraw 0.02%", sequencer.update(host));
        assertEquals(-0.0002, host.nudges.get(0).deltaFraction, 1e-9);
    }

    @Test
    void ascendingLargeNegativeErrorWithdrawsOnePercent() {
        ReactorAutoControlSequencer sequencer = new ReactorAutoControlSequencer();
        sequencer.toggleActive();
        FakeAutoControlHost host = rodControlHost(0.5, -0.004, 0.012, 1.0);

        assertEquals("withdraw 0.2%", sequencer.update(host));
        assertEquals(-0.002, host.nudges.get(0).deltaFraction, 1e-9);
    }

    @Test
    void holdModeLargePositiveErrorInsertsOnePercent() {
        ReactorAutoControlSequencer sequencer = new ReactorAutoControlSequencer();
        sequencer.toggleActive();
        FakeAutoControlHost host = rodControlHost(0.95, 0.009, 0.0, 0.0);

        assertEquals("insert 0.2%", sequencer.update(host));
        assertEquals(0.002, host.nudges.get(0).deltaFraction, 1e-9);
    }

    @Test
    void holdModeMediumErrorInsertsPointOnePercent() {
        ReactorAutoControlSequencer sequencer = new ReactorAutoControlSequencer();
        sequencer.toggleActive();
        FakeAutoControlHost host = rodControlHost(0.95, 0.005, 0.0, 0.0);

        assertEquals("insert 0.02%", sequencer.update(host));
        assertEquals(0.0002, host.nudges.get(0).deltaFraction, 1e-9);
    }

    @Test
    void holdModeSmallPositiveErrorInsertsPointZeroOnePercent() {
        ReactorAutoControlSequencer sequencer = new ReactorAutoControlSequencer();
        sequencer.toggleActive();
        FakeAutoControlHost host = rodControlHost(0.95, 0.0031, 0.0, 0.0);

        assertEquals("insert 0.002%", sequencer.update(host));
        assertEquals(0.00002, host.nudges.get(0).deltaFraction, 1e-9);
    }

    @Test
    void holdModeWithinDeadbandHolds() {
        ReactorAutoControlSequencer sequencer = new ReactorAutoControlSequencer();
        sequencer.toggleActive();
        FakeAutoControlHost host = rodControlHost(0.95, 0.003, 0.0, 0.0);

        assertEquals("hold", sequencer.update(host));
        assertTrue(host.nudges.isEmpty());
    }

    @Test
    void holdModeSmallNegativeErrorWithdrawsPointZeroOnePercent() {
        ReactorAutoControlSequencer sequencer = new ReactorAutoControlSequencer();
        sequencer.toggleActive();
        FakeAutoControlHost host = rodControlHost(0.95, 0.0, 0.001, 1.0);

        assertEquals("withdraw 0.002%", sequencer.update(host));
        assertEquals(-0.00002, host.nudges.get(0).deltaFraction, 1e-9);
    }

    @Test
    void holdModeMediumNegativeErrorWithdrawsPointOnePercent() {
        ReactorAutoControlSequencer sequencer = new ReactorAutoControlSequencer();
        sequencer.toggleActive();
        FakeAutoControlHost host = rodControlHost(0.95, 0.0, 0.01, 1.0);

        assertEquals("withdraw 0.02%", sequencer.update(host));
        assertEquals(-0.0002, host.nudges.get(0).deltaFraction, 1e-9);
    }

    @Test
    void overPowerPenaltyShiftsHoldModeTargetFromHoldToInsert() {
        ReactorAutoControlSequencer sequencer = new ReactorAutoControlSequencer();
        sequencer.toggleActive();
        // Without the power-ceiling override this would be a plain error=0 "hold" (bank0Worth=0, reactivityPcm=0).
        FakeAutoControlHost host = rodControlHost(1.05, 0.0, 0.0, 0.0);

        assertEquals("insert 0.02%", sequencer.update(host));
        assertEquals(0.0002, host.nudges.get(0).deltaFraction, 1e-9);
    }

    private static final class NudgeCall {

        final int bankId;
        final double deltaFraction;

        NudgeCall(int bankId, double deltaFraction) {
            this.bankId = bankId;
            this.deltaFraction = deltaFraction;
        }
    }

    /** Minimal in-memory {@link AutoControlHost}: mirrors the real reactor's own toggle/mutual-exclusion semantics. */
    private static final class FakeAutoControlHost implements AutoControlHost {

        double boronPpm = 0.0;
        boolean boronDilutionActive = false;
        boolean boronConcentrationActive = false;
        ReactorOperatingState operatingState = ReactorOperatingState.initial();
        double averageControlRodInsertionPercent = 100.0;
        double bank0AverageInsertionFraction = 1.0;
        double bankTotalWorthReactivity = 0.0;
        double totalReactivity = 0.0;
        double relativeNeutronPopulation = 0.0;
        double reactorPeriodSeconds = Double.POSITIVE_INFINITY;
        // 0.02 keeps the 0.15 $ target cap at the 300 pcm the pre-2026-11-28 tests below were written against.
        double delayedNeutronFraction = 0.02;

        @Override
        public double getReactorPeriodSeconds() {
            return reactorPeriodSeconds;
        }

        @Override
        public double getDelayedNeutronFraction() {
            return delayedNeutronFraction;
        }

        boolean coolantInjectionRequested = false;
        boolean pressurizationRequested = false;
        boolean startupRequested = false;
        final List<NudgeCall> nudges = new ArrayList<>();

        @Override
        public double getBoronPpm() {
            return boronPpm;
        }

        @Override
        public boolean isBoronDilutionActive() {
            return boronDilutionActive;
        }

        @Override
        public boolean isBoronConcentrationActive() {
            return boronConcentrationActive;
        }

        @Override
        public void requestToggleBoronDilution() {
            boronDilutionActive = !boronDilutionActive;
            if (boronDilutionActive) boronConcentrationActive = false;
        }

        @Override
        public void requestToggleBoronConcentration() {
            boronConcentrationActive = !boronConcentrationActive;
            if (boronConcentrationActive) boronDilutionActive = false;
        }

        @Override
        public void requestBeginCoolantInjection() {
            coolantInjectionRequested = true;
        }

        @Override
        public void requestBeginPressurization() {
            pressurizationRequested = true;
        }

        @Override
        public void requestStartup() {
            startupRequested = true;
        }

        @Override
        @NotNull
        public ReactorOperatingState getOperatingState() {
            return operatingState;
        }

        @Override
        public double getAverageControlRodInsertionPercent() {
            return averageControlRodInsertionPercent;
        }

        @Override
        public double getBank0AverageInsertionFraction() {
            return bank0AverageInsertionFraction;
        }

        @Override
        public double getBankTotalWorthReactivity(int bankId) {
            return bankTotalWorthReactivity;
        }

        @Override
        public double getTotalReactivity() {
            return totalReactivity;
        }

        @Override
        public double getRelativeNeutronPopulation() {
            return relativeNeutronPopulation;
        }

        @Override
        public void requestNudgeBank(int bankId, double deltaFraction) {
            nudges.add(new NudgeCall(bankId, deltaFraction));
        }
    }

    @Test
    void shortPeriodForcesInsertionRegardlessOfTarget() {
        ReactorAutoControlSequencer sequencer = new ReactorAutoControlSequencer();
        sequencer.toggleActive();
        // Same state that drives "withdraw 0.2%" in withdrawWhenWellBelowTarget-style tests, but power is already
        // climbing with a 10 s period.
        FakeAutoControlHost host = rodControlHost(0.5, -0.004, 0.012, 1.0);
        host.reactorPeriodSeconds = 10.0;
        assertEquals("insert 0.2% (period)", sequencer.update(host));
        assertEquals(0.002, host.nudges.get(0).deltaFraction, 1e-9);
    }

    @Test
    void moderatePeriodBlocksWithdrawalOnly() {
        ReactorAutoControlSequencer sequencer = new ReactorAutoControlSequencer();
        sequencer.toggleActive();
        FakeAutoControlHost host = rodControlHost(0.5, -0.004, 0.012, 1.0);
        host.reactorPeriodSeconds = 40.0;
        assertEquals("hold (period)", sequencer.update(host));
        assertTrue(host.nudges.isEmpty());

        // A falling power (negative period) never blocks anything.
        ReactorAutoControlSequencer other = new ReactorAutoControlSequencer();
        other.toggleActive();
        FakeAutoControlHost falling = rodControlHost(0.5, -0.004, 0.012, 1.0);
        falling.reactorPeriodSeconds = -40.0;
        assertEquals("withdraw 0.2%", other.update(falling));
    }

    @Test
    void targetCapIsInDollars() {
        ReactorAutoControlSequencer sequencer = new ReactorAutoControlSequencer();
        sequencer.toggleActive();
        // beta 0.0065 -> cap 0.15 $ = 97.5 pcm; at +150 pcm the loop must insert even though the uncapped margin
        // target (a fully inserted, large bank) would sit far higher.
        FakeAutoControlHost host = rodControlHost(0.5, 0.0015, 0.5, 1.0);
        host.delayedNeutronFraction = 0.0065;
        String action = sequencer.update(host);
        assertTrue(action.startsWith("insert"), action);
    }
}
