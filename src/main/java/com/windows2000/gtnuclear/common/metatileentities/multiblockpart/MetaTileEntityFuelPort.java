package com.windows2000.gtnuclear.common.metatileentities.multiblockpart;

import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.interfaces.IGregTechTileEntity;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.unification.Element;

import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.items.IItemHandlerModifiable;

import com.windows2000.gtnuclear.api.metatileentity.multiblock.GTNLMultiblockAbility;
import com.windows2000.gtnuclear.api.nuclear.fuel.FuelComposition;
import com.windows2000.gtnuclear.common.item.ItemFuelRod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/** Sits atop a fuel channel (a column of {@code FuelRodHousing} blocks) and holds one fuel rod item. */
public class MetaTileEntityFuelPort extends MetaTileEntityReactorItemPort {

    public MetaTileEntityFuelPort(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId);
    }

    @Override
    public MetaTileEntity createMetaTileEntity(IGregTechTileEntity tileEntity) {
        return new MetaTileEntityFuelPort(metaTileEntityId);
    }

    @Override
    protected boolean isItemAllowed(@NotNull ItemStack stack) {
        return stack.getItem() instanceof ItemFuelRod;
    }

    /**
     * The currently-loaded fuel rod's live composition, derived from its own NBT-tracked isotope inventory (see
     * {@link ItemFuelRod#getIsotopeAmounts}) rather than its fixed enrichment-tier recipe -- so performance
     * reflects however much that specific rod has already burned up, not just what type it is.
     */
    @Nullable
    public FuelComposition getLoadedComposition() {
        ItemStack stack = getInsertedItem();
        if (!(stack.getItem() instanceof ItemFuelRod fuelRod)) return null;
        return new FuelComposition(fuelRod.initialComposition.id, fuelRod.getIsotopeAmounts(stack));
    }

    /**
     * Persists {@code amounts} as the currently-loaded fuel rod's new isotope inventory, if a fuel rod is loaded
     * (a no-op otherwise). Called by the reactor controller's own burnup tick -- see
     * {@code MetaTileEntityPressurizedWaterReactor#updateFuelBurnup()}.
     */
    public void setLoadedIsotopeAmounts(@NotNull Map<Element, Double> amounts) {
        ItemStack stack = getInsertedItem();
        if (!(stack.getItem() instanceof ItemFuelRod fuelRod)) return;
        fuelRod.setIsotopeAmounts(stack, amounts);
        inventory.setStackInSlot(0, stack);
        markDirty();
    }

    @NotNull
    @Override
    protected MultiblockAbility<IItemHandlerModifiable> getPortAbility() {
        return GTNLMultiblockAbility.FUEL_PORT;
    }
}
