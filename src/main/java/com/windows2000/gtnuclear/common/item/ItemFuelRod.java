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
import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;

import com.windows2000.gtnuclear.api.nuclear.fuel.FuelAssemblyState;
import com.windows2000.gtnuclear.api.nuclear.fuel.FuelComposition;
import com.windows2000.gtnuclear.api.nuclear.fuel.PackedDoubles;
import com.windows2000.gtnuclear.api.nuclear.neutronics.XenonIodineState;
import com.windows2000.gtnuclear.common.ConfigHolder;
import com.windows2000.gtnuclear.common.metatileentities.multiblockpart.FuelAssemblyPortText;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A fuel assembly item (shown as "Fuel Assembly" -- the class keeps its historical name) -- one registered
 * {@code Item} per enrichment tier (see {@code GTNLFuelCompositions}), each stack carrying its own fabricated
 * length and per-layer state (2026-11-21, stage 1-3 of this mod's reactor physics rework, see this mod's design
 * memory and {@link FuelAssemblyState}'s own JavaDoc for the composition unit and versioning rules).
 * <p>
 * NBT layout: {@code Length} (layers, see {@link AssemblyLength}), {@code StateVersion}, {@code Nuclides} (symbol
 * header), {@code Composition}/{@code Poisons}/{@code DecayHeat} (layer-major doubles packed into
 * {@code NBTTagIntArray}s via {@link PackedDoubles}), {@code DecayHeatGroups}. A stack with a length but no state
 * yet is fresh (state is created lazily on first write). A stack without a length cannot be loaded at all.
 * <p>
 * Stage 1 (until per-node physics in stage 4): the reactor still evaluates one value per channel, so
 * {@link #setIsotopeAmounts}/{@link #setLocalXenonIodineState} write the same value into every layer and the
 * getters read layer averages.
 */
public class ItemFuelRod extends Item implements ITintedItem {

    private static final String NBT_STATE_VERSION = "StateVersion";
    private static final String NBT_NUCLIDES = "Nuclides";
    private static final String NBT_COMPOSITION = "Composition";
    private static final String NBT_POISONS = "Poisons";
    private static final String NBT_DECAY_HEAT = "DecayHeat";
    private static final String NBT_DECAY_HEAT_GROUPS = "DecayHeatGroups";

    /** Mass ratio UO2 / U (270 / 238) -- converts heavy-metal mass into fuel (oxide) mass for heat capacity. */
    public static final double UO2_TO_URANIUM_MASS_RATIO = 270.0 / 238.0;

    /** This assembly type's fresh-from-the-factory enrichment tier -- see {@code GTNLFuelCompositions}. */
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

    @Override
    public void getSubItems(@NotNull CreativeTabs tab, @NotNull NonNullList<ItemStack> items) {
        if (!isInCreativeTab(tab)) return;
        for (int layers : AssemblyLength.CREATIVE_TAB_LENGTHS) {
            items.add(AssemblyLength.withLength(new ItemStack(this), layers));
        }
    }

    /** Fresh composition by symbol ({@code n_k} of a never-irradiated layer). */
    @NotNull
    public Map<String, Double> freshFractions() {
        Map<String, Double> fractions = new LinkedHashMap<>();
        double total = 0.0;
        for (double value : initialComposition.moleFractionByIsotope.values()) total += value;
        for (Map.Entry<Element, Double> entry : initialComposition.moleFractionByIsotope.entrySet()) {
            fractions.put(entry.getKey().symbol, total > 0 ? entry.getValue() / total : 0.0);
        }
        return fractions;
    }

    /** U-235 atom fraction of a fresh layer, for display. */
    public double freshU235Fraction() {
        return freshFractions().getOrDefault(Elements.U235.symbol, 0.0);
    }

    /** @return this stack's state, or {@code null} if it has no fabricated length (not loadable). */
    @Nullable
    public FuelAssemblyState getState(@NotNull ItemStack stack) {
        int length = AssemblyLength.get(stack);
        if (length <= 0) return null;
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null || !tag.hasKey(NBT_COMPOSITION)) return FuelAssemblyState.fresh(length, freshFractions());

        List<String> nuclides = new ArrayList<>();
        NBTTagList list = tag.getTagList(NBT_NUCLIDES, Constants.NBT.TAG_STRING);
        for (int i = 0; i < list.tagCount(); i++) nuclides.add(list.getStringTagAt(i));
        double[] decayHeat = tag.hasKey(NBT_DECAY_HEAT, Constants.NBT.TAG_INT_ARRAY) ?
                PackedDoubles.unpackFloats(tag.getIntArray(NBT_DECAY_HEAT)) : new double[0];
        return FuelAssemblyState.restore(length, tag.getInteger(NBT_STATE_VERSION), nuclides,
                unpack(tag, NBT_COMPOSITION), unpack(tag, NBT_POISONS), decayHeat,
                tag.getInteger(NBT_DECAY_HEAT_GROUPS), freshFractions());
    }

    /** Persists {@code state} into {@code stack} (keeping its own {@code Length}). */
    public void setState(@NotNull ItemStack stack, @NotNull FuelAssemblyState state) {
        if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
        NBTTagCompound tag = stack.getTagCompound();
        tag.setInteger(NBT_STATE_VERSION, FuelAssemblyState.CURRENT_VERSION);
        NBTTagList nuclides = new NBTTagList();
        for (String symbol : state.nuclides()) nuclides.appendTag(new NBTTagString(symbol));
        tag.setTag(NBT_NUCLIDES, nuclides);
        tag.setTag(NBT_COMPOSITION, new NBTTagIntArray(PackedDoubles.pack(state.compositionArray())));
        tag.setTag(NBT_POISONS, new NBTTagIntArray(PackedDoubles.pack(state.poisonArray())));
        // Decay heat groups are saved single precision (stage 2-3a, decision F1 -- see PackedDoubles#packFloats).
        tag.setTag(NBT_DECAY_HEAT, new NBTTagIntArray(PackedDoubles.packFloats(state.decayHeatArray())));
        tag.setInteger(NBT_DECAY_HEAT_GROUPS, state.decayHeatGroups());
    }

    @NotNull
    private static double[] unpack(@NotNull NBTTagCompound tag, @NotNull String key) {
        if (!tag.hasKey(key, Constants.NBT.TAG_INT_ARRAY)) return new double[0];
        return PackedDoubles.unpack(tag.getIntArray(key));
    }

    /**
     * @return the layer-averaged composition ({@code n_k}, atoms per initial heavy-metal atom) mapped to
     *         {@link Element}s, or a fresh composition for a stack without a length. Callers using this as
     *         "amounts" (e.g. {@code FuelBurnupEngine}) are unit-agnostic, so the dimensionless {@code n_k} works
     *         unchanged.
     */
    @NotNull
    public Map<Element, Double> getIsotopeAmounts(@NotNull ItemStack stack) {
        return isotopeAmounts(getState(stack));
    }

    /** {@link #getIsotopeAmounts} from an already-loaded state (stage 2-3a: the port keeps it in memory). */
    @NotNull
    public Map<Element, Double> isotopeAmounts(@Nullable FuelAssemblyState state) {
        Map<String, Double> bySymbol = state != null ? state.averageComposition() : freshFractions();
        Map<Element, Double> amounts = new LinkedHashMap<>();
        for (Map.Entry<String, Double> entry : bySymbol.entrySet()) {
            Element element = Elements.get(entry.getKey());
            if (element != null) amounts.put(element, entry.getValue());
        }
        return amounts;
    }

    /** Writes {@code amounts} ({@code n_k}) into every layer -- stage 1's one-value-per-channel interim. */
    public void setIsotopeAmounts(@NotNull ItemStack stack, @NotNull Map<Element, Double> amounts) {
        FuelAssemblyState state = getState(stack);
        if (state == null) return;
        applyIsotopeAmounts(state, amounts);
        setState(stack, state);
    }

    /** {@link #setIsotopeAmounts} on an in-memory state (stage 2-3a). */
    public static void applyIsotopeAmounts(@NotNull FuelAssemblyState state, @NotNull Map<Element, Double> amounts) {
        Map<String, Double> bySymbol = new LinkedHashMap<>();
        for (Map.Entry<Element, Double> entry : amounts.entrySet()) bySymbol.put(entry.getKey().symbol,
                entry.getValue());
        state.setAllLayersComposition(bySymbol);
    }

    /** Layer-averaged xenon/iodine of an in-memory state (stage 2-3a). */
    @NotNull
    public static XenonIodineState xenonIodineState(@NotNull FuelAssemblyState state) {
        return new XenonIodineState(state.averagePoison(FuelAssemblyState.POISON_IODINE),
                state.averagePoison(FuelAssemblyState.POISON_XENON));
    }

    /** Writes {@code xenonState} into every layer of an in-memory state (stage 2-3a). */
    public static void applyXenonIodineState(@NotNull FuelAssemblyState state, @NotNull XenonIodineState xenonState) {
        for (int layer = 0; layer < state.length(); layer++) {
            state.setPoison(layer, FuelAssemblyState.POISON_IODINE, xenonState.iodineConcentration());
            state.setPoison(layer, FuelAssemblyState.POISON_XENON, xenonState.xenonConcentration());
        }
    }

    /**
     * @param fallback returned for a stack whose poisons have never been written (all zero is a legitimate fresh
     *                 state, so "never written" is detected by the absence of any composition state at all)
     * @return the layer-averaged xenon/iodine state
     */
    @NotNull
    public XenonIodineState getLocalXenonIodineState(@NotNull ItemStack stack, @NotNull XenonIodineState fallback) {
        NBTTagCompound tag = stack.getTagCompound();
        FuelAssemblyState state = getState(stack);
        if (state == null || tag == null || !tag.hasKey(NBT_COMPOSITION)) return fallback;
        return new XenonIodineState(state.averagePoison(FuelAssemblyState.POISON_IODINE),
                state.averagePoison(FuelAssemblyState.POISON_XENON));
    }

    /** Writes {@code xenonState} into every layer -- stage 1's one-value-per-channel interim. */
    public void setLocalXenonIodineState(@NotNull ItemStack stack, @NotNull XenonIodineState xenonState) {
        FuelAssemblyState state = getState(stack);
        if (state == null) return;
        for (int layer = 0; layer < state.length(); layer++) {
            state.setPoison(layer, FuelAssemblyState.POISON_IODINE, xenonState.iodineConcentration());
            state.setPoison(layer, FuelAssemblyState.POISON_XENON, xenonState.xenonConcentration());
        }
        setState(stack, state);
    }

    /** Initial uranium (heavy metal) mass of the whole assembly (kg). */
    public double initialUraniumMassKg(@NotNull ItemStack stack) {
        return AssemblyLength.get(stack) * ConfigHolder.reactor.fuelUraniumPerLayerKg;
    }

    /** Current mass (kg) of one nuclide in the whole assembly, from its {@code n_k} and the fresh heavy-metal mass. */
    public double isotopeMassKg(@NotNull ItemStack stack, @NotNull Element element) {
        FuelAssemblyState state = getState(stack);
        if (state == null) return 0.0;
        double freshMeanMass = 0.0;
        for (Map.Entry<String, Double> entry : freshFractions().entrySet()) {
            Element fresh = Elements.get(entry.getKey());
            if (fresh != null) freshMeanMass += entry.getValue() * fresh.getMass();
        }
        if (freshMeanMass <= 0) return 0.0;
        double atomsPerInitialHeavyMetalAtom = state.averageComposition().getOrDefault(element.symbol, 0.0);
        return initialUraniumMassKg(stack) * atomsPerInitialHeavyMetalAtom * element.getMass() / freshMeanMass;
    }

    /**
     * Every tracked nuclide's current mass in grams, keyed by symbol -- {@link #isotopeMassKg} for all of them at
     * once, reading the NBT state a single time (used for the fuel's own spontaneous fission source, stage 2-1).
     */
    @NotNull
    public Map<String, Double> isotopeMassesGrams(@NotNull ItemStack stack) {
        return isotopeMassesGrams(getState(stack));
    }

    /** {@link #isotopeMassesGrams(ItemStack)} from an in-memory state (stage 2-3a). */
    @NotNull
    public Map<String, Double> isotopeMassesGrams(@Nullable FuelAssemblyState state) {
        Map<String, Double> masses = new java.util.HashMap<>();
        if (state == null) return masses;
        double freshMeanMass = 0.0;
        for (Map.Entry<String, Double> entry : freshFractions().entrySet()) {
            Element fresh = Elements.get(entry.getKey());
            if (fresh != null) freshMeanMass += entry.getValue() * fresh.getMass();
        }
        if (freshMeanMass <= 0) return masses;
        double initialGrams = state.length() * ConfigHolder.reactor.fuelUraniumPerLayerKg * 1000.0;
        for (Map.Entry<String, Double> entry : state.averageComposition().entrySet()) {
            Element element = Elements.get(entry.getKey());
            if (element == null || entry.getValue() <= 0) continue;
            masses.put(entry.getKey(), initialGrams * entry.getValue() * element.getMass() / freshMeanMass);
        }
        return masses;
    }

    /**
     * No hard "fuel lifetime" threshold is modeled (2026-09 design decision, see this mod's design memory) -- an
     * assembly is simply whatever fraction of its original fission-producing capability remains. Weighted by each
     * nuclide's own fission cross-sections, so fertile U-238 barely counts and bred Pu-239 is credited.
     *
     * @return current / fresh fission-weighted content, clamped to {@code [0, 1]}
     */
    public double getFissileCapabilityFraction(@NotNull ItemStack stack) {
        return fissileCapabilityFraction(getState(stack));
    }

    /** {@link #getFissileCapabilityFraction} from an in-memory state (stage 2-3a). */
    public double fissileCapabilityFraction(@Nullable FuelAssemblyState state) {
        double initial = fissionWeighted(freshFractions());
        if (initial <= 0) return 0.0;
        double current = fissionWeighted(state != null ? state.averageComposition() : freshFractions());
        return Math.max(0.0, Math.min(1.0, current / initial));
    }

    private static double fissionWeighted(@NotNull Map<String, Double> bySymbol) {
        double total = 0.0;
        for (Map.Entry<String, Double> entry : bySymbol.entrySet()) {
            Element element = Elements.get(entry.getKey());
            if (element == null) continue;
            NeutronCrossSections crossSections = element.crossSections;
            if (crossSections == null) continue;
            total += entry.getValue() * (crossSections.thermalFissionBarns + crossSections.fastFissionBarns);
        }
        return total;
    }

    /** Display threshold below which the tooltip recommends replacement -- not a gameplay cutoff. */
    private static final double REPLACEMENT_RECOMMENDED_THRESHOLD = 0.2;

    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag advanced) {
        super.addInformation(stack, world, tooltip, advanced);
        int length = AssemblyLength.get(stack);
        if (length <= 0) {
            tooltip.add(TextFormatting.RED + I18n.format("gtnuclear.assembly.tooltip.no_length"));
            return;
        }
        tooltip.add(TextFormatting.GRAY + I18n.format("gtnuclear.assembly.tooltip.length",
                FuelAssemblyPortText.length(length)));
        tooltip.add(TextFormatting.GRAY + I18n.format("gtnuclear.fuel_rod.tooltip.uranium",
                FuelAssemblyPortText.kilograms(initialUraniumMassKg(stack))));
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
