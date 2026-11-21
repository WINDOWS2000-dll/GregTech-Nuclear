package com.windows2000.gtnuclear.api.nuclear.neutronics;

/**
 * An integer grid coordinate for a single CMFD node, local to one reactor's own geometry -- deliberately not a
 * Minecraft {@code BlockPos}, since this package (like {@code api.nuclear.decay}) has no Minecraft/world
 * dependency and is unit-testable standalone.
 * <p>
 * A record (2026-10-06, Java 25 upgrade pass): {@code equals}/{@code hashCode} are exactly what the old
 * hand-written versions already did (field-by-field value equality), now generated instead of maintained by
 * hand. {@link #toString()} is overridden to keep the old {@code "(x, y, z)"} display format rather than a
 * record's default {@code "NodePosition[x=.., y=.., z=..]"}.
 */
public record NodePosition(int x, int y, int z) {

    public NodePosition offset(int dx, int dy, int dz) {
        return new NodePosition(x + dx, y + dy, z + dz);
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ", " + z + ")";
    }
}
