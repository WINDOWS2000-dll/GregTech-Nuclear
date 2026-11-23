package com.windows2000.gtnuclear.api.nuclear.fuel;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One fuel assembly's per-axial-layer state (2026-11-21, stage 1-3 (c) of this mod's reactor physics rework, see
 * this mod's design memory). Pure Java -- the NBT encoding lives in {@code ItemFuelRod}.
 * <p>
 * <b>Composition unit</b>: {@code n_k = N_k / N_HM,0}, atoms of nuclide {@code k} per <i>initial</i> heavy-metal
 * atom of the same layer. A fresh LEU layer reads {@code U235 = 0.045, U238 = 0.955}; nuclides absent when fresh
 * (Pu-239, U-236, ...) are representable without dividing by a zero initial amount. The absolute amount of any
 * nuclide is {@code n_k} times that layer's own initial heavy-metal atom count, fixed by the assembly geometry.
 * <p>
 * <b>Layout</b>: three flat layer-major arrays -- composition ({@code length * nuclides().size()}), poisons
 * ({@code length * POISON_COUNT}) and decay heat ({@code length * decayHeatGroups}). Nuclides are identified by
 * symbol (a header list) so new nuclides can be added later without breaking older saves.
 * <p>
 * <b>Versioning</b>: poison and decay-heat state are model-dependent (stage 2 changes decay heat, stage 4 turns
 * xenon from reactivity units into number densities), so a saved state whose {@code version} differs from
 * {@link #CURRENT_VERSION} keeps its composition but resets poisons and decay heat to zero.
 */
public final class FuelAssemblyState {

    /** Bump whenever the meaning of {@link #poisons} or {@link #decayHeat} changes. */
    public static final int CURRENT_VERSION = 1;

    public static final int POISON_IODINE = 0;
    public static final int POISON_XENON = 1;
    public static final int POISON_COUNT = 2;

    private final int length;
    private final List<String> nuclides;
    private double[] composition;
    private final double[] poisons;
    private final double[] decayHeat;
    private final int decayHeatGroups;

    private FuelAssemblyState(int length, @NotNull List<String> nuclides, @NotNull double[] composition,
                              @NotNull double[] poisons, @NotNull double[] decayHeat, int decayHeatGroups) {
        this.length = length;
        this.nuclides = nuclides;
        this.composition = composition;
        this.poisons = poisons;
        this.decayHeat = decayHeat;
        this.decayHeatGroups = decayHeatGroups;
    }

    /** A fresh assembly of {@code length} layers, every layer holding {@code freshFractions}. */
    @NotNull
    public static FuelAssemblyState fresh(int length, @NotNull Map<String, Double> freshFractions) {
        if (length <= 0) throw new IllegalArgumentException("length must be positive, was " + length);
        List<String> nuclides = new ArrayList<>(freshFractions.keySet());
        double[] composition = new double[length * nuclides.size()];
        for (int layer = 0; layer < length; layer++) {
            for (int k = 0; k < nuclides.size(); k++) {
                composition[layer * nuclides.size() + k] = freshFractions.get(nuclides.get(k));
            }
        }
        return new FuelAssemblyState(length, nuclides, composition, new double[length * POISON_COUNT],
                new double[0], 0);
    }

    /**
     * Restores a saved state, falling back to {@link #fresh} if the saved arrays are malformed (wrong sizes) or
     * missing, and resetting poisons/decay heat if {@code version != CURRENT_VERSION} -- see this class's own
     * JavaDoc.
     */
    @NotNull
    public static FuelAssemblyState restore(int length, int version, @NotNull List<String> nuclides,
                                            @NotNull double[] composition, @NotNull double[] poisons,
                                            @NotNull double[] decayHeat, int decayHeatGroups,
                                            @NotNull Map<String, Double> freshFractions) {
        if (length <= 0 || nuclides.isEmpty() || composition.length != length * nuclides.size()) {
            return fresh(Math.max(length, 1), freshFractions);
        }
        boolean sameVersion = version == CURRENT_VERSION;
        double[] restoredPoisons = sameVersion && poisons.length == length * POISON_COUNT ? poisons.clone() :
                new double[length * POISON_COUNT];
        boolean decayValid = sameVersion && decayHeatGroups >= 0 && decayHeat.length == length * decayHeatGroups;
        return new FuelAssemblyState(length, new ArrayList<>(nuclides), composition.clone(), restoredPoisons,
                decayValid ? decayHeat.clone() : new double[0], decayValid ? decayHeatGroups : 0);
    }

    public int length() {
        return length;
    }

    @NotNull
    public List<String> nuclides() {
        return Collections.unmodifiableList(nuclides);
    }

    @NotNull
    public double[] compositionArray() {
        return composition.clone();
    }

    @NotNull
    public double[] poisonArray() {
        return poisons.clone();
    }

    @NotNull
    public double[] decayHeatArray() {
        return decayHeat.clone();
    }

    public int decayHeatGroups() {
        return decayHeatGroups;
    }

    /** One layer's composition ({@code n_k} by symbol). */
    @NotNull
    public Map<String, Double> layerComposition(int layer) {
        checkLayer(layer);
        Map<String, Double> result = new LinkedHashMap<>();
        for (int k = 0; k < nuclides.size(); k++) {
            result.put(nuclides.get(k), composition[layer * nuclides.size() + k]);
        }
        return result;
    }

    /** Layer-averaged composition ({@code n_k} by symbol). */
    @NotNull
    public Map<String, Double> averageComposition() {
        Map<String, Double> result = new LinkedHashMap<>();
        for (int k = 0; k < nuclides.size(); k++) {
            double sum = 0.0;
            for (int layer = 0; layer < length; layer++) sum += composition[layer * nuclides.size() + k];
            result.put(nuclides.get(k), sum / length);
        }
        return result;
    }

    /**
     * Overwrites one layer's composition. Nuclides not yet in the header are appended (every other layer reads
     * them as 0); nuclides in the header but absent from {@code values} are set to 0 for this layer.
     */
    public void setLayerComposition(int layer, @NotNull Map<String, Double> values) {
        checkLayer(layer);
        for (String symbol : values.keySet()) {
            if (!nuclides.contains(symbol)) appendNuclide(symbol);
        }
        for (int k = 0; k < nuclides.size(); k++) {
            composition[layer * nuclides.size() + k] = values.getOrDefault(nuclides.get(k), 0.0);
        }
    }

    /** Applies {@link #setLayerComposition} to every layer. */
    public void setAllLayersComposition(@NotNull Map<String, Double> values) {
        for (int layer = 0; layer < length; layer++) setLayerComposition(layer, values);
    }

    public double poison(int layer, int index) {
        checkLayer(layer);
        return poisons[layer * POISON_COUNT + index];
    }

    public void setPoison(int layer, int index, double value) {
        checkLayer(layer);
        poisons[layer * POISON_COUNT + index] = value;
    }

    /** Layer-averaged value of one poison entry. */
    public double averagePoison(int index) {
        double sum = 0.0;
        for (int layer = 0; layer < length; layer++) sum += poisons[layer * POISON_COUNT + index];
        return sum / length;
    }

    private void appendNuclide(@NotNull String symbol) {
        int oldCount = nuclides.size();
        nuclides.add(symbol);
        double[] expanded = new double[length * (oldCount + 1)];
        for (int layer = 0; layer < length; layer++) {
            System.arraycopy(composition, layer * oldCount, expanded, layer * (oldCount + 1), oldCount);
        }
        composition = expanded;
    }

    private void checkLayer(int layer) {
        if (layer < 0 || layer >= length) {
            throw new IndexOutOfBoundsException("layer " + layer + " out of range [0, " + length + ")");
        }
    }
}
