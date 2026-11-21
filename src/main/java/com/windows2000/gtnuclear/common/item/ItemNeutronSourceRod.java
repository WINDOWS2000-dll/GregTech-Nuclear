package com.windows2000.gtnuclear.common.item;

import gregtech.api.unification.Element;
import gregtech.api.unification.Elements;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;

import com.windows2000.gtnuclear.api.unification.materials.GTNLElements;
import com.windows2000.gtnuclear.common.ConfigHolder;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A startup neutron source rod item, holding an NBT-tracked isotope inventory (same shape as
 * {@link ItemFuelRod}) starting from {@code ConfigHolder.reactor.neutronSourceInitialCf252Grams} of Californium-
 * 252 -- see {@code MetaTileEntityPressurizedWaterReactor}'s own JavaDoc for why a real, depletable source
 * replaces this mod's earlier unconditional point-kinetics seeding placeholder. Decay is driven by the reactor
 * controller's own tick loop (see that class's neutron-source-decay method), not this item itself -- mirroring
 * how {@link ItemFuelRod} never advances its own burnup either.
 */
public class ItemNeutronSourceRod extends Item implements ITintedItem {

    private static final String NBT_ISOTOPES = "Isotopes";
    private static final String NBT_SYMBOL = "Symbol";
    private static final String NBT_AMOUNT = "Amount";

    /** See {@link ITintedItem}'s own JavaDoc -- only one instance of this class exists, but kept consistent. */
    private final int tintColor;

    public ItemNeutronSourceRod(@NotNull String translationKeySuffix, int tintColor) {
        this.tintColor = tintColor;
        setMaxStackSize(1);
        setTranslationKey("gtnuclear.neutron_source_rod." + translationKeySuffix);
        setCreativeTab(CreativeTabs.MISC);
    }

    @Override
    public int getTintColor() {
        return tintColor;
    }

    /**
     * @return {@code stack}'s current isotope inventory (absolute amounts), reading persisted NBT if present, or
     *         a fresh full-strength inventory otherwise. Never empty.
     */
    @NotNull
    public Map<Element, Double> getIsotopeAmounts(@NotNull ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null || !tag.hasKey(NBT_ISOTOPES)) return freshIsotopeAmounts();

        Map<Element, Double> amounts = new LinkedHashMap<>();
        NBTTagList list = tag.getTagList(NBT_ISOTOPES, Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound entryTag = list.getCompoundTagAt(i);
            Element element = Elements.get(entryTag.getString(NBT_SYMBOL));
            if (element != null) {
                amounts.put(element, entryTag.getDouble(NBT_AMOUNT));
            }
        }
        return amounts.isEmpty() ? freshIsotopeAmounts() : amounts;
    }

    /** Persists {@code amounts} as {@code stack}'s current isotope inventory. */
    public void setIsotopeAmounts(@NotNull ItemStack stack, @NotNull Map<Element, Double> amounts) {
        NBTTagList list = new NBTTagList();
        for (Map.Entry<Element, Double> entry : amounts.entrySet()) {
            NBTTagCompound entryTag = new NBTTagCompound();
            entryTag.setString(NBT_SYMBOL, entry.getKey().symbol);
            entryTag.setDouble(NBT_AMOUNT, entry.getValue());
            list.appendTag(entryTag);
        }
        stack.setTagInfo(NBT_ISOTOPES, list);
    }

    @NotNull
    private Map<Element, Double> freshIsotopeAmounts() {
        return Collections.singletonMap(GTNLElements.Cf252, ConfigHolder.reactor.neutronSourceInitialCf252Grams);
    }
}
