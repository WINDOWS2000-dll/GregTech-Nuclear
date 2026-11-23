package com.windows2000.gtnuclear.common.metatileentities;

import gregtech.api.GregTechAPI;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.unification.material.Material;
import gregtech.api.unification.material.Materials;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.IFluidTank;

import com.windows2000.gtnuclear.api.GTNuclearValues;
import com.windows2000.gtnuclear.api.metatileentity.multiblock.GTNLMultiblockAbility;
import com.windows2000.gtnuclear.api.unification.materials.GTNLMaterials;
import com.windows2000.gtnuclear.common.metatileentities.multi.MetaTileEntityControlConsole;
import com.windows2000.gtnuclear.common.metatileentities.multi.MetaTileEntityHeatExchanger;
import com.windows2000.gtnuclear.common.metatileentities.multi.MetaTileEntityHighPressureSteamTurbine;
import com.windows2000.gtnuclear.common.metatileentities.multi.MetaTileEntityLowPressureSteamTurbine;
import com.windows2000.gtnuclear.common.metatileentities.multi.MetaTileEntityPressurizedWaterReactor;
import com.windows2000.gtnuclear.common.metatileentities.multiblockpart.MetaTileEntityConsoleScreen;
import com.windows2000.gtnuclear.common.metatileentities.multiblockpart.MetaTileEntityFuelAssemblyPort;
import com.windows2000.gtnuclear.common.metatileentities.multiblockpart.MetaTileEntityNeutronSourceHatch;
import com.windows2000.gtnuclear.common.metatileentities.multiblockpart.MetaTileEntityReactorFluidPort;
import com.windows2000.gtnuclear.common.metatileentities.multiblockpart.MetaTileEntityReactorLinkHatch;
import com.windows2000.gtnuclear.common.metatileentities.multiblockpart.MetaTileEntityTurbineRotorHolder;
import com.windows2000.gtnuclear.common.metatileentities.storage.MetaTileEntityIsotopeStorage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.windows2000.gtnuclear.api.util.GTNLUtils.gtnlID;
import static com.windows2000.gtnuclear.common.metatileentities.GTNLMetaTileEntities.*;

/**
 * Actually constructs and registers this mod's MetaTileEntities into its own MTE registry (created earlier by
 * {@link GTNLMTERegistryHandler}). Called from {@code CoreModule#preInit}, mirroring how
 * {@code GTNLMetaBlocks.init()} is already called there -- unlike material/MTE-registry creation, individual MTE
 * registration has no dedicated addon-hook event and has no ordering constraint against GT's own preInit finishing
 * first.
 */
public class GTNLMetaTileEntityRegistration {

    private GTNLMetaTileEntityRegistration() {}

    public static void register() {
        ResourceLocation isotopeStorageId = gtnlID("isotope_storage");
        ISOTOPE_STORAGE = new MetaTileEntityIsotopeStorage(isotopeStorageId);
        GregTechAPI.mteManager.getRegistry(GTNuclearValues.MODID).register(0, isotopeStorageId, ISOTOPE_STORAGE);

        ResourceLocation pressurizedWaterReactorId = gtnlID("pressurized_water_reactor");
        PRESSURIZED_WATER_REACTOR = new MetaTileEntityPressurizedWaterReactor(pressurizedWaterReactorId);
        GregTechAPI.mteManager.getRegistry(GTNuclearValues.MODID)
                .register(1, pressurizedWaterReactorId, PRESSURIZED_WATER_REACTOR);

        // PWR reactor dedicated fluid ports. Capacities are placeholders (bulk-flow ports larger than the
        // auxiliary/letdown ones) pending playtesting-driven tuning.
        // Primary coolant ports accept any of GTNLMaterials.ReactorCoolantWaterBands -- see
        // ReactorCoolantTemperatureBands's own JavaDoc for why the primary loop's fluid transport is quantized
        // into several discrete-temperature fluids rather than a single fluid carrying a continuous NBT tag.
        Collection<Fluid> reactorCoolantBandFluids = Arrays.stream(GTNLMaterials.ReactorCoolantWaterBands)
                .map(Material::getFluid).collect(Collectors.toList());
        // Primary coolant/emergency coolant hatch capacities revised 2026-09-24 (real-playtest-driven, see this
        // mod's design memory) alongside ConfigHolder.reactor.primaryCirculationRateKgPerSecond's own real-PWR-
        // driven increase (500.0 -> 27222.4, a 54.4448x jump): a real playtest hit LOCA (coolantMassKg dropping
        // below locaCoolantFraction of the now much larger rated mass) because these tank capacities, left at
        // their old values, could not physically hold even one tick's worth of the new circulation rate
        // (27222.4 kg/s * 0.05s =~ 1361 kg =~ 1,361,120 mb per tick, versus the old 256,000 mb basic hatch
        // capacity) -- fill() was silently capping transfers far below the intended rate. Scaled up by the same
        // 54.4448x factor as the circulation rate itself, preserving each hatch's original "how many seconds of
        // full-rate flow can this buffer" ratio.
        REACTOR_COOLANT_IMPORT_HATCH = registerFluidPort(2, "reactor_coolant_import_hatch", false,
                GTNLMultiblockAbility.REACTOR_COOLANT_IMPORT, reactorCoolantBandFluids, 25000000);
        REACTOR_COOLANT_EXPORT_HATCH = registerFluidPort(3, "reactor_coolant_export_hatch", true,
                GTNLMultiblockAbility.REACTOR_COOLANT_EXPORT, reactorCoolantBandFluids, 25000000);
        // Sized (2026-09-26, alongside boronAdjustmentDurationMinutes's own switch to a time-based rate -- see
        // that config field's own comment) against the reference reactor's own derived rate at the default
        // 25-minute duration (computeRatedCoolantMassKg() / (25*60) =~ 297.06 kg/s =~ 297059 mb/s) so these
        // buffers' own turnover time (buffer / flow rate, ~58s import / ~14.5s export, preserved from this same
        // sizing exercise's earlier iteration) stays about the same, rather than the export hatch's headroom
        // collapsing to zero within a fraction of a second and silently throttling updateBoronAdjustment() back
        // down.
        BORIC_ACID_IMPORT_HATCH = registerFluidPort(4, "boric_acid_import_hatch", false,
                GTNLMultiblockAbility.BORIC_ACID_IMPORT, GTNLMaterials.BoricAcidSolution.getFluid(), 18000000);
        PURE_WATER_IMPORT_HATCH = registerFluidPort(5, "pure_water_import_hatch", false,
                GTNLMultiblockAbility.PURE_WATER_IMPORT, Materials.DistilledWater.getFluid(), 18000000);
        EMERGENCY_COOLANT_IMPORT_HATCH = registerFluidPort(6, "emergency_coolant_import_hatch", false,
                GTNLMultiblockAbility.EMERGENCY_COOLANT_IMPORT, reactorCoolantBandFluids, 7000000);
        RADIOACTIVE_WASTEWATER_EXPORT_HATCH = registerFluidPort(7, "radioactive_wastewater_export_hatch", true,
                GTNLMultiblockAbility.RADIOACTIVE_WASTEWATER_EXPORT, GTNLMaterials.RadioactiveWastewater.getFluid(),
                4309333);
        ADVANCED_COOLANT_IMPORT_HATCH = registerFluidPort(8, "advanced_reactor_coolant_import_hatch", false,
                GTNLMultiblockAbility.REACTOR_COOLANT_IMPORT, reactorCoolantBandFluids, 50000000);
        ADVANCED_COOLANT_EXPORT_HATCH = registerFluidPort(9, "advanced_reactor_coolant_export_hatch", true,
                GTNLMultiblockAbility.REACTOR_COOLANT_EXPORT, reactorCoolantBandFluids, 50000000);

        // Stage 1-3 (2026-11-21): the former fuel_port_hatch (ID 10) and control_rod_controller_hatch (ID 11)
        // were merged into one fuel assembly port, which takes over ID 10. ID 11 is retired -- do not reuse it.
        ResourceLocation fuelAssemblyPortId = gtnlID("fuel_assembly_port");
        FUEL_ASSEMBLY_PORT = new MetaTileEntityFuelAssemblyPort(fuelAssemblyPortId);
        GregTechAPI.mteManager.getRegistry(GTNuclearValues.MODID).register(10, fuelAssemblyPortId,
                FUEL_ASSEMBLY_PORT);
        MultiblockAbility.registerMultiblockAbility(GTNLMultiblockAbility.FUEL_ASSEMBLY_PORT, FUEL_ASSEMBLY_PORT);

        ResourceLocation linkTransmitterId = gtnlID("reactor_link_transmitter_hatch");
        REACTOR_LINK_TRANSMITTER_HATCH = new MetaTileEntityReactorLinkHatch(linkTransmitterId, true);
        GregTechAPI.mteManager.getRegistry(GTNuclearValues.MODID)
                .register(12, linkTransmitterId, REACTOR_LINK_TRANSMITTER_HATCH);
        MultiblockAbility.registerMultiblockAbility(GTNLMultiblockAbility.REACTOR_LINK_TRANSMITTER,
                REACTOR_LINK_TRANSMITTER_HATCH);

        ResourceLocation linkReceiverId = gtnlID("reactor_link_receiver_hatch");
        REACTOR_LINK_RECEIVER_HATCH = new MetaTileEntityReactorLinkHatch(linkReceiverId, false);
        GregTechAPI.mteManager.getRegistry(GTNuclearValues.MODID)
                .register(13, linkReceiverId, REACTOR_LINK_RECEIVER_HATCH);
        MultiblockAbility.registerMultiblockAbility(GTNLMultiblockAbility.REACTOR_LINK_RECEIVER,
                REACTOR_LINK_RECEIVER_HATCH);

        ResourceLocation consoleScreenId = gtnlID("console_screen");
        CONSOLE_SCREEN = new MetaTileEntityConsoleScreen(consoleScreenId);
        GregTechAPI.mteManager.getRegistry(GTNuclearValues.MODID).register(14, consoleScreenId, CONSOLE_SCREEN);
        MultiblockAbility.registerMultiblockAbility(GTNLMultiblockAbility.CONSOLE_SCREEN, CONSOLE_SCREEN);

        ResourceLocation controlConsoleId = gtnlID("control_console");
        CONTROL_CONSOLE = new MetaTileEntityControlConsole(controlConsoleId);
        GregTechAPI.mteManager.getRegistry(GTNuclearValues.MODID).register(15, controlConsoleId, CONTROL_CONSOLE);

        ResourceLocation neutronSourceHatchId = gtnlID("neutron_source_hatch");
        NEUTRON_SOURCE_HATCH = new MetaTileEntityNeutronSourceHatch(neutronSourceHatchId);
        GregTechAPI.mteManager.getRegistry(GTNuclearValues.MODID)
                .register(16, neutronSourceHatchId, NEUTRON_SOURCE_HATCH);
        MultiblockAbility.registerMultiblockAbility(GTNLMultiblockAbility.NEUTRON_SOURCE_PORT, NEUTRON_SOURCE_HATCH);

        // Heat exchanger secondary-side fluid ports and controller -- primary side reuses
        // REACTOR_COOLANT_IMPORT/EXPORT_HATCH (IDs 2/3) registered above, no dedicated hatch of its own.
        FEEDWATER_IMPORT_HATCH = registerFluidPort(17, "feedwater_import_hatch", false,
                GTNLMultiblockAbility.FEEDWATER_IMPORT, Materials.Water.getFluid(), 12800000);
        HIGH_PRESSURE_STEAM_EXPORT_HATCH = registerFluidPort(18, "high_pressure_steam_export_hatch", true,
                GTNLMultiblockAbility.HIGH_PRESSURE_STEAM_EXPORT, GTNLMaterials.HighPressureSteam.getFluid(), 12800000);

        ResourceLocation heatExchangerId = gtnlID("heat_exchanger");
        HEAT_EXCHANGER = new MetaTileEntityHeatExchanger(heatExchangerId);
        GregTechAPI.mteManager.getRegistry(GTNuclearValues.MODID).register(19, heatExchangerId, HEAT_EXCHANGER);

        // HP/LP turbine stages -- see MetaTileEntityHighPressureSteamTurbine/MetaTileEntityLowPressureSteamTurbine's
        // own JavaDoc for the two-stage design. Capacities are placeholders (same bulk-flow sizing rationale as
        // the reactor's own primary coolant hatches) pending playtesting.
        HIGH_PRESSURE_STEAM_IMPORT_HATCH = registerFluidPort(20, "high_pressure_steam_import_hatch", false,
                GTNLMultiblockAbility.HIGH_PRESSURE_STEAM_IMPORT, GTNLMaterials.HighPressureSteam.getFluid(),
                12800000);
        LOW_PRESSURE_STEAM_EXPORT_HATCH = registerFluidPort(21, "low_pressure_steam_export_hatch", true,
                GTNLMultiblockAbility.LOW_PRESSURE_STEAM_EXPORT, GTNLMaterials.LowPressureSteam.getFluid(),
                12800000);
        LOW_PRESSURE_STEAM_IMPORT_HATCH = registerFluidPort(22, "low_pressure_steam_import_hatch", false,
                GTNLMultiblockAbility.LOW_PRESSURE_STEAM_IMPORT, GTNLMaterials.LowPressureSteam.getFluid(),
                12800000);
        CONDENSATE_EXPORT_HATCH = registerFluidPort(23, "condensate_export_hatch", true,
                GTNLMultiblockAbility.CONDENSATE_EXPORT, Materials.Water.getFluid(), 12800000);

        ResourceLocation turbineRotorHolderId = gtnlID("turbine_rotor_holder");
        TURBINE_ROTOR_HOLDER = new MetaTileEntityTurbineRotorHolder(turbineRotorHolderId);
        GregTechAPI.mteManager.getRegistry(GTNuclearValues.MODID)
                .register(24, turbineRotorHolderId, TURBINE_ROTOR_HOLDER);
        MultiblockAbility.registerMultiblockAbility(GTNLMultiblockAbility.TURBINE_ROTOR_SLOT, TURBINE_ROTOR_HOLDER);

        ResourceLocation highPressureSteamTurbineId = gtnlID("high_pressure_steam_turbine");
        HIGH_PRESSURE_STEAM_TURBINE = new MetaTileEntityHighPressureSteamTurbine(highPressureSteamTurbineId);
        GregTechAPI.mteManager.getRegistry(GTNuclearValues.MODID)
                .register(25, highPressureSteamTurbineId, HIGH_PRESSURE_STEAM_TURBINE);

        ResourceLocation lowPressureSteamTurbineId = gtnlID("low_pressure_steam_turbine");
        LOW_PRESSURE_STEAM_TURBINE = new MetaTileEntityLowPressureSteamTurbine(lowPressureSteamTurbineId);
        GregTechAPI.mteManager.getRegistry(GTNuclearValues.MODID)
                .register(26, lowPressureSteamTurbineId, LOW_PRESSURE_STEAM_TURBINE);

        // Residual heat removal cooling water ports (2026-11-24, stage 1-4a) -- the non-radioactive cooling side of
        // the reactor's built-in RHR heat exchanger. The import accepts plain/distilled water as well as any
        // cooling water band (its own fluid temperature sets the cooling capacity); the export emits the band
        // matching the computed outlet temperature. Capacity: ~2 s of the reference-size reactor's design flow.
        Collection<Fluid> coolingWaterBandFluids = Arrays.stream(GTNLMaterials.CoolingWaterBands)
                .map(Material::getFluid).collect(Collectors.toList());
        List<Fluid> rhrInletFluids = new ArrayList<>();
        rhrInletFluids.add(Materials.Water.getFluid());
        rhrInletFluids.add(Materials.DistilledWater.getFluid());
        rhrInletFluids.addAll(coolingWaterBandFluids);
        RHR_COOLING_WATER_IMPORT_HATCH = registerFluidPort(27, "rhr_cooling_water_import_hatch", false,
                GTNLMultiblockAbility.RHR_COOLING_WATER_IMPORT, rhrInletFluids, 4000000);
        RHR_COOLING_WATER_EXPORT_HATCH = registerFluidPort(28, "rhr_cooling_water_export_hatch", true,
                GTNLMultiblockAbility.RHR_COOLING_WATER_EXPORT, coolingWaterBandFluids, 4000000);
    }

    private static MetaTileEntityReactorFluidPort registerFluidPort(int id, String name, boolean isExportHatch,
                                                                    MultiblockAbility<IFluidTank> ability,
                                                                    Fluid allowedFluid, int capacity) {
        return registerFluidPort(id, name, isExportHatch, ability, Collections.singleton(allowedFluid), capacity);
    }

    /**
     * As the single-fluid overload, but accepting any fluid in {@code allowedFluids} -- see
     * {@code MetaTileEntityReactorFluidPort}'s own multi-fluid constructor JavaDoc.
     */
    private static MetaTileEntityReactorFluidPort registerFluidPort(int id, String name, boolean isExportHatch,
                                                                    MultiblockAbility<IFluidTank> ability,
                                                                    Collection<Fluid> allowedFluids, int capacity) {
        ResourceLocation hatchId = gtnlID(name);
        MetaTileEntityReactorFluidPort hatch = new MetaTileEntityReactorFluidPort(hatchId, isExportHatch, ability,
                allowedFluids, capacity);
        GregTechAPI.mteManager.getRegistry(GTNuclearValues.MODID).register(id, hatchId, hatch);
        // Unlike GT's own MetaTileEntities.registerMetaTileEntity(...), the registry's own register(...) does not
        // populate MultiblockAbility.REGISTRY automatically -- structure patterns calling abilities(ability) look
        // this registry up directly (MultiblockControllerBase#abilities), and a missing entry NPEs rather than
        // returning an empty list, so this registration is required, not optional.
        MultiblockAbility.registerMultiblockAbility(ability, hatch);
        return hatch;
    }
}
