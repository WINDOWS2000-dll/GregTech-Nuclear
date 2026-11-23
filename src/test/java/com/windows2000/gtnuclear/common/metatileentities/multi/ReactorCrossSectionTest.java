package com.windows2000.gtnuclear.common.metatileentities.multi;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReactorCrossSectionTest {

    @Test
    void interiorCellCountsForReferenceDiameters() {
        assertEquals(21, ReactorCrossSection.of(7).interiorCells().size());
        assertEquals(101, ReactorCrossSection.of(13).interiorCells().size());
        assertEquals(177, ReactorCrossSection.of(17).interiorCells().size());
        assertEquals(665, ReactorCrossSection.of(31).interiorCells().size());
    }

    @Test
    void interiorIsMirrorSymmetricAndContainsCenter() {
        for (int diameter = 7; diameter <= 31; diameter += 2) {
            Set<ReactorCrossSection.Cell> cells = new HashSet<>(ReactorCrossSection.of(diameter).interiorCells());
            assertTrue(cells.contains(new ReactorCrossSection.Cell(0, 0)));
            for (ReactorCrossSection.Cell cell : cells) {
                assertTrue(cells.contains(new ReactorCrossSection.Cell(cell.rowOffset(), -cell.columnOffset())));
                assertTrue(cells.contains(new ReactorCrossSection.Cell(-cell.rowOffset(), cell.columnOffset())));
                assertTrue(cells.contains(new ReactorCrossSection.Cell(cell.columnOffset(), cell.rowOffset())));
            }
        }
    }

    @Test
    void frontWallCenterIsBoundaryNotInterior() {
        ReactorCrossSection section = ReactorCrossSection.of(13);
        int center = 13 / 2;
        // The controller's own cell (row 0, center column) is the vessel wall.
        assertTrue(section.isInside(0, center));
        assertTrue(section.isBoundary(0, center));
        assertFalse(section.isInterior(0, center));
        assertTrue(section.isInterior(center, center));
        assertFalse(section.isInside(0, 0));
    }
}
