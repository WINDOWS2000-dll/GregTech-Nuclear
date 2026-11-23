package com.windows2000.gtnuclear.common.metatileentities.multiblockpart;

import com.windows2000.gtnuclear.common.metatileentities.multiblockpart.FuelAssemblySlotRules.Context;
import com.windows2000.gtnuclear.common.metatileentities.multiblockpart.FuelAssemblySlotRules.ItemKind;
import org.junit.jupiter.api.Test;

import static com.windows2000.gtnuclear.common.metatileentities.multiblockpart.FuelAssemblySlotRules.SLOT_FUEL;
import static com.windows2000.gtnuclear.common.metatileentities.multiblockpart.FuelAssemblySlotRules.SLOT_INSERT;
import static com.windows2000.gtnuclear.common.metatileentities.multiblockpart.FuelAssemblySlotRules.canExtract;
import static com.windows2000.gtnuclear.common.metatileentities.multiblockpart.FuelAssemblySlotRules.canInsert;
import static com.windows2000.gtnuclear.common.metatileentities.multiblockpart.FuelAssemblySlotRules.isKindAccepted;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FuelAssemblySlotRulesTest {

    private static Context shutdown(boolean fuel, boolean insert) {
        return new Context(true, true, 18, fuel, insert);
    }

    private static Context running(boolean fuel, boolean insert) {
        return new Context(true, false, 18, fuel, insert);
    }

    private static Context unformed(boolean fuel, boolean insert) {
        return new Context(false, false, 0, fuel, insert);
    }

    @Test
    void kindFiltering() {
        assertTrue(isKindAccepted(SLOT_FUEL, ItemKind.FUEL_ASSEMBLY));
        assertFalse(isKindAccepted(SLOT_FUEL, ItemKind.CONTROL_ROD));
        assertTrue(isKindAccepted(SLOT_INSERT, ItemKind.CONTROL_ROD));
        assertTrue(isKindAccepted(SLOT_INSERT, ItemKind.BROKEN_CONTROL_ROD));
        assertFalse(isKindAccepted(SLOT_INSERT, ItemKind.OTHER));
        assertFalse(isKindAccepted(5, ItemKind.FUEL_ASSEMBLY));
    }

    @Test
    void brokenRodCanNeverBeInserted() {
        assertFalse(canInsert(SLOT_INSERT, ItemKind.BROKEN_CONTROL_ROD, 17, shutdown(true, false)));
        assertFalse(canInsert(SLOT_INSERT, ItemKind.BROKEN_CONTROL_ROD, 17, unformed(true, false)));
    }

    @Test
    void lockBlocksEverythingWhileRunning() {
        assertFalse(canInsert(SLOT_FUEL, ItemKind.FUEL_ASSEMBLY, 17, running(false, false)));
        assertFalse(canInsert(SLOT_INSERT, ItemKind.CONTROL_ROD, 17, running(true, false)));
        assertFalse(canExtract(SLOT_FUEL, running(true, false)));
        assertFalse(canExtract(SLOT_INSERT, running(true, true)));
    }

    @Test
    void unformedPortIsUnlockedAndIgnoresChannelHeight() {
        assertTrue(canInsert(SLOT_FUEL, ItemKind.FUEL_ASSEMBLY, 36, unformed(false, false)));
        assertTrue(canExtract(SLOT_FUEL, unformed(true, false)));
    }

    @Test
    void lengthMustBePositiveAndFitTheChannel() {
        assertFalse(canInsert(SLOT_FUEL, ItemKind.FUEL_ASSEMBLY, 0, shutdown(false, false)));
        assertFalse(canInsert(SLOT_FUEL, ItemKind.FUEL_ASSEMBLY, 0, unformed(false, false)));
        assertTrue(canInsert(SLOT_FUEL, ItemKind.FUEL_ASSEMBLY, 18, shutdown(false, false)));
        assertFalse(canInsert(SLOT_FUEL, ItemKind.FUEL_ASSEMBLY, 19, shutdown(false, false)));
        assertFalse(canInsert(SLOT_INSERT, ItemKind.CONTROL_ROD, 19, shutdown(true, false)));
    }

    @Test
    void insertRequiresFuelAndFuelExtractionRequiresEmptyInsert() {
        assertFalse(canInsert(SLOT_INSERT, ItemKind.CONTROL_ROD, 17, shutdown(false, false)));
        assertTrue(canInsert(SLOT_INSERT, ItemKind.CONTROL_ROD, 17, shutdown(true, false)));
        assertFalse(canExtract(SLOT_FUEL, shutdown(true, true)));
        assertTrue(canExtract(SLOT_INSERT, shutdown(true, true)));
        assertTrue(canExtract(SLOT_FUEL, shutdown(true, false)));
    }

    @Test
    void occupiedSlotsRejectInsertion() {
        assertFalse(canInsert(SLOT_FUEL, ItemKind.FUEL_ASSEMBLY, 17, shutdown(true, false)));
        assertFalse(canInsert(SLOT_INSERT, ItemKind.CONTROL_ROD, 17, shutdown(true, true)));
    }

    @Test
    void wrongKindPerSlotIsRejected() {
        assertFalse(canInsert(SLOT_FUEL, ItemKind.CONTROL_ROD, 17, shutdown(false, false)));
        assertFalse(canInsert(SLOT_INSERT, ItemKind.FUEL_ASSEMBLY, 17, shutdown(true, false)));
        assertFalse(canInsert(SLOT_FUEL, ItemKind.OTHER, 17, unformed(false, false)));
    }
}
