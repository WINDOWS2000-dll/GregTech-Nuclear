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

        assertEquals("manual", sequencer.update(host));
        for (int i = 0; i < 20; i++) {
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
        // Same host state that drives "withdraw 1%" while OPERATING (see the ascending tests below), but tripped.
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

        assertEquals("insert 1%", sequencer.update(host));
        assertEquals(1, host.nudges.size());
        assertEquals(0, host.nudges.get(0).bankId);
        assertEquals(0.01, host.nudges.get(0).deltaFraction, 1e-9);
    }

    @Test
    void ascendingModerateErrorInsertsPointOnePercent() {
        ReactorAutoControlSequencer sequencer = new ReactorAutoControlSequencer();
        sequencer.toggleActive();
        FakeAutoControlHost host = rodControlHost(0.5, 0.005, 0.0, 0.0);

        assertEquals("insert 0.1%", sequencer.update(host));
        assertEquals(0.001, host.nudges.get(0).deltaFraction, 1e-9);
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

        assertEquals("withdraw 0.1%", sequencer.update(host));
        assertEquals(-0.001, host.nudges.get(0).deltaFraction, 1e-9);
    }

    @Test
    void ascendingLargeNegativeErrorWithdrawsOnePercent() {
        ReactorAutoControlSequencer sequencer = new ReactorAutoControlSequencer();
        sequencer.toggleActive();
        FakeAutoControlHost host = rodControlHost(0.5, -0.004, 0.012, 1.0);

        assertEquals("withdraw 1%", sequencer.update(host));
        assertEquals(-0.01, host.nudges.get(0).deltaFraction, 1e-9);
    }

    @Test
    void holdModeLargePositiveErrorInsertsOnePercent() {
        ReactorAutoControlSequencer sequencer = new ReactorAutoControlSequencer();
        sequencer.toggleActive();
        FakeAutoControlHost host = rodControlHost(0.95, 0.009, 0.0, 0.0);

        assertEquals("insert 1%", sequencer.update(host));
        assertEquals(0.01, host.nudges.get(0).deltaFraction, 1e-9);
    }

    @Test
    void holdModeMediumErrorInsertsPointOnePercent() {
        ReactorAutoControlSequencer sequencer = new ReactorAutoControlSequencer();
        sequencer.toggleActive();
        FakeAutoControlHost host = rodControlHost(0.95, 0.005, 0.0, 0.0);

        assertEquals("insert 0.1%", sequencer.update(host));
        assertEquals(0.001, host.nudges.get(0).deltaFraction, 1e-9);
    }

    @Test
    void holdModeSmallPositiveErrorInsertsPointZeroOnePercent() {
        ReactorAutoControlSequencer sequencer = new ReactorAutoControlSequencer();
        sequencer.toggleActive();
        FakeAutoControlHost host = rodControlHost(0.95, 0.0031, 0.0, 0.0);

        assertEquals("insert 0.01%", sequencer.update(host));
        assertEquals(0.0001, host.nudges.get(0).deltaFraction, 1e-9);
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

        assertEquals("withdraw 0.01%", sequencer.update(host));
        assertEquals(-0.0001, host.nudges.get(0).deltaFraction, 1e-9);
    }

    @Test
    void holdModeMediumNegativeErrorWithdrawsPointOnePercent() {
        ReactorAutoControlSequencer sequencer = new ReactorAutoControlSequencer();
        sequencer.toggleActive();
        FakeAutoControlHost host = rodControlHost(0.95, 0.0, 0.01, 1.0);

        assertEquals("withdraw 0.1%", sequencer.update(host));
        assertEquals(-0.001, host.nudges.get(0).deltaFraction, 1e-9);
    }

    @Test
    void overPowerPenaltyShiftsHoldModeTargetFromHoldToInsert() {
        ReactorAutoControlSequencer sequencer = new ReactorAutoControlSequencer();
        sequencer.toggleActive();
        // Without the power-ceiling override this would be a plain error=0 "hold" (bank0Worth=0, reactivityPcm=0).
        FakeAutoControlHost host = rodControlHost(1.05, 0.0, 0.0, 0.0);

        assertEquals("insert 0.1%", sequencer.update(host));
        assertEquals(0.001, host.nudges.get(0).deltaFraction, 1e-9);
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
}
