package com.windows2000.gtnuclear.common.metatileentities.multiblockpart;

import gregtech.api.GTValues;
import gregtech.api.capability.impl.NotifiableItemStackHandler;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.multiblock.AbilityInstances;
import gregtech.api.metatileentity.multiblock.IMultiblockAbilityPart;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.metatileentity.multiblock.MultiblockControllerBase;
import gregtech.api.mui.GTGuiTextures;
import gregtech.api.mui.GTGuis;
import gregtech.common.metatileentities.multi.multiblockpart.MetaTileEntityMultiblockNotifiablePart;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.items.IItemHandlerModifiable;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.SyncHandlers;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * Shared base for the PWR reactor's fuel port and control rod controller hatches -- both are a single item slot
 * accepting exactly one item type, sitting atop a vertical channel of housing blocks (see this mod's design
 * memory: housing-column consistency is verified at runtime, not by the structure pattern itself, since GT's
 * pattern matcher cannot express "this whole column is uniform" as a single-cell predicate).
 */
public abstract class MetaTileEntityReactorItemPort extends MetaTileEntityMultiblockNotifiablePart
                                                    implements IMultiblockAbilityPart<IItemHandlerModifiable> {

    protected final NotifiableItemStackHandler inventory;

    protected MetaTileEntityReactorItemPort(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId, GTValues.IV, false);
        this.inventory = new SingleSlotItemHandler(this);
    }

    /**
     * @param stack the full candidate stack, not just its {@code Item} -- a GT {@code MetaItem} (e.g. a turbine
     *              rotor) packs many unrelated sub-items into one shared {@code Item} class distinguished only
     *              by metadata/NBT, so validity cannot always be decided from the item class alone.
     */
    protected abstract boolean isItemAllowed(@NotNull ItemStack stack);

    @NotNull
    protected abstract MultiblockAbility<IItemHandlerModifiable> getPortAbility();

    @Override
    public @NotNull List<MultiblockAbility<?>> getAbilities() {
        return Collections.singletonList(getPortAbility());
    }

    @Override
    public void registerAbilities(@NotNull AbilityInstances abilityInstances) {
        abilityInstances.add(inventory);
    }

    @Override
    public void addToMultiBlock(MultiblockControllerBase controllerBase) {
        super.addToMultiBlock(controllerBase);
        inventory.addNotifiableMetaTileEntity(controllerBase);
    }

    @Override
    public void removeFromMultiBlock(MultiblockControllerBase controllerBase) {
        super.removeFromMultiBlock(controllerBase);
        inventory.removeNotifiableMetaTileEntity(controllerBase);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        data.setTag("Inventory", inventory.serializeNBT());
        return data;
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        inventory.deserializeNBT(data.getCompoundTag("Inventory"));
    }

    @Override
    public ModularPanel buildUI(PosGuiData guiData, PanelSyncManager syncManager, UISettings settings) {
        return GTGuis.createPanel(this, 176, 166)
                .child(IKey.lang(getMetaFullName()).asWidget().pos(6, 6))
                .child(new ItemSlot()
                        .slot(SyncHandlers.itemSlot(inventory, 0))
                        .background(GTGuiTextures.SLOT)
                        .pos(79, 36))
                .bindPlayerInventory();
    }

    @NotNull
    public ItemStack getInsertedItem() {
        return inventory.getStackInSlot(0);
    }

    /**
     * Called whenever the inserted item stack changes (insertion, extraction, or a GUI-driven in-place swap) --
     * a no-op by default. Subclasses whose own display state depends on the loaded item (e.g.
     * {@code MetaTileEntityTurbineRotorHolder}'s own rotor color) should recompute and sync that state here,
     * rather than waiting for whatever periodic/controller-driven update happens to run next -- a player
     * inserting an item expects its effect to show immediately, not only once the owning multiblock's own tick
     * loop next happens to notice.
     */
    protected void onInsertedItemChanged() {}

    private class SingleSlotItemHandler extends NotifiableItemStackHandler {

        SingleSlotItemHandler(MetaTileEntity metaTileEntity) {
            super(metaTileEntity, 1, metaTileEntity, false);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return stack.isEmpty() || isItemAllowed(stack);
        }

        @Override
        public void onContentsChanged(int slot) {
            super.onContentsChanged(slot);
            onInsertedItemChanged();
        }

        /**
         * Mirrors GT's own {@code MetaTileEntityRotorHolder.InventoryRotorHolder#extractItem} -- that class
         * explicitly calls {@code setRotorColor(-1)} here <i>in addition to</i> relying on
         * {@link #onContentsChanged}, rather than trusting {@code onContentsChanged} alone to fire reliably for a
         * GUI-driven take-out (confirmed via a real playtest, 2026-10: pulling a rotor back out of this mod's own
         * holder through its GUI left the last-loaded color/spin state rendering indefinitely). Belt-and-suspenders
         * by design, not a fix for a known cause -- GT's own precedent doesn't explain why
         * {@code onContentsChanged} alone isn't trusted either, it just also doesn't rely on it alone.
         */
        @NotNull
        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            ItemStack extracted = super.extractItem(slot, amount, simulate);
            if (!simulate && !extracted.isEmpty()) onInsertedItemChanged();
            return extracted;
        }
    }
}
