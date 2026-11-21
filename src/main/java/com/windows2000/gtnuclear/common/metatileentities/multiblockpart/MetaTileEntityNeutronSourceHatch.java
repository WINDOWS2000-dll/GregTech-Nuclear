package com.windows2000.gtnuclear.common.metatileentities.multiblockpart;

import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.interfaces.IGregTechTileEntity;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.unification.Element;

import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.items.IItemHandlerModifiable;

import com.windows2000.gtnuclear.api.metatileentity.multiblock.GTNLMultiblockAbility;
import com.windows2000.gtnuclear.common.item.ItemNeutronSourceRod;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Sits at the reactor's fixed 'N' pattern slot (front face, immediately left of the controller) and holds one
 * {@link ItemNeutronSourceRod}. See {@code MetaTileEntityPressurizedWaterReactor}'s own JavaDoc for why this
 * hatch's loaded source is what actually lets the reactor start from a true-zero neutron population.
 */
public class MetaTileEntityNeutronSourceHatch extends MetaTileEntityReactorItemPort {

    public MetaTileEntityNeutronSourceHatch(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId);
    }

    @Override
    public MetaTileEntity createMetaTileEntity(IGregTechTileEntity tileEntity) {
        return new MetaTileEntityNeutronSourceHatch(metaTileEntityId);
    }

    @Override
    protected boolean isItemAllowed(@NotNull ItemStack stack) {
        return stack.getItem() instanceof ItemNeutronSourceRod;
    }

    @NotNull
    @Override
    protected MultiblockAbility<IItemHandlerModifiable> getPortAbility() {
        return GTNLMultiblockAbility.NEUTRON_SOURCE_PORT;
    }

    /**
     * Persists {@code amounts} as the currently-loaded source rod's new isotope inventory, if one is loaded (a
     * no-op otherwise). Called by the reactor controller's own neutron-source-decay tick.
     */
    public void setLoadedIsotopeAmounts(@NotNull Map<Element, Double> amounts) {
        ItemStack stack = getInsertedItem();
        if (!(stack.getItem() instanceof ItemNeutronSourceRod sourceRod)) return;
        sourceRod.setIsotopeAmounts(stack, amounts);
        inventory.setStackInSlot(0, stack);
        markDirty();
    }
}
