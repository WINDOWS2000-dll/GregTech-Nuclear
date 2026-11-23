package com.windows2000.gtnuclear.api.nuclear.fuel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PackedDoublesTest {

    @Test
    void roundTripIsBitExact() {
        double[] values = { 0.0, -0.0, 1.0, 0.045, -123.456e-300, Double.MIN_VALUE, Double.MAX_VALUE,
                Double.POSITIVE_INFINITY, Double.NaN, 1.0 / 3.0 };
        double[] restored = PackedDoubles.unpack(PackedDoubles.pack(values));
        assertEquals(values.length, restored.length);
        for (int i = 0; i < values.length; i++) {
            assertEquals(Double.doubleToRawLongBits(values[i]), Double.doubleToRawLongBits(restored[i]), "index " + i);
        }
    }

    @Test
    void packsTwoIntsPerDouble() {
        assertEquals(6, PackedDoubles.pack(new double[3]).length);
    }

    @Test
    void emptyAndOddLengthInputs() {
        assertArrayEquals(new double[0], PackedDoubles.unpack(new int[0]));
        assertEquals(1, PackedDoubles.unpack(new int[3]).length);
    }
}
