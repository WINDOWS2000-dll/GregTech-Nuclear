package com.windows2000.gtnuclear.api.nuclear.fuel;

import org.jetbrains.annotations.NotNull;

/**
 * Lossless {@code double[]} &lt;-&gt; {@code int[]} conversion via raw IEEE-754 bits, two ints per double (high word
 * first) (2026-11-21, stage 1-3 (c) of this mod's reactor physics rework, see this mod's design memory). Minecraft
 * 1.12's NBT has no double-array tag; {@code NBTTagIntArray} is used rather than {@code NBTTagLongArray} because
 * the latter has no public accessor for its contents on this mod's compile classpath, while
 * {@code NBTTagCompound#getIntArray} is vanilla API. Same size on disk either way.
 */
public final class PackedDoubles {

    private PackedDoubles() {}

    @NotNull
    public static int[] pack(@NotNull double[] values) {
        int[] packed = new int[values.length * 2];
        for (int i = 0; i < values.length; i++) {
            long bits = Double.doubleToRawLongBits(values[i]);
            packed[2 * i] = (int) (bits >>> 32);
            packed[2 * i + 1] = (int) bits;
        }
        return packed;
    }

    /** An odd-length input (malformed data) drops its trailing int. */
    @NotNull
    public static double[] unpack(@NotNull int[] packed) {
        double[] values = new double[packed.length / 2];
        for (int i = 0; i < values.length; i++) {
            long bits = ((long) packed[2 * i] << 32) | (packed[2 * i + 1] & 0xFFFFFFFFL);
            values[i] = Double.longBitsToDouble(bits);
        }
        return values;
    }
}
