package com.windows2000.gtnuclear.api.nuclear.operation;

import com.windows2000.gtnuclear.api.nuclear.operation.ReactorStartupRequirements.Blocker;
import com.windows2000.gtnuclear.api.nuclear.operation.ReactorStartupRequirements.Inputs;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ReactorStartupRequirementsTest {

    private static Inputs inputs(boolean housing, boolean tooLong, int fuel, int rods, boolean rcp, double coolant,
                                 double pressure, double temperature) {
        return new Inputs(housing, tooLong, fuel, rods, rcp, coolant, 950.0, pressure, 15.0, temperature, 550.0);
    }

    private static Inputs ready() {
        return inputs(false, false, 100, 8, true, 1000.0, 15.5, 560.0);
    }

    @Test
    void readyPlantHasNoBlocker() {
        assertNull(ReactorStartupRequirements.firstBlocker(ready()));
    }

    @Test
    void eachConditionBlocksOnItsOwn() {
        assertEquals(Blocker.HOUSING_MISMATCH,
                ReactorStartupRequirements.firstBlocker(inputs(true, false, 100, 8, true, 1000, 15.5, 560)));
        assertEquals(Blocker.ASSEMBLY_TOO_LONG,
                ReactorStartupRequirements.firstBlocker(inputs(false, true, 100, 8, true, 1000, 15.5, 560)));
        assertEquals(Blocker.NO_FUEL,
                ReactorStartupRequirements.firstBlocker(inputs(false, false, 0, 8, true, 1000, 15.5, 560)));
        assertEquals(Blocker.INSUFFICIENT_CONTROL_RODS, ReactorStartupRequirements.firstBlocker(inputs(false,
                false, 100, ReactorStartupRequirements.MIN_CONTROL_ROD_CLUSTERS - 1, true, 1000, 15.5, 560)));
        assertEquals(Blocker.RCP_NOT_RUNNING,
                ReactorStartupRequirements.firstBlocker(inputs(false, false, 100, 8, false, 1000, 15.5, 560)));
        assertEquals(Blocker.COOLANT_NOT_FILLED,
                ReactorStartupRequirements.firstBlocker(inputs(false, false, 100, 8, true, 900, 15.5, 560)));
        assertEquals(Blocker.NOT_PRESSURIZED,
                ReactorStartupRequirements.firstBlocker(inputs(false, false, 100, 8, true, 1000, 10, 560)));
        assertEquals(Blocker.NOT_HEATED_UP,
                ReactorStartupRequirements.firstBlocker(inputs(false, false, 100, 8, true, 1000, 15.5, 300)));
    }

    @Test
    void exactlyMinimumRodsIsEnough() {
        assertNull(ReactorStartupRequirements.firstBlocker(inputs(false, false, 1,
                ReactorStartupRequirements.MIN_CONTROL_ROD_CLUSTERS, true, 950, 15, 550)));
    }

    @Test
    void reportsFirstBlockerInDisplayOrder() {
        assertEquals(Blocker.HOUSING_MISMATCH,
                ReactorStartupRequirements.firstBlocker(inputs(true, true, 0, 0, false, 0, 0.1, 300)));
        assertEquals(Blocker.NO_FUEL,
                ReactorStartupRequirements.firstBlocker(inputs(false, false, 0, 0, false, 0, 0.1, 300)));
        assertEquals(Blocker.RCP_NOT_RUNNING,
                ReactorStartupRequirements.firstBlocker(inputs(false, false, 5, 4, false, 0, 0.1, 300)));
    }
}
