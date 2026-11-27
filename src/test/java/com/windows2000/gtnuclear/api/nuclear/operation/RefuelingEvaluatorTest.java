package com.windows2000.gtnuclear.api.nuclear.operation;

import com.windows2000.gtnuclear.api.nuclear.operation.RefuelingEvaluator.Condition;
import com.windows2000.gtnuclear.api.nuclear.operation.RefuelingEvaluator.HoldReason;
import com.windows2000.gtnuclear.api.nuclear.operation.RefuelingEvaluator.Inputs;
import com.windows2000.gtnuclear.api.nuclear.operation.RefuelingEvaluator.Limits;
import com.windows2000.gtnuclear.api.nuclear.operation.RefuelingEvaluator.Status;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RefuelingEvaluatorTest {

    private static final Limits LIMITS = new Limits(1.0e-3, 2000.0, 366.0, 0.102325, 115.0);

    private static Inputs ready() {
        return new Inputs(true, true, 1.0e-6, 2100.0, 330.0, 0.101325, 200.0, 1000.0, 1000.0);
    }

    @Test
    void readyPlantMayStart() {
        Map<Condition, Status> checklist = RefuelingEvaluator.checklist(ready(), LIMITS);
        assertTrue(RefuelingEvaluator.canStart(checklist));
        assertEquals(Status.SUBSTITUTED, checklist.get(Condition.SHUTDOWN_MARGIN));
        assertEquals("REFUEL: 1o 2o 3o 4o 5- 6o 7o 8o 9o", RefuelingEvaluator.summaryLine(checklist, false));
    }

    @Test
    void eachConditionIsChecked() {
        Inputs r = ready();
        assertUnmet(Condition.SHUTDOWN, new Inputs(false, true, 1e-6, 2100, 330, 0.101325, 200, 1000, 1000));
        assertUnmet(Condition.RODS_INSERTED, new Inputs(true, false, 1e-6, 2100, 330, 0.101325, 200, 1000, 1000));
        assertUnmet(Condition.SUBCRITICAL, new Inputs(true, true, 0.01, 2100, 330, 0.101325, 200, 1000, 1000));
        assertUnmet(Condition.BORON, new Inputs(true, true, 1e-6, 800, 330, 0.101325, 200, 1000, 1000));
        assertUnmet(Condition.TEMPERATURE, new Inputs(true, true, 1e-6, 2100, 400, 0.101325, 200, 1000, 1000));
        assertUnmet(Condition.PRESSURE, new Inputs(true, true, 1e-6, 2100, 330, 0.5, 200, 1000, 1000));
        assertUnmet(Condition.WAIT_TIME, new Inputs(true, true, 1e-6, 2100, 330, 0.101325, 30, 1000, 1000));
        assertUnmet(Condition.COOLANT, new Inputs(true, true, 1e-6, 2100, 330, 0.101325, 200, 900, 1000));
        assertTrue(RefuelingEvaluator.canStart(RefuelingEvaluator.checklist(r, LIMITS)));
    }

    private static void assertUnmet(Condition expected, Inputs inputs) {
        Map<Condition, Status> checklist = RefuelingEvaluator.checklist(inputs, LIMITS);
        assertEquals(expected, RefuelingEvaluator.firstUnmet(checklist, true));
        assertFalse(RefuelingEvaluator.canStart(checklist));
    }

    @Test
    void neverCriticalCoreSkipsTheWait() {
        Inputs fresh = new Inputs(true, true, 0, 2100, 330, 0.101325, Double.POSITIVE_INFINITY, 1000, 1000);
        assertEquals(Status.MET, RefuelingEvaluator.checklist(fresh, LIMITS).get(Condition.WAIT_TIME));
    }

    @Test
    void resumeIgnoresTheStageCondition() {
        Inputs refueling = new Inputs(false, true, 1e-6, 2100, 330, 0.101325, 200, 1000, 1000);
        Map<Condition, Status> checklist = RefuelingEvaluator.checklist(refueling, LIMITS);
        assertFalse(RefuelingEvaluator.canStart(checklist));
        assertTrue(RefuelingEvaluator.canResume(checklist));
        assertEquals("REFUEL: 1o 2o 3o 4o 5- 6o 7o 8o 9o", RefuelingEvaluator.summaryLine(checklist, true));
    }

    @Test
    void multiplication() {
        assertEquals(1.0, RefuelingEvaluator.multiplication(-0.3, false));
        // rho = -0.5 -> k = 2/3 -> M = 3.
        assertEquals(3.0, RefuelingEvaluator.multiplication(-0.5, true), 1e-12);
        // rho = -1 -> k = 0.5 -> M = 2.
        assertEquals(2.0, RefuelingEvaluator.multiplication(-1.0, true), 1e-12);
        assertEquals(Double.POSITIVE_INFINITY, RefuelingEvaluator.multiplication(0.0, true));
        assertEquals(Double.POSITIVE_INFINITY, RefuelingEvaluator.multiplication(0.01, true));
    }

    @Test
    void holdReasons() {
        // No population baseline (no source, no fuel): the computed multiplication stands in.
        assertNull(RefuelingEvaluator.holdReason(0, 0, 1.5, 1.0, 330, 1000, 1000, 2100, LIMITS));
        assertEquals(HoldReason.COUNT_RATE_DOUBLED,
                RefuelingEvaluator.holdReason(0, 0, 2.0, 1.0, 330, 1000, 1000, 2100, LIMITS));
        assertEquals(HoldReason.COUNT_RATE_DOUBLED, RefuelingEvaluator.holdReason(0, 0, Double.POSITIVE_INFINITY,
                1.2, 330, 1000, 1000, 2100, LIMITS));
        assertEquals(HoldReason.TEMPERATURE_HIGH,
                RefuelingEvaluator.holdReason(0, 0, 1.0, 1.0, 370, 1000, 1000, 2100, LIMITS));
        assertEquals(HoldReason.COOLANT_LOW,
                RefuelingEvaluator.holdReason(0, 0, 1.0, 1.0, 330, 999, 1000, 2100, LIMITS));
        assertEquals(HoldReason.BORON_LOW,
                RefuelingEvaluator.holdReason(0, 0, 1.0, 1.0, 330, 1000, 1000, 1999, LIMITS));
    }

    @Test
    void countRateDoublingUsesThePopulationWhenItHasABaseline() {
        // Stage 2-1: the source-driven population is the count rate; M is ignored once a baseline exists.
        assertNull(RefuelingEvaluator.holdReason(1.9e-10, 1.0e-10, 99.0, 1.0, 330, 1000, 1000, 2100, LIMITS));
        assertEquals(HoldReason.COUNT_RATE_DOUBLED,
                RefuelingEvaluator.holdReason(2.0e-10, 1.0e-10, 1.0, 1.0, 330, 1000, 1000, 2100, LIMITS));
    }

    @Test
    void operatingModes() {
        assertEquals(6, RefuelingEvaluator.operatingMode(true, -0.5, true, 0, 330));
        assertEquals(1, RefuelingEvaluator.operatingMode(false, 0.0, true, 1.0, 580));
        assertEquals(2, RefuelingEvaluator.operatingMode(false, 0.0, true, 0.01, 560));
        assertEquals(3, RefuelingEvaluator.operatingMode(false, -0.05, true, 0, 560));
        assertEquals(4, RefuelingEvaluator.operatingMode(false, -0.05, true, 0, 400));
        assertEquals(5, RefuelingEvaluator.operatingMode(false, -0.05, true, 0, 366));
        // No fuel: never critical.
        assertEquals(5, RefuelingEvaluator.operatingMode(false, 0.0, false, 0, 300));
        // k just below 0.99 is not critical: rho = (k-1)/k = -0.0101 for k = 0.99.
        assertEquals(3, RefuelingEvaluator.operatingMode(false, -0.0102, true, 0, 560));
        assertEquals(2, RefuelingEvaluator.operatingMode(false, -0.0100, true, 0, 560));
    }
}
