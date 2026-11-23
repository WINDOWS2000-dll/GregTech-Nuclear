package com.windows2000.gtnuclear.common.item;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.jetbrains.annotations.NotNull;

/**
 * The axial length (in channel layers) a fuel assembly / control rod cluster item is fabricated with (2026-11-21,
 * stage 1-3 of this mod's reactor physics rework, see this mod's design memory: decided "S1", length fixed at
 * fabrication, so fuel cost scales with how much heavy metal the assembly actually holds). An item without a length
 * is not loadable into a fuel assembly port.
 */
public final class AssemblyLength {

    public static final String NBT_LENGTH = "Length";
    /** Longest channel any buildable reactor has (37-layer maximum height minus the solid bottom cap). */
    public static final int MAX_LAYERS = 36;
    /** Lengths offered in the creative tab: a small reactor, the real-size reference reactor, near-maximum. */
    public static final int[] CREATIVE_TAB_LENGTHS = { 9, 17, 35 };

    private AssemblyLength() {}

    /** @return the stack's length in layers, or 0 if it has none. */
    public static int get(@NotNull ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        return tag == null ? 0 : Math.max(0, tag.getInteger(NBT_LENGTH));
    }

    public static void set(@NotNull ItemStack stack, int layers) {
        if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
        stack.getTagCompound().setInteger(NBT_LENGTH, Math.max(0, Math.min(MAX_LAYERS, layers)));
    }

    /** A copy of {@code stack} with {@code layers} set. */
    @NotNull
    public static ItemStack withLength(@NotNull ItemStack stack, int layers) {
        ItemStack copy = stack.copy();
        set(copy, layers);
        return copy;
    }
}
