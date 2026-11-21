package com.windows2000.gtnuclear.common.item;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;

import org.jetbrains.annotations.NotNull;

/**
 * A control rod item carrying its own fully-inserted reactivity worth (see
 * {@code com.windows2000.gtnuclear.api.nuclear.neutronics.ControlRodWorth}) -- one registered {@code Item}
 * instance per type (2026-09-20 decision: two types, standard and high-value, see {@code GTNLItems}). Wear tied
 * to captured neutron flux is wired from {@code MetaTileEntityPressurizedWaterReactor}'s tick loop, not this
 * class itself.
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
}
