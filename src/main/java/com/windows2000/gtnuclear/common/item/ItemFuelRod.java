package com.windows2000.gtnuclear.common.item;

import gregtech.api.unification.Element;
import gregtech.api.unification.Elements;
import gregtech.api.unification.NeutronCrossSections;

import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagDouble;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;

import com.windows2000.gtnuclear.api.nuclear.fuel.FuelComposition;
import com.windows2000.gtnuclear.api.nuclear.neutronics.XenonIodineState;
import com.windows2000.gtnuclear.common.ConfigHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A fuel rod item carrying an evolving, NBT-tracked absolute isotope inventory ({@code Map<Element, Double>},
 * mirroring {@code MetaTileEntityIsotopeStorage}'s NBT shape) rather than a fixed composition -- one registered
 * {@code Item} instance per enrichment tier (see {@code GTNLFuelCompositions}), but each individual
 * {@link ItemStack} tracks its own depleting inventory as {@link com.windows2000.gtnuclear.api.nuclear.fuel.
 * FuelBurnupEngine} consumes it (wiring into the reactor's tick loop is a separate, not-yet-done task -- see this
 * mod's design memory). A stack with no NBT yet (freshly crafted, or an existing stack before this field existed)
 * is treated as full: {@link #getIsotopeAmounts} seeds it from {@link #initialComposition}'s mole fractions scaled
 * by {@code ConfigHolder.reactor.fuelRodUraniumMassKg}.
 * <p>
 * {@link #getIsotopeAmounts}/{@link #setIsotopeAmounts} are the only way anything should read or write a fuel
 * rod's current inventory -- {@code MetaTileEntityFuelPort#getLoadedComposition()} derives its
 * {@link FuelComposition} from {@link #getIsotopeAmounts} every call, not from {@link #initialComposition}, so a
 * rod's in-game neutronics performance degrades automatically as its real isotope makeup shifts.
 */
public class ItemFuelRod extends Item implements ITintedItem {

    private static final String NBT_ISOTOPES = "Isotopes";
    private static final String NBT_SYMBOL = "Symbol";
    private static final String NBT_AMOUNT = "Amount";
    private static final String NBT_LOCAL_IODINE = "LocalIodineConcentration";
    private static final String NBT_LOCAL_XENON = "LocalXenonConcentration";

    /** This rod type's fresh-from-the-factory enrichment tier -- see {@code GTNLFuelCompositions}. */
    @NotNull
    public final FuelComposition initialComposition;
    /** This tier's own tint color -- see {@link ITintedItem}'s own JavaDoc on why every tier shares one texture. */
    private final int tintColor;

    public ItemFuelRod(@NotNull FuelComposition initialComposition, @NotNull String translationKeySuffix,
                       int tintColor) {
        this.initialComposition = initialComposition;
        this.tintColor = tintColor;
        setMaxStackSize(1);
        setTranslationKey("gtnuclear.fuel_rod." + translationKeySuffix);
        setCreativeTab(CreativeTabs.MISC);
    }

    @Override
    public int getTintColor() {
        return tintColor;
    }

    /**
     * @return {@code stack}'s current isotope inventory (absolute amounts, e.g. kg), reading persisted NBT if
     *         present, or a fresh full inventory derived from {@link #initialComposition} otherwise. Never empty.
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

    /**
     * @param fallback this rod's own local xenon/iodine state if never tracked yet (a freshly crafted rod, or
     *                 one loaded before this mod tracked per-rod xenon) -- callers seed this from the
     *                 reactor's own previously-reactor-wide xenon state the first time a rod is read, so
     *                 introducing per-rod tracking does not discontinuously reset accumulated poisoning on an
     *                 existing save (see {@code MetaTileEntityPressurizedWaterReactor}'s own JavaDoc for where
     *                 this is called).
     * @return {@code stack}'s own locally-tracked xenon/iodine state, or {@code fallback} if never tracked yet
     */
    @NotNull
    public XenonIodineState getLocalXenonIodineState(@NotNull ItemStack stack, @NotNull XenonIodineState fallback) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null || !tag.hasKey(NBT_LOCAL_XENON)) return fallback;
        return new XenonIodineState(tag.getDouble(NBT_LOCAL_IODINE), tag.getDouble(NBT_LOCAL_XENON));
    }

    /** Persists {@code state} as {@code stack}'s own local xenon/iodine state. */
    public void setLocalXenonIodineState(@NotNull ItemStack stack, @NotNull XenonIodineState state) {
        stack.setTagInfo(NBT_LOCAL_IODINE, new NBTTagDouble(state.iodineConcentration()));
        stack.setTagInfo(NBT_LOCAL_XENON, new NBTTagDouble(state.xenonConcentration()));
    }

    @NotNull
    private Map<Element, Double> freshIsotopeAmounts() {
        double totalMassKg = ConfigHolder.reactor.fuelRodUraniumMassKg;
        Map<Element, Double> amounts = new LinkedHashMap<>();
        for (Map.Entry<Element, Double> entry : initialComposition.moleFractionByIsotope.entrySet()) {
            amounts.put(entry.getKey(), entry.getValue() * totalMassKg);
        }
        return amounts;
    }

    /**
     * No hard "fuel lifetime" threshold is modeled (2026-09 design decision, see this mod's design memory) -- a
     * rod is simply whatever fraction of its original fission-producing capability remains, which the reactor's
     * own neutronics already degrades smoothly as burnup proceeds. Weighted by each isotope's own fission cross-
     * sections ({@link NeutronCrossSections#thermalFissionBarns}/{@code fastFissionBarns}, the same data
     * {@link com.windows2000.gtnuclear.api.nuclear.fuel.FuelBurnupEngine} and {@link com.windows2000.gtnuclear.api.
     * nuclear.neutronics.ReactorNodeFactory} already use) rather than a flat "U-235 only" mass ratio, so this
     * naturally: (a) barely counts fertile U-238 (its own fission cross-sections are negligible), and (b) credits
     * bred Pu-239 back towards the total exactly as much as it actually contributes to fissioning -- a rod that
     * breeds a useful amount of plutonium as its U-235 depletes reads as "holding up better" than one that
     * doesn't, which is physically correct.
     *
     * @return current / fresh fission-weighted mass, clamped to {@code [0, 1]}; {@code 1.0} for a freshly-crafted
     *         rod, trending towards {@code 0.0} as fissile content is consumed
     */
    public double getFissileCapabilityFraction(@NotNull ItemStack stack) {
        double initial = fissionWeightedMass(freshIsotopeAmounts());
        if (initial <= 0) return 0.0;
        double current = fissionWeightedMass(getIsotopeAmounts(stack));
        return Math.max(0.0, Math.min(1.0, current / initial));
    }

    private static double fissionWeightedMass(@NotNull Map<Element, Double> amounts) {
        double total = 0.0;
        for (Map.Entry<Element, Double> entry : amounts.entrySet()) {
            NeutronCrossSections crossSections = entry.getKey().crossSections;
            if (crossSections == null) continue;
            total += entry.getValue() * (crossSections.thermalFissionBarns + crossSections.fastFissionBarns);
        }
        return total;
    }

    /**
     * Below this {@link #getFissileCapabilityFraction} threshold, {@link #addInformation} adds a replacement-
     * recommended tooltip line -- an implementer-chosen display threshold (2026-09 design memory explicitly left
     * this unspecified), not a hard gameplay cutoff; the rod keeps fissioning (at a correspondingly reduced rate)
     * below it exactly like above it.
     */
    private static final double REPLACEMENT_RECOMMENDED_THRESHOLD = 0.2;

    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag advanced) {
        super.addInformation(stack, world, tooltip, advanced);
        double remaining = getFissileCapabilityFraction(stack);
        TextFormatting color = remaining < REPLACEMENT_RECOMMENDED_THRESHOLD ? TextFormatting.RED :
                remaining < 0.5 ? TextFormatting.YELLOW : TextFormatting.GREEN;
        tooltip.add(color + I18n.format("gtnuclear.fuel_rod.tooltip.remaining", Math.round(remaining * 100)));
        if (remaining < REPLACEMENT_RECOMMENDED_THRESHOLD) {
            tooltip.add(TextFormatting.RED + "" + TextFormatting.ITALIC +
                    I18n.format("gtnuclear.fuel_rod.tooltip.replacement_recommended"));
        }
    }
}
