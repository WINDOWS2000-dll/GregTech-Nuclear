package com.windows2000.gtnuclear.common.metatileentities;

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

/**
 * MetaTileEntity field declarations for this mod, mirroring GT's own {@code common.metatileentities.MetaTileEntities}:
 * fields live here, actual construction/registration happens in {@link GTNLMetaTileEntityRegistration}.
 */
public class GTNLMetaTileEntities {

    private GTNLMetaTileEntities() {}

    public static MetaTileEntityIsotopeStorage ISOTOPE_STORAGE;
    public static MetaTileEntityPressurizedWaterReactor PRESSURIZED_WATER_REACTOR;

    // PWR reactor dedicated fluid ports -- see MetaTileEntityReactorFluidPort's own JavaDoc.
    public static MetaTileEntityReactorFluidPort REACTOR_COOLANT_IMPORT_HATCH;
    public static MetaTileEntityReactorFluidPort REACTOR_COOLANT_EXPORT_HATCH;
    public static MetaTileEntityReactorFluidPort ADVANCED_COOLANT_IMPORT_HATCH;
    public static MetaTileEntityReactorFluidPort ADVANCED_COOLANT_EXPORT_HATCH;
    public static MetaTileEntityReactorFluidPort BORIC_ACID_IMPORT_HATCH;
    public static MetaTileEntityReactorFluidPort PURE_WATER_IMPORT_HATCH;
    public static MetaTileEntityReactorFluidPort EMERGENCY_COOLANT_IMPORT_HATCH;
    public static MetaTileEntityReactorFluidPort RADIOACTIVE_WASTEWATER_EXPORT_HATCH;
    // Residual heat removal cooling water ports (stage 1-4a) -- see ResidualHeatRemoval's own JavaDoc.
    public static MetaTileEntityReactorFluidPort RHR_COOLING_WATER_IMPORT_HATCH;
    public static MetaTileEntityReactorFluidPort RHR_COOLING_WATER_EXPORT_HATCH;

    // PWR reactor fuel assembly channel port (fuel + guide-tube insert) -- see its own JavaDoc.
    public static MetaTileEntityFuelAssemblyPort FUEL_ASSEMBLY_PORT;
    public static MetaTileEntityNeutronSourceHatch NEUTRON_SOURCE_HATCH;

    // Reactor-console link hatches -- see MetaTileEntityReactorLinkHatch's own JavaDoc.
    public static MetaTileEntityReactorLinkHatch REACTOR_LINK_TRANSMITTER_HATCH;
    public static MetaTileEntityReactorLinkHatch REACTOR_LINK_RECEIVER_HATCH;

    // Control console and its telemetry screen part -- see their own JavaDoc.
    public static MetaTileEntityControlConsole CONTROL_CONSOLE;
    public static MetaTileEntityConsoleScreen CONSOLE_SCREEN;

    // Secondary loop heat exchanger and its own dedicated fluid ports -- see MetaTileEntityHeatExchanger's own
    // JavaDoc. Primary side reuses REACTOR_COOLANT_IMPORT/EXPORT_HATCH above, no dedicated hatch of its own.
    public static MetaTileEntityHeatExchanger HEAT_EXCHANGER;
    public static MetaTileEntityReactorFluidPort FEEDWATER_IMPORT_HATCH;
    public static MetaTileEntityReactorFluidPort HIGH_PRESSURE_STEAM_EXPORT_HATCH;
    public static MetaTileEntityReactorFluidPort STEAM_DUMP_CONDENSATE_EXPORT_HATCH;

    // HP/LP turbine stages and their own dedicated fluid ports/rotor slot -- see
    // MetaTileEntityHighPressureSteamTurbine/MetaTileEntityLowPressureSteamTurbine's own JavaDoc.
    public static MetaTileEntityReactorFluidPort HIGH_PRESSURE_STEAM_IMPORT_HATCH;
    public static MetaTileEntityReactorFluidPort LOW_PRESSURE_STEAM_EXPORT_HATCH;
    public static MetaTileEntityReactorFluidPort LOW_PRESSURE_STEAM_IMPORT_HATCH;
    public static MetaTileEntityReactorFluidPort CONDENSATE_EXPORT_HATCH;
    public static MetaTileEntityTurbineRotorHolder TURBINE_ROTOR_HOLDER;
    public static MetaTileEntityHighPressureSteamTurbine HIGH_PRESSURE_STEAM_TURBINE;
    public static MetaTileEntityLowPressureSteamTurbine LOW_PRESSURE_STEAM_TURBINE;
}
