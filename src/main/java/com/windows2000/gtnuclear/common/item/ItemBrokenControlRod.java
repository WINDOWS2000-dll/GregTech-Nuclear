package com.windows2000.gtnuclear.common.item;

import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import com.windows2000.gtnuclear.common.metatileentities.multiblockpart.FuelAssemblyPortText;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * What a loaded {@link ItemControlRod} turns into once its accumulated wear reaches {@code getMaxDamage()} (2026-10
 * decision) -- a plain, inert, single shared item (absorber-type-agnostic) marking that the guide-tube slot held a
 * now-exhausted rod rather than leaving it silently empty. Can be taken out of a fuel assembly port but never put
 * in. Since 2026-11-21 (stage 1-3) it keeps the original rod's {@link AssemblyLength} for future reprocessing
 * (roadmap J) and no longer stacks, so differently-sized broken rods never merge.
 */
public class ItemBrokenControlRod extends Item {

    public ItemBrokenControlRod() {
        setMaxStackSize(1);
        setTranslationKey("gtnuclear.broken_control_rod");
        setCreativeTab(CreativeTabs.MISC);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag advanced) {
        super.addInformation(stack, world, tooltip, advanced);
        int length = AssemblyLength.get(stack);
        if (length > 0) {
            tooltip.add(TextFormatting.GRAY + I18n.format("gtnuclear.assembly.tooltip.length",
                    FuelAssemblyPortText.length(length)));
        }
    }
}
