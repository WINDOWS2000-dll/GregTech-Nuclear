package com.windows2000.gtnuclear.api.nuclear.neutronics;

import com.windows2000.gtnuclear.api.nuclear.fuel.FuelComposition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Builds a vertical column of CMFD nodes for one fuel/control-rod channel (2026-10-07, extracted from
 * {@code MetaTileEntityPressurizedWaterReactor#addChannelColumn} as part of that class's own decomposition --
 * see this mod's design memory). Already had no reactor-multiblock dependency beyond translating a
 * {@code BlockPos} into relative offsets before the move -- {@link #addChannelColumn} takes those offsets
 * directly instead, so this class needs no Minecraft/GT types at all.
 */
public final class ReactorGeometryBuilder {

    private ReactorGeometryBuilder() {}

    /**
     * One CMFD node per vertical layer of this channel, from {@code bottomYRelative} up to {@code topYRelative}
     * (both relative to the reactor controller's own Y, inclusive) -- see this mod's design memory (2026-09-20)
     * for why this is a "formal" 3D geometry (repeating the same node up the column) rather than one node per
     * whole channel: the current block-level design has no way to place different fuel at different heights
     * within one channel, so there is no per-height data to vary by, but the solver still resolves real axial
     * leakage this way.
     *
     * @param composition {@code null} for a control-rod channel (always a fixed moderator-like node regardless
     *                    of insertion depth -- rod worth is a separate per-tick amplitude-stage term, not a CMFD
     *                    shape input, see {@link ReactorNodeFactory#moderatorNode}'s own JavaDoc), otherwise the
     *                    loaded fuel composition for a fuel channel.
     */
    public static void addChannelColumn(@NotNull ReactorGeometry geometry, int dx, int dz, int bottomYRelative,
                                        int topYRelative, @Nullable FuelComposition composition) {
        for (int y = bottomYRelative; y <= topYRelative; y++) {
            NodePosition position = new NodePosition(dx, y, dz);
            geometry.put(composition != null ? ReactorNodeFactory.fuelNode(position, composition) :
                    ReactorNodeFactory.moderatorNode(position));
        }
    }

    /**
     * One fuel assembly channel (2026-11-21, stage 1-3 of this mod's reactor physics rework): fuel nodes from
     * {@code bottomYRelative} up through {@code bottomYRelative + fuelLengthLayers - 1}, water (moderator) nodes
     * for the rest of the channel up to {@code topYRelative} -- an assembly shorter than its channel sits on the
     * channel floor with water above it. {@code fuelLengthLayers <= 0} or a {@code null} composition yields a
     * pure water channel (an empty slot, or a port with no fuel loaded).
     */
    public static void addFuelAssemblyColumn(@NotNull ReactorGeometry geometry, int dx, int dz,
                                             int bottomYRelative, int topYRelative, int fuelLengthLayers,
                                             @Nullable FuelComposition composition) {
        int fuelTopY = composition != null ? bottomYRelative + fuelLengthLayers - 1 : bottomYRelative - 1;
        for (int y = bottomYRelative; y <= topYRelative; y++) {
            NodePosition position = new NodePosition(dx, y, dz);
            geometry.put(y <= fuelTopY ? ReactorNodeFactory.fuelNode(position, composition) :
                    ReactorNodeFactory.moderatorNode(position));
        }
    }
}
