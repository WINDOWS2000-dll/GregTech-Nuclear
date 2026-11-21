package com.windows2000.gtnuclear.api.nuclear.thermal;

import com.windows2000.gtnuclear.api.nuclear.accident.LOCADetector;

/**
 * The reactor's "full tank" primary coolant mass, derived from its own interior volume rather than a size-
 * independent constant -- a larger-diameter/taller reactor (more interior cells in the circular cross-section,
 * see {@code MetaTileEntityPressurizedWaterReactor#createStructurePattern()}) has proportionally more coolant
 * when full. Used both as the fill target for the startup coolant injection sequence and as the reference value
 * {@link LOCADetector} compares the current coolant mass against.
 */
public final class RatedCoolantMass {

    private RatedCoolantMass() {}

    /**
     * @param interiorCellCount number of interior (non-boundary, non-air) cells across the reactor's full height
     * @param massPerCellKg     representative coolant mass held by one such cell when full (kg)
     * @return the reactor's rated ("full tank") primary coolant mass (kg)
     */
    public static double ratedMassKg(int interiorCellCount, double massPerCellKg) {
        return Math.max(interiorCellCount, 0) * massPerCellKg;
    }
}
