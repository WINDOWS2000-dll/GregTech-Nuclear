package com.windows2000.gtnuclear.api.unification.materials;

import gregtech.api.GregTechAPI;
import gregtech.api.unification.material.Material;
import gregtech.api.unification.material.event.MaterialEvent;
import gregtech.api.unification.material.event.MaterialRegistryEvent;

import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import com.windows2000.gtnuclear.api.GTNuclearValues;
import com.windows2000.gtnuclear.api.nuclear.thermal.CoolingWaterTemperatureBands;
import com.windows2000.gtnuclear.api.nuclear.thermal.ReactorCoolantTemperatureBands;
import com.windows2000.gtnuclear.api.unification.materials.reactor.ReactorFluidMaterials;
import com.windows2000.gtnuclear.api.unification.materials.series.ActiniumSeriesMaterials;
import com.windows2000.gtnuclear.api.unification.materials.series.NeptuniumSeriesMaterials;
import com.windows2000.gtnuclear.api.unification.materials.series.NeutronSourceMaterials;
import com.windows2000.gtnuclear.api.unification.materials.series.ReactorBurnupProductMaterials;
import com.windows2000.gtnuclear.api.unification.materials.series.ThoriumSeriesMaterials;
import com.windows2000.gtnuclear.api.unification.materials.series.UraniumSeriesMaterials;

public class GTNLMaterials {

    private GTNLMaterials() {}

    /*
     * Uranium series (U-238 decay chain), IDs 1-14. See UraniumSeriesMaterials for the actual registration and
     * nuclide data.
     */
    public static Material Thorium234;
    public static Material Protactinium234;
    public static Material Uranium234;
    public static Material Thorium230;
    public static Material Radium226;
    public static Material Radon222;
    public static Material Polonium218;
    public static Material Lead214;
    public static Material Bismuth214;
    public static Material Polonium214;
    public static Material Lead210;
    public static Material Bismuth210;
    public static Material Polonium210;
    public static Material Lead206;

    /*
     * Actinium series (U-235 decay chain), IDs 15-27. See ActiniumSeriesMaterials. Includes two branches at/above
     * the 0.1% inclusion threshold: Actinium-227's 1.38% alpha branch (-> Francium-223) and Bismuth-211's 0.276%
     * beta-minus branch (-> Polonium-211); both reconverge on the main chain (Ra-223 / Pb-207 respectively).
     */
    public static Material Thorium231;
    public static Material Protactinium231;
    public static Material Actinium227;
    public static Material Francium223;
    public static Material Thorium227;
    public static Material Radium223;
    public static Material Radon219;
    public static Material Polonium215;
    public static Material Lead211;
    public static Material Bismuth211;
    public static Material Polonium211;
    public static Material Thallium207;
    public static Material Lead207;

    /*
     * Thorium series (Th-232 decay chain), IDs 28-39. See ThoriumSeriesMaterials. Unlike the uranium/actinium
     * series, GTST has no existing isotope-flagged "Th-232" Element to build on (only the generic, non-isotope
     * "Thorium" used by GTST's own Materials.Thorium) -- Thorium232 here is a brand-new isotope Element/Material,
     * coexisting with GTST's plain Thorium exactly the way GTST's own generic Uranium/Plutonium coexist with their
     * specific Uranium238/Plutonium239 isotopes. No GTST changes needed for this series.
     */
    public static Material Thorium232;
    public static Material Radium228;
    public static Material Actinium228;
    public static Material Thorium228;
    public static Material Radium224;
    public static Material Radon220;
    public static Material Polonium216;
    public static Material Lead212;
    public static Material Bismuth212;
    public static Material Polonium212;
    public static Material Thallium208;
    public static Material Lead208;

    /*
     * Neptunium series (4n+1 chain, reached from Pu-241 -> Am-241 -> Np-237; already defined natively in GTST, see
     * Elements.Pu241's decayModes), IDs 40-53. See NeptuniumSeriesMaterials. Includes one branch at/above the 0.1%
     * inclusion threshold (Bismuth-213: 97.80% beta-minus, 2.20% alpha), reconverging on Pb-209 one step later.
     * Bismuth-209 (the chain's endpoint) is technically alpha-radioactive in reality (half-life ~2.01e19 years,
     * over a billion times the age of the universe) but is modeled as stable here -- see NeptuniumSeriesMaterials.
     */
    public static Material Americium241;
    public static Material Neptunium237;
    public static Material Protactinium233;
    public static Material Uranium233;
    public static Material Thorium229;
    public static Material Radium225;
    public static Material Actinium225;
    public static Material Francium221;
    public static Material Astatine217;
    public static Material Bismuth213;
    public static Material Polonium213;
    public static Material Thallium209;
    public static Material Lead209;
    public static Material Bismuth209;

    /*
     * PWR primary coolant loop, IDs 54-55. See ReactorFluidMaterials: ReactorCoolantWater carries the
     * REACTOR_COOLANT_WATER FluidAttribute (rejected by every ordinary GT fluid pipe automatically);
     * ReactorCoolantPipeAlloy is the dedicated high-throughput pipe material that explicitly declares support for
     * that attribute (and, being gas-proof, also carries ordinary Steam for the secondary loop).
     */
    public static Material ReactorCoolantWater;
    public static Material ReactorCoolantPipeAlloy;

    /*
     * Boron reactivity control fluids, IDs 56-57. See ReactorFluidMaterials. BoricAcidSolution is an ordinary GT
     * fluid (no dedicated pipe/attribute -- see this mod's design memory for why only coolant/wastewater needed
     * one). RadioactiveWastewater carries the RADIOACTIVE_WASTEWATER FluidAttribute and reuses
     * ReactorCoolantPipeAlloy as its dedicated pipe, for the same "ordinary pipes must reject this" reason as the
     * primary coolant itself.
     */
    public static Material BoricAcidSolution;
    public static Material RadioactiveWastewater;

    /*
     * Reactor burnup products (non-fissile neutron-capture terminal poisons), IDs 58-59. See
     * ReactorBurnupProductMaterials: Uranium236 (from U-235 capture-without-fission) and Plutonium240 (from
     * Pu-239 capture-without-fission), both with real half-life/decay data in addition to burnup-relevant
     * neutron cross-sections.
     */
    public static Material Uranium236;
    public static Material Plutonium240;

    /*
     * Startup neutron source isotopes, IDs 60-61. See NeutronSourceMaterials: Californium252 (real spontaneous-
     * fission startup source material, used by the dedicated neutron source hatch item) and Curium248 (its alpha-
     * decay daughter, modeled as stable -- see GTNLElements' own comment on that choice).
     */
    public static Material Californium252;
    public static Material Curium248;

    /**
     * Secondary loop steam output, ID 62. See ReactorFluidMaterials: produced by the heat exchanger's own
     * secondary-side boiling calculation, exported through the HIGH_PRESSURE_STEAM_EXPORT hatch -- a new fluid
     * rather than GTST's own Materials.Steam, per this mod's design memory (2026-09-22).
     */
    public static Material HighPressureSteam;

    /**
     * High-pressure turbine exhaust / low-pressure turbine inlet, ID 143. See {@code ReactorFluidMaterials} for
     * why 480K anchors the HP/LP turbine thermal-efficiency split.
     */
    public static Material LowPressureSteam;

    /**
     * Discrete primary coolant temperature bands, IDs 63-142 (81 bands, 5K spacing -- band 0 reuses the existing
     * {@link #ReactorCoolantWater} at ID54, preserving whatever recipe/source already produces it). See
     * {@link ReactorCoolantTemperatureBands}'s own JavaDoc for why this replaces a single continuously-tagged
     * fluid, and for why 5K spacing specifically. Indices correspond 1:1 with
     * {@link ReactorCoolantTemperatureBands#BAND_TEMPERATURES_KELVIN}.
     */
    public static Material[] ReactorCoolantWaterBands;

    /**
     * @return the registered coolant band fluid whose temperature is closest to {@code temperatureKelvin} -- see
     *         {@link ReactorCoolantTemperatureBands}'s own JavaDoc. The Forge-dependent glue around that class's
     *         pure {@code nearestBandIndex}.
     */
    public static Fluid nearestReactorCoolantBandFluid(double temperatureKelvin) {
        int index = ReactorCoolantTemperatureBands.nearestBandIndex(
                ReactorCoolantTemperatureBands.BAND_TEMPERATURES_KELVIN, temperatureKelvin);
        return ReactorCoolantWaterBands[index].getFluid();
    }

    /**
     * Non-radioactive cooling water temperature bands, IDs 144-158 (15 bands, 300-370 K, 5 K spacing; 159-199 are
     * reserved for future cooling-system fluids) -- see {@link CoolingWaterTemperatureBands}. Indices correspond
     * 1:1 with {@link CoolingWaterTemperatureBands#BAND_TEMPERATURES_KELVIN}.
     */
    public static Material[] CoolingWaterBands;

    /** The registered cooling water band fluid nearest {@code temperatureKelvin}. */
    public static Fluid nearestCoolingWaterBandFluid(double temperatureKelvin) {
        return CoolingWaterBands[CoolingWaterTemperatureBands.nearestBandIndex(temperatureKelvin)].getFluid();
    }

    @SubscribeEvent
    public static void onMaterialRegistryEvent(MaterialRegistryEvent event) {
        GregTechAPI.materialManager.createRegistry(GTNuclearValues.MODID);
    }

    @SubscribeEvent
    public static void onMaterialEvent(MaterialEvent event) {
        UraniumSeriesMaterials.register();
        ActiniumSeriesMaterials.register();
        ThoriumSeriesMaterials.register();
        NeptuniumSeriesMaterials.register();
        ReactorBurnupProductMaterials.register();
        NeutronSourceMaterials.register();
        ReactorFluidMaterials.register();
    }
}
