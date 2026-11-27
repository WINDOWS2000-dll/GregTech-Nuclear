package com.windows2000.gtnuclear.api.nuclear.operation;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

class ReactorOperatingEvaluatorTest {

    @Test
    void initialStateIsShutdownWithNoScramReason() {
        ReactorOperatingState state = ReactorOperatingState.initial();
        assertThat(state.stage, is(ReactorOperatingStage.SHUTDOWN));
        assertThat(state.lastScramReason, nullValue());
    }

    @Test
    void beginStartupMovesFromShutdownToStartup() {
        ReactorOperatingState next = ReactorOperatingEvaluator.beginStartup(ReactorOperatingState.initial());
        assertThat(next.stage, is(ReactorOperatingStage.STARTUP));
    }

    @Test
    void beginStartupIsNoOpWhenNotShutDown() {
        ReactorOperatingState operating = new ReactorOperatingState(ReactorOperatingStage.OPERATING, null);
        ReactorOperatingState next = ReactorOperatingEvaluator.beginStartup(operating);
        assertThat(next.stage, is(ReactorOperatingStage.OPERATING));
    }

    @Test
    void criticalityAchievedMovesFromStartupToOperating() {
        ReactorOperatingState startup = new ReactorOperatingState(ReactorOperatingStage.STARTUP, null);
        ReactorOperatingState next = ReactorOperatingEvaluator.criticalityAchieved(startup);
        assertThat(next.stage, is(ReactorOperatingStage.OPERATING));
    }

    @Test
    void criticalityAchievedIsNoOpWhenNotInStartup() {
        ReactorOperatingState next = ReactorOperatingEvaluator.criticalityAchieved(ReactorOperatingState.initial());
        assertThat(next.stage, is(ReactorOperatingStage.SHUTDOWN));
    }

    @Test
    void beginShutdownMovesFromOperatingToShuttingDown() {
        ReactorOperatingState operating = new ReactorOperatingState(ReactorOperatingStage.OPERATING, null);
        ReactorOperatingState next = ReactorOperatingEvaluator.beginShutdown(operating);
        assertThat(next.stage, is(ReactorOperatingStage.SHUTTING_DOWN));
    }

    @Test
    void beginShutdownIsNoOpWhenNotOperating() {
        ReactorOperatingState next = ReactorOperatingEvaluator.beginShutdown(ReactorOperatingState.initial());
        assertThat(next.stage, is(ReactorOperatingStage.SHUTDOWN));
    }

    @Test
    void shutdownCompleteMovesFromShuttingDownToShutdown() {
        ReactorOperatingState shuttingDown = new ReactorOperatingState(ReactorOperatingStage.SHUTTING_DOWN, null);
        ReactorOperatingState next = ReactorOperatingEvaluator.shutdownComplete(shuttingDown);
        assertThat(next.stage, is(ReactorOperatingStage.SHUTDOWN));
    }

    @Test
    void shutdownCompleteIsNoOpWhenNotShuttingDown() {
        ReactorOperatingState operating = new ReactorOperatingState(ReactorOperatingStage.OPERATING, null);
        ReactorOperatingState next = ReactorOperatingEvaluator.shutdownComplete(operating);
        assertThat(next.stage, is(ReactorOperatingStage.OPERATING));
    }

    @Test
    void scramTripsFromStartup() {
        ReactorOperatingState startup = new ReactorOperatingState(ReactorOperatingStage.STARTUP, null);
        ReactorOperatingState next = ReactorOperatingEvaluator.scram(startup, ScramReason.COOLANT_LOSS);
        assertThat(next.stage, is(ReactorOperatingStage.TRIPPED));
        assertThat(next.lastScramReason, is(ScramReason.COOLANT_LOSS));
    }

    @Test
    void scramTripsFromOperating() {
        ReactorOperatingState operating = new ReactorOperatingState(ReactorOperatingStage.OPERATING, null);
        ReactorOperatingState next = ReactorOperatingEvaluator.scram(operating, ScramReason.TEMPERATURE_LIMIT);
        assertThat(next.stage, is(ReactorOperatingStage.TRIPPED));
        assertThat(next.lastScramReason, is(ScramReason.TEMPERATURE_LIMIT));
    }

    @Test
    void scramTripsFromShuttingDown() {
        ReactorOperatingState shuttingDown = new ReactorOperatingState(ReactorOperatingStage.SHUTTING_DOWN, null);
        ReactorOperatingState next = ReactorOperatingEvaluator.scram(shuttingDown, ScramReason.MANUAL);
        assertThat(next.stage, is(ReactorOperatingStage.TRIPPED));
    }

    @Test
    void scramIsNoOpWhenAlreadyShutDown() {
        ReactorOperatingState next = ReactorOperatingEvaluator.scram(ReactorOperatingState.initial(),
                ScramReason.MANUAL);
        assertThat(next.stage, is(ReactorOperatingStage.SHUTDOWN));
        assertThat(next.lastScramReason, nullValue());
    }

    @Test
    void scramDoesNotOverwriteTheOriginalReasonWhenAlreadyTripped() {
        ReactorOperatingState tripped = new ReactorOperatingState(ReactorOperatingStage.TRIPPED,
                ScramReason.COOLANT_LOSS);
        ReactorOperatingState next = ReactorOperatingEvaluator.scram(tripped, ScramReason.MANUAL);
        assertThat(next.lastScramReason, is(ScramReason.COOLANT_LOSS));
    }

    @Test
    void manualResetMovesFromTrippedToShutdownAndClearsReason() {
        ReactorOperatingState tripped = new ReactorOperatingState(ReactorOperatingStage.TRIPPED,
                ScramReason.REDSTONE_SIGNAL);
        ReactorOperatingState next = ReactorOperatingEvaluator.manualReset(tripped);
        assertThat(next.stage, is(ReactorOperatingStage.SHUTDOWN));
        assertThat(next.lastScramReason, nullValue());
    }

    @Test
    void manualResetIsNoOpWhenNotTripped() {
        ReactorOperatingState operating = new ReactorOperatingState(ReactorOperatingStage.OPERATING, null);
        ReactorOperatingState next = ReactorOperatingEvaluator.manualReset(operating);
        assertThat(next.stage, is(ReactorOperatingStage.OPERATING));
    }

    @Test
    void fullStartupToShutdownCycle() {
        ReactorOperatingState state = ReactorOperatingState.initial();
        state = ReactorOperatingEvaluator.beginStartup(state);
        assertThat(state.stage, is(ReactorOperatingStage.STARTUP));
        state = ReactorOperatingEvaluator.criticalityAchieved(state);
        assertThat(state.stage, is(ReactorOperatingStage.OPERATING));
        state = ReactorOperatingEvaluator.beginShutdown(state);
        assertThat(state.stage, is(ReactorOperatingStage.SHUTTING_DOWN));
        state = ReactorOperatingEvaluator.shutdownComplete(state);
        assertThat(state.stage, is(ReactorOperatingStage.SHUTDOWN));
    }

    @Test
    void fullTripAndResetCycle() {
        ReactorOperatingState state = ReactorOperatingState.initial();
        state = ReactorOperatingEvaluator.beginStartup(state);
        state = ReactorOperatingEvaluator.criticalityAchieved(state);
        state = ReactorOperatingEvaluator.scram(state, ScramReason.PRESSURE_LIMIT);
        assertThat(state.stage, is(ReactorOperatingStage.TRIPPED));
        state = ReactorOperatingEvaluator.manualReset(state);
        assertThat(state.stage, is(ReactorOperatingStage.SHUTDOWN));
        assertThat(state.lastScramReason, nullValue());
    }

    @Test
    void refuelingOnlyFromShutdownAndBackToShutdown() {
        ReactorOperatingState refueling = ReactorOperatingEvaluator.beginRefueling(ReactorOperatingState.initial());
        assertThat(refueling.stage, is(ReactorOperatingStage.REFUELING));
        assertThat(ReactorOperatingEvaluator.endRefueling(refueling).stage, is(ReactorOperatingStage.SHUTDOWN));

        ReactorOperatingState tripped = new ReactorOperatingState(ReactorOperatingStage.TRIPPED, ScramReason.MANUAL);
        assertThat(ReactorOperatingEvaluator.beginRefueling(tripped).stage, is(ReactorOperatingStage.TRIPPED));
        ReactorOperatingState operating = new ReactorOperatingState(ReactorOperatingStage.OPERATING, null);
        assertThat(ReactorOperatingEvaluator.beginRefueling(operating).stage, is(ReactorOperatingStage.OPERATING));
        assertThat(ReactorOperatingEvaluator.endRefueling(operating).stage, is(ReactorOperatingStage.OPERATING));
    }

    @Test
    void refuelingCannotBeTrippedOrStarted() {
        ReactorOperatingState refueling = new ReactorOperatingState(ReactorOperatingStage.REFUELING, null);
        assertThat(ReactorOperatingEvaluator.scram(refueling, ScramReason.MANUAL).stage,
                is(ReactorOperatingStage.REFUELING));
        assertThat(ReactorOperatingEvaluator.beginStartup(refueling).stage, is(ReactorOperatingStage.REFUELING));
    }
}
