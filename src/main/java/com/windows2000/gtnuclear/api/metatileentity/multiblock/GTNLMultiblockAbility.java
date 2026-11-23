package com.windows2000.gtnuclear.api.metatileentity.multiblock;

import gregtech.api.metatileentity.multiblock.MultiblockAbility;

import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.items.IItemHandlerModifiable;

import com.windows2000.gtnuclear.api.capability.IReactorLinkHatch;
import com.windows2000.gtnuclear.common.metatileentities.multiblockpart.MetaTileEntityConsoleScreen;

/**
 * This mod's reactor-specific {@link MultiblockAbility} instances -- one per dedicated hatch type, rather than
 * reusing GT's generic {@code IMPORT_FLUIDS}/{@code EXPORT_FLUIDS}/{@code IMPORT_ITEMS}. A generic ability would
 * only let the structure pattern check "is some fluid/item hatch here", not "is the <i>correct</i> hatch here"
 * (e.g. rejecting a boric acid hatch mistakenly placed at the coolant import position) -- mirroring how the
 * Supercritical addon's {@code SCMultiblockAbility} defines a distinct ability per coolant/fuel-rod port rather
 * than reusing GT's generic abilities. See this mod's design memory for the full reactor hatch layout.
 */
public final class GTNLMultiblockAbility {

    public static final MultiblockAbility<IFluidTank> REACTOR_COOLANT_IMPORT = fluidAbility("reactor_coolant_import");
    public static final MultiblockAbility<IFluidTank> REACTOR_COOLANT_EXPORT = fluidAbility("reactor_coolant_export");
    public static final MultiblockAbility<IFluidTank> BORIC_ACID_IMPORT = fluidAbility("boric_acid_import");
    public static final MultiblockAbility<IFluidTank> PURE_WATER_IMPORT = fluidAbility("pure_water_import");
    public static final MultiblockAbility<IFluidTank> EMERGENCY_COOLANT_IMPORT = fluidAbility(
            "emergency_coolant_import");
    public static final MultiblockAbility<IFluidTank> RADIOACTIVE_WASTEWATER_EXPORT = fluidAbility(
            "radioactive_wastewater_export");

    /**
     * Cooling (non-radioactive) side of the reactor's built-in residual heat removal heat exchanger (2026-11-24,
     * stage 1-4a) -- see {@code ResidualHeatRemoval}. The radioactive primary side never leaves the reactor.
     */
    public static final MultiblockAbility<IFluidTank> RHR_COOLING_WATER_IMPORT = fluidAbility(
            "rhr_cooling_water_import");
    public static final MultiblockAbility<IFluidTank> RHR_COOLING_WATER_EXPORT = fluidAbility(
            "rhr_cooling_water_export");

    /** Heat exchanger's secondary-side ports -- see {@code MetaTileEntityHeatExchanger}'s own JavaDoc. */
    public static final MultiblockAbility<IFluidTank> FEEDWATER_IMPORT = fluidAbility("feedwater_import");
    public static final MultiblockAbility<IFluidTank> HIGH_PRESSURE_STEAM_EXPORT = fluidAbility(
            "high_pressure_steam_export");

    /**
     * Turbine fluid ports -- see {@code MetaTileEntityHighPressureSteamTurbine}/
     * {@code MetaTileEntityLowPressureSteamTurbine}'s own JavaDoc for the two-stage design these anchor. Distinct
     * import/export abilities per stage, matching this class's own JavaDoc on why a dedicated ability per hatch
     * role (not just per fluid) is used throughout this mod.
     */
    public static final MultiblockAbility<IFluidTank> HIGH_PRESSURE_STEAM_IMPORT = fluidAbility(
            "high_pressure_steam_import");
    public static final MultiblockAbility<IFluidTank> LOW_PRESSURE_STEAM_EXPORT = fluidAbility(
            "low_pressure_steam_export");
    public static final MultiblockAbility<IFluidTank> LOW_PRESSURE_STEAM_IMPORT = fluidAbility(
            "low_pressure_steam_import");
    /** The low-pressure turbine's own final exhaust (plain water), piped back toward feedwater. */
    public static final MultiblockAbility<IFluidTank> CONDENSATE_EXPORT = fluidAbility("condensate_export");

    /**
     * Fuel assembly port (fuel slot + guide-tube insert slot) -- replaces the former separate {@code fuel_port} and
     * {@code control_rod_port} abilities since 2026-11-21 (stage 1-3 of this mod's reactor physics rework: control
     * rod clusters live inside fuel assemblies' guide tubes, as in a real PWR).
     */
    public static final MultiblockAbility<IItemHandlerModifiable> FUEL_ASSEMBLY_PORT = itemAbility(
            "fuel_assembly_port");
    public static final MultiblockAbility<IItemHandlerModifiable> NEUTRON_SOURCE_PORT = itemAbility(
            "neutron_source_port");
    /**
     * Single-slot GT turbine rotor holder, shared by both HP/LP turbine stages -- deliberately a new ability
     * rather than GT's own {@code MultiblockAbility.ROTOR_HOLDER}/{@code IRotorHolder}, which would let GT's own
     * turbine structure-pattern checks mistake this part for one of its own (see
     * {@code MetaTileEntityTurbineRotorHolder}'s own JavaDoc for why this mod reads GT's rotor item data directly
     * instead of reusing GT's {@code MetaTileEntityRotorHolder} part class).
     */
    public static final MultiblockAbility<IItemHandlerModifiable> TURBINE_ROTOR_SLOT = itemAbility(
            "turbine_rotor_slot");

    /** Reactor-side (source) end of the reactor-console link, sat at the reactor's own 'L' pattern slot. */
    public static final MultiblockAbility<IReactorLinkHatch> REACTOR_LINK_TRANSMITTER = linkAbility(
            "reactor_link_transmitter");
    /** Console-side (sink) end of the reactor-console link. */
    public static final MultiblockAbility<IReactorLinkHatch> REACTOR_LINK_RECEIVER = linkAbility(
            "reactor_link_receiver");

    /** The control console's telemetry display part -- see {@link MetaTileEntityConsoleScreen}'s own JavaDoc. */
    public static final MultiblockAbility<MetaTileEntityConsoleScreen> CONSOLE_SCREEN = MultiblockAbility.ability(
            "console_screen", MetaTileEntityConsoleScreen.class);

    private static MultiblockAbility<IFluidTank> fluidAbility(String name) {
        return MultiblockAbility.ability(name, IFluidTank.class);
    }

    private static MultiblockAbility<IItemHandlerModifiable> itemAbility(String name) {
        return MultiblockAbility.ability(name, IItemHandlerModifiable.class);
    }

    private static MultiblockAbility<IReactorLinkHatch> linkAbility(String name) {
        return MultiblockAbility.ability(name, IReactorLinkHatch.class);
    }

    private GTNLMultiblockAbility() {}
}
