package com.windows2000.gtnuclear.api.nuclear.thermal;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EnergyLedgerTest {

    private enum TestCategory implements EnergyLedger.Category {

        HEAT_IN(EnergyLedger.Kind.INFLOW),
        MASS_IN(EnergyLedger.Kind.INFLOW),
        MASS_OUT(EnergyLedger.Kind.OUTFLOW),
        STORED(EnergyLedger.Kind.STORED_CHANGE),
        NOTE(EnergyLedger.Kind.INFO);

        private final EnergyLedger.Kind kind;

        TestCategory(EnergyLedger.Kind kind) {
            this.kind = kind;
        }

        @NotNull
        @Override
        public EnergyLedger.Kind kind() {
            return kind;
        }
    }

    @Test
    void accumulatesPerCategoryAcrossCalls() {
        EnergyLedger<TestCategory> ledger = new EnergyLedger<>(TestCategory.class);
        ledger.add(TestCategory.HEAT_IN, 10.0);
        ledger.add(TestCategory.HEAT_IN, 5.0);
        ledger.add(TestCategory.MASS_OUT, 3.0);

        assertEquals(15.0, ledger.get(TestCategory.HEAT_IN), 1e-12);
        assertEquals(3.0, ledger.get(TestCategory.MASS_OUT), 1e-12);
        assertEquals(0.0, ledger.get(TestCategory.MASS_IN), 1e-12);
    }

    @Test
    void conservingBalanceHasZeroResidual() {
        EnergyLedger<TestCategory> ledger = new EnergyLedger<>(TestCategory.class);
        ledger.add(TestCategory.HEAT_IN, 100.0);
        ledger.add(TestCategory.MASS_IN, 20.0);
        ledger.add(TestCategory.MASS_OUT, 70.0);
        ledger.add(TestCategory.STORED, 50.0);

        assertEquals(0.0, ledger.residualJoules(TestCategory.values()), 1e-12);
    }

    @Test
    void residualExposesCreatedOrDestroyedEnergyAndIgnoresInfo() {
        EnergyLedger<TestCategory> ledger = new EnergyLedger<>(TestCategory.class);
        ledger.add(TestCategory.HEAT_IN, 100.0);
        ledger.add(TestCategory.STORED, 60.0);
        ledger.add(TestCategory.NOTE, 1.0e9);

        // 100 J in, only 60 J stored, nothing left: 40 J was silently destroyed.
        assertEquals(40.0, ledger.residualJoules(TestCategory.values()), 1e-12);
    }

    @Test
    void negativeStoredChangeIsHandled() {
        EnergyLedger<TestCategory> ledger = new EnergyLedger<>(TestCategory.class);
        ledger.add(TestCategory.MASS_OUT, 30.0);
        ledger.add(TestCategory.STORED, -30.0);

        assertEquals(0.0, ledger.residualJoules(TestCategory.values()), 1e-12);
    }

    @Test
    void ticksCountAndResetClearsEverything() {
        EnergyLedger<TestCategory> ledger = new EnergyLedger<>(TestCategory.class);
        ledger.add(TestCategory.HEAT_IN, 7.0);
        ledger.endTick();
        ledger.endTick();
        assertEquals(2, ledger.ticks());

        ledger.reset();
        assertEquals(0, ledger.ticks());
        assertEquals(0.0, ledger.get(TestCategory.HEAT_IN), 1e-12);
        assertEquals(0.0, ledger.residualJoules(TestCategory.values()), 1e-12);
    }
}
