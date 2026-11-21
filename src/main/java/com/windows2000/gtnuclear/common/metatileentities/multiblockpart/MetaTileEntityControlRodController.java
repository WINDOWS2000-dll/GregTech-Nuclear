package com.windows2000.gtnuclear.common.metatileentities.multiblockpart;

import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.interfaces.IGregTechTileEntity;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.items.IItemHandlerModifiable;

import com.windows2000.gtnuclear.api.metatileentity.multiblock.GTNLMultiblockAbility;
import com.windows2000.gtnuclear.common.item.GTNLItems;
import com.windows2000.gtnuclear.common.item.ItemControlRod;
import org.jetbrains.annotations.NotNull;

/**
 * Sits atop a control rod channel (a column of {@code ControlRodHousing} blocks) and holds one control rod item.
 * Also tracks insertion depth (0.0 = fully withdrawn, 1.0 = fully inserted) -- per this mod's design memory,
 * players adjust this manually rather than via an automatic withdrawal button, and it stays adjustable while the
 * reactor is running (unlike the rod item itself, which can only be swapped while stopped). Defaults to fully
 * inserted, matching the fresh-fuel/all-rods-inserted starting condition for initial startup.
 * <p>
 * The GUI currently only displays this value as text; an interactive slider and the actual reactivity-feedback
 * wiring are deferred along with the rest of this mod's fuel/control-rod item design (see this mod's design
 * memory).
 */
public class MetaTileEntityControlRodController extends MetaTileEntityReactorItemPort {

    /** Number of real banks (1-{@value}); 0 itself means "unassigned", not a bank -- see {@link #setBankId}. */
    public static final int BANK_COUNT = 4;

    /**
     * Sub-integer wear accumulator NBT key (see {@link #applyWear}) -- vanilla item damage is an integer, but
     * {@code ConfigHolder.reactor.controlRodWearRateCoefficient}'s own per-tick wear is tiny (calibrated for a
     * ~400-hour rod lifetime), so accumulating only in whole-damage-point steps would round every tick's wear
     * down to zero forever.
     */
    private static final String NBT_WEAR_ACCUMULATOR = "WearAccumulator";

    private double insertionDepth = 1.0;
    /**
     * Which console-side "bank" (see this mod's design memory, 2026-09-17: bank grouping is a software concept,
     * no new hardware) this controller belongs to -- 0 means unassigned (not in any bank, so bank-wide nudge
     * commands skip it). Assigned via the console's control rod list (stage 5 of this mod's startup/shutdown
     * sequence work, 2026-09-21), not this part's own GUI.
     */
    private int bankId = 0;

    public MetaTileEntityControlRodController(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId);
    }

    @Override
    public MetaTileEntity createMetaTileEntity(IGregTechTileEntity tileEntity) {
        return new MetaTileEntityControlRodController(metaTileEntityId);
    }

    @Override
    protected boolean isItemAllowed(@NotNull ItemStack stack) {
        return stack.getItem() instanceof ItemControlRod;
    }

    @NotNull
    @Override
    protected MultiblockAbility<IItemHandlerModifiable> getPortAbility() {
        return GTNLMultiblockAbility.CONTROL_ROD_PORT;
    }

    /** The fully-inserted reactivity worth of whatever control rod is currently loaded; 0 if the slot is empty. */
    public double getMaxWorthReactivity() {
        return getInsertedItem().getItem() instanceof ItemControlRod rod ? rod.maxWorthReactivity : 0.0;
    }

    public double getInsertionDepth() {
        return insertionDepth;
    }

    public void setInsertionDepth(double insertionDepth) {
        this.insertionDepth = Math.max(0.0, Math.min(1.0, insertionDepth));
    }

    public int getBankId() {
        return bankId;
    }

    /** Clamped to {@code [0, BANK_COUNT]}, not wrapped -- callers that want cycling handle the wraparound. */
    public void setBankId(int bankId) {
        this.bankId = Math.max(0, Math.min(BANK_COUNT, bankId));
    }

    /**
     * Applies {@code amount} (fractional vanilla-damage units, already scaled by local flux/power/dt by the
     * caller -- see {@code MetaTileEntityPressurizedWaterReactor#updateControlRodWear}) of wear to the loaded rod.
     * No-op with an empty slot or a non-{@link ItemControlRod} item. Once the accumulated total reaches the rod's
     * own {@code getMaxDamage()}, the stack is replaced outright with {@link GTNLItems#BROKEN_CONTROL_ROD} (2026-10
     * decision) rather than vanishing -- see that item's own JavaDoc.
     */
    public void applyWear(double amount) {
        if (amount <= 0) return;
        ItemStack stack = getInsertedItem();
        if (!(stack.getItem() instanceof ItemControlRod rod)) return;

        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        double accumulated = tag.getDouble(NBT_WEAR_ACCUMULATOR) + amount;
        if (accumulated >= rod.getMaxDamage()) {
            inventory.setStackInSlot(0, new ItemStack(GTNLItems.BROKEN_CONTROL_ROD));
        } else {
            tag.setDouble(NBT_WEAR_ACCUMULATOR, accumulated);
            stack.setItemDamage((int) accumulated);
        }
        markDirty();
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        data.setDouble("InsertionDepth", insertionDepth);
        data.setInteger("BankId", bankId);
        return data;
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        if (data.hasKey("InsertionDepth")) {
            this.insertionDepth = data.getDouble("InsertionDepth");
        }
        bankId = data.getInteger("BankId");
    }
}
