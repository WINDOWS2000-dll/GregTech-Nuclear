package com.windows2000.gtnuclear.common.metatileentities.multi;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The PWR reactor's circular cross-section rasterization, extracted as a pure function (2026-11-21, stage 1-3 (b)
 * of this mod's reactor physics rework) so both the structure pattern and the runtime channel bookkeeping derive
 * from exactly the same geometry, and so it can be unit-tested. A cell is inside the vessel if its center lies
 * within {@code radius + 0.5} of the disk center; an inside cell is a <b>boundary</b> (vessel wall) cell if any of
 * its four neighbors is outside, otherwise an <b>interior</b> cell -- one fuel assembly channel position.
 */
public final class ReactorCrossSection {

    /** One interior cell, as row/column offsets from the disk center (rows run front to back). */
    public record Cell(int rowOffset, int columnOffset) {}

    private final int diameter;
    private final boolean[][] inside;
    private final boolean[][] boundary;
    private final List<Cell> interiorCells;

    private ReactorCrossSection(int diameter, boolean[][] inside, boolean[][] boundary, List<Cell> interiorCells) {
        this.diameter = diameter;
        this.inside = inside;
        this.boundary = boundary;
        this.interiorCells = interiorCells;
    }

    @NotNull
    public static ReactorCrossSection of(int diameter) {
        int center = diameter / 2;
        int radius = diameter / 2;
        boolean[][] inside = new boolean[diameter][diameter];
        boolean[][] boundary = new boolean[diameter][diameter];
        List<Cell> interior = new ArrayList<>();
        for (int row = 0; row < diameter; row++) {
            for (int column = 0; column < diameter; column++) {
                inside[row][column] = within(row, column, center, radius);
            }
        }
        for (int row = 0; row < diameter; row++) {
            for (int column = 0; column < diameter; column++) {
                if (!inside[row][column]) continue;
                boundary[row][column] = !within(row - 1, column, center, radius) ||
                        !within(row + 1, column, center, radius) || !within(row, column - 1, center, radius) ||
                        !within(row, column + 1, center, radius);
                if (!boundary[row][column]) interior.add(new Cell(row - center, column - center));
            }
        }
        return new ReactorCrossSection(diameter, inside, boundary, Collections.unmodifiableList(interior));
    }

    private static boolean within(int row, int column, int center, int radius) {
        double dRow = row - center;
        double dColumn = column - center;
        double limit = radius + 0.5;
        return dRow * dRow + dColumn * dColumn < limit * limit;
    }

    public int diameter() {
        return diameter;
    }

    public boolean isInside(int row, int column) {
        return inside[row][column];
    }

    public boolean isBoundary(int row, int column) {
        return boundary[row][column];
    }

    public boolean isInterior(int row, int column) {
        return inside[row][column] && !boundary[row][column];
    }

    /** Every interior (channel) cell, in row-major order. */
    @NotNull
    public List<Cell> interiorCells() {
        return interiorCells;
    }
}
