package com.windows2000.gtnuclear.api.nuclear.operation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScramRodDropTest {

    private static final double TICK_SECONDS = 0.05;

    @Test
    void fullyWithdrawnRodReachesFullInsertionAfterDropTime() {
        // 2.2 s / 0.05 s = 44 ticks: still short of full insertion one tick early, fully in one tick late
        // (checked on both sides so floating-point accumulation at exactly tick 44 cannot make this flaky).
        double depth = 0.0;
        for (int i = 0; i < 43; i++) depth = ScramRodDrop.advance(depth, TICK_SECONDS, 2.2);
        assertEquals(43 * TICK_SECONDS / 2.2, depth, 1e-9);
        for (int i = 0; i < 2; i++) depth = ScramRodDrop.advance(depth, TICK_SECONDS, 2.2);
        assertEquals(1.0, depth, 0.0);
    }

    @Test
    void neverExceedsFullInsertion() {
        assertEquals(1.0, ScramRodDrop.advance(0.99, 1.0, 2.2), 1e-12);
        assertEquals(1.0, ScramRodDrop.advance(1.0, TICK_SECONDS, 2.2), 1e-12);
    }

    @Test
    void splittingTheTimeStepGivesTheSameResult() {
        double coarse = ScramRodDrop.advance(0.2, 0.5, 2.2);
        double fine = 0.2;
        for (int i = 0; i < 10; i++) {
            fine = ScramRodDrop.advance(fine, 0.05, 2.2);
        }
        assertEquals(coarse, fine, 1e-12);
    }

    @Test
    void nonPositiveDropTimeInsertsInstantly() {
        assertEquals(1.0, ScramRodDrop.advance(0.0, TICK_SECONDS, 0.0), 1e-12);
    }

    @Test
    void outOfRangeInputIsClampedFirst() {
        assertEquals(TICK_SECONDS / 2.2, ScramRodDrop.advance(-0.5, TICK_SECONDS, 2.2), 1e-12);
    }
}
