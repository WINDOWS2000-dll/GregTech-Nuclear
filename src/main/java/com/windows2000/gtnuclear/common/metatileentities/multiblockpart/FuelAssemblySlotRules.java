package com.windows2000.gtnuclear.common.metatileentities.multiblockpart;

/**
 * Pure insertion/extraction rules for {@link MetaTileEntityFuelAssemblyPort}'s two slots (2026-11-21, stage 1-3 (a)
 * of this mod's reactor physics rework, see this mod's design memory). Kept free of Minecraft types so every rule
 * is unit-testable; the port translates its own live state into these plain arguments.
 * <ol>
 * <li><b>Maintenance lock</b>: while the port is part of a formed reactor, nothing goes in or out unless the
 * reactor currently allows fuel handling. An unformed port (reactor still being assembled) is unrestricted.</li>
 * <li><b>Kind</b>: slot 0 takes fuel assemblies only; slot 1 takes intact control rod clusters only (a broken rod
 * can be taken out, never put in).</li>
 * <li><b>Length</b>: an item without a length is rejected outright; inside a formed reactor its length must not
 * exceed the channel height.</li>
 * <li><b>Order</b>: a guide-tube insert needs fuel already present, and fuel can only come out once the insert slot
 * is empty -- the port can never hold an insert without fuel.</li>
 * </ol>
 */
public final class FuelAssemblySlotRules {

    public static final int SLOT_FUEL = 0;
    public static final int SLOT_INSERT = 1;

    /** What kind of item is being considered. */
    public enum ItemKind {
        FUEL_ASSEMBLY,
        CONTROL_ROD,
        BROKEN_CONTROL_ROD,
        OTHER
    }

    /** The port's own situation at the moment of the check. */
    public record Context(boolean formed, boolean fuelHandlingAllowed, int channelHeightLayers, boolean fuelPresent,
                          boolean insertPresent) {}

    private FuelAssemblySlotRules() {}

    /** Whether {@code kind} may ever sit in {@code slot}, ignoring lock/length/order (used for slot filtering). */
    public static boolean isKindAccepted(int slot, ItemKind kind) {
        return switch (slot) {
            case SLOT_FUEL -> kind == ItemKind.FUEL_ASSEMBLY;
            case SLOT_INSERT -> kind == ItemKind.CONTROL_ROD || kind == ItemKind.BROKEN_CONTROL_ROD;
            default -> false;
        };
    }

    public static boolean canInsert(int slot, ItemKind kind, int itemLengthLayers, Context context) {
        if (!isLockOpen(context)) return false;
        if (slot == SLOT_FUEL && kind != ItemKind.FUEL_ASSEMBLY) return false;
        if (slot == SLOT_INSERT && kind != ItemKind.CONTROL_ROD) return false;
        if (slot != SLOT_FUEL && slot != SLOT_INSERT) return false;
        if (itemLengthLayers <= 0) return false;
        if (context.formed() && itemLengthLayers > context.channelHeightLayers()) return false;
        if (slot == SLOT_FUEL) return !context.fuelPresent();
        return context.fuelPresent() && !context.insertPresent();
    }

    public static boolean canExtract(int slot, Context context) {
        if (!isLockOpen(context)) return false;
        if (slot == SLOT_FUEL) return !context.insertPresent();
        return slot == SLOT_INSERT;
    }

    private static boolean isLockOpen(Context context) {
        return !context.formed() || context.fuelHandlingAllowed();
    }
}
