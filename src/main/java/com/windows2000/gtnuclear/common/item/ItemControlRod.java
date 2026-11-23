package com.windows2000.gtnuclear.common.item;

import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import com.windows2000.gtnuclear.common.metatileentities.multiblockpart.FuelAssemblyPortText;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A rod cluster control assembly (shown as "Control Rod Cluster") carrying its own fully-inserted reactivity
 * worth (see {@code com.windows2000.gtnuclear.api.nuclear.neutronics.ControlRodWorth}; replaced by a CMFD
 * absorber cross-section increment in stage 4) -- one registered {@code Item} per absorber type (standard
 * Ag-In-Cd, high-value B4C). Since 2026-11-21 (stage 1-3 of this mod's reactor physics rework) it lives in a fuel
 * assembly port's guide-tube slot and carries a fabricated length ({@link AssemblyLength}) like a fuel assembly.
 * Wear tied to captured neutron flux is applied by the owning port.
 */
public class ItemControlRod extends Item implements ITintedItem {

    public final double maxWorthReactivity;
    /** This type's own tint color -- see {@link ITintedItem}'s own JavaDoc on why every type shares one texture. */
    private final int tintColor;

    public ItemControlRod(double maxWorthReactivity, @NotNull String translationKeySuffix, int tintColor) {
        this.maxWorthReactivity = maxWorthReactivity;
        this.tintColor = tintColor;
        setMaxStackSize(1);
        setMaxDamage(1000);
        setTranslationKey("gtnuclear.control_rod." + translationKeySuffix);
        setCreativeTab(CreativeTabs.MISC);
    }

    @Override
    public int getTintColor() {
        return tintColor;
    }

    @Override
    public void getSubItems(@NotNull CreativeTabs tab, @NotNull NonNullList<ItemStack> items) {
        if (!isInCreativeTab(tab)) return;
        for (int layers : AssemblyLength.CREATIVE_TAB_LENGTHS) {
            items.add(AssemblyLength.withLength(new ItemStack(this), layers));
        }
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag advanced) {
        super.addInformation(stack, world, tooltip, advanced);
        int length = AssemblyLength.get(stack);
        tooltip.add(length > 0 ?
                TextFormatting.GRAY + I18n.format("gtnuclear.assembly.tooltip.length",
                        FuelAssemblyPortText.length(length)) :
                TextFormatting.RED + I18n.format("gtnuclear.assembly.tooltip.no_length"));
    }
}
