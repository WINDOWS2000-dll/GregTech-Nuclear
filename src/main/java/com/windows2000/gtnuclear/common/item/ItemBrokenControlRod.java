package com.windows2000.gtnuclear.common.item;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;

/**
 * What a loaded {@link ItemControlRod} turns into once {@code MetaTileEntityControlRodController#applyWear}'s
 * accumulated wear reaches its {@code getMaxDamage()} (2026-10 decision) -- a plain, inert, single shared item
 * (material-type-agnostic: a worn-out standard rod and a worn-out high-value rod both become this, since neither
 * carries any remaining function to distinguish) marking that the slot held a now-exhausted rod rather than
 * leaving it silently empty. Not itself insertable into a control rod controller
 * ({@code MetaTileEntityControlRodController#isItemAllowed} only accepts {@link ItemControlRod}).
 */
public class ItemBrokenControlRod extends Item {

    public ItemBrokenControlRod() {
        setMaxStackSize(16);
        setTranslationKey("gtnuclear.broken_control_rod");
        setCreativeTab(CreativeTabs.MISC);
    }
}
