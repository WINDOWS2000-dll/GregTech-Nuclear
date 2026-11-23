package com.windows2000.gtnuclear.api.nuclear.fuel;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FuelAssemblyStateTest {

    private static final double EPS = 1e-12;

    private static Map<String, Double> leu() {
        Map<String, Double> fresh = new LinkedHashMap<>();
        fresh.put("U235", 0.045);
        fresh.put("U238", 0.955);
        return fresh;
    }

    @Test
    void freshStateFillsEveryLayer() {
        FuelAssemblyState state = FuelAssemblyState.fresh(17, leu());
        assertEquals(17, state.length());
        assertEquals(17 * 2, state.compositionArray().length);
        assertEquals(17 * FuelAssemblyState.POISON_COUNT, state.poisonArray().length);
        assertEquals(0.045, state.layerComposition(16).get("U235"), EPS);
        assertEquals(0.955, state.averageComposition().get("U238"), EPS);
    }

    @Test
    void rejectsNonPositiveLength() {
        assertThrows(IllegalArgumentException.class, () -> FuelAssemblyState.fresh(0, leu()));
    }

    @Test
    void setLayerAppendsNewNuclideAndAveragesOverLayers() {
        FuelAssemblyState state = FuelAssemblyState.fresh(4, leu());
        Map<String, Double> burned = new LinkedHashMap<>(leu());
        burned.put("U235", 0.025);
        burned.put("Pu239", 0.008);
        state.setLayerComposition(1, burned);

        assertEquals(List.of("U235", "U238", "Pu239"), state.nuclides());
        assertEquals(0.0, state.layerComposition(0).get("Pu239"), EPS);
        assertEquals(0.008, state.layerComposition(1).get("Pu239"), EPS);
        assertEquals(0.045, state.layerComposition(2).get("U235"), EPS);
        assertEquals((0.045 * 3 + 0.025) / 4, state.averageComposition().get("U235"), EPS);
        assertEquals(0.002, state.averageComposition().get("Pu239"), EPS);
    }

    @Test
    void setAllLayersOverwritesEverything() {
        FuelAssemblyState state = FuelAssemblyState.fresh(3, leu());
        state.setAllLayersComposition(Map.of("U235", 0.01));
        for (int layer = 0; layer < 3; layer++) {
            assertEquals(0.01, state.layerComposition(layer).get("U235"), EPS);
            assertEquals(0.0, state.layerComposition(layer).get("U238"), EPS);
        }
    }

    @Test
    void poisonsArePerLayer() {
        FuelAssemblyState state = FuelAssemblyState.fresh(2, leu());
        state.setPoison(0, FuelAssemblyState.POISON_XENON, 0.03);
        state.setPoison(1, FuelAssemblyState.POISON_XENON, 0.01);
        assertEquals(0.03, state.poison(0, FuelAssemblyState.POISON_XENON), EPS);
        assertEquals(0.02, state.averagePoison(FuelAssemblyState.POISON_XENON), EPS);
        assertEquals(0.0, state.averagePoison(FuelAssemblyState.POISON_IODINE), EPS);
    }

    @Test
    void restoreRoundTripsSavedArrays() {
        FuelAssemblyState original = FuelAssemblyState.fresh(5, leu());
        original.setLayerComposition(2, Map.of("U235", 0.02, "U238", 0.95, "Pu239", 0.005));
        original.setPoison(3, FuelAssemblyState.POISON_IODINE, 0.7);

        FuelAssemblyState restored = FuelAssemblyState.restore(5, FuelAssemblyState.CURRENT_VERSION,
                original.nuclides(), original.compositionArray(), original.poisonArray(), new double[0], 0, leu());
        assertEquals(original.nuclides(), restored.nuclides());
        assertEquals(0.005, restored.layerComposition(2).get("Pu239"), EPS);
        assertEquals(0.7, restored.poison(3, FuelAssemblyState.POISON_IODINE), EPS);
    }

    @Test
    void restoreFallsBackToFreshWhenMalformed() {
        FuelAssemblyState restored = FuelAssemblyState.restore(5, FuelAssemblyState.CURRENT_VERSION,
                List.of("U235", "U238"), new double[3], new double[0], new double[0], 0, leu());
        assertEquals(5, restored.length());
        assertEquals(0.045, restored.layerComposition(4).get("U235"), EPS);
    }

    @Test
    void restoreResetsPoisonsOnVersionChangeButKeepsComposition() {
        FuelAssemblyState original = FuelAssemblyState.fresh(2, leu());
        original.setLayerComposition(0, Map.of("U235", 0.03, "U238", 0.96));
        original.setPoison(0, FuelAssemblyState.POISON_XENON, 0.5);

        FuelAssemblyState restored = FuelAssemblyState.restore(2, FuelAssemblyState.CURRENT_VERSION - 1,
                original.nuclides(), original.compositionArray(), original.poisonArray(), new double[0], 0, leu());
        assertEquals(0.03, restored.layerComposition(0).get("U235"), EPS);
        assertEquals(0.0, restored.poison(0, FuelAssemblyState.POISON_XENON), EPS);
    }
}
