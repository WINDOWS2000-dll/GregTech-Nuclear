package com.windows2000.gtnuclear.common.metatileentities.multi;

import gregtech.api.capability.IEnergyContainer;
import gregtech.api.capability.impl.EnergyContainerList;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.interfaces.IGregTechTileEntity;
import gregtech.api.metatileentity.multiblock.IMultiblockPart;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.metatileentity.multiblock.MultiblockWithDisplayBase;
import gregtech.api.metatileentity.multiblock.ui.MultiblockUIBuilder;
import gregtech.api.pattern.BlockPattern;
import gregtech.api.pattern.FactoryBlockPattern;
import gregtech.api.pattern.PatternMatchContext;
import gregtech.api.unification.Element;
import gregtech.api.unification.Elements;
import gregtech.api.util.KeyUtil;
import gregtech.api.util.RelativeDirection;
import gregtech.client.renderer.ICubeRenderer;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagDouble;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Matrix4;
import com.windows2000.gtnuclear.api.capability.IReactorLinkHatch;
import com.windows2000.gtnuclear.api.metatileentity.multiblock.GTNLMultiblockAbility;
import com.windows2000.gtnuclear.api.nuclear.accident.AccidentHost;
import com.windows2000.gtnuclear.api.nuclear.accident.SevereAccidentConstants;
import com.windows2000.gtnuclear.api.nuclear.accident.SevereAccidentProgressionTracker;
import com.windows2000.gtnuclear.api.nuclear.accident.SevereAccidentStage;
import com.windows2000.gtnuclear.api.nuclear.accident.SevereAccidentState;
import com.windows2000.gtnuclear.api.nuclear.decay.DecayEngine;
import com.windows2000.gtnuclear.api.nuclear.fuel.FuelBurnupEngine;
import com.windows2000.gtnuclear.api.nuclear.fuel.FuelComposition;
import com.windows2000.gtnuclear.api.nuclear.neutronics.BoronReactivity;
import com.windows2000.gtnuclear.api.nuclear.neutronics.CMFDResult;
import com.windows2000.gtnuclear.api.nuclear.neutronics.CMFDSolver;
import com.windows2000.gtnuclear.api.nuclear.neutronics.ControlRodWorth;
import com.windows2000.gtnuclear.api.nuclear.neutronics.FluxWeightedAverage;
import com.windows2000.gtnuclear.api.nuclear.neutronics.ModeratorTemperatureReactivity;
import com.windows2000.gtnuclear.api.nuclear.neutronics.NeutronSourceStrength;
import com.windows2000.gtnuclear.api.nuclear.neutronics.NodePosition;
import com.windows2000.gtnuclear.api.nuclear.neutronics.PointKineticsConstants;
import com.windows2000.gtnuclear.api.nuclear.neutronics.PointKineticsSolver;
import com.windows2000.gtnuclear.api.nuclear.neutronics.PointKineticsState;
import com.windows2000.gtnuclear.api.nuclear.neutronics.ReactorGeometry;
import com.windows2000.gtnuclear.api.nuclear.neutronics.ReactorGeometryBuilder;
import com.windows2000.gtnuclear.api.nuclear.neutronics.ReactorNode;
import com.windows2000.gtnuclear.api.nuclear.neutronics.ReactorNodeFactory;
import com.windows2000.gtnuclear.api.nuclear.neutronics.XenonDeadTimeCalibration;
import com.windows2000.gtnuclear.api.nuclear.neutronics.XenonIodineConstants;
import com.windows2000.gtnuclear.api.nuclear.neutronics.XenonIodineSolver;
import com.windows2000.gtnuclear.api.nuclear.neutronics.XenonIodineState;
import com.windows2000.gtnuclear.api.nuclear.operation.AutoControlHost;
import com.windows2000.gtnuclear.api.nuclear.operation.ReactorAutoControlSequencer;
import com.windows2000.gtnuclear.api.nuclear.operation.ReactorOperatingEvaluator;
import com.windows2000.gtnuclear.api.nuclear.operation.ReactorOperatingStage;
import com.windows2000.gtnuclear.api.nuclear.operation.ReactorOperatingState;
import com.windows2000.gtnuclear.api.nuclear.operation.ReactorScramEvaluator;
import com.windows2000.gtnuclear.api.nuclear.operation.ReactorStartupRequirements;
import com.windows2000.gtnuclear.api.nuclear.operation.RefuelingEvaluator;
import com.windows2000.gtnuclear.api.nuclear.operation.ScramHost;
import com.windows2000.gtnuclear.api.nuclear.operation.ScramReason;
import com.windows2000.gtnuclear.api.nuclear.operation.ScramRodDrop;
import com.windows2000.gtnuclear.api.nuclear.power.ReactorPowerConsumption;
import com.windows2000.gtnuclear.api.nuclear.thermal.CoolantThermalSolver;
import com.windows2000.gtnuclear.api.nuclear.thermal.CoolingWaterTemperatureBands;
import com.windows2000.gtnuclear.api.nuclear.thermal.EnergyLedger;
import com.windows2000.gtnuclear.api.nuclear.thermal.FuelCoolantSubstepping;
import com.windows2000.gtnuclear.api.nuclear.thermal.FuelTemperatureStep;
import com.windows2000.gtnuclear.api.nuclear.thermal.FuelThermalConstants;
import com.windows2000.gtnuclear.api.nuclear.thermal.RatedCoolantMass;
import com.windows2000.gtnuclear.api.nuclear.thermal.ResidualHeatRemoval;
import com.windows2000.gtnuclear.api.nuclear.thermal.SteamProperties;
import com.windows2000.gtnuclear.api.nuclear.thermal.WaterSaturationCurve;
import com.windows2000.gtnuclear.api.unification.materials.GTNLElements;
import com.windows2000.gtnuclear.api.unification.materials.GTNLMaterials;
import com.windows2000.gtnuclear.client.renderer.texture.GTNLTextures;
import com.windows2000.gtnuclear.common.ConfigHolder;
import com.windows2000.gtnuclear.common.blocks.GTNLMetaBlocks;
import com.windows2000.gtnuclear.common.blocks.reactor.ReactorVesselCasing;
import com.windows2000.gtnuclear.common.item.ItemFuelRod;
import com.windows2000.gtnuclear.common.item.ItemNeutronSourceRod;
import com.windows2000.gtnuclear.common.metatileentities.multiblockpart.MetaTileEntityFuelAssemblyPort;
import com.windows2000.gtnuclear.common.metatileentities.multiblockpart.MetaTileEntityNeutronSourceHatch;
import com.windows2000.gtnuclear.common.util.ReactorEnergyCategory;
import com.windows2000.gtnuclear.common.util.ReactorTelemetryLogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A pressurized water reactor's cylindrical vessel multiblock: a circular (grid-rasterized) cross-section of
 * variable diameter (7-31, odd) and height (up to 37 total), with the controller fixed on the front face's
 * horizontal center at the 3rd layer from the bottom (see this mod's design memory for why -- a deliberate,
 * fixed placement rather than a fully free-form scan like {@code MetaTileEntityCleanroom}'s).
 * <p>
 * <b>Layer layout (bottom to top):</b>
 * <ol>
 * <li>Solid bottom cap.</li>
 * <li>Ring layer with the primary coolant <b>import</b> port ('I', left face center), <b>maintenance
 * hatch</b> ('M', front face center -- directly below the controller), <b>energy hatch</b> ('E', back
 * face center) and the <b>radioactive wastewater drain hatch</b> ('D', right face center, used when
 * adjusting primary coolant volume) -- the fixed "2nd layer from the bottom."</li>
 * <li>Ring layer with the <b>controller</b> ('S', front face horizontal center), the <b>control console
 * link hatch</b> ('L', immediately to the controller's right, same row), and the <b>startup neutron
 * source hatch</b> ('N', immediately to the controller's left, same row) -- this is the fixed "3rd layer
 * from the bottom."</li>
 * <li>Zero or more plain ring layers.</li>
 * <li>Ring layer with the <b>distilled/pure water injection hatch</b> ('P', back face center) -- positioned
 * as close to the true vertical center of the whole structure as the flexible layer budget allows (see
 * {@link #createStructurePattern()} for the exact split).</li>
 * <li>Zero or more further plain ring layers (together with the layer above, these two flexible blocks make
 * up the only size-flexible part of the height).</li>
 * <li>Ring layer with the <b>boric acid injection hatch</b> ('X', left face center) and the primary coolant
 * <b>export</b> port ('O', right face center) -- always the "3rd layer from the top," regardless of how
 * many flexible layers are in the middle.</li>
 * <li>Ring layer with the <b>emergency coolant injection hatch</b> ('Q', left face center) -- always the
 * "2nd layer from the top."</li>
 * <li>Solid top cap, except every non-boundary (interior) cell is a fuel assembly channel slot -- see below.</li>
 * </ol>
 * The fixed layers below the controller (2) plus the fixed layers from the export ring up to the top cap (3)
 * plus the one flexible layer reserved for the pure water hatch force a minimum total height of 7.
 * <p>
 * <b>Fuel assembly channels</b> ('F', stage 1-3 of this mod's reactor physics rework, 2026-11-21): every
 * non-boundary cell of the top cap accepts a fuel assembly port
 * ({@link com.windows2000.gtnuclear.common.metatileentities.multiblockpart.MetaTileEntityFuelAssemblyPort}) or plain
 * vessel casing; the column below a port must be uniformly {@code fuel_rod_housing}. As in a real PWR, control rod
 * clusters sit in the fuel assemblies' guide tubes, so the former separate control rod channels ('C') are gone.
 * The structure only requires {@value ReactorStartupRequirements#MIN_CONTROL_ROD_CLUSTERS} ports; what is loaded
 * in them (fuel present, at least 4 intact clusters, lengths fitting the channel) is checked at startup instead
 * ({@link ReactorStartupRequirements}), since GT's pattern check only sees blocks, not slot contents. Every
 * interior cell is one CMFD column (see {@link #buildReactorGeometry()}).
 * <p>
 * The maintenance hatch and energy hatch use GTST's own standard abilities ({@code MAINTENANCE_HATCH} via
 * {@link #maintenancePredicate()}, {@code INPUT_ENERGY}). The six fluid ports (coolant import/export, boric
 * acid, pure water, emergency coolant, radioactive wastewater) are fully wired up too, via
 * {@link com.windows2000.gtnuclear.common.metatileentities.multiblockpart.MetaTileEntityReactorFluidPort}
 * instances and this mod's own dedicated {@link GTNLMultiblockAbility} per port (not GT's generic
 * {@code IMPORT_FLUIDS}/{@code EXPORT_FLUIDS} -- see {@link GTNLMultiblockAbility}'s own JavaDoc for why). The
 * control console link hatch ('L') is wired up too, via
 * {@link com.windows2000.gtnuclear.common.metatileentities.multiblockpart.MetaTileEntityReactorLinkHatch}
 * (transmitter role) -- this reactor will not do anything meaningful without a control console linked through it
 * (see this mod's design memory). The startup neutron source hatch ('N') is wired up too, via
 * {@link com.windows2000.gtnuclear.common.metatileentities.multiblockpart.MetaTileEntityNeutronSourceHatch}
 * -- see {@link #updatePhysicsShape()}/{@link #computeSourceNeutronsPerSecond()} for how its loaded
 * {@link com.windows2000.gtnuclear.common.item.ItemNeutronSourceRod} is what actually lets the reactor start from
 * a true-zero neutron population, replacing this class's earlier unconditional placeholder seed.
 * <p>
 * This is still a shape/port-position-only pass; interior fuel/control-rod channel layout is not modeled yet, so
 * {@link #updateFormedValid()} does not drive any of this mod's neutron/thermal physics classes.
 * <p>
 * The circular cross-section approximation and dynamic per-size pattern regeneration technique (scan actual
 * placed casing to detect size, rasterize a filled disk via a distance-squared test, rebuild the
 * {@link FactoryBlockPattern} every time the structure needs re-checking) mirrors the approach used by the
 * Supercritical addon's {@code MetaTileEntityFissionReactor}, itself built on the same underlying
 * {@code FactoryBlockPattern}/{@code BlockPattern} engine this fork inherited from GTCEu.
 */
public class MetaTileEntityPressurizedWaterReactor extends MultiblockWithDisplayBase
                                                   implements AutoControlHost, ScramHost, AccidentHost {

    private static final int MIN_DIAMETER = 7;
    private static final int MAX_DIAMETER = 31;
    private static final int MAX_TOTAL_HEIGHT = 37;
    /** Layers strictly below the controller's own layer: 1 solid bottom cap + 1 import-ring layer. */
    private static final int LAYERS_BELOW_CONTROLLER = 2;
    /** Fixed layers strictly above the controller's own layer: export-ring + 1 plain ring + solid top cap. */
    private static final int FIXED_LAYERS_ABOVE_CONTROLLER = 3;
    /** One flexible layer is always reserved for the pure/distilled water hatch, even at minimum height. */
    private static final int MIN_FLEXIBLE_MIDDLE_LAYERS = 1;
    private static final int MIN_TOTAL_HEIGHT = LAYERS_BELOW_CONTROLLER + 1 + MIN_FLEXIBLE_MIDDLE_LAYERS +
            FIXED_LAYERS_ABOVE_CONTROLLER;
    private static final int MAX_FLEXIBLE_MIDDLE_LAYERS = MAX_TOTAL_HEIGHT - LAYERS_BELOW_CONTROLLER - 1 -
            FIXED_LAYERS_ABOVE_CONTROLLER;

    private static final int UPDATE_OPERATING_STATUS = 300;

    private int diameter = MIN_DIAMETER;
    private int flexibleMiddleLayers = MIN_FLEXIBLE_MIDDLE_LAYERS;
    /**
     * Set by {@link #formStructure(PatternMatchContext)}: true if any fuel assembly port sits atop a channel
     * column (from the layer directly below it down to the bottom cap) that is not uniformly filled with
     * {@code fuel_rod_housing} (shown as "Fuel Assembly Housing"). The structure pattern cannot express "this whole
     * column is one block type," so this is verified at runtime instead.
     */
    private boolean housingMismatch = false;
    /**
     * Set by {@link #formStructure(PatternMatchContext)} and every shape recompute: true if any loaded fuel
     * assembly or control rod cluster is longer than this vessel's channel height (stage 1-3, 2026-11-21) --
     * possible when a loaded vessel is rebuilt shorter. Blocks startup; the slot rules already prevent inserting
     * one in the first place.
     */
    private boolean lengthMismatch = false;
    /**
     * Latched by {@link #invalidateStructure()} when the structure breaks while running (stage 1-3) -- shown on
     * the console until the next manual reset, and persisted so a reload does not hide it.
     */
    private boolean structureBreachRecorded = false;

    /** Read by the control console screen part over the reactor-console link, and by {@link AccidentHost}. */
    @Override
    public boolean isHousingMismatch() {
        return housingMismatch;
    }

    /** Read by the control console screen part over the reactor-console link. */
    public boolean isLengthMismatch() {
        return lengthMismatch;
    }

    /** Read by the control console screen part over the reactor-console link. */
    public boolean isStructureBreachRecorded() {
        return structureBreachRecorded;
    }

    /** What occupies one interior (channel) cell of the top cap, for the console's core map. */
    public enum CellKind {
        /** Plain vessel casing: no port, the channel is a water column. */
        CASING,
        /** A fuel assembly port without a fuel assembly loaded (also a water column). */
        EMPTY_PORT,
        /** A fuel assembly port with a fuel assembly loaded. */
        FUEL
    }

    /** Sentinel for {@link CoreMapCell#rodLevel}: no intact control rod cluster in this channel. */
    public static final byte NO_ROD = -1;

    /**
     * One interior channel cell as synced to the console's core map (stage 1-3 (c)): its position relative to
     * the controller, what occupies it, the fuel's depletion level ({@code 0-100}, {@code 1 -
     * getFissileCapabilityFraction} in percent; 0 for non-fuel cells) and the control rod cluster's insertion
     * ({@code 0-100}, or {@link #NO_ROD}).
     * <p>
     * A plain snapshot rather than a live {@link IMultiblockPart} reference: {@link #getMultiblockParts()} is
     * server-only bookkeeping, so {@link #updateCoreMapCells()} resolves it server-side and {@link #coreMapCells}
     * is synced as plain data instead.
     */
    public record CoreMapCell(int dx, int dz, @NotNull CellKind kind, byte depletionPercent, byte rodLevel) {

        public boolean hasRod() {
            return rodLevel != NO_ROD;
        }
    }

    private List<CoreMapCell> coreMapCells = new ArrayList<>();

    /**
     * Every fuel assembly port in the structure, refreshed once per tick by {@link #updateChannelCaches()} and
     * reused by every per-tick method instead of each re-walking {@link #getMultiblockParts()} (2026-10-06
     * optimization, kept through the stage 1-3 port merge).
     */
    @NotNull
    private List<MetaTileEntityFuelAssemblyPort> cachedAssemblyPorts = new ArrayList<>();
    /** The subset of {@link #cachedAssemblyPorts} with a fuel assembly loaded. */
    @NotNull
    private List<MetaTileEntityFuelAssemblyPort> cachedFuelPorts = new ArrayList<>();
    /** The subset of {@link #cachedAssemblyPorts} with an intact control rod cluster loaded. */
    @NotNull
    private List<MetaTileEntityFuelAssemblyPort> cachedControlRods = new ArrayList<>();

    /** See {@link #cachedAssemblyPorts}'s own JavaDoc. Must run before any other method that reads the caches. */
    private void updateChannelCaches() {
        List<MetaTileEntityFuelAssemblyPort> ports = new ArrayList<>();
        List<MetaTileEntityFuelAssemblyPort> fuelPorts = new ArrayList<>();
        List<MetaTileEntityFuelAssemblyPort> controlRods = new ArrayList<>();
        for (IMultiblockPart part : getMultiblockParts()) {
            if (part instanceof MetaTileEntityFuelAssemblyPort port) {
                ports.add(port);
                if (port.hasFuel()) fuelPorts.add(port);
                if (port.hasControlRod()) controlRods.add(port);
            }
        }
        cachedAssemblyPorts = ports;
        cachedFuelPorts = fuelPorts;
        cachedControlRods = controlRods;
    }

    /** Client-synced copies of the cache sizes and startup check (see {@link #updateSyncedCoreSummary()}). */
    private int loadedFuelAssemblyCountSynced = 0;
    private int loadedControlRodClusterCountSynced = 0;
    @Nullable
    private ReactorStartupRequirements.Blocker startupBlocker = null;

    /** Server side, once per tick after the caches are refreshed. */
    private void updateSyncedCoreSummary() {
        loadedFuelAssemblyCountSynced = cachedFuelPorts.size();
        loadedControlRodClusterCountSynced = cachedControlRods.size();
        startupBlocker = getStartupBlocker();
    }

    /** Number of loaded fuel assemblies (client-synced). */
    public int getLoadedFuelAssemblyCount() {
        return loadedFuelAssemblyCountSynced;
    }

    /** Number of loaded intact control rod clusters (client-synced). */
    public int getLoadedControlRodClusterCount() {
        return loadedControlRodClusterCountSynced;
    }

    /** The first unmet startup precondition as last synced, or {@code null} if startup may begin. */
    @Nullable
    public ReactorStartupRequirements.Blocker getSyncedStartupBlocker() {
        return startupBlocker;
    }

    /** Read by the control console screen part over the reactor-console link. */
    @NotNull
    public List<CoreMapCell> getCoreMapCells() {
        return coreMapCells;
    }

    @Nullable
    private ReactorCrossSection cachedCrossSection;

    /** The current diameter's cross-section (rebuilt only when the diameter changes). */
    @NotNull
    private ReactorCrossSection crossSection() {
        if (cachedCrossSection == null || cachedCrossSection.diameter() != diameter) {
            cachedCrossSection = ReactorCrossSection.of(diameter);
        }
        return cachedCrossSection;
    }

    /**
     * World position of the top-cap cell at the given cross-section offset -- the disk center is
     * {@code radius} blocks behind the controller (which sits on the front wall), and the top cap is
     * {@link #channelTopYRelative()} above it. Columns run along {@code frontFacing.rotateY()}; the disk is
     * mirror-symmetric, so the lateral sign does not matter.
     */
    @NotNull
    private BlockPos topCapPosition(@NotNull ReactorCrossSection.Cell cell) {
        EnumFacing back = getFrontFacing().getOpposite();
        EnumFacing lateral = getFrontFacing().rotateY();
        return getPos().offset(back, diameter / 2 + cell.rowOffset()).offset(lateral, cell.columnOffset())
                .up(channelTopYRelative());
    }

    /**
     * Server-side snapshot of every interior channel cell -- see {@link CoreMapCell}'s own JavaDoc. Cells without
     * a port are listed too, so the console can show the whole core layout rather than only the loaded part.
     */
    private void updateCoreMapCells() {
        Map<Long, MetaTileEntityFuelAssemblyPort> portsByPos = new HashMap<>();
        for (MetaTileEntityFuelAssemblyPort port : cachedAssemblyPorts) portsByPos.put(port.getPos().toLong(), port);
        List<CoreMapCell> cells = new ArrayList<>();
        for (ReactorCrossSection.Cell cell : crossSection().interiorCells()) {
            BlockPos pos = topCapPosition(cell);
            int dx = pos.getX() - getPos().getX();
            int dz = pos.getZ() - getPos().getZ();
            MetaTileEntityFuelAssemblyPort port = portsByPos.get(pos.toLong());
            if (port == null) {
                cells.add(new CoreMapCell(dx, dz, CellKind.CASING, (byte) 0, NO_ROD));
                continue;
            }
            CellKind kind = port.hasFuel() ? CellKind.FUEL : CellKind.EMPTY_PORT;
            byte depletion = 0;
            if (port.hasFuel()) {
                depletion = (byte) Math.round(100.0 * (1.0 - port.getFissileCapabilityFraction()));
            }
            byte rod = port.hasControlRod() ? (byte) Math.round(100.0 * port.getInsertionDepth()) : NO_ROD;
            cells.add(new CoreMapCell(dx, dz, kind, depletion, rod));
        }
        coreMapCells = cells;
    }

    /**
     * One control rod cluster's position (relative to the controller), current bank assignment, and insertion
     * depth -- what the console's control rod list needs to render itself and target bank-cycle clicks. Same
     * indirection reasoning as {@link CoreMapCell}: a synced snapshot, not a live {@link IMultiblockPart}
     * reference.
     */
    public record ControlRodSummary(int dx, int dz, int bankId, double insertionDepth) {}

    private List<ControlRodSummary> controlRodSummaries = new ArrayList<>();

    /** Read by the control console screen part over the reactor-console link. */
    @NotNull
    public List<ControlRodSummary> getControlRodSummaries() {
        return controlRodSummaries;
    }

    /** Server-side snapshot backing {@link #controlRodSummaries} -- see {@link ControlRodSummary}'s own JavaDoc. */
    private void updateControlRodSummaries() {
        List<ControlRodSummary> summaries = new ArrayList<>();
        for (MetaTileEntityFuelAssemblyPort controlRod : cachedControlRods) {
            BlockPos pos = controlRod.getPos();
            summaries.add(new ControlRodSummary(pos.getX() - getPos().getX(), pos.getZ() - getPos().getZ(),
                    controlRod.getBankId(), controlRod.getInsertionDepth()));
        }
        controlRodSummaries = summaries;
    }

    /**
     * Console write-direction command: cycle the control rod cluster at world offset ({@code dx}, {@code dz})
     * (relative to this controller) through {@code 0 -> 1 -> ... -> BANK_COUNT -> 0}. A no-op if no cluster is
     * loaded at that offset (e.g. it was removed between the console's last sync and this click).
     */
    public void requestCycleControlRodBank(int dx, int dz) {
        for (MetaTileEntityFuelAssemblyPort controlRod : cachedControlRods) {
            BlockPos pos = controlRod.getPos();
            if (pos.getX() - getPos().getX() == dx && pos.getZ() - getPos().getZ() == dz) {
                controlRod.setBankId((controlRod.getBankId() + 1) % (MetaTileEntityFuelAssemblyPort.BANK_COUNT + 1));
                markDirty();
                return;
            }
        }
    }

    /**
     * Console write-direction command: nudge every control rod cluster assigned to {@code bankId} (0-
     * {@link MetaTileEntityFuelAssemblyPort#BANK_COUNT}, 0 being the default "unassigned" bank) by
     * {@code deltaFraction} (e.g. +-0.01 for a +-1% step) -- deliberately a manual per-click nudge, not a one-shot
     * "withdraw/insert fully" jump, per this mod's design memory (2026-09-17: "自動引き抜きボタンは採用しない").
     */
    public void requestNudgeBank(int bankId, double deltaFraction) {
        // Stage 1-1 (2026-11-21): a tripped reactor's rods are dropping/latched in (see applyScramRodDrop()), so
        // any withdrawal command -- manual console click or ReactorAutoControlSequencer alike -- is refused until
        // a manual reset; insertion is still harmless and allowed.
        if (operatingState.stage == ReactorOperatingStage.TRIPPED && deltaFraction < 0) return;
        // Stage 1-4c: rod drives are disconnected during refueling.
        if (operatingState.stage == ReactorOperatingStage.REFUELING) return;
        for (MetaTileEntityFuelAssemblyPort controlRod : cachedControlRods) {
            if (controlRod.getBankId() == bankId) {
                controlRod.setInsertionDepth(controlRod.getInsertionDepth() + deltaFraction);
            }
        }
        markDirty();
    }

    // ---------------------------------------------------------------- fuel assembly port support (stage 1-3)

    /** Rel. Y of the lowest channel layer (directly above the solid bottom cap). */
    private static final int CHANNEL_BOTTOM_Y_RELATIVE = -LAYERS_BELOW_CONTROLLER + 1;

    /** Rel. Y of the top cap, where the fuel assembly ports sit. */
    private int channelTopYRelative() {
        return flexibleMiddleLayers + FIXED_LAYERS_ABOVE_CONTROLLER;
    }

    /**
     * The channel height H in layers (stage 1-3 (b)): from the layer above the bottom cap up to and including the
     * top cap, i.e. {@code totalHeight - 1}. A fuel assembly or control rod cluster may be at most this long.
     */
    public int getChannelHeightLayers() {
        return channelTopYRelative() - CHANNEL_BOTTOM_Y_RELATIVE + 1;
    }

    /**
     * Maintenance lock (stage 1-3 (a), interim until stage 1-4's REFUELING stage): fuel assemblies and inserts
     * may only be moved while the formed reactor is in {@link ReactorOperatingStage#SHUTDOWN}.
     */
    public boolean isFuelHandlingAllowed() {
        return getFuelHandlingLock() == FuelHandlingLock.NONE;
    }

    /** Next tick's {@link #updatePhysicsShape()} recomputes immediately (a port's contents changed). */
    public void requestShapeRecompute() {
        physicsShapeTicksUntilRecompute = 0;
    }

    /** Per-port local values for the port's own UI; {@link #NONE} when nothing is known. */
    public record PortTelemetry(double localRelativePower, double xenonPcm, double fuelTemperatureKelvin) {

        public static final PortTelemetry NONE = new PortTelemetry(Double.NaN, Double.NaN, Double.NaN);
    }

    /** Server side: the given port's local power, xenon worth and local fuel temperature from the last CMFD shape. */
    @NotNull
    public PortTelemetry getPortTelemetry(@NotNull MetaTileEntityFuelAssemblyPort port) {
        ItemStack stack = port.getFuelStack();
        if (cmfdResult == null || !(stack.getItem() instanceof ItemFuelRod fuelRod)) return PortTelemetry.NONE;
        NodePosition topPosition = fuelPortTopPosition(port);
        double fastFlux = cmfdResult.fastFlux().getOrDefault(topPosition, 0.0);
        double thermalFlux = cmfdResult.thermalFlux().getOrDefault(topPosition, 0.0);
        double globalRelativePower = pointKineticsState.relativeNeutronPopulation;
        double localRelativePower = (fastFlux + thermalFlux) * globalRelativePower;
        double fuelCoolantUA = computeFuelCoolantUA();
        double temperatureSensitivity = fuelCoolantUA > 0 ? computeRatedThermalPowerWatts() / fuelCoolantUA : 0.0;
        double localFuelTemperature = fuelTemperatureKelvin +
                temperatureSensitivity * (localRelativePower - globalRelativePower);
        double xenonPcm = -port.getXenonIodineState(xenonIodineState).xenonConcentration() * 1.0e5;
        return new PortTelemetry(localRelativePower, xenonPcm, localFuelTemperature);
    }

    /** Whether any loaded assembly or insert is longer than the channel (see {@link #lengthMismatch}). */
    private boolean computeLengthMismatch() {
        int height = getChannelHeightLayers();
        for (MetaTileEntityFuelAssemblyPort port : cachedAssemblyPorts) {
            if (port.getFuelLength() > height || port.getInsertLength() > height) return true;
        }
        return false;
    }

    /** The first unmet startup precondition, or {@code null} if startup may begin. Server side. */
    @Nullable
    public ReactorStartupRequirements.Blocker getStartupBlocker() {
        return ReactorStartupRequirements.firstBlocker(new ReactorStartupRequirements.Inputs(housingMismatch,
                lengthMismatch, cachedFuelPorts.size(), cachedControlRods.size(), rcpRunning, coolantMassKg,
                computeRatedCoolantMassKg() * ConfigHolder.reactor.startupCoolantReadyFraction, pressureMPa,
                ConfigHolder.reactor.targetOperatingPressureMPa * ConfigHolder.reactor.startupPressureReadyFraction,
                temperatureKelvin,
                ConfigHolder.reactor.heatupTargetTemperatureKelvin * ConfigHolder.reactor.startupHeatupReadyFraction));
    }

    /**
     * Interior (non-boundary) cell count of a single circular cross-section layer -- recomputed by
     * {@link #createStructurePattern()} every time the structure is (re)checked. Combined with
     * {@link #totalHeightLayers()} to approximate the reactor's full 3D interior volume for
     * {@link RatedCoolantMass}; not NBT-persisted since it is always freshly recomputed before it is needed.
     */
    private int interiorCellsPerLayer = 0;
    /**
     * Whether {@link #updateReactorPowerConsumption()} found its {@code INPUT_ENERGY} hatch(es) completely
     * unable to supply any of the desired coolant pump draw this tick -- read by {@link #scramEvaluator} (via
     * {@link #isPumpPowerUnavailable()}). The consecutive-tick debounce counters this used to drive directly
     * ({@code linkLossTicks}/{@code powerLossTicks}) now live on {@link ReactorScramEvaluator} itself
     * (2026-10-06, extracted as the second step of this class's own decomposition -- see this mod's design
     * memory).
     */
    private boolean pumpPowerUnavailable = false;

    /** {@link ScramHost#isPumpPowerUnavailable()}. */
    @Override
    public boolean isPumpPowerUnavailable() {
        return pumpPowerUnavailable;
    }

    /**
     * The reactor's coarse startup/shutdown/SCRAM mode -- see {@link ReactorOperatingState}. Nothing outside
     * {@link #checkScramConditions()} drives this yet (the console's write-direction startup/shutdown/manual-
     * reset commands are a later phase, per this mod's design memory), so today this can only ever move from
     * {@link ReactorOperatingStage#SHUTDOWN} into {@link ReactorOperatingStage#TRIPPED} and no further -- the
     * wiring is complete and tested, but not yet reachable end-to-end in play.
     */
    private ReactorOperatingState operatingState = ReactorOperatingState.initial();
    /** Current primary coolant mass (kg); 0 until the startup coolant injection sequence adds to it. */
    private double coolantMassKg = 0.0;
    /**
     * Cached, client-synced copy of {@link #computeRatedCoolantMassKg()} -- recomputed every tick in
     * {@link #checkScramConditions()} (which already needs the fresh value for its own LOCA check) rather than
     * synced separately, purely for display (see {@link #getRatedCoolantMassKg()}); at most one tick stale, which
     * only matters immediately after a structure resize.
     */
    private double ratedCoolantMassKg = 0.0;
    /**
     * Whether the console's "begin coolant injection" command is currently active -- see
     * {@link #requestBeginCoolantInjection()}/{@link #updateCoolantInjection()}. Auto-clears once
     * {@link #coolantMassKg} reaches {@link #computeRatedCoolantMassKg()}.
     */
    private boolean coolantInjectionActive = false;
    /**
     * Primary coolant pressure (MPa, <b>absolute</b>), an operator-managed status value -- see this mod's design
     * memory. Never below {@link #ATMOSPHERIC_PRESSURE_MPA} (stage 1-2, 2026-11-21): a depressurized loop is vented
     * to atmosphere, not evacuated -- see {@link WaterSaturationCurve#ATMOSPHERIC_PRESSURE_PASCALS}'s own JavaDoc
     * for the real-playtest bug the previous 0 MPa floor caused.
     */
    private double pressureMPa = ATMOSPHERIC_PRESSURE_MPA;
    private static final double ATMOSPHERIC_PRESSURE_MPA = WaterSaturationCurve.ATMOSPHERIC_PRESSURE_PASCALS / 1.0e6;
    /**
     * Relative neutron population at or above which the core counts as an active heat source for
     * {@link #hasCoreHeatSource()} (stage 1-2, 2026-11-21) -- a provisional 0.01% of rated, to be replaced by a
     * fission + decay heat power threshold once decay heat exists (stage 2).
     */
    private static final double CORE_HEAT_SOURCE_RELATIVE_POWER_THRESHOLD = 1.0e-4;
    /**
     * Whether the console's "begin pressurization" command is currently active -- provisional (stage 3 of this
     * mod's startup/shutdown sequence work, 2026-09-21) pending the secondary coolant loop design, see
     * {@code ConfigHolder.reactor.targetOperatingPressureMPa}'s own comment. Mutually exclusive with
     * {@link #depressurizationActive}; auto-clears once {@link #pressureMPa} reaches the target.
     */
    private boolean pressurizationActive = false;
    /** As {@link #pressurizationActive}, ramping toward 0 instead. Auto-clears once {@link #pressureMPa} hits 0. */
    private boolean depressurizationActive = false;
    /** Primary coolant temperature (Kelvin); ambient until {@code CoolantThermalSolver} is wired in. */
    private double temperatureKelvin = 300.0;
    /**
     * Total EU/t actually drawn this tick (baseline instrumentation + coolant pump combined, see
     * {@link #updateReactorPowerConsumption()}) -- display-only, not persisted (recomputed fresh every tick).
     */
    private long totalPowerConsumptionEuPerTick = 0L;
    /**
     * Primary coolant actually pumped out toward the export hatch(es) this tick (mb, see
     * {@link #updatePrimaryCirculation()}) -- the reactor's own analog of a real RCP's current flow rate.
     * Display-only, not persisted.
     */
    private int primaryCirculationFlowMb = 0;

    // ---------------------------------------------------------------- refueling, Mode 6 (stage 1-4c, 2026-11-27)

    /** Loading is on automatic hold (ports locked) -- see {@link RefuelingEvaluator#holdReason}. */
    private boolean refuelingHold = false;
    @Nullable
    private RefuelingEvaluator.HoldReason refuelingHoldReason = null;
    /** Subcritical multiplication at refueling start / last resume -- fallback count-rate-doubling reference. */
    private double refuelingBaselineMultiplication = 1.0;
    /**
     * Neutron population (the source-range count rate) at refueling start / last resume -- the count-rate-doubling
     * reference since stage 2-1 gave subcritical cores a real source-driven population.
     */
    private double refuelingBaselinePopulation = 0.0;
    /** Whether this reactor has ever been above the shutdown level (fresh cores skip the decay wait). */
    private boolean everCritical = false;
    /** World time (ticks) when the reactor was last at or above the shutdown level -- see condition (8). */
    private long subcriticalSinceWorldTick = 0L;
    /** Sentinel: not known yet (a world saved before stage 1-4c); set on the first tick. */
    private static final long UNKNOWN_SUBCRITICAL_SINCE = Long.MIN_VALUE;
    // Display-only (synced, not persisted).
    private double subcriticalMultiplication = 1.0;
    private int operatingMode = 5;
    @NotNull
    private String refuelChecklistLine = "";
    @NotNull
    private String refuelDetailLine = "";

    /** Why the fuel assembly ports are locked right now. */
    public enum FuelHandlingLock {
        NONE,
        NOT_REFUELING,
        HOLD,
        SEVERE_ACCIDENT
    }

    @NotNull
    public FuelHandlingLock getFuelHandlingLock() {
        if (severeAccidentState.stage != SevereAccidentStage.NORMAL) return FuelHandlingLock.SEVERE_ACCIDENT;
        if (operatingState.stage != ReactorOperatingStage.REFUELING) return FuelHandlingLock.NOT_REFUELING;
        if (refuelingHold) return FuelHandlingLock.HOLD;
        return FuelHandlingLock.NONE;
    }

    public boolean isRefuelingHold() {
        return refuelingHold;
    }

    @Nullable
    public RefuelingEvaluator.HoldReason getRefuelingHoldReason() {
        return refuelingHoldReason;
    }

    public double getSubcriticalMultiplication() {
        return subcriticalMultiplication;
    }

    public double getRefuelingBaselineMultiplication() {
        return refuelingBaselineMultiplication;
    }

    public double getRefuelingBaselinePopulation() {
        return refuelingBaselinePopulation;
    }

    public int getOperatingMode() {
        return operatingMode;
    }

    @NotNull
    public String getRefuelChecklistLine() {
        return refuelChecklistLine;
    }

    @NotNull
    public String getRefuelDetailLine() {
        return refuelDetailLine;
    }

    @NotNull
    private RefuelingEvaluator.Limits refuelingLimits() {
        return new RefuelingEvaluator.Limits(ConfigHolder.reactor.shutdownCompletePowerThreshold,
                ConfigHolder.reactor.refuelingMinBoronPpm, ConfigHolder.reactor.refuelingMaxTemperatureKelvin,
                ATMOSPHERIC_PRESSURE_MPA + 0.001, ConfigHolder.reactor.refuelingMinSubcriticalMinutes);
    }

    /** Minutes since the reactor was last at or above the shutdown level; infinite for a never-critical core. */
    private double subcriticalMinutes() {
        if (!everCritical || getWorld() == null) return Double.POSITIVE_INFINITY;
        if (subcriticalSinceWorldTick == UNKNOWN_SUBCRITICAL_SINCE) return 0.0;
        return Math.max(0L, getWorld().getTotalWorldTime() - subcriticalSinceWorldTick) / 20.0 / 60.0;
    }

    private boolean allRodDrivesInserted() {
        for (MetaTileEntityFuelAssemblyPort port : cachedControlRods) {
            if (port.getInsertionDepth() < FULLY_INSERTED_THRESHOLD) return false;
        }
        return true;
    }

    @NotNull
    private Map<RefuelingEvaluator.Condition, RefuelingEvaluator.Status> refuelingChecklist() {
        return RefuelingEvaluator.checklist(new RefuelingEvaluator.Inputs(
                operatingState.stage == ReactorOperatingStage.SHUTDOWN, allRodDrivesInserted(),
                pointKineticsState.relativeNeutronPopulation, boronPpm, temperatureKelvin, pressureMPa,
                subcriticalMinutes(), coolantMassKg, computeRatedCoolantMassKg()), refuelingLimits());
    }

    private double currentMultiplication() {
        return RefuelingEvaluator.multiplication(totalReactivity, pointKineticsConstants != null);
    }

    /** Condition (8)'s clock: world time, so decay "continues" while this reactor is unformed or unloaded. */
    private void updateSubcriticalClock() {
        if (getWorld() == null) return;
        if (subcriticalSinceWorldTick == UNKNOWN_SUBCRITICAL_SINCE) {
            subcriticalSinceWorldTick = getWorld().getTotalWorldTime();
            markDirty();
        }
        if (pointKineticsState.relativeNeutronPopulation >= ConfigHolder.reactor.shutdownCompletePowerThreshold) {
            if (!everCritical) markDirty();
            everCritical = true;
            subcriticalSinceWorldTick = getWorld().getTotalWorldTime();
        }
    }

    /** Per tick: rod drives held in, the automatic hold, and the console's checklist/mode display. */
    private void updateRefueling() {
        subcriticalMultiplication = currentMultiplication();
        operatingMode = RefuelingEvaluator.operatingMode(operatingState.stage == ReactorOperatingStage.REFUELING,
                totalReactivity, pointKineticsConstants != null, pointKineticsState.relativeNeutronPopulation,
                temperatureKelvin);
        Map<RefuelingEvaluator.Condition, RefuelingEvaluator.Status> checklist = refuelingChecklist();
        boolean refueling = operatingState.stage == ReactorOperatingStage.REFUELING;
        refuelChecklistLine = RefuelingEvaluator.summaryLine(checklist, refueling);
        refuelDetailLine = refuelDetail(RefuelingEvaluator.firstUnmet(checklist, !refueling), refueling);

        if (!refueling) return;
        // Rod drives are disconnected: every port's drive is held fully inserted.
        for (MetaTileEntityFuelAssemblyPort port : cachedAssemblyPorts) {
            if (port.getInsertionDepth() < 1.0) port.setInsertionDepth(1.0);
        }
        if (refuelingHold) return;
        RefuelingEvaluator.HoldReason reason = RefuelingEvaluator.holdReason(
                pointKineticsState.relativeNeutronPopulation, refuelingBaselinePopulation, subcriticalMultiplication,
                refuelingBaselineMultiplication, temperatureKelvin, coolantMassKg, computeRatedCoolantMassKg(),
                boronPpm, refuelingLimits());
        if (reason != null) {
            refuelingHold = true;
            refuelingHoldReason = reason;
            writeCustomData(UPDATE_OPERATING_STATUS, this::writeOperatingStatus);
            markDirty();
        }
    }

    @NotNull
    private String refuelDetail(@Nullable RefuelingEvaluator.Condition unmet, boolean refueling) {
        if (unmet == null) return refueling ? "" : "NEXT: READY";
        RefuelingEvaluator.Limits limits = refuelingLimits();
        return "NEXT: " + switch (unmet) {
            case SHUTDOWN -> "STAGE " + operatingState.stage;
            case RODS_INSERTED -> "RODS NOT FULLY INSERTED";
            case SUBCRITICAL -> String.format("POWER %.4f/%.4f", pointKineticsState.relativeNeutronPopulation,
                    limits.shutdownPowerLevel());
            case BORON -> String.format("BORON %.0f/%.0f ppm", boronPpm, limits.minBoronPpm());
            case SHUTDOWN_MARGIN -> "SHUTDOWN MARGIN";
            case TEMPERATURE -> String.format("TEMP %.0f/%.0f K", temperatureKelvin, limits.maxTemperatureKelvin());
            case PRESSURE -> String.format("PRESS %.2f/%.2f MPa", pressureMPa, limits.maxPressureMPa());
            case WAIT_TIME -> String.format("WAIT %.0f/%.0f min", subcriticalMinutes(),
                    limits.minSubcriticalMinutes());
            case COOLANT -> String.format("COOLANT %.0f/%.0f mb", coolantMassKg * 1000.0,
                    computeRatedCoolantMassKg() * 1000.0);
        };
    }

    /**
     * Console command: begin refueling (from SHUTDOWN, every condition (1)-(9) met) or end it (structure
     * requirements met and the core at or below the shutdown level -- allowed even while on hold, decided
     * 2026-11-27: leaving Mode 6 is the safe direction, and a hold that locks the ports could otherwise trap the
     * operator).
     */
    public void requestToggleRefueling() {
        if (operatingState.stage == ReactorOperatingStage.REFUELING) {
            if (housingMismatch || lengthMismatch || pointKineticsState.relativeNeutronPopulation >
                    ConfigHolder.reactor.shutdownCompletePowerThreshold) {
                return;
            }
            refuelingHold = false;
            refuelingHoldReason = null;
            applyOperatingStateChange(ReactorOperatingEvaluator.endRefueling(operatingState));
        } else {
            if (!RefuelingEvaluator.canStart(refuelingChecklist())) return;
            boronDilutionActive = false;
            pressurizationActive = false;
            refuelingHold = false;
            refuelingHoldReason = null;
            refuelingBaselineMultiplication = currentMultiplication();
            refuelingBaselinePopulation = pointKineticsState.relativeNeutronPopulation;
            applyOperatingStateChange(ReactorOperatingEvaluator.beginRefueling(operatingState));
        }
        writeCustomData(UPDATE_OPERATING_STATUS, this::writeOperatingStatus);
        markDirty();
    }

    /** Console command: lift a refueling hold once conditions (2)-(9) hold again; the count-rate baseline resets. */
    public void requestResumeRefueling() {
        if (operatingState.stage != ReactorOperatingStage.REFUELING || !refuelingHold) return;
        if (!RefuelingEvaluator.canResume(refuelingChecklist())) return;
        refuelingHold = false;
        refuelingHoldReason = null;
        refuelingBaselineMultiplication = currentMultiplication();
        refuelingBaselinePopulation = pointKineticsState.relativeNeutronPopulation;
        writeCustomData(UPDATE_OPERATING_STATUS, this::writeOperatingStatus);
        markDirty();
    }

    // ---------------------------------------------------------------- RCP and RHR (stage 1-4a, 2026-11-24)

    /**
     * Whether the reactor coolant pumps are running -- an explicit operator command since stage 1-4a (before
     * that the pumps were implicitly always on, and forced to full heatup duty while SHUTDOWN below
     * {@code heatupTargetTemperatureKelvin}). Heatup by pump heat and circulation to the heat exchanger both
     * happen only while running. Stopping is refused while STARTUP/OPERATING/SHUTTING_DOWN (in a real PWR a reactor
     * coolant pump trip is itself a reactor trip, so the command simply is not offered then). Defaults to running so
     * worlds saved before this field existed keep their old behavior.
     */
    private boolean rcpRunning = true;

    /** Whether the residual heat removal system has been placed in service -- see {@link ResidualHeatRemoval}. */
    private boolean rhrInService = false;

    /** The last RHR event worth showing on the console (a refused start or an automatic isolation). */
    public enum RhrNotice {
        NONE,
        START_REFUSED_TEMPERATURE,
        START_REFUSED_PRESSURE,
        ISOLATED_OVERPRESSURE
    }

    @NotNull
    private RhrNotice rhrNotice = RhrNotice.NONE;

    // Display-only RHR values for the last tick (synced, not persisted).
    private double rhrHeatRemovalWatts = 0.0;
    private double rhrCoolingInletKelvin = 0.0;
    private double rhrCoolingOutletKelvin = 0.0;
    private double rhrCoolingFlowKgPerSecond = 0.0;
    private boolean rhrOutletCapped = false;
    private boolean rhrNoPower = false;
    private boolean rhrNoCoolingWater = false;

    public boolean isRcpRunning() {
        return rcpRunning;
    }

    public boolean isRhrInService() {
        return rhrInService;
    }

    @NotNull
    public RhrNotice getRhrNotice() {
        return rhrNotice;
    }

    public double getRhrHeatRemovalWatts() {
        return rhrHeatRemovalWatts;
    }

    public double getRhrCoolingInletKelvin() {
        return rhrCoolingInletKelvin;
    }

    public double getRhrCoolingOutletKelvin() {
        return rhrCoolingOutletKelvin;
    }

    public double getRhrCoolingFlowKgPerSecond() {
        return rhrCoolingFlowKgPerSecond;
    }

    public boolean isRhrOutletCapped() {
        return rhrOutletCapped;
    }

    public boolean isRhrNoPower() {
        return rhrNoPower;
    }

    public boolean isRhrNoCoolingWater() {
        return rhrNoCoolingWater;
    }

    /** Console command: start or stop the reactor coolant pumps (stopping refused while running the core). */
    public void requestToggleRcp() {
        if (rcpRunning) {
            ReactorOperatingStage stage = operatingState.stage;
            if (stage == ReactorOperatingStage.STARTUP || stage == ReactorOperatingStage.OPERATING ||
                    stage == ReactorOperatingStage.SHUTTING_DOWN) {
                return;
            }
            rcpRunning = false;
        } else {
            rcpRunning = true;
        }
        writeCustomData(UPDATE_OPERATING_STATUS, this::writeOperatingStatus);
        markDirty();
    }

    /**
     * Console command: place RHR in service (only if {@link ResidualHeatRemoval#startBlocker} allows it, otherwise
     * the refusal reason is shown) or take it out of service (always allowed).
     */
    public void requestToggleRhr() {
        if (rhrInService) {
            rhrInService = false;
            rhrNotice = RhrNotice.NONE;
        } else {
            ResidualHeatRemoval.StartBlocker blocker = ResidualHeatRemoval.startBlocker(temperatureKelvin,
                    pressureMPa, ConfigHolder.reactor.rhrMaxStartTemperatureKelvin,
                    ConfigHolder.reactor.rhrMaxStartPressureMPa);
            if (blocker == null) {
                rhrInService = true;
                rhrNotice = RhrNotice.NONE;
            } else {
                rhrNotice = blocker == ResidualHeatRemoval.StartBlocker.TEMPERATURE_TOO_HIGH ?
                        RhrNotice.START_REFUSED_TEMPERATURE : RhrNotice.START_REFUSED_PRESSURE;
            }
        }
        writeCustomData(UPDATE_OPERATING_STATUS, this::writeOperatingStatus);
        markDirty();
    }

    /** This reactor's rated thermal power relative to the plant the {@code rhr*} config values describe. */
    private double rhrScale() {
        double reference = ConfigHolder.reactor.rhrReferenceThermalPowerWatts;
        return reference > 0 ? computeRatedThermalPowerWatts() / reference : 0.0;
    }

    /**
     * One tick of the residual heat removal system: draws RHR pump power, moves cooling water from the import to
     * the export hatch(es) (outlet temperature band from the heat exchanger result), and records the energy in
     * {@link #energyLedger}. Called before the fuel-coolant sub-step loop using the tick-start coolant temperature.
     *
     * @return net heat (W) into the primary coolant this tick: RHR pump heat minus heat removed
     */
    private double updateResidualHeatRemoval() {
        rhrHeatRemovalWatts = 0.0;
        rhrCoolingFlowKgPerSecond = 0.0;
        rhrOutletCapped = false;
        rhrNoPower = false;
        rhrNoCoolingWater = false;
        if (!rhrInService) return 0.0;
        if (ResidualHeatRemoval.mustIsolate(pressureMPa, ConfigHolder.reactor.rhrIsolationPressureMPa)) {
            rhrInService = false;
            rhrNotice = RhrNotice.ISOLATED_OVERPRESSURE;
            writeCustomData(UPDATE_OPERATING_STATUS, this::writeOperatingStatus);
            markDirty();
            return 0.0;
        }

        double scale = rhrScale();
        long desiredEu = Math.round(ConfigHolder.reactor.rhrPumpEuPerTick * scale);
        long drawnEu = ReactorPowerConsumption.drawEnergy(
                new EnergyContainerList(getAbilities(MultiblockAbility.INPUT_ENERGY)), desiredEu);
        totalPowerConsumptionEuPerTick += drawnEu;
        double pumpFraction = desiredEu > 0 ? (double) drawnEu / desiredEu : 1.0;
        if (pumpFraction <= 0) {
            rhrNoPower = true;
            return 0.0;
        }
        double pumpHeatWatts = ConfigHolder.reactor.rhrPumpHeatWatts * scale * pumpFraction;
        energyLedger.add(ReactorEnergyCategory.RHR_PUMP, pumpHeatWatts * TICK_SECONDS);

        // Primary flow can never exceed the whole coolant inventory per tick -- with epsilon <= 1 that keeps the
        // removed heat below m * c * (T - T_cooling,in), so the forward-Euler update cannot overshoot.
        double primaryFlowKgPerSecond = Math.min(ConfigHolder.reactor.rhrPrimaryFlowKgPerSecond * scale * pumpFraction,
                coolantMassKg / TICK_SECONDS);

        // Cooling water available this tick (simulated), with its mass-weighted inlet temperature.
        int desiredMb = (int) Math.round(ConfigHolder.reactor.rhrCoolingWaterFlowKgPerSecond * scale *
                TICK_SECONDS * 1000.0);
        int availableMb = 0;
        double inletEnthalpySum = 0.0;
        for (IFluidTank tank : getAbilities(GTNLMultiblockAbility.RHR_COOLING_WATER_IMPORT)) {
            if (availableMb >= desiredMb) break;
            FluidStack simulated = tank.drain(desiredMb - availableMb, false);
            if (simulated == null || simulated.amount <= 0) continue;
            availableMb += simulated.amount;
            inletEnthalpySum += simulated.amount * (double) simulated.getFluid().getTemperature();
        }
        if (availableMb <= 0) {
            rhrNoCoolingWater = true;
            return pumpHeatWatts;
        }
        double inletKelvin = inletEnthalpySum / availableMb;
        rhrCoolingInletKelvin = inletKelvin;

        // Size the exchange to what the export side accepts (one re-evaluation if it accepts less).
        int flowMb = availableMb;
        ResidualHeatRemoval.Exchange exchange = rhrExchange(primaryFlowKgPerSecond, inletKelvin, flowMb);
        Fluid outletFluid = GTNLMaterials.nearestCoolingWaterBandFluid(exchange.coolingOutletKelvin());
        int acceptedMb = fillCoolingWaterExport(outletFluid, flowMb, false);
        if (acceptedMb < flowMb) {
            flowMb = acceptedMb;
            if (flowMb > 0) {
                exchange = rhrExchange(primaryFlowKgPerSecond, inletKelvin, flowMb);
                outletFluid = GTNLMaterials.nearestCoolingWaterBandFluid(exchange.coolingOutletKelvin());
                flowMb = Math.min(flowMb, fillCoolingWaterExport(outletFluid, flowMb, false));
            }
        }
        if (flowMb <= 0) {
            rhrNoCoolingWater = true;
            return pumpHeatWatts;
        }

        // Commit: drain exactly what the export took.
        int drainedMb = 0;
        for (IFluidTank tank : getAbilities(GTNLMultiblockAbility.RHR_COOLING_WATER_IMPORT)) {
            if (drainedMb >= flowMb) break;
            FluidStack drained = tank.drain(flowMb - drainedMb, true);
            if (drained != null) drainedMb += drained.amount;
        }
        fillCoolingWaterExport(outletFluid, drainedMb, true);
        if (drainedMb <= 0) {
            rhrNoCoolingWater = true;
            return pumpHeatWatts;
        }
        // Final evaluation at the flow actually moved (the outlet band was chosen just above; any difference is
        // band rounding outside this reactor's own boundary).
        exchange = rhrExchange(primaryFlowKgPerSecond, inletKelvin, drainedMb);

        double removedWatts = exchange.heatWatts() * ConfigHolder.reactor.rhrCooldownAssistFactor;
        energyLedger.add(ReactorEnergyCategory.RHR, removedWatts * TICK_SECONDS);
        rhrHeatRemovalWatts = removedWatts;
        rhrCoolingOutletKelvin = exchange.coolingOutletKelvin();
        rhrCoolingFlowKgPerSecond = drainedMb / 1000.0 / TICK_SECONDS;
        rhrOutletCapped = exchange.outletCapped();
        return pumpHeatWatts - removedWatts;
    }

    @NotNull
    private ResidualHeatRemoval.Exchange rhrExchange(double primaryFlowKgPerSecond, double inletKelvin, int flowMb) {
        return ResidualHeatRemoval.exchange(temperatureKelvin, inletKelvin, primaryFlowKgPerSecond,
                flowMb / 1000.0 / TICK_SECONDS, ConfigHolder.reactor.rhrHeatExchangerUaWattsPerKelvin * rhrScale(),
                CoolantThermalSolver.REPRESENTATIVE_SPECIFIC_HEAT_CAPACITY,
                ResidualHeatRemoval.COOLING_WATER_SPECIFIC_HEAT,
                CoolingWaterTemperatureBands.MAX_BAND_TEMPERATURE_KELVIN);
    }

    /** Fills up to {@code amountMb} of {@code fluid} across the cooling water export hatch(es). */
    private int fillCoolingWaterExport(@NotNull Fluid fluid, int amountMb, boolean doFill) {
        int filledMb = 0;
        for (IFluidTank tank : getAbilities(GTNLMultiblockAbility.RHR_COOLING_WATER_EXPORT)) {
            if (filledMb >= amountMb) break;
            filledMb += tank.fill(new FluidStack(fluid, amountMb - filledMb), doFill);
        }
        return filledMb;
    }

    /** Primary coolant boron concentration (ppm), an operator-managed status value. */
    private double boronPpm = 0.0;
    /**
     * Whether the console's "begin boron dilution" command is currently active -- stage 4 of this mod's
     * startup/shutdown sequence work (2026-09-21). Unlike coolant injection/pressurization, boron adjustment has
     * no natural stopping target (a real operator dilutes/concentrates by feel, watching reactivity), so this is
     * a plain toggle (see {@link #requestToggleBoronDilution()}) rather than something that auto-clears. Mutually
     * exclusive with {@link #boronConcentrationActive}.
     */
    private boolean boronDilutionActive = false;
    /** As {@link #boronDilutionActive}, consuming boric acid solution instead of distilled water. */
    private boolean boronConcentrationActive = false;
    /**
     * Experimental (2026-09-24, balance-tuning tooling; extracted into its own class 2026-10-06, see this mod's
     * design memory on this class's own decomposition): the optional automatic startup/boron/control-rod
     * sequencing loop. {@link #updateBoronAdjustment()} reads its {@code getBoronAdjustmentRateFraction()} the
     * same way it used to read a local field. Deliberately NOT persisted to NBT (see {@link #onRemoval()}) --
     * {@link ReactorAutoControlSequencer#isActive()} always resets to off across a world reload, since this is
     * explicitly a short-session experimental tool (not something meant to silently keep running unattended).
     */
    @NotNull
    private final ReactorAutoControlSequencer autoControlSequencer = new ReactorAutoControlSequencer();
    /**
     * This reactor's own operating-state machine (automatic stage transitions + every non-manual SCRAM trigger)
     * -- extracted 2026-10-06 as the second step of this class's own decomposition, see this mod's design
     * memory. See {@link #checkAutomaticStageTransitions()}/{@link #checkScramConditions()}.
     */
    @NotNull
    private final ReactorScramEvaluator scramEvaluator = new ReactorScramEvaluator();
    /** As {@link #scramEvaluator}, but for severe-accident (melt/explosion) progression -- see that field's own. */
    @NotNull
    private final SevereAccidentProgressionTracker accidentTracker = new SevereAccidentProgressionTracker();
    /**
     * Experimental (2026-09-24, balance-tuning tooling): CSV telemetry, opened lazily on the first tick
     * {@link #autoControlSequencer}'s own {@code update(...)} returns a non-null action label, and closed on
     * {@link #onRemoval()} -- see {@link ReactorTelemetryLogger}'s own JavaDoc. Decoupled from
     * {@link #autoControlSequencer}'s own active/inactive toggle (2026-09-29, real-playtest-driven, see this
     * mod's design memory): logging used to open/close in lockstep with {@link #requestToggleAutoRodControl()},
     * so toggling auto control off to make a manual adjustment (or just observe) silently lost telemetry, and
     * toggling back on fragmented one continuous session across multiple files. Now mirrors
     * {@code MetaTileEntityHeatExchanger}'s own always-on-once-formed logger -- it keeps running regardless of
     * the auto-control toggle, logging {@code "manual"} as the action label whenever the loop is inactive. Not
     * NBT-persisted, matching {@link #autoControlSequencer}'s own reasoning (re-opening a log file mid-append
     * across a reload would need more bookkeeping than this experimental feature's scope warrants) -- a fresh
     * file opens again after any reload.
     */
    @Nullable
    private ReactorTelemetryLogger telemetryLogger = null;
    /**
     * Stage 0-3 of this mod's reactor physics rework (2026-11-21, see this mod's design memory): per-category
     * energy bookkeeping over one telemetry interval, logged by {@link #telemetryLogger} and then reset -- see
     * {@link EnergyLedger}'s own JavaDoc. Not persisted; a world reload just starts a fresh window.
     */
    private final EnergyLedger<ReactorEnergyCategory> energyLedger = new EnergyLedger<>(ReactorEnergyCategory.class);
    /**
     * Fuel's own temperature (Kelvin), decoupled from {@link #temperatureKelvin} (the primary coolant's) -- see
     * {@link #updateFuelAndCoolantThermal()}. A real meltdown is fundamentally a "fuel heats up faster than
     * coolant can carry the heat away" phenomenon, so a single shared temperature node cannot represent it.
     */
    private double fuelTemperatureKelvin = 300.0;
    /**
     * Fraction (0-1) of the core's total fuel mass that has become corium -- 0 until fuel reaches
     * {@code SevereAccidentConstants.FUEL_MELTING_TEMPERATURE_KELVIN} and begins melting (Stage 4 wires this into
     * {@link com.windows2000.gtnuclear.api.nuclear.accident.SevereAccidentEvaluator}). Tracked as an absolute
     * mass here (not the evaluator's own derived fraction) since it accumulates independently every tick.
     */
    private double meltedFuelMassKg = 0.0;

    /** {@link AccidentHost#getMeltedFuelMassKg()}. */
    @Override
    public double getMeltedFuelMassKg() {
        return meltedFuelMassKg;
    }

    /**
     * {@link #computeTotalCoreFuelMassKg()}'s value captured the instant {@link #meltedFuelMassKg} first becomes
     * positive, then reused for the rest of that meltdown instead of a freshly recomputed live value -- see
     * {@link #updateFuelTemperatureStep(double, double, double, double, double)}/
     * {@link #updateSevereAccidentState()}'s own JavaDoc for why:
     * corium already formed from a specific core's fuel does not care how much fuel happens to be loaded right
     * now, but both the melt-progression cap and the meltdown-fraction calculation were comparing
     * {@link #meltedFuelMassKg} against whatever fuel is CURRENTLY loaded -- reloading even a small amount of
     * fresh fuel after fuel was removed following an earlier meltdown made that small new total look almost
     * entirely melted, up to instantly re-triggering EXPLOSION (confirmed via a real playtest report, 2026-09-22:
     * inserting fresh LEU into an otherwise completely inert, sub-critical, sourceless, shut-down reactor caused
     * an immediate explosion).
     */
    private double coreFuelMassAtMeltdownStartKg = 0.0;

    /** {@link AccidentHost#getCoreFuelMassAtMeltdownStartKg()}. */
    @Override
    public double getCoreFuelMassAtMeltdownStartKg() {
        return coreFuelMassAtMeltdownStartKg;
    }

    /** {@link AccidentHost#hasCoreHeatSource()} -- see {@link #CORE_HEAT_SOURCE_RELATIVE_POWER_THRESHOLD}. */
    @Override
    public boolean hasCoreHeatSource() {
        return pointKineticsState.relativeNeutronPopulation >= CORE_HEAT_SOURCE_RELATIVE_POWER_THRESHOLD;
    }

    /**
     * Fraction (0-1) of primary coolant currently flashed to steam -- see {@link #updateFuelAndCoolantThermal()}'s
     * subcooling-margin check. Feeds a negative reactivity term in {@link #updatePhysicsAmplitude()} (a PWR's
     * inherent void feedback) and Stage 4's severe accident "abnormal cooling" condition.
     */
    private double primaryVoidFraction = 0.0;
    /**
     * Severe accident staging (NORMAL -&gt; UNCONTROLLED_HEATING -&gt; MELTDOWN -&gt; EXPLOSION) -- see
     * {@link #updateSevereAccidentState()}. {@link SevereAccidentProgressionTracker} itself is
     * structure-independent/pure; this field is this reactor's own instance of the state it advances.
     */
    @NotNull
    private SevereAccidentState severeAccidentState = SevereAccidentState.normal();

    /** Read by the control console screen part over the reactor-console link. */
    public double getFuelTemperatureKelvin() {
        return fuelTemperatureKelvin;
    }

    /** Read by the control console screen part over the reactor-console link. */
    public double getPrimaryVoidFraction() {
        return primaryVoidFraction;
    }

    /** Read by the control console screen part over the reactor-console link. */
    @NotNull
    public SevereAccidentState getSevereAccidentState() {
        return severeAccidentState;
    }

    /** Read by the control console screen part over the reactor-console link. */
    @NotNull
    public ReactorOperatingState getOperatingState() {
        return operatingState;
    }

    /** Read by the control console screen part over the reactor-console link. */
    public double getCoolantMassKg() {
        return coolantMassKg;
    }

    /** Read by the control console screen part over the reactor-console link -- see {@link #ratedCoolantMassKg}. */
    public double getRatedCoolantMassKg() {
        return ratedCoolantMassKg;
    }

    /** Read by the control console screen part over the reactor-console link. */
    public boolean isCoolantInjectionActive() {
        return coolantInjectionActive;
    }

    /** Read by the control console screen part over the reactor-console link. */
    public double getPressureMPa() {
        return pressureMPa;
    }

    /** Read by the control console screen part over the reactor-console link. */
    public boolean isPressurizationActive() {
        return pressurizationActive;
    }

    /** Read by the control console screen part over the reactor-console link. */
    public boolean isDepressurizationActive() {
        return depressurizationActive;
    }

    /** Read by the control console screen part over the reactor-console link. */
    public double getTemperatureKelvin() {
        return temperatureKelvin;
    }

    /** Read by the control console screen part over the reactor-console link. */
    public boolean isBoronDilutionActive() {
        return boronDilutionActive;
    }

    /** Read by the control console screen part over the reactor-console link. */
    public boolean isBoronConcentrationActive() {
        return boronConcentrationActive;
    }

    /** Read by the control console screen part over the reactor-console link. */
    public double getBoronPpm() {
        return boronPpm;
    }

    /**
     * Read by the control console screen part over the reactor-console link -- see
     * {@link #totalPowerConsumptionEuPerTick}.
     */
    public long getTotalPowerConsumptionEuPerTick() {
        return totalPowerConsumptionEuPerTick;
    }

    /**
     * Read by the control console screen part over the reactor-console link -- see {@link #primaryCirculationFlowMb}.
     */
    public int getPrimaryCirculationFlowMb() {
        return primaryCirculationFlowMb;
    }

    /** Side length of a cubic CMFD node (cm), representative of a real PWR fuel assembly pitch. */
    private static final double CHANNEL_NODE_SIZE_CM = 20.0;
    /**
     * How rarely the CMFD "shape" solve and fuel-composition-derived point kinetics constants are recomputed --
     * the quasi-static method's own division of labor (see this mod's design memory) calls for this to be rare,
     * not every tick. Unlike the reactor's own block structure (which only re-triggers
     * {@link #formStructure(PatternMatchContext)} on a fresh unformed-to-formed transition, per GTST's own
     * {@code MultiblockControllerBase#checkStructurePattern()}), a player loading/removing a fuel rod does not
     * re-trigger that hook at all -- so a periodic timer is used instead of trying to hook every possible change
     * source (structure change, fuel swap, etc.) individually.
     */
    private static final int PHYSICS_SHAPE_RECOMPUTE_INTERVAL_TICKS = 100;
    private static final double TICK_SECONDS = 1.0 / 20.0;

    private int physicsShapeTicksUntilRecompute = 0;
    @Nullable
    private CMFDResult cmfdResult;
    @Nullable
    private PointKineticsConstants pointKineticsConstants;
    private PointKineticsState pointKineticsState = new PointKineticsState(0.0, new double[0]);
    private XenonIodineState xenonIodineState = new XenonIodineState(0.0, 0.0);
    /**
     * Last computed total reactivity (shape + control rods + xenon + boron); display-only, not itself a state
     * variable fed back into anything.
     */
    private double totalReactivity = 0.0;
    /**
     * Experimental (2026-09-29, debug tooling, see this mod's design memory): {@link #cmfdResult}'s own raw shape
     * reactivity (pcm), i.e. {@code (kEffective - 1) / kEffective * 1.0e5}, <b>before</b> control rods/xenon/
     * boron/void/MTC/Doppler are subtracted -- the one component of {@link #totalReactivity} that a player cannot
     * otherwise observe directly, since every other term (rod insertion, boron ppm, temperature) is already shown
     * on the console. Added specifically to let a real playtest measure this reactor's actual built shape
     * reactivity directly, rather than needing to back-derive it by subtracting the other displayed terms from
     * {@link #totalReactivity} by hand -- exactly the manual reverse-engineering a real playtest needed to
     * diagnose why bank 0 was running out of withdrawal margin even at 0% insertion. {@code cmfdResult} itself is
     * never synced to the client (it also carries a full per-node flux map, unneeded for display), so this is
     * computed server-side in {@link #updatePhysicsAmplitude()} and synced as a plain double like every other
     * reactivity-term getter here.
     */
    private double shapeReactivityPcm = 0.0;
    /**
     * Flux-squared-weighted average, across every loaded fuel port's own local Doppler reactivity, of the same
     * formula {@link #getDopplerReactivityPcm()} used to expose as a single reactor-wide value -- see
     * {@link #updateLocalDopplerAndXenonReactivity()}'s own JavaDoc for the full local-feedback derivation.
     * Computed server-side in {@link #updatePhysicsAmplitude()}, synced as a plain double like every other
     * reactivity-term field here.
     */
    private double localDopplerReactivityPcm = 0.0;
    /**
     * Max-minus-min spread (pcm) of local Doppler reactivity across every loaded fuel port, computed alongside
     * {@link #localDopplerReactivityPcm} in {@link #updateLocalDopplerAndXenonReactivity()} -- a debug-only
     * indicator that this reactor's own local feedback is actually diverging port-to-port (0 whenever every
     * port's local conditions coincide, which is otherwise indistinguishable from "local feedback isn't
     * working" by looking at {@link #localDopplerReactivityPcm} alone, since that value is identical to the old
     * flat model in exactly that same uniform case).
     */
    private double dopplerReactivityPcmSpread = 0.0;
    /** As {@link #dopplerReactivityPcmSpread}, but for local Xenon reactivity. */
    private double xenonReactivityPcmSpread = 0.0;
    /**
     * Flux-squared-weighted average, across every axial layer's own local moderator-temperature reactivity, of
     * the same quadratic formula {@link #getModeratorTemperatureReactivityPcm()} used to expose as a single
     * reactor-wide value -- see {@link #computeLocalModeratorTemperatureReactivityPcm()}'s own JavaDoc for the
     * axial-profile derivation. Computed server-side in {@link #updatePhysicsAmplitude()}, synced as a plain
     * double like every other reactivity-term field here.
     */
    private double localModeratorTemperatureReactivityPcm = 0.0;
    /**
     * Console display estimate (stage 6 of this mod's startup/shutdown sequence work, 2026-09-21) of remaining
     * "iodine pit" duration -- see {@link XenonDeadTimeCalibration#estimateRemainingDeadTimeSeconds}'s own
     * JavaDoc. Only recomputed (in {@link #syncPhysicsDisplayPeriodically()}) while not {@code OPERATING}/
     * {@code STARTUP}, since it assumes zero neutron population going forward; stays at its last value otherwise
     * (display-only, never gates anything).
     */
    private double xenonDeadTimeRemainingSeconds = 0.0;

    /** Read by the control console screen part over the reactor-console link. */
    public double getRelativeNeutronPopulation() {
        return pointKineticsState.relativeNeutronPopulation;
    }

    /**
     * Experimental (2026-09-28, diagnostic-only, see this mod's design memory): exposes
     * the source term's relative rate and whether {@link #pointKineticsConstants}/
     * {@link #cmfdResult} are currently non-null, for {@link ReactorTelemetryLogger} to log directly -- narrows
     * down why {@link #pointKineticsState}'s own population has stayed at exactly 0 despite a real playtest
     * confirming a fresh (99.66%-remaining) neutron source rod is loaded and reactivity is comfortably positive.
     */
    public double getNeutronSourceSeedForDebug() {
        // Since stage 2-1 this is the source term's relative rate (per second), not a one-off seed.
        return neutronSourceRate;
    }

    /** See {@link #getNeutronSourceSeedForDebug()}'s own JavaDoc. */
    public boolean hasPointKineticsConstantsForDebug() {
        return pointKineticsConstants != null;
    }

    /** See {@link #getNeutronSourceSeedForDebug()}'s own JavaDoc. */
    public boolean hasCmfdResultForDebug() {
        return cmfdResult != null;
    }

    /** Read by the control console screen part over the reactor-console link. */
    public double getTotalReactivity() {
        return totalReactivity;
    }

    /** See {@link #shapeReactivityPcm}'s own JavaDoc. Read by the control console screen part. */
    public double getShapeReactivityPcm() {
        return shapeReactivityPcm;
    }

    /** Read by the control console screen part over the reactor-console link. */
    public double getXenonReactivityWorth() {
        return xenonIodineState.xenonConcentration();
    }

    /**
     * Read by the control console screen part over the reactor-console link -- see
     * {@link #xenonDeadTimeRemainingSeconds}.
     */
    public double getXenonDeadTimeRemainingSeconds() {
        return xenonDeadTimeRemainingSeconds;
    }

    /**
     * Reactivity contribution (pcm) from the moderator temperature coefficient -- the flux-squared-weighted
     * average of each axial layer's own local moderator-temperature reactivity, not a single reactor-wide value
     * computed from {@link #temperatureKelvin} directly (see
     * {@link #computeLocalModeratorTemperatureReactivityPcm()}'s own JavaDoc for why, and for the axial-profile
     * derivation in general). See {@link ModeratorTemperatureReactivity}'s own JavaDoc for why each layer's own
     * evaluation is a quadratic (not flat-linear) curve in temperature.
     */
    public double getModeratorTemperatureReactivityPcm() {
        return localModeratorTemperatureReactivityPcm;
    }

    /**
     * Reactivity contribution (pcm) from the Doppler (fuel temperature) coefficient -- the flux-squared-weighted
     * average of each fuel port's own local Doppler reactivity, not a single reactor-wide value computed from
     * {@link #fuelTemperatureKelvin} directly (see {@link #updateLocalDopplerAndXenonReactivity()}'s own JavaDoc
     * for why, and for the local-feedback derivation in general). Equal to the old flat-{@link
     * #fuelTemperatureKelvin} formula whenever every port's local conditions happen to coincide.
     */
    public double getDopplerReactivityPcm() {
        return localDopplerReactivityPcm;
    }

    /** See {@link #dopplerReactivityPcmSpread}'s own JavaDoc. Read by the control console screen part. */
    public double getDopplerReactivityPcmSpread() {
        return dopplerReactivityPcmSpread;
    }

    /** See {@link #xenonReactivityPcmSpread}'s own JavaDoc. Read by the control console screen part. */
    public double getXenonReactivityPcmSpread() {
        return xenonReactivityPcmSpread;
    }

    private int totalHeightLayers() {
        return LAYERS_BELOW_CONTROLLER + 1 + flexibleMiddleLayers + FIXED_LAYERS_ABOVE_CONTROLLER;
    }

    /**
     * This reactor's own size-scaled "full tank" primary coolant mass -- see {@link RatedCoolantMass}. Computed
     * fresh server-side wherever it's needed; {@link #ratedCoolantMassKg} is the cached, client-synced copy
     * {@link #getRatedCoolantMassKg()} actually returns (see that field's own JavaDoc).
     */
    @Override
    public double computeRatedCoolantMassKg() {
        return RatedCoolantMass.ratedMassKg(interiorCellsPerLayer * totalHeightLayers(),
                ConfigHolder.reactor.ratedCoolantMassPerInteriorCellKg);
    }

    /**
     * This reactor's own size-scaled rated thermal power (W) at relative power 1.0 -- same scaling pattern as
     * {@link #computeRatedCoolantMassKg()}.
     */
    private double computeRatedThermalPowerWatts() {
        return RatedCoolantMass.ratedMassKg(interiorCellsPerLayer * totalHeightLayers(),
                ConfigHolder.reactor.ratedThermalPowerPerInteriorCellWatts);
    }

    /** This reactor's own size-scaled fuel-to-coolant heat transfer coefficient (W/K) -- same scaling pattern. */
    private double computeFuelCoolantUA() {
        return RatedCoolantMass.ratedMassKg(interiorCellsPerLayer * totalHeightLayers(),
                ConfigHolder.reactor.fuelCoolantHeatTransferPerInteriorCellWattsPerKelvin);
    }

    /**
     * Total UO2 fuel mass (kg) currently loaded -- each assembly holds {@code L *
     * ConfigHolder.reactor.fuelUraniumPerLayerKg} of uranium (stage 1-3: proportional to its fabricated length),
     * converted to oxide mass, ignoring cladding and the (slight, mass-conserving) composition shift from burnup.
     */
    @Override
    public double computeTotalCoreFuelMassKg() {
        double uraniumKg = 0.0;
        for (MetaTileEntityFuelAssemblyPort fuelPort : cachedFuelPorts) {
            uraniumKg += fuelPort.getFuelLength() * ConfigHolder.reactor.fuelUraniumPerLayerKg;
        }
        return uraniumKg * ItemFuelRod.UO2_TO_URANIUM_MASS_RATIO;
    }

    /**
     * The "shape" half of the quasi-static method: rebuilds {@link #cmfdResult} and
     * {@link #pointKineticsConstants} from the reactor's currently-loaded fuel every
     * {@link #PHYSICS_SHAPE_RECOMPUTE_INTERVAL_TICKS} ticks, with the delayed neutron data weighted by each
     * nuclide's fission rate in the new shape (stage 2-2), and refreshes the external neutron source term
     * ({@link #computeSourceNeutronsPerSecond()}, stage 2-1). Before stage 2-1 there was no source term: a population
     * at zero was instead "seeded" once from the source hatch's strength, and a subcritical core decayed toward
     * zero (the 1e-97 telemetry readings); the source term now holds a subcritical core at its physical
     * source-driven level and starts a core from a true zero.
     */
    private void updatePhysicsShape() {
        if (physicsShapeTicksUntilRecompute > 0) {
            physicsShapeTicksUntilRecompute--;
            return;
        }
        physicsShapeTicksUntilRecompute = PHYSICS_SHAPE_RECOMPUTE_INTERVAL_TICKS;

        advanceNeutronSourceDecay();
        lengthMismatch = computeLengthMismatch();
        // Stage 2-3a: write every port's in-memory fuel state back into its item once per shape recompute (5 s).
        for (MetaTileEntityFuelAssemblyPort port : cachedAssemblyPorts) port.flushFuelState();

        ReactorGeometry geometry = buildReactorGeometry();
        cmfdResult = geometry.size() > 0 ? CMFDSolver.solve(geometry) : null;

        // Stage 2-2: delayed neutron data weighted by each nuclide's fission rate in this CMFD shape.
        Map<Element, Double> fissionRates = new HashMap<>();
        if (cmfdResult != null) {
            for (MetaTileEntityFuelAssemblyPort fuelPort : cachedFuelPorts) {
                FuelComposition composition = fuelPort.getLoadedComposition();
                if (composition == null) continue;
                NodePosition top = fuelPortTopPosition(fuelPort);
                double fastSum = 0.0;
                double thermalSum = 0.0;
                for (int y = CHANNEL_BOTTOM_Y_RELATIVE; y <= top.y(); y++) {
                    NodePosition node = new NodePosition(top.x(), y, top.z());
                    fastSum += cmfdResult.fastFlux().getOrDefault(node, 0.0);
                    thermalSum += cmfdResult.thermalFlux().getOrDefault(node, 0.0);
                }
                PointKineticsConstants.accumulateFissionRates(fissionRates, composition.moleFractionByIsotope,
                        fastSum, thermalSum);
            }
        }
        pointKineticsConstants = fissionRates.isEmpty() ? null :
                PointKineticsConstants.fissionWeighted(fissionRates,
                        PointKineticsConstants.STANDARD_PROMPT_NEUTRON_LIFETIME);
        // Stage 2-1: a real source term replaces the old one-off "seed" -- see NeutronSourceStrength.
        sourceNeutronsPerSecond = computeSourceNeutronsPerSecond();
        neutronSourceRate = NeutronSourceStrength.relativeRate(sourceNeutronsPerSecond,
                computeRatedThermalPowerWatts(), PointKineticsConstants.STANDARD_PROMPT_NEUTRON_LIFETIME);
    }

    /**
     * External neutron source (neutrons/s) for point kinetics, refreshed with every shape recompute (stage 2-1):
     * the startup source hatch's Cf-252 plus the loaded fuel's own spontaneous fission, times
     * {@code ConfigHolder.reactor.neutronSourceStrengthMultiplier}.
     */
    private double sourceNeutronsPerSecond = 0.0;
    /** {@link #sourceNeutronsPerSecond} in point kinetics' relative units (per second). */
    private double neutronSourceRate = 0.0;

    public double getSourceNeutronsPerSecond() {
        return sourceNeutronsPerSecond;
    }

    /**
     * Advances the loaded neutron source hatch's rod (if any) by one shape-recompute interval's worth of decay,
     * using a dedicated compressed half-life ({@code ConfigHolder.reactor.neutronSourceCompressedHalfLifeDays})
     * rather than the shared {@code TimeCompression} -- see {@code NeutronSourceMaterials}'s own JavaDoc for why
     * reusing that shared calibration is not appropriate here. Only Californium-252 itself ever has a positive
     * half-life in this item's inventory (its daughter, Curium-248, is registered stable -- see
     * {@code GTNLElements}), so this fixed-value compressor never needs to vary by isotope.
     */
    private void advanceNeutronSourceDecay() {
        for (IMultiblockPart part : getMultiblockParts()) {
            if (!(part instanceof MetaTileEntityNeutronSourceHatch sourceHatch)) continue;
            ItemStack stack = sourceHatch.getInsertedItem();
            if (!(stack.getItem() instanceof ItemNeutronSourceRod sourceRod)) return;

            Map<Element, Double> current = sourceRod.getIsotopeAmounts(stack);
            double elapsedSeconds = PHYSICS_SHAPE_RECOMPUTE_INTERVAL_TICKS * TICK_SECONDS;
            Map<Element, Double> decayed = DecayEngine.advance(current, elapsedSeconds,
                    real -> ConfigHolder.reactor.neutronSourceCompressedHalfLifeDays * 86400.0);
            sourceHatch.setLoadedIsotopeAmounts(decayed);
            return;
        }
    }

    /**
     * @return the Cf-252 source (physical strength from its real half-life -- see
     *         {@link NeutronSourceStrength#cf252NeutronsPerSecond}) plus the fuel's own spontaneous fission, in
     *         neutrons/s, scaled by {@code ConfigHolder.reactor.neutronSourceStrengthMultiplier}
     */
    private double computeSourceNeutronsPerSecond() {
        double neutrons = 0.0;
        for (IMultiblockPart part : getMultiblockParts()) {
            if (!(part instanceof MetaTileEntityNeutronSourceHatch sourceHatch)) continue;
            ItemStack stack = sourceHatch.getInsertedItem();
            if (stack.getItem() instanceof ItemNeutronSourceRod sourceRod) {
                // The source rod's inventory is kept in grams.
                neutrons += NeutronSourceStrength.cf252NeutronsPerSecond(
                        sourceRod.getIsotopeAmounts(stack).getOrDefault(GTNLElements.Cf252, 0.0));
            }
            break;
        }
        for (MetaTileEntityFuelAssemblyPort fuelPort : cachedFuelPorts) {
            neutrons += NeutronSourceStrength.intrinsicNeutronsPerSecond(fuelPort.getIsotopeMassesGrams());
        }
        return neutrons * ConfigHolder.reactor.neutronSourceStrengthMultiplier;
    }

    /**
     * One CMFD column per interior channel cell of the vessel (stage 1-3 (b), 2026-11-21), from
     * {@link #CHANNEL_BOTTOM_Y_RELATIVE} up to the top cap: a loaded fuel assembly fills the bottom {@code L}
     * layers of its column with its own composition and leaves water above it; a fuel-less port or a plain casing
     * cell is a full water column. Control rod clusters do not change the shape (their worth is still the
     * separate per-tick amplitude term until stage 4). Node positions are plain world offsets from the controller
     * -- {@link ReactorGeometry} only needs consistent face-adjacency.
     */
    @NotNull
    private ReactorGeometry buildReactorGeometry() {
        ReactorGeometry geometry = new ReactorGeometry(CHANNEL_NODE_SIZE_CM);
        if (getWorld() == null) return geometry;
        Map<Long, MetaTileEntityFuelAssemblyPort> fuelPortsByPos = new HashMap<>();
        for (MetaTileEntityFuelAssemblyPort port : cachedFuelPorts) fuelPortsByPos.put(port.getPos().toLong(), port);
        int topYRelative = channelTopYRelative();
        for (ReactorCrossSection.Cell cell : crossSection().interiorCells()) {
            BlockPos pos = topCapPosition(cell);
            int dx = pos.getX() - getPos().getX();
            int dz = pos.getZ() - getPos().getZ();
            MetaTileEntityFuelAssemblyPort fuelPort = fuelPortsByPos.get(pos.toLong());
            FuelComposition composition = fuelPort != null ? fuelPort.getLoadedComposition() : null;
            int fuelLength = fuelPort != null ? Math.min(fuelPort.getFuelLength(), getChannelHeightLayers()) : 0;
            ReactorGeometryBuilder.addFuelAssemblyColumn(geometry, dx, dz, CHANNEL_BOTTOM_Y_RELATIVE, topYRelative,
                    fuelLength, composition);
        }
        return geometry;
    }

    /**
     * The "amplitude" half of the quasi-static method: every tick, sums this tick's total reactivity (CMFD shape
     * + control rods + xenon + boron) and advances both {@link #pointKineticsState} and {@link #xenonIodineState}
     * by one tick. Runs unconditionally regardless of {@link #operatingState} -- with control rod controllers
     * defaulting to fully inserted (see that class's own JavaDoc), correctness falls out of the physics itself
     * (deeply negative control rod reactivity keeps the population decayed near zero) rather than needing an
     * explicit "only run while OPERATING" gate, which would anticipate console write-direction commands
     * (starting/stopping the reactor) that do not exist yet.
     */
    private void updatePhysicsAmplitude() {
        if (cmfdResult == null || pointKineticsConstants == null) {
            pointKineticsState = new PointKineticsState(0.0, new double[0]);
            xenonIodineState = new XenonIodineState(0.0, 0.0);
            totalReactivity = 0.0;
            shapeReactivityPcm = 0.0;
            localDopplerReactivityPcm = 0.0;
            localModeratorTemperatureReactivityPcm = 0.0;
            return;
        }

        // Without this, pointKineticsState could stay at its initial zero-length precursor array while
        // pointKineticsConstants.groupCount() is already nonzero (fuel loaded), and PointKineticsSolver.advance()
        // would throw ArrayIndexOutOfBoundsException (a real playtest crash, 2026-09-22). Falls back to a genuine
        // zero population of the correct length; the source term (stage 2-1) then raises it physically.
        if (pointKineticsState.precursorConcentrations.length != pointKineticsConstants.groupCount()) {
            pointKineticsState = new PointKineticsState(0.0, new double[pointKineticsConstants.groupCount()]);
        }

        double shapeReactivity = cmfdResult.kEffective() > 0 ? (cmfdResult.kEffective() - 1) / cmfdResult.kEffective() :
                -1.0;
        shapeReactivityPcm = shapeReactivity * 1.0e5;
        double controlRodReactivity = sumControlRodReactivity();
        // Local (per-fuel-port) Doppler/Xenon -- see updateLocalDopplerAndXenonReactivity()'s own JavaDoc. Writes
        // localDopplerReactivityPcm and (as a flux-squared-weighted display aggregate, no longer an independently
        // simulated state) xenonIodineState, both read below exactly as the pre-local-feedback code read them.
        updateLocalDopplerAndXenonReactivity();
        double xenonReactivity = xenonIodineState.reactivity();
        double boronReactivityValue = BoronReactivity.reactivity(boronPpm, ConfigHolder.reactor.boronWorthPerPpm);
        // A PWR's inherent void feedback: primary coolant voiding (see updateFuelAndCoolantThermal()'s
        // subcooling-margin check) reduces moderation, a negative feedback that -- unlike xenon/boron -- responds
        // within the same tick it occurs. Added 2026-09-24 (real-playtest-driven): unlike this, the moderator
        // temperature coefficient below responds continuously across the ENTIRE operating temperature range, not
        // only once bulk boiling has already started -- this reactor's actual first line of defense, matching a
        // real PWR's own continuous negative feedback (see getModeratorTemperatureReactivityPcm()'s own JavaDoc).
        double voidReactivity = ConfigHolder.reactor.voidReactivityCoefficientPcm * primaryVoidFraction / 1.0e5;
        localModeratorTemperatureReactivityPcm = computeLocalModeratorTemperatureReactivityPcm();
        double moderatorTemperatureReactivity = localModeratorTemperatureReactivityPcm / 1.0e5;
        // Doppler (fuel temperature) feedback: unlike moderatorTemperatureReactivity above (which lags behind
        // fission power changes through fuelCoolantHeatTransferPerInteriorCellWattsPerKelvin's own heat-transfer
        // delay), this responds to local fuel temperature directly -- a real PWR's actual fastest-acting negative
        // feedback, present from the instant fuel itself starts heating. See dopplerCoefficientPcmPerKelvin's own
        // JavaDoc for why this was missing entirely before (2026-09-26, real-PWR-data-driven analysis).
        double dopplerReactivity = localDopplerReactivityPcm / 1.0e5;
        totalReactivity = shapeReactivity + controlRodReactivity + xenonReactivity + boronReactivityValue +
                voidReactivity + moderatorTemperatureReactivity + dopplerReactivity;

        pointKineticsState = PointKineticsSolver.advance(pointKineticsState, totalReactivity, TICK_SECONDS,
                pointKineticsConstants, neutronSourceRate);

        // Complementary post-advance half of updateLocalDopplerAndXenonReactivity() -- see
        // advanceLocalXenonStates()'s own JavaDoc for why this runs after (not folded into) the pass above.
        advanceLocalXenonStates();
    }

    /**
     * The CMFD node position of this port's fuel assembly's top fuel layer ({@code CHANNEL_BOTTOM + L - 1}, stage
     * 1-3), relative to this controller -- the shared key {@link #cmfdResult}'s flux maps are read by, in
     * {@link #updateFuelBurnup()}, every local-feedback method, and the control rod flux weighting (a cluster
     * always sits in a fuelled channel: the slot rules require the fuel before the insert).
     */
    @NotNull
    private NodePosition fuelPortTopPosition(@NotNull MetaTileEntityFuelAssemblyPort fuelPort) {
        BlockPos pos = fuelPort.getPos();
        int length = Math.max(1, Math.min(fuelPort.getFuelLength(), getChannelHeightLayers()));
        return new NodePosition(pos.getX() - getPos().getX(), CHANNEL_BOTTOM_Y_RELATIVE + length - 1,
                pos.getZ() - getPos().getZ());
    }

    /**
     * Local (per-fuel-port) Doppler and Xenon feedback -- see this mod's design memory for the full derivation:
     * first-order perturbation theory for a two-group diffusion eigenvalue problem reduces, for a self-adjoint-
     * ish system, to a flux-squared-weighted average of each node's own local reactivity-formula evaluation
     * (Doppler against the fast-group flux, since Doppler/resonance capture is a fast/epithermal phenomenon in
     * this mod's two-group split; Xenon against the thermal-group flux, since Xe-135 absorption is almost purely
     * thermal). Reusing {@link #cmfdResult}'s already-normalized flux (core average 1.0) rather than CMFD's own
     * raw node data means no new calibration constant or diagnostic test is needed: whenever every port shares
     * the same local value, the weighted average ({@link FluxWeightedAverage}) is exactly that value regardless
     * of the weight distribution, reproducing the old reactor-wide-scalar behavior exactly rather than only
     * approximately.
     * <p>
     * Called from {@link #updatePhysicsAmplitude()} <i>before</i> this tick's {@link #pointKineticsState}
     * advance, reading whichever local xenon state each fuel rod's own NBT currently holds (i.e. the end of the
     * previous tick) -- mirroring how the pre-local-feedback code read the old (reactor-wide) {@link
     * #xenonIodineState} before advancing it. {@link #advanceLocalXenonStates()} is the complementary
     * post-advance half.
     * <p>
     * Local fuel temperature is deliberately <b>not</b> its own independently-simulated per-port state (unlike
     * local xenon, which genuinely needs one -- iodine-precursor decay gives it real multi-hour memory that an
     * instantaneous formula cannot capture): {@link #fuelTemperatureKelvin}'s own fuel-coolant coupling already
     * needs many sub-steps per tick to stay numerically stable (see {@link #computeFuelCoolantSubSteps}'s own
     * JavaDoc), meaning real fuel-coolant equilibration is fast compared to a tick, i.e. already close to
     * quasi-steady-state at tick resolution. Each port's own local temperature is instead computed as a
     * <i>deviation</i> from the existing {@link #fuelTemperatureKelvin} baseline, proportional to how far that
     * port's own local relative power sits from the reactor-wide average: {@code localTemp =
     * fuelTemperatureKelvin + (ratedThermalPowerWatts / fuelCoolantUA) * (localRelativePower -
     * globalRelativePower)} -- the local steady-state fuel-coolant gap implied by that port's own power share,
     * added on top of whatever the real (possibly still-transient) global gap currently is. When every port
     * shares the same local relative power the correction is exactly zero and every port's local temperature
     * exactly equals {@link #fuelTemperatureKelvin}, preserving backward compatibility even mid-transient, not
     * only at steady state.
     */
    private void updateLocalDopplerAndXenonReactivity() {
        List<MetaTileEntityFuelAssemblyPort> fuelPorts = new ArrayList<>();
        for (MetaTileEntityFuelAssemblyPort fuelPort : cachedFuelPorts) {
            if (fuelPort.getFuelStack().getItem() instanceof ItemFuelRod) {
                fuelPorts.add(fuelPort);
            }
        }
        if (cmfdResult == null || fuelPorts.isEmpty()) {
            localDopplerReactivityPcm = 0.0;
            dopplerReactivityPcmSpread = 0.0;
            xenonReactivityPcmSpread = 0.0;
            xenonIodineState = new XenonIodineState(0.0, 0.0);
            return;
        }

        double globalRelativePower = pointKineticsState.relativeNeutronPopulation;
        double fuelCoolantUA = computeFuelCoolantUA();
        double temperatureSensitivity = fuelCoolantUA > 0 ? computeRatedThermalPowerWatts() / fuelCoolantUA : 0.0;

        double[] dopplerValues = new double[fuelPorts.size()];
        double[] dopplerWeights = new double[fuelPorts.size()];
        double[] iodineValues = new double[fuelPorts.size()];
        double[] xenonValues = new double[fuelPorts.size()];
        double[] xenonWeights = new double[fuelPorts.size()];

        for (int i = 0; i < fuelPorts.size(); i++) {
            MetaTileEntityFuelAssemblyPort fuelPort = fuelPorts.get(i);
            ItemStack stack = fuelPort.getFuelStack();
            ItemFuelRod fuelRod = (ItemFuelRod) stack.getItem();
            NodePosition topPosition = fuelPortTopPosition(fuelPort);
            double fastFlux = cmfdResult.fastFlux().getOrDefault(topPosition, 0.0);
            double thermalFlux = cmfdResult.thermalFlux().getOrDefault(topPosition, 0.0);
            double localRelativePower = (fastFlux + thermalFlux) * globalRelativePower;

            double localFuelTemperatureKelvin = fuelTemperatureKelvin +
                    temperatureSensitivity * (localRelativePower - globalRelativePower);
            dopplerValues[i] = ConfigHolder.reactor.dopplerCoefficientPcmPerKelvin *
                    (localFuelTemperatureKelvin - AMBIENT_TEMPERATURE_KELVIN);
            dopplerWeights[i] = fastFlux * fastFlux;

            XenonIodineState localState = fuelPort.getXenonIodineState(xenonIodineState);
            iodineValues[i] = localState.iodineConcentration();
            xenonValues[i] = localState.xenonConcentration();
            xenonWeights[i] = thermalFlux * thermalFlux;
        }

        localDopplerReactivityPcm = FluxWeightedAverage.average(dopplerValues, dopplerWeights);
        double averageIodine = FluxWeightedAverage.average(iodineValues, xenonWeights);
        double averageXenon = FluxWeightedAverage.average(xenonValues, xenonWeights);
        xenonIodineState = new XenonIodineState(averageIodine, averageXenon);

        // Debug-only spread indicators -- see dopplerReactivityPcmSpread's own JavaDoc for why these exist
        // separately from the weighted averages above.
        dopplerReactivityPcmSpread = FluxWeightedAverage.spread(dopplerValues);
        double[] xenonReactivityPcmValues = new double[xenonValues.length];
        for (int i = 0; i < xenonValues.length; i++) xenonReactivityPcmValues[i] = -xenonValues[i] * 1.0e5;
        xenonReactivityPcmSpread = FluxWeightedAverage.spread(xenonReactivityPcmValues);
    }

    /**
     * Steps each fuel port's own local xenon/iodine ODE forward one tick using THIS tick's just-advanced
     * {@link #pointKineticsState} -- the complementary post-advance half of
     * {@link #updateLocalDopplerAndXenonReactivity()}, mirroring the pre-local-feedback ordering ({@code
     * xenonIodineState = XenonIodineSolver.advance(xenonIodineState, pointKineticsState.relativeNeutronPopulation,
     * ...)} ran after the point-kinetics advance, not before).
     */
    private void advanceLocalXenonStates() {
        if (cmfdResult == null) return;
        double globalRelativePower = pointKineticsState.relativeNeutronPopulation;
        XenonIodineConstants xenonConstants = new XenonIodineConstants(ConfigHolder.reactor.xenonDeadTimeSeconds,
                ConfigHolder.reactor.equilibriumXenonWorth);
        for (MetaTileEntityFuelAssemblyPort fuelPort : cachedFuelPorts) {
            ItemStack stack = fuelPort.getFuelStack();
            if (!(stack.getItem() instanceof ItemFuelRod fuelRod)) continue;

            NodePosition topPosition = fuelPortTopPosition(fuelPort);
            double fastFlux = cmfdResult.fastFlux().getOrDefault(topPosition, 0.0);
            double thermalFlux = cmfdResult.thermalFlux().getOrDefault(topPosition, 0.0);
            double localRelativePower = (fastFlux + thermalFlux) * globalRelativePower;

            XenonIodineState localState = fuelPort.getXenonIodineState(xenonIodineState);
            XenonIodineState advanced = XenonIodineSolver.advance(localState, localRelativePower, TICK_SECONDS,
                    xenonConstants);
            fuelPort.setXenonIodineState(advanced);
        }
    }

    /**
     * Axial (per-layer) local moderator-temperature-coefficient feedback -- see this mod's design memory for
     * the full derivation. Unlike local Doppler/Xenon (radial, per fuel port), MTC varies primarily along the
     * primary coolant's own flow direction (bottom to top of each channel): real PWR coolant genuinely heats up
     * as it rises through the core, and this reactor's own CMFD geometry already resolves flux per axial layer
     * (not just per channel), so the real local power shape at each layer is directly available.
     * <p>
     * Derivation, in three steps:
     * <ol>
     * <li>For each layer y, sum the raw fission source ({@code nuFission * flux}, both groups) across every
     * fuel channel's own node at that specific y -- {@link ReactorNodeFactory#fuelNode} is a pure function of
     * position and composition (no solve needed), so each fuel port's own node is reconstructed once and reused
     * for every layer in its own column (the same "formal 3D, same composition repeated per layer" geometry
     * {@link #buildReactorGeometry()} itself uses). Rescaling this raw per-layer sum so the reactor-wide total
     * matches the existing global {@code ratedThermalPowerWatts * relativeNeutronPopulation} figure gives each
     * layer's own local power in real Watts, consistent with the existing global energy accounting.</li>
     * <li>Converts each layer's local power into a temperature <i>rise</i> via the textbook heat balance
     * {@code deltaT = Q / (massFlowRate * specificHeat)} ({@code primaryCirculationRateKgPerSecond} /
     * {@link CoolantThermalSolver#REPRESENTATIVE_SPECIFIC_HEAT_CAPACITY}), then cumulatively sums bottom-to-top
     * for the raw (shape-only) axial profile.</li>
     * <li><b>Anchoring</b>: shifts that raw profile (a uniform offset, preserving its shape) so the
     * thermal-flux-squared-weighted average of the shifted profile across every layer (fuel <i>and</i> control
     * rod channel nodes alike -- coolant flows through both) exactly equals the existing bulk
     * {@link #temperatureKelvin}. {@link #temperatureKelvin} is this reactor's own single lumped average, not
     * specifically an inlet or outlet value, so anchoring the weighted <i>average</i> (not either endpoint) is
     * the correct backward-compatible reference -- this does not change {@link #temperatureKelvin}'s own total
     * mass/energy accounting (SCRAM threshold included) at all, it is purely a display/feedback-only overlay.
     * </li>
     * </ol>
     * Each layer's own {@link ModeratorTemperatureReactivity#reactivityPcm} evaluation at its anchored
     * temperature is then combined via the same thermal-flux-squared {@link FluxWeightedAverage} as every other
     * local feedback term. Unlike local Doppler/Xenon, a perfectly radially-uniform core does <i>not</i> reduce
     * this to the old flat-single-temperature value exactly (even uniform local power still produces a
     * genuinely rising axial temperature profile as coolant flows through it, and {@link
     * ModeratorTemperatureReactivity}'s own quadratic curve is not linear in that spread) -- the anchoring step
     * is what preserves consistency with the existing accounting, not an exact-equality guarantee the way local
     * Doppler/Xenon's radial averaging has.
     */
    private double computeLocalModeratorTemperatureReactivityPcm() {
        if (cmfdResult == null || getWorld() == null) return 0.0;

        // Stage 1-3: every interior channel is a CMFD column (fuel below, water above / water only), so coolant
        // weighting runs over all of them while fission only comes from the fuelled layers of fuelled columns.
        List<NodePosition> fuelColumnPositions = new ArrayList<>();
        List<ReactorNode> fuelColumnNodes = new ArrayList<>();
        List<Integer> fuelColumnTopY = new ArrayList<>();
        for (MetaTileEntityFuelAssemblyPort fuelPort : cachedFuelPorts) {
            FuelComposition composition = fuelPort.getLoadedComposition();
            if (composition == null) continue;
            NodePosition topPosition = fuelPortTopPosition(fuelPort);
            fuelColumnPositions.add(topPosition);
            fuelColumnNodes.add(ReactorNodeFactory.fuelNode(topPosition, composition));
            fuelColumnTopY.add(topPosition.y());
        }
        if (fuelColumnPositions.isEmpty()) return 0.0;
        List<NodePosition> allColumnPositions = new ArrayList<>();
        for (ReactorCrossSection.Cell cell : crossSection().interiorCells()) {
            BlockPos pos = topCapPosition(cell);
            allColumnPositions.add(new NodePosition(pos.getX() - getPos().getX(), 0, pos.getZ() - getPos().getZ()));
        }

        int bottomLocalY = CHANNEL_BOTTOM_Y_RELATIVE;
        int topLocalY = channelTopYRelative();
        int layerCount = topLocalY - bottomLocalY + 1;
        if (layerCount <= 0) return 0.0;

        double[] layerRawFissionSource = new double[layerCount];
        double[] layerThermalFluxSquaredSum = new double[layerCount];
        double totalRawFissionSource = 0.0;
        for (int y = bottomLocalY; y <= topLocalY; y++) {
            int index = y - bottomLocalY;
            double layerSum = 0.0;
            for (int i = 0; i < fuelColumnPositions.size(); i++) {
                if (y > fuelColumnTopY.get(i)) continue;
                NodePosition fuelXz = fuelColumnPositions.get(i);
                NodePosition pos = new NodePosition(fuelXz.x(), y, fuelXz.z());
                ReactorNode node = fuelColumnNodes.get(i);
                double fastFlux = cmfdResult.fastFlux().getOrDefault(pos, 0.0);
                double thermalFlux = cmfdResult.thermalFlux().getOrDefault(pos, 0.0);
                layerSum += node.fast().nuFission() * fastFlux + node.thermal().nuFission() * thermalFlux;
            }
            for (NodePosition columnXz : allColumnPositions) {
                NodePosition pos = new NodePosition(columnXz.x(), y, columnXz.z());
                double thermalFlux = cmfdResult.thermalFlux().getOrDefault(pos, 0.0);
                layerThermalFluxSquaredSum[index] += thermalFlux * thermalFlux;
            }
            layerRawFissionSource[index] = layerSum;
            totalRawFissionSource += layerSum;
        }
        if (totalRawFissionSource <= 0.0) return 0.0;

        double totalPowerWatts = computeRatedThermalPowerWatts() * pointKineticsState.relativeNeutronPopulation;
        double massFlowRateKgPerSecond = ConfigHolder.reactor.primaryCirculationRateKgPerSecond;
        if (massFlowRateKgPerSecond <= 0.0) return 0.0;

        double[] rawProfileKelvin = new double[layerCount];
        double cumulativeRise = 0.0;
        for (int index = 0; index < layerCount; index++) {
            double layerPowerWatts = totalPowerWatts * layerRawFissionSource[index] / totalRawFissionSource;
            double layerRiseKelvin = layerPowerWatts /
                    (massFlowRateKgPerSecond * CoolantThermalSolver.REPRESENTATIVE_SPECIFIC_HEAT_CAPACITY);
            cumulativeRise += layerRiseKelvin;
            rawProfileKelvin[index] = cumulativeRise;
        }

        double weightedAverageRawKelvin = FluxWeightedAverage.average(rawProfileKelvin, layerThermalFluxSquaredSum);
        double anchorOffsetKelvin = temperatureKelvin - weightedAverageRawKelvin;

        double[] mtcPcmPerLayer = new double[layerCount];
        for (int index = 0; index < layerCount; index++) {
            double anchoredKelvin = rawProfileKelvin[index] + anchorOffsetKelvin;
            mtcPcmPerLayer[index] = ModeratorTemperatureReactivity.reactivityPcm(anchoredKelvin,
                    AMBIENT_TEMPERATURE_KELVIN, ConfigHolder.reactor.heatupTargetTemperatureKelvin,
                    ConfigHolder.reactor.moderatorTemperatureCoefficientPcmPerKelvin);
        }
        return FluxWeightedAverage.average(mtcPcmPerLayer, layerThermalFluxSquaredSum);
    }

    /**
     * Which isotope becomes which on neutron capture without fission -- see {@link FuelBurnupEngine}'s own
     * JavaDoc for why this mapping is deliberately NOT hardcoded inside that pure-function class itself (it must
     * stay safely referenceable from unit tests, which never fire Forge's {@code MaterialEvent}), and is instead
     * built here, in the gameplay layer, where {@link GTNLMaterials}' fields are guaranteed already populated by
     * the time any reactor actually ticks.
     */
    @NotNull
    private static Map<Element, Element> buildCaptureDaughters() {
        Map<Element, Element> daughters = new HashMap<>();
        daughters.put(Elements.U235, GTNLMaterials.Uranium236.getElement());
        daughters.put(Elements.U238, Elements.Pu239);
        daughters.put(Elements.Pu239, GTNLMaterials.Plutonium240.getElement());
        return daughters;
    }

    /**
     * Consumes each loaded fuel rod's own isotope inventory via {@link FuelBurnupEngine}, sampling the last CMFD
     * solve's local flux at that fuel port's own top-of-channel node -- the "formal 3D" geometry repeats the same
     * fuel composition at every layer of a channel (see {@link #buildReactorGeometry()}'s own JavaDoc), so a
     * single flux sample per fuel port is a reasonable simplification, same spirit as
     * {@link #aggregateFissionShares()}'s equal-weighting approximation. Runs every tick (unlike the rare CMFD
     * shape recompute) using whichever {@link #cmfdResult} is currently cached -- flux shape changes slowly
     * relative to burnup itself, so reusing a slightly stale flux distribution between shape recomputes is an
     * acceptable quasi-static approximation, the same division of labor used everywhere else in this class.
     */
    private void updateFuelBurnup() {
        if (cmfdResult == null || getWorld() == null) return;
        double relativePower = pointKineticsState.relativeNeutronPopulation;
        if (relativePower <= 0) return;
        Map<Element, Element> captureDaughters = buildCaptureDaughters();
        for (MetaTileEntityFuelAssemblyPort fuelPort : cachedFuelPorts) {
            ItemStack stack = fuelPort.getFuelStack();
            if (!(stack.getItem() instanceof ItemFuelRod fuelRod)) continue;

            NodePosition topPosition = fuelPortTopPosition(fuelPort);
            double localFastFlux = cmfdResult.fastFlux().getOrDefault(topPosition, 0.0);
            double localThermalFlux = cmfdResult.thermalFlux().getOrDefault(topPosition, 0.0);

            Map<Element, Double> current = fuelPort.getLoadedIsotopeAmounts();
            Map<Element, Double> next = FuelBurnupEngine.advance(current, captureDaughters, localFastFlux,
                    localThermalFlux, relativePower, ConfigHolder.reactor.fuelBurnupRateCoefficient, TICK_SECONDS);
            fuelPort.setLoadedIsotopeAmounts(next);
        }
    }

    /**
     * Wears every loaded control rod by {@code controlRodWearRateCoefficient * insertionDepth * localThermalFlux *
     * relativePower * dtSeconds} -- the approved "local neutron capture ~= CMFD thermal flux x power" approximation
     * (2026-09-20 design decision, see this mod's design memory), scaled by how much of the rod is actually
     * inserted into the flux region. Reuses the same per-rod {@link #cmfdResult} thermal-flux sample as
     * {@link #sumControlRodReactivity()} -- a rod sitting withdrawn near 0 insertion depth barely wears regardless
     * of local flux, matching the real-world pattern of load-following banks wearing much faster than banks left
     * withdrawn at rated power. {@link MetaTileEntityFuelAssemblyPort#applyWear} itself no-ops for an empty
     * slot or a non-rod item, so this loop does not need to check that first.
     */
    private void updateControlRodWear() {
        if (cmfdResult == null || getWorld() == null) return;
        double relativePower = pointKineticsState.relativeNeutronPopulation;
        if (relativePower <= 0) return;
        for (MetaTileEntityFuelAssemblyPort controlRod : cachedControlRods) {
            NodePosition topPosition = fuelPortTopPosition(controlRod);
            double thermalFlux = cmfdResult.thermalFlux().getOrDefault(topPosition, 0.0);
            double wear = ConfigHolder.reactor.controlRodWearRateCoefficient * controlRod.getInsertionDepth() *
                    thermalFlux * relativePower * TICK_SECONDS;
            controlRod.applyWear(wear);
        }
    }

    /**
     * Reference "cold shutdown" coolant temperature (K) -- both this reactor's own initial/reset value and
     * {@code ReactorCoolantTemperatureBands}'s own band 0. Named here since {@link #updatePrimaryCirculation()}
     * compares against it directly (gating the pump-out half on there actually being heat to move).
     */
    private static final double AMBIENT_TEMPERATURE_KELVIN = 300.0;

    /**
     * Advances {@link #fuelTemperatureKelvin} and {@link #temperatureKelvin} by one tick -- the fuel node
     * generates fission heat and sheds it to the coolant node via a size-scaled UA, the coolant node in turn
     * physically circulates to/from a piped heat exchanger (if any) and checks its own subcooling margin. See
     * this mod's design memory (2026-09-22) for why fuel and coolant are separate nodes: a real meltdown is a
     * "fuel heats up faster than coolant can carry the heat away" phenomenon that a single shared temperature
     * cannot represent.
     */
    private void updateFuelAndCoolantThermal() {
        // Root-caused 2026-09-25 (real-playtest-driven, see this mod's design memory): repeated floating-point
        // subtraction in updatePrimaryCirculation() can leave coolantMassKg as a tiny nonzero residual (observed:
        // ~3.7e-11 kg) instead of landing on exactly 0.0 once the primary loop genuinely runs dry (e.g. a
        // physically-stalled return circulation). Because computeFuelCoolantSubSteps()/CoolantThermalSolver#advance
        // both gate on a bare "> 0" check, that residual was treated as real coolant with an astronomically tiny
        // heat capacity, driving the required sub-step count past Integer.MAX_VALUE (confirmed via diagnostic
        // logging: subSteps saturated at 2147483647, ~12-13 SECONDS per tick, and fuelTemperatureKelvin/
        // temperatureKelvin both NaN). Snapping any sub-milligram residue to exactly 0.0 here lets the existing
        // "zero heat capacity is excluded" guards in both those call sites work as originally intended.
        if (coolantMassKg < 1.0e-6) coolantMassKg = 0.0;

        double fissionPowerWatts = computeRatedThermalPowerWatts() * pointKineticsState.relativeNeutronPopulation;
        double rcpHeatWatts = updateReactorPowerConsumption();
        double rhrNetWatts = updateResidualHeatRemoval();
        energyLedger.add(ReactorEnergyCategory.FISSION, fissionPowerWatts * TICK_SECONDS);
        energyLedger.add(ReactorEnergyCategory.RCP, rcpHeatWatts * TICK_SECONDS);
        double totalFuelMassKg = computeTotalCoreFuelMassKg();
        double fuelHeatCapacity = totalFuelMassKg * FuelThermalConstants.SPECIFIC_HEAT_CAPACITY_J_PER_KG_KELVIN;
        double coolantHeatCapacity = coolantMassKg * CoolantThermalSolver.REPRESENTATIVE_SPECIFIC_HEAT_CAPACITY;
        double fuelCoolantUA = computeFuelCoolantUA();

        int subSteps = FuelCoolantSubstepping.subStepCount(fuelCoolantUA, fuelHeatCapacity, coolantHeatCapacity,
                TICK_SECONDS);
        double subDtSeconds = TICK_SECONDS / subSteps;
        for (int i = 0; i < subSteps; i++) {
            double transferWatts = fuelCoolantUA * (fuelTemperatureKelvin - temperatureKelvin);
            FuelTemperatureStep.Result step = FuelTemperatureStep.advance(fuelTemperatureKelvin, meltedFuelMassKg,
                    coreFuelMassAtMeltdownStartKg, fissionPowerWatts, transferWatts, totalFuelMassKg,
                    fuelHeatCapacity, subDtSeconds);
            fuelTemperatureKelvin = step.fuelTemperatureKelvin();
            meltedFuelMassKg = step.meltedFuelMassKg();
            coreFuelMassAtMeltdownStartKg = step.coreFuelMassAtMeltdownStartKg();
            temperatureKelvin = CoolantThermalSolver.advance(temperatureKelvin,
                    transferWatts + rcpHeatWatts + rhrNetWatts,
                    coolantMassKg, CoolantThermalSolver.REPRESENTATIVE_SPECIFIC_HEAT_CAPACITY, subDtSeconds);
        }

        updatePrimaryCirculation();
        applyCoolantSubcoolingClamp();
        markDirty();
    }

    /**
     * Draws this tick's EU/t for the reactor's own instrumentation and coolant pump loads (see
     * {@link ReactorPowerConsumption}'s own JavaDoc for the two-term formula) from whatever
     * {@link MultiblockAbility#INPUT_ENERGY} hatch(es) are piped in, then converts the actually-drawn pump
     * portion (not the baseline instrumentation load, which represents non-heat-generating electronics) into
     * real Watts via {@code ConfigHolder.reactor.euPerTickToWattsConversion} -- modeling reactor coolant pump
     * friction heat entering the coolant directly, added 2026-09-28 (real-PWR-data-driven, see this mod's design
     * memory). This is a real, continuous physical process in an actual RCP, not exclusive to pre-criticality
     * heatup, so it applies identically regardless of {@link #operatingState}.
     * <p>
     * The pump load itself runs at full rated draw -- rather than scaling down with
     * {@link PointKineticsState#relativeNeutronPopulation} the way it does once {@code STARTUP}/{@code OPERATING}
     * -- while {@code SHUTDOWN} and still below {@code ConfigHolder.reactor.heatupTargetTemperatureKelvin}: a
     * real RCP runs near rated flow well before criticality specifically to heat up via friction, not because
     * there is yet any reactor heat to actually remove. If the connected hatch(es) cannot supply the full
     * desired amount, both terms simply draw whatever is available (capped, never negative) -- an underpowered
     * reactor heats up slower and generates less pump friction heat, rather than erroring.
     *
     * @return heat (W) actually delivered to the coolant this tick from the pump's own actually-drawn energy
     */
    private double updateReactorPowerConsumption() {
        IEnergyContainer energyContainer = new EnergyContainerList(getAbilities(MultiblockAbility.INPUT_ENERGY));
        int interiorCellCount = interiorCellsPerLayer * totalHeightLayers();
        double ratedThermalPowerWatts = computeRatedThermalPowerWatts();

        boolean heatupActive = operatingState.stage == ReactorOperatingStage.SHUTDOWN &&
                temperatureKelvin < ConfigHolder.reactor.heatupTargetTemperatureKelvin;
        double effectiveThermalOutputWatts = heatupActive ? ratedThermalPowerWatts :
                ratedThermalPowerWatts * pointKineticsState.relativeNeutronPopulation;

        long baseDesiredEuPerTick = Math.round(ReactorPowerConsumption.basePowerEuPerTick(interiorCellCount,
                ConfigHolder.reactor.baseConsumptionPerChannel));
        double pumpCoefficient = RatedCoolantMass.ratedMassKg(interiorCellCount,
                ConfigHolder.reactor.pumpConsumptionPerInteriorCellAtRatedOutput);
        // Stage 1-4a: the pumps (and with them heatup and their friction heat) only run on operator command.
        long pumpDesiredEuPerTick = !rcpRunning ? 0L :
                Math.round(ReactorPowerConsumption.pumpPowerEuPerTick(effectiveThermalOutputWatts,
                        ratedThermalPowerWatts, pumpCoefficient));

        long baseDrawnEuPerTick = ReactorPowerConsumption.drawEnergy(energyContainer, baseDesiredEuPerTick);
        long pumpDrawnEuPerTick = ReactorPowerConsumption.drawEnergy(energyContainer, pumpDesiredEuPerTick);
        totalPowerConsumptionEuPerTick = baseDrawnEuPerTick + pumpDrawnEuPerTick;
        pumpPowerUnavailable = pumpDesiredEuPerTick > 0 && pumpDrawnEuPerTick <= 0;

        return pumpDrawnEuPerTick * ConfigHolder.reactor.euPerTickToWattsConversion;
    }

    /**
     * Pumps hot primary coolant out to a piped heat exchanger's {@code REACTOR_COOLANT_IMPORT} hatch (via this
     * reactor's own, previously-unused {@code REACTOR_COOLANT_EXPORT} hatch), then drains whatever cooled coolant
     * has returned to this reactor's own {@code REACTOR_COOLANT_IMPORT} hatch, mixing it in by mass-weighted
     * temperature average. A no-op both ways with no heat exchanger piped in (the export tank fills up and stays
     * full, the import tank has nothing new to drain) -- degrading to this mod's previous "no heat removal path"
     * behavior automatically, no special-casing needed. See this mod's design memory (2026-09-22): primary
     * coolant circulates via real fluid pipes, not a logical link, so the reactor never needs to know whether a
     * heat exchanger exists at all.
     * <p>
     * Temperature is carried across the fluid boundary via
     * {@link GTNLMaterials#nearestReactorCoolantBandFluid}/{@link Fluid#getTemperature()} (a discrete, per-type
     * property), not {@code FluidStack} NBT -- see
     * {@code ReactorCoolantTemperatureBands}'s own JavaDoc for why an NBT tag that changes every tick is
     * fundamentally incompatible with Forge/GT's exact-match fluid merging (confirmed via a real playtest report,
     * 2026-09-22, of circulation silently stalling). The pump-out half only runs while {@link #temperatureKelvin}
     * is actually above ambient -- otherwise (no fuel, cold shutdown) there is nothing to circulate, and running
     * it anyway just dumps freshly-injected coolant into the export hatch with nothing to return it (confirmed via
     * a real playtest report, 2026-09-22, of injected coolant appearing to vanish). The return-drain half always
     * runs regardless -- whatever comes back is always worth accepting -- but is capped at this reactor's own
     * rated capacity ({@link #computeRatedCoolantMassKg()}), same as {@link #updateCoolantInjection()}'s own cap:
     * a closed loop's returning flow should never exceed a full tank, and without this cap a player continuously
     * pouring fresh coolant into the import hatch by hand (not at all the same thing as genuine circulated
     * return-flow, but indistinguishable from it at this hatch) could grow {@link #coolantMassKg} without bound
     * (confirmed via a real playtest report, 2026-09-22: coolant reaching over 20x rated capacity).
     * <p>
     * The pump-out half is further capped 2026-09-25 (real-playtest-driven, see this mod's design memory) to
     * never drain {@link #coolantMassKg} below {@code ConfigHolder.reactor.locaCoolantFraction} of
     * {@link #computeRatedCoolantMassKg()} -- reusing the same threshold {@link LOCADetector} already trips a
     * SCRAM on, rather than introducing a separate concept. Without this floor, pump-out starts at full budget
     * the instant {@link #temperatureKelvin} first crosses ambient with no ramp-up, and a real playtest log
     * showed this alone empties a full-rated reactor (445,588 kg) in ~16 seconds -- faster than a piped heat
     * exchanger's return flow could plausibly prime over any non-trivial pipe run, driving coolantMassKg to a
     * near-zero floating-point residue and, before the fix backing this, a multi-second-per-tick server stall.
     */
    private void updatePrimaryCirculation() {
        primaryCirculationFlowMb = 0;
        // Stage 1-4a: no reactor coolant pump, no loop flow (natural circulation is not modeled yet).
        if (!rcpRunning) return;
        int budgetMb = (int) Math.round(ConfigHolder.reactor.primaryCirculationRateKgPerSecond * TICK_SECONDS *
                1000.0);
        if (budgetMb <= 0) return;

        if (temperatureKelvin > AMBIENT_TEMPERATURE_KELVIN) {
            double minRetainedCoolantKg = ConfigHolder.reactor.locaCoolantFraction * computeRatedCoolantMassKg();
            double availableToExportKg = Math.max(0.0, coolantMassKg - minRetainedCoolantKg);
            int pumpedMb = Math.min(budgetMb, (int) Math.round(availableToExportKg * 1000.0));
            if (pumpedMb > 0) {
                Fluid outgoingFluid = GTNLMaterials.nearestReactorCoolantBandFluid(temperatureKelvin);
                int filledMb = 0;
                for (IFluidTank tank : getAbilities(GTNLMultiblockAbility.REACTOR_COOLANT_EXPORT)) {
                    if (filledMb >= pumpedMb) break;
                    filledMb += tank.fill(new FluidStack(outgoingFluid, pumpedMb - filledMb), true);
                }
                if (filledMb > 0) {
                    double exportedMassKg = filledMb / 1000.0;
                    energyLedger.add(ReactorEnergyCategory.EXPORT,
                            liquidEnthalpyJoules(exportedMassKg, temperatureKelvin));
                    energyLedger.add(ReactorEnergyCategory.BAND_ROUNDING, liquidEnthalpyJoules(exportedMassKg,
                            temperatureKelvin) - liquidEnthalpyJoules(exportedMassKg, outgoingFluid.getTemperature()));
                    coolantMassKg -= exportedMassKg;
                }
                primaryCirculationFlowMb = filledMb;
            }
        }

        double headroomKg = Math.max(0.0, computeRatedCoolantMassKg() - coolantMassKg);
        int drainBudgetMb = Math.min(budgetMb, (int) Math.round(headroomKg * 1000.0));
        int drainedMb = 0;
        for (IFluidTank tank : getAbilities(GTNLMultiblockAbility.REACTOR_COOLANT_IMPORT)) {
            if (drainedMb >= drainBudgetMb) break;
            FluidStack drained = tank.drain(drainBudgetMb - drainedMb, true);
            if (drained == null || drained.amount <= 0) continue;
            double drainedTemperatureKelvin = drained.getFluid().getTemperature();
            double drainedMassKg = drained.amount / 1000.0;
            energyLedger.add(ReactorEnergyCategory.IMPORT,
                    liquidEnthalpyJoules(drainedMassKg, drainedTemperatureKelvin));
            temperatureKelvin = CoolantThermalSolver.mixTemperatures(coolantMassKg, temperatureKelvin,
                    drainedMassKg, drainedTemperatureKelvin);
            coolantMassKg += drainedMassKg;
            drainedMb += drained.amount;
        }
    }

    /**
     * Checked once per tick, after {@link #temperatureKelvin} has already been advanced through the fuel-coolant
     * coupling sub-step loop in {@link #updateFuelAndCoolantThermal()}: once {@link #temperatureKelvin} is at or
     * above the primary loop's own saturation temperature at its current {@link #pressureMPa} (see
     * {@link WaterSaturationCurve}, written with exactly this "PWR primary coolant should stay subcooled" case in
     * mind), clamps it there and converts the excess energy into {@link #primaryVoidFraction} via water's latent
     * heat of vaporization instead -- the third instance of this mod's phase-change-plateau pattern (fuel
     * melting, this, and the secondary loop's own boiling). Below saturation, {@link #primaryVoidFraction}
     * instead decays back toward 0 (condensing), never instantly.
     */
    private void applyCoolantSubcoolingClamp() {
        double saturationTemperatureKelvin = WaterSaturationCurve.saturationTemperature(
                Math.max(pressureMPa, ATMOSPHERIC_PRESSURE_MPA) * 1.0e6);
        if (temperatureKelvin >= saturationTemperatureKelvin && coolantMassKg > 0) {
            double excessJoules = (temperatureKelvin - saturationTemperatureKelvin) * coolantMassKg *
                    CoolantThermalSolver.REPRESENTATIVE_SPECIFIC_HEAT_CAPACITY;
            temperatureKelvin = saturationTemperatureKelvin;
            primaryVoidFraction = Math.min(1.0, primaryVoidFraction +
                    excessJoules / (coolantMassKg * SteamProperties.LATENT_HEAT_OF_VAPORIZATION_J_PER_KG));
        } else {
            primaryVoidFraction = Math.max(0.0,
                    primaryVoidFraction - ConfigHolder.reactor.voidCondenseRatePerSecond * TICK_SECONDS);
        }
    }

    /**
     * Radially flux-squared-weighted sum of every control rod controller's own {@link ControlRodWorth#reactivity}
     * -- see this mod's design memory for the full local-feedback derivation. Weighting by {@link #cmfdResult}'s
     * own thermal flux at each rod's own channel position (not equally, as the old flat sum implicitly did)
     * means a rod in a higher-flux (typically more central) channel now correctly matters more than one in a
     * lower-flux (typically edge) channel. {@link ControlRodWorth}'s own cosine curve is left completely
     * untouched -- it already captures the <i>axial</i> worth shape (see that class's own JavaDoc: it is itself
     * derived from integrating a flux-squared axial mode shape over the inserted depth), so only the
     * <i>radial</i> (rod-to-rod) weighting is new here. Only called from {@link #updatePhysicsAmplitude()},
     * always after that method's own {@code cmfdResult == null} guard, so {@link #cmfdResult} is always
     * non-null here.
     * <p>
     * {@code FluxWeightedAverage.average(...) * rodCount} is the weighted-sum-normalized-by-mean-weight form
     * (see {@link FluxWeightedAverage}'s own JavaDoc): whenever every rod happens to sit at the same local
     * thermal flux (a symmetric core, the implicit assumption the old flat sum made), this reduces to exactly
     * the old {@code Σ_rods ControlRodWorth.reactivity(...)} sum.
     */
    private double sumControlRodReactivity() {
        List<Double> values = new ArrayList<>();
        List<Double> weights = new ArrayList<>();
        for (MetaTileEntityFuelAssemblyPort controlRod : cachedControlRods) {
            NodePosition topPosition = fuelPortTopPosition(controlRod);
            double thermalFlux = cmfdResult.thermalFlux().getOrDefault(topPosition, 0.0);
            values.add(ControlRodWorth.reactivity(controlRod.getInsertionDepth(), controlRod.getMaxWorthReactivity()));
            weights.add(thermalFlux * thermalFlux);
        }
        if (values.isEmpty()) return 0.0;
        double[] valuesArray = new double[values.size()];
        double[] weightsArray = new double[weights.size()];
        for (int i = 0; i < values.size(); i++) {
            valuesArray[i] = values.get(i);
            weightsArray[i] = weights.get(i);
        }
        return FluxWeightedAverage.average(valuesArray, weightsArray) * valuesArray.length;
    }

    /**
     * Sum of {@code maxWorthReactivity} (fraction units) across every currently-loaded control rod controller
     * assigned to {@code bankId} -- the bank's own total capacity, not its currently-applied reactivity (see
     * {@link #sumControlRodReactivity()} for that). Read by {@link ReactorAutoControlSequencer} (via
     * {@link AutoControlHost}) to derive a reactivity target from whatever rod worth this specific reactor
     * actually has built, rather than a flat constant that only happened to fit one reference reactor
     * (2026-09-29, see this mod's design memory on replacing magic constants with values computed from the
     * actually-built structure wherever the underlying quantity is not itself a real physical constant).
     */
    @Override
    public double getBankTotalWorthReactivity(int bankId) {
        double sum = 0.0;
        for (MetaTileEntityFuelAssemblyPort controlRod : cachedControlRods) {
            if (controlRod.getBankId() == bankId) sum += controlRod.getMaxWorthReactivity();
        }
        return sum;
    }

    /** {@link AutoControlHost#getAverageControlRodInsertionPercent()}: average over every control rod, any bank. */
    @Override
    public double getAverageControlRodInsertionPercent() {
        return controlRodSummaries.stream().mapToDouble(summary -> summary.insertionDepth() * 100.0)
                .average().orElse(0.0);
    }

    /** {@link AutoControlHost#getBank0AverageInsertionFraction()}: average over bank 0's own control rods only. */
    @Override
    public double getBank0AverageInsertionFraction() {
        return controlRodSummaries.stream().filter(summary -> summary.bankId() == 0)
                .mapToDouble(ControlRodSummary::insertionDepth).average().orElse(0.0);
    }

    /** A control rod at or above this insertion depth counts as "fully inserted" for {@link #isShutdownComplete()}. */
    private static final double FULLY_INSERTED_THRESHOLD = 0.999;

    /**
     * Console write-direction command: request the reactor begin startup. No-op unless currently
     * {@link ReactorOperatingStage#SHUTDOWN} -- see {@link ReactorOperatingEvaluator#beginStartup} -- and
     * {@link #isStartupReady()} (stage 6 of this mod's startup/shutdown sequence work, 2026-09-21: the plant must
     * actually be physically prepared -- filled, pressurized, AND (added 2026-09-28, mirroring a real PWR's own
     * pre-criticality heatup and Tech-Spec-enforced minimum criticality temperature) heated up -- first). Boron/
     * control rod adjustment are deliberately not preconditions here -- they happen within the STARTUP stage
     * itself, not before entering it.
     */
    public void requestStartup() {
        if (!isStartupReady()) return;
        applyOperatingStateChange(ReactorOperatingEvaluator.beginStartup(operatingState));
    }

    /** See {@link #requestStartup()}'s own JavaDoc. */
    private boolean isStartupReady() {
        // Stage 1-3: the core-operability checks (housing, lengths, fuel, >= 4 rod clusters) joined the plant
        // preparation checks here -- see ReactorStartupRequirements.
        return getStartupBlocker() == null;
    }

    /** Console write-direction command: request a planned shutdown. No-op unless currently {@code OPERATING}. */
    public void requestShutdown() {
        applyOperatingStateChange(ReactorOperatingEvaluator.beginShutdown(operatingState));
    }

    /** Console write-direction command: the only way out of {@code TRIPPED}. No-op unless currently tripped. */
    public void requestManualReset() {
        ReactorOperatingState before = operatingState;
        applyOperatingStateChange(ReactorOperatingEvaluator.manualReset(operatingState));
        if (operatingState != before && structureBreachRecorded) {
            structureBreachRecorded = false;
            markDirty();
        }
    }

    /** Console write-direction command: manual SCRAM button. See {@link #checkScramConditions()} for the rest. */
    public void requestManualScram() {
        applyOperatingStateChange(ReactorOperatingEvaluator.scram(operatingState, ScramReason.MANUAL));
    }

    /**
     * Console write-direction command: begin (or resume) primary coolant injection -- stage 2 of this mod's
     * startup/shutdown sequence work (2026-09-21). A no-op unless {@link #coolantMassKg} is at or below
     * {@code ConfigHolder.reactor.coolantInjectionTriggerFraction} of {@link #computeRatedCoolantMassKg()} --
     * this intentionally does not distinguish a true first-ever startup from a post-maintenance restart (see
     * this mod's design memory, 2026-09-21): whichever the case, "coolant is currently low" is exactly the
     * condition that makes injection meaningful, and it falls out of this same check either way (a fresh reactor
     * simply starts at 0 kg, trivially satisfying it).
     */
    public void requestBeginCoolantInjection() {
        // 2026-11-24: a toggle -- pressing again while injecting stops it (previously it could only stop on its
        // own at the rated mass). Starting keeps the original trigger-fraction condition.
        if (coolantInjectionActive) {
            coolantInjectionActive = false;
        } else if (coolantMassKg <= computeRatedCoolantMassKg() *
                ConfigHolder.reactor.coolantInjectionTriggerFraction) {
                    coolantInjectionActive = true;
                } else {
                    return;
                }
        writeCustomData(UPDATE_OPERATING_STATUS, this::writeOperatingStatus);
        markDirty();
    }

    /**
     * While {@link #coolantInjectionActive}, drains available fluid from the reactor coolant import and
     * emergency coolant import hatches' tanks (whichever ability instances happen to be placed -- basic or
     * advanced hatch variants alike, both register under the same {@link GTNLMultiblockAbility#REACTOR_COOLANT_IMPORT}/
     * {@link GTNLMultiblockAbility#EMERGENCY_COOLANT_IMPORT} abilities), rate-limited by
     * {@code ConfigHolder.reactor.coolantInjectionRateKgPerSecond}, converting 1000 mB to 1 kg. Auto-clears
     * {@link #coolantInjectionActive} once {@link #coolantMassKg} reaches {@link #computeRatedCoolantMassKg()}.
     */
    private void updateCoolantInjection() {
        if (!coolantInjectionActive) return;
        double currentRatedCoolantMassKg = computeRatedCoolantMassKg();
        if (coolantMassKg >= currentRatedCoolantMassKg) {
            coolantInjectionActive = false;
            writeCustomData(UPDATE_OPERATING_STATUS, this::writeOperatingStatus);
            markDirty();
            return;
        }

        double budgetKg = Math.min(ConfigHolder.reactor.coolantInjectionRateKgPerSecond * TICK_SECONDS,
                currentRatedCoolantMassKg - coolantMassKg);
        int budgetMb = (int) Math.round(budgetKg * 1000.0);
        if (budgetMb <= 0) {
            // The remaining deficit is sub-mb (below the fluid API's own integer precision), so it can never be
            // closed by draining -- snap directly to the target instead of leaving coolantInjectionActive stuck
            // on forever (confirmed via a real playtest, 2026-09-26: COOLANT showed 445588265/445588265 mb yet
            // still displayed "(INJECTING)" indefinitely).
            coolantMassKg = currentRatedCoolantMassKg;
            coolantInjectionActive = false;
            writeCustomData(UPDATE_OPERATING_STATUS, this::writeOperatingStatus);
            markDirty();
            return;
        }

        int drainedMb = 0;
        for (MultiblockAbility<IFluidTank> ability : Arrays.asList(GTNLMultiblockAbility.REACTOR_COOLANT_IMPORT,
                GTNLMultiblockAbility.EMERGENCY_COOLANT_IMPORT)) {
            for (IFluidTank tank : getAbilities(ability)) {
                if (drainedMb >= budgetMb) break;
                FluidStack drained = tank.drain(budgetMb - drainedMb, true);
                if (drained != null) {
                    drainedMb += drained.amount;
                    recordMakeupInflow(drained);
                }
            }
        }
        if (drainedMb > 0) {
            coolantMassKg += drainedMb / 1000.0;
            markDirty();
        }
    }

    /**
     * {@link ReactorEnergyCategory#MAKEUP_IN} for one drained batch of injection/boron-adjustment input, at the
     * fluid's own registered temperature -- the current model mixes these batches in without any temperature
     * effect, so the resulting residual is exactly the energy that simplification creates or destroys (see this
     * mod's design memory, X6).
     */
    private void recordMakeupInflow(@NotNull FluidStack drained) {
        energyLedger.add(ReactorEnergyCategory.MAKEUP_IN,
                liquidEnthalpyJoules(drained.amount / 1000.0, drained.getFluid().getTemperature()));
    }

    /**
     * Console write-direction command: begin (or resume) raising {@link #pressureMPa} toward
     * {@code ConfigHolder.reactor.targetOperatingPressureMPa}. Stage 3 of this mod's startup/shutdown sequence
     * work (2026-09-21), deliberately provisional -- see that config field's own comment for why. A no-op if
     * already at or above the target (matches {@link #requestBeginCoolantInjection()}'s own idle-no-op style).
     */
    public void requestBeginPressurization() {
        // 2026-11-24: a toggle -- pressing again while pressurizing holds the current pressure (previously the
        // only stop was reaching the target). Interim until stage 5's heater/spray + pressure controller.
        if (pressurizationActive) {
            pressurizationActive = false;
        } else if (operatingState.stage == ReactorOperatingStage.REFUELING) {
            return; // stage 1-4c: the vessel stays at atmospheric pressure during refueling
        } else if (pressureMPa < ConfigHolder.reactor.targetOperatingPressureMPa) {
            pressurizationActive = true;
            depressurizationActive = false;
        } else {
            return;
        }
        writeCustomData(UPDATE_OPERATING_STATUS, this::writeOperatingStatus);
        markDirty();
    }

    /**
     * As {@link #requestBeginPressurization()}, ramping {@link #pressureMPa} down toward atmospheric pressure
     * instead (stage 1-2, 2026-11-21: previously toward 0, i.e. vacuum).
     */
    public void requestBeginDepressurization() {
        // 2026-11-24: a toggle, as requestBeginPressurization().
        if (depressurizationActive) {
            depressurizationActive = false;
        } else if (pressureMPa > ATMOSPHERIC_PRESSURE_MPA) {
            depressurizationActive = true;
            pressurizationActive = false;
        } else {
            return;
        }
        writeCustomData(UPDATE_OPERATING_STATUS, this::writeOperatingStatus);
        markDirty();
    }

    /**
     * While {@link #pressurizationActive}/{@link #depressurizationActive}, ramps {@link #pressureMPa} toward its
     * target at {@code ConfigHolder.reactor.pressureAdjustmentRateMPaPerSecond}, auto-clearing the active flag
     * once reached -- no fluid/hatch interaction at all (unlike coolant injection), consistent with pressure
     * being a plain operator-managed status value rather than physically simulated (see this mod's design
     * memory); unrelated to the secondary loop this stage is deliberately not waiting on.
     */
    private void updatePressureAdjustment() {
        double rateThisTick = ConfigHolder.reactor.pressureAdjustmentRateMPaPerSecond * TICK_SECONDS;
        if (pressurizationActive) {
            double target = ConfigHolder.reactor.targetOperatingPressureMPa;
            pressureMPa = Math.min(pressureMPa + rateThisTick, target);
            if (pressureMPa >= target) {
                pressurizationActive = false;
                writeCustomData(UPDATE_OPERATING_STATUS, this::writeOperatingStatus);
            }
            markDirty();
        } else if (depressurizationActive) {
            pressureMPa = Math.max(pressureMPa - rateThisTick, ATMOSPHERIC_PRESSURE_MPA);
            if (pressureMPa <= ATMOSPHERIC_PRESSURE_MPA) {
                depressurizationActive = false;
                writeCustomData(UPDATE_OPERATING_STATUS, this::writeOperatingStatus);
            }
            markDirty();
        }
    }

    /**
     * Console write-direction command: toggle boron dilution (distilled water injection). Stage 4 of this mod's
     * startup/shutdown sequence work (2026-09-21). Unlike {@link #requestBeginCoolantInjection()}/
     * {@link #requestBeginPressurization()}, this has no auto-stop target -- calling it again while already
     * active turns it back off, matching how a real operator dilutes/concentrates by feel rather than toward a
     * fixed setpoint.
     */
    public void requestToggleBoronDilution() {
        // Stage 1-4c: no dilution source may be started during refueling.
        if (!boronDilutionActive && operatingState.stage == ReactorOperatingStage.REFUELING) return;
        boronDilutionActive = !boronDilutionActive;
        if (boronDilutionActive) boronConcentrationActive = false;
        writeCustomData(UPDATE_OPERATING_STATUS, this::writeOperatingStatus);
        markDirty();
    }

    /** As {@link #requestToggleBoronDilution()}, toggling boron concentration (boric acid injection) instead. */
    public void requestToggleBoronConcentration() {
        boronConcentrationActive = !boronConcentrationActive;
        if (boronConcentrationActive) boronDilutionActive = false;
        writeCustomData(UPDATE_OPERATING_STATUS, this::writeOperatingStatus);
        markDirty();
    }

    /**
     * While {@link #boronDilutionActive}/{@link #boronConcentrationActive}, consumes distilled water/boric acid
     * solution from the corresponding import hatch and exports an equal volume of radioactive wastewater (see
     * this mod's design memory, 2026-09-17: letdown volume always matches input volume, so
     * {@link #coolantMassKg} itself never changes from this operation -- only {@link #boronPpm} does), applying
     * the mass-balance formula {@code newPpm = (oldPpm*coolantMassKg + inputPpm*inputMassKg) /
     * (coolantMassKg+inputMassKg)} already decided for this mod (2026-09-17), confirmed still authoritative over
     * a since-superseded "letdown carries the pre-injection concentration" description in the same design
     * decision (2026-09-21). The operative rate is derived fresh from {@link #computeRatedCoolantMassKg()} and
     * {@code ConfigHolder.reactor.boronAdjustmentDurationMinutes} -- see that field's own JavaDoc for why a
     * time-based knob (rather than a flat mb/s rate calibrated for one specific reactor size) is used -- then
     * scaled by {@link ReactorAutoControlSequencer#getBoronAdjustmentRateFraction()} (see that method's own
     * JavaDoc) and finally capped by
     * whichever is smaller: available input fluid, or free space in the wastewater export tank (draining more
     * input than could actually be exported would silently destroy volume).
     */
    private void updateBoronAdjustment() {
        if (!boronDilutionActive && !boronConcentrationActive) return;
        MultiblockAbility<IFluidTank> importAbility = boronDilutionActive ? GTNLMultiblockAbility.PURE_WATER_IMPORT :
                GTNLMultiblockAbility.BORIC_ACID_IMPORT;
        double inputConcentrationPpm = boronDilutionActive ? 0.0 :
                ConfigHolder.reactor.boricAcidSolutionConcentrationPpm;

        double rateKgPerSecond = computeRatedCoolantMassKg() /
                (ConfigHolder.reactor.boronAdjustmentDurationMinutes * 60.0) *
                autoControlSequencer.getBoronAdjustmentRateFraction();
        int budgetMb = (int) Math.round(rateKgPerSecond * TICK_SECONDS * 1000.0);
        if (budgetMb <= 0) return;

        int exportHeadroomMb = 0;
        for (IFluidTank tank : getAbilities(GTNLMultiblockAbility.RADIOACTIVE_WASTEWATER_EXPORT)) {
            FluidStack existing = tank.getFluid();
            exportHeadroomMb += tank.getCapacity() - (existing != null ? existing.amount : 0);
        }
        budgetMb = Math.min(budgetMb, exportHeadroomMb);
        if (budgetMb <= 0) return;

        int drainedMb = 0;
        for (IFluidTank tank : getAbilities(importAbility)) {
            if (drainedMb >= budgetMb) break;
            FluidStack drained = tank.drain(budgetMb - drainedMb, true);
            if (drained != null) {
                drainedMb += drained.amount;
                recordMakeupInflow(drained);
            }
        }
        if (drainedMb <= 0) return;
        energyLedger.add(ReactorEnergyCategory.MAKEUP_OUT, liquidEnthalpyJoules(drainedMb / 1000.0, temperatureKelvin));

        int remainingToExport = drainedMb;
        for (IFluidTank tank : getAbilities(GTNLMultiblockAbility.RADIOACTIVE_WASTEWATER_EXPORT)) {
            if (remainingToExport <= 0) break;
            remainingToExport -= tank.fill(
                    new FluidStack(GTNLMaterials.RadioactiveWastewater.getFluid(), remainingToExport), true);
        }

        double inputMassKg = drainedMb / 1000.0;
        boronPpm = (boronPpm * coolantMassKg + inputConcentrationPpm * inputMassKg) / (coolantMassKg + inputMassKg);
        markDirty();
    }

    /**
     * Experimental (2026-09-24, balance-tuning tooling; scope widened 2026-09-26; extracted into
     * {@link ReactorAutoControlSequencer} 2026-10-06, see this mod's design memory on this class's own
     * decomposition): console write-direction command toggling the whole automatic control loop on/off.
     */
    public void requestToggleAutoRodControl() {
        autoControlSequencer.toggleActive();
        writeCustomData(UPDATE_OPERATING_STATUS, this::writeOperatingStatus);
        markDirty();
    }

    /** Read by the control console screen part over the reactor-console link. */
    public boolean isAutoRodControlActive() {
        return autoControlSequencer.isActive();
    }

    /**
     * Drives {@link #autoControlSequencer} one tick, then (since telemetry needs dozens of getters spanning
     * every reactor subsystem, not just auto-control state -- see {@link ReactorAutoControlSequencer}'s own
     * JavaDoc) logs the resulting action label to {@link #telemetryLogger} whenever that sequencer actually made
     * a rod-nudging decision this tick (as opposed to still counting down toward its next one).
     */
    private void updateAutoControl() {
        String action = autoControlSequencer.update(this);
        if (action == null) return;
        if (telemetryLogger == null) telemetryLogger = ReactorTelemetryLogger.open(getPos());
        telemetryLogger.logRow(autoControlSequencer.getElapsedSeconds(), this, action);
        energyLedger.reset();
    }

    /** Read by {@link ReactorTelemetryLogger} -- see {@link #energyLedger}'s own JavaDoc. */
    @NotNull
    public EnergyLedger<ReactorEnergyCategory> getEnergyLedger() {
        return energyLedger;
    }

    private void applyOperatingStateChange(@NotNull ReactorOperatingState next) {
        if (next != operatingState) {
            // Stage 1-1 (2026-11-21): a trip isolates boron dilution -- a real PWR's protection system cuts off
            // dilution sources on a reactor trip, and a still-running dilution would otherwise keep adding
            // positive reactivity to a reactor that has just been shut down for a reason.
            if (next.stage == ReactorOperatingStage.TRIPPED && operatingState.stage != ReactorOperatingStage.TRIPPED) {
                boronDilutionActive = false;
            }
            operatingState = next;
            writeCustomData(UPDATE_OPERATING_STATUS, this::writeOperatingStatus);
            markDirty();
        }
    }

    /**
     * The two automatic (non-operator-command) stage transitions plus every non-manual SCRAM trigger, both now
     * delegated to {@link #scramEvaluator} (extracted 2026-10-06, see this mod's design memory). Checked every
     * tick. {@link #isShutdownComplete()}/{@link #hasActiveConsoleLink()}/{@link #isWorldBlockPowered()} stay
     * host-side -- {@link ScramHost}'s own JavaDoc on why.
     */
    private void checkAutomaticStageTransitions() {
        applyOperatingStateChange(scramEvaluator.checkAutomaticStageTransitions(this, operatingState));
    }

    @Override
    public boolean isShutdownComplete() {
        if (pointKineticsState.relativeNeutronPopulation >= ConfigHolder.reactor.shutdownCompletePowerThreshold) {
            return false;
        }
        for (MetaTileEntityFuelAssemblyPort controlRod : cachedControlRods) {
            if (controlRod.getInsertionDepth() < FULLY_INSERTED_THRESHOLD) {
                return false;
            }
        }
        return true;
    }

    /** See {@link #checkAutomaticStageTransitions()}'s own JavaDoc. */
    private void checkScramConditions() {
        ratedCoolantMassKg = computeRatedCoolantMassKg();
        applyOperatingStateChange(scramEvaluator.checkScramConditions(this, operatingState));
    }

    @Override
    public boolean isWorldBlockPowered() {
        return getWorld().isBlockPowered(getPos());
    }

    /**
     * Advances {@link #severeAccidentState} by one tick via {@link #accidentTracker} (extracted 2026-10-06, see
     * this mod's design memory) -- deliberately independent of {@link #operatingState}: SCRAM tripping does not
     * by itself resolve an ongoing LOCA/void condition (decay heat continues after shutdown), matching real
     * accident progression.
     */
    private void updateSevereAccidentState() {
        SevereAccidentStage previousStage = severeAccidentState.stage;
        severeAccidentState = accidentTracker.advance(this, severeAccidentState);

        if (severeAccidentState.stage != previousStage) {
            writeCustomData(UPDATE_OPERATING_STATUS, this::writeOperatingStatus);
            markDirty();
            // Edge-triggered exactly once, the single tick the stage flips into EXPLOSION -- SevereAccidentEvaluator
            // itself treats EXPLOSION as a terminal/absorbing state (see its own JavaDoc), so this can never fire
            // again for the same accident.
            if (previousStage != SevereAccidentStage.EXPLOSION &&
                    severeAccidentState.stage == SevereAccidentStage.EXPLOSION) {
                triggerExplosion(new SevereAccidentConstants(ConfigHolder.reactor.explosionMeltFractionThreshold,
                        ConfigHolder.reactor.explosionMagnitudeCoefficient));
            }
        }
    }

    /**
     * Fires the actual in-world consequence of reaching {@link SevereAccidentStage#EXPLOSION} -- a vanilla/Forge
     * explosion at this reactor's own position, strength derived from {@link SevereAccidentState#explosionMagnitude}
     * (itself already a gameplay-balance formula, see its own JavaDoc) scaled and capped into Minecraft's own
     * explosion-strength units via {@code ConfigHolder.reactor.explosionStrengthScale}/{@code maxExplosionStrength}.
     */
    private void triggerExplosion(@NotNull SevereAccidentConstants accidentConstants) {
        double magnitude = severeAccidentState.explosionMagnitude(meltedFuelMassKg, accidentConstants);
        float strength = (float) Math.min(magnitude * ConfigHolder.reactor.explosionStrengthScale,
                ConfigHolder.reactor.maxExplosionStrength);
        BlockPos pos = getPos();
        getWorld().newExplosion(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, strength, false,
                ConfigHolder.reactor.explosionDestroysBlocks);
    }

    /**
     * Unlike {@link #checkScramConditions()}'s change-triggered sync, {@link #pointKineticsState}/
     * {@link #totalReactivity}/{@link #xenonIodineState} change every single tick once fuel is loaded --
     * syncing on every change would mean every tick, so this pushes a fresh snapshot on a plain timer instead
     * (once a second is plenty for a console display, not a real-time control surface).
     */
    private static final int PHYSICS_DISPLAY_SYNC_INTERVAL_TICKS = 20;
    private int physicsDisplaySyncTicks = 0;

    /**
     * Reactor period (s, the e-folding time of the neutron population) measured over the last display interval, as
     * a real period meter does from the rate of change of the neutron level (stage 2-1, 2026-11-27); infinite when
     * steady, negative while power falls. {@link #displayDelayedNeutronFraction} is the current beta, for showing
     * reactivity in dollars.
     */
    private double reactorPeriodSeconds = Double.POSITIVE_INFINITY;
    private double periodReferencePopulation = 0.0;
    private double displayDelayedNeutronFraction = 0.0;

    private void updateReactorPeriod() {
        double now = pointKineticsState.relativeNeutronPopulation;
        double intervalSeconds = PHYSICS_DISPLAY_SYNC_INTERVAL_TICKS * TICK_SECONDS;
        if (now > 0 && periodReferencePopulation > 0) {
            double growth = Math.log(now / periodReferencePopulation);
            reactorPeriodSeconds = Math.abs(growth) < 1.0e-9 ? Double.POSITIVE_INFINITY : intervalSeconds / growth;
        } else {
            reactorPeriodSeconds = Double.POSITIVE_INFINITY;
        }
        periodReferencePopulation = now;
        displayDelayedNeutronFraction = pointKineticsConstants != null ?
                pointKineticsConstants.totalDelayedNeutronFraction : 0.0;
    }

    public double getReactorPeriodSeconds() {
        return reactorPeriodSeconds;
    }

    /** Current total delayed neutron fraction (0 without fuel), client-synced -- reactivity in dollars is rho/beta. */
    public double getDelayedNeutronFraction() {
        return displayDelayedNeutronFraction;
    }

    /** Bounds {@link #updateXenonDeadTimeEstimate()}'s lookahead loop -- see its own JavaDoc. */
    private static final double XENON_DEAD_TIME_MAX_LOOKAHEAD_SECONDS = 4.0 * 3600.0;

    private void syncPhysicsDisplayPeriodically() {
        if (physicsDisplaySyncTicks-- > 0) return;
        physicsDisplaySyncTicks = PHYSICS_DISPLAY_SYNC_INTERVAL_TICKS - 1;
        updateReactorPeriod();
        updateXenonDeadTimeEstimate();
        writeCustomData(UPDATE_OPERATING_STATUS, this::writeOperatingStatus);
    }

    /**
     * Refreshes {@link #xenonDeadTimeRemainingSeconds} -- only while not {@code OPERATING}/{@code STARTUP} (see
     * that field's own JavaDoc for why the zero-population lookahead assumption only holds then); left at its
     * last value otherwise, matching the pattern of every other display-only field in this class.
     */
    private void updateXenonDeadTimeEstimate() {
        if (operatingState.stage == ReactorOperatingStage.OPERATING ||
                operatingState.stage == ReactorOperatingStage.STARTUP) {
            return;
        }
        XenonIodineConstants xenonConstants = new XenonIodineConstants(ConfigHolder.reactor.xenonDeadTimeSeconds,
                ConfigHolder.reactor.equilibriumXenonWorth);
        xenonDeadTimeRemainingSeconds = XenonDeadTimeCalibration.estimateRemainingDeadTimeSeconds(xenonIodineState,
                xenonConstants, ConfigHolder.reactor.equilibriumXenonWorth, XENON_DEAD_TIME_MAX_LOOKAHEAD_SECONDS);
    }

    /**
     * Server-side only (called from {@link #updateFormedValid()}), so unlike
     * {@code MetaTileEntityConsoleScreen#getLinkedReactor()} this can safely use {@link #getAbilities} directly --
     * no client-sync pitfall here, see {@link GTNLMultiblockAbility#REACTOR_LINK_TRANSMITTER}'s own part.
     */
    @Override
    public boolean hasActiveConsoleLink() {
        List<IReactorLinkHatch> transmitters = getAbilities(GTNLMultiblockAbility.REACTOR_LINK_TRANSMITTER);
        return !transmitters.isEmpty() && transmitters.get(0).hasActiveLink();
    }

    private void writeOperatingStatus(@NotNull PacketBuffer buf) {
        buf.writeEnumValue(operatingState.stage);
        buf.writeBoolean(operatingState.lastScramReason != null);
        if (operatingState.lastScramReason != null) {
            buf.writeEnumValue(operatingState.lastScramReason);
        }
        buf.writeDouble(coolantMassKg);
        buf.writeBoolean(coolantInjectionActive);
        buf.writeDouble(ratedCoolantMassKg);
        buf.writeDouble(pressureMPa);
        buf.writeBoolean(pressurizationActive);
        buf.writeBoolean(depressurizationActive);
        buf.writeDouble(temperatureKelvin);
        buf.writeDouble(fuelTemperatureKelvin);
        buf.writeDouble(meltedFuelMassKg);
        buf.writeDouble(coreFuelMassAtMeltdownStartKg);
        buf.writeDouble(primaryVoidFraction);
        buf.writeEnumValue(severeAccidentState.stage);
        buf.writeDouble(severeAccidentState.coreMeltFraction);
        buf.writeDouble(boronPpm);
        buf.writeLong(totalPowerConsumptionEuPerTick);
        buf.writeVarInt(primaryCirculationFlowMb);
        buf.writeBoolean(boronDilutionActive);
        buf.writeBoolean(boronConcentrationActive);
        buf.writeBoolean(autoControlSequencer.isActive());
        buf.writeBoolean(housingMismatch);
        buf.writeBoolean(lengthMismatch);
        buf.writeBoolean(structureBreachRecorded);
        buf.writeBoolean(startupBlocker != null);
        if (startupBlocker != null) buf.writeEnumValue(startupBlocker);
        buf.writeVarInt(loadedFuelAssemblyCountSynced);
        buf.writeVarInt(loadedControlRodClusterCountSynced);
        buf.writeBoolean(rcpRunning);
        buf.writeBoolean(rhrInService);
        buf.writeEnumValue(rhrNotice);
        buf.writeDouble(rhrHeatRemovalWatts);
        buf.writeDouble(rhrCoolingInletKelvin);
        buf.writeDouble(rhrCoolingOutletKelvin);
        buf.writeDouble(rhrCoolingFlowKgPerSecond);
        buf.writeBoolean(rhrOutletCapped);
        buf.writeBoolean(rhrNoPower);
        buf.writeBoolean(rhrNoCoolingWater);
        buf.writeBoolean(refuelingHold);
        buf.writeBoolean(refuelingHoldReason != null);
        if (refuelingHoldReason != null) buf.writeEnumValue(refuelingHoldReason);
        buf.writeDouble(subcriticalMultiplication);
        buf.writeDouble(refuelingBaselineMultiplication);
        buf.writeVarInt(operatingMode);
        buf.writeString(refuelChecklistLine);
        buf.writeString(refuelDetailLine);
        buf.writeDouble(refuelingBaselinePopulation);
        buf.writeDouble(reactorPeriodSeconds);
        buf.writeDouble(displayDelayedNeutronFraction);
        buf.writeDouble(sourceNeutronsPerSecond);
        buf.writeDouble(pointKineticsState.relativeNeutronPopulation);
        buf.writeDouble(totalReactivity);
        buf.writeDouble(shapeReactivityPcm);
        buf.writeDouble(localDopplerReactivityPcm);
        buf.writeDouble(localModeratorTemperatureReactivityPcm);
        buf.writeDouble(dopplerReactivityPcmSpread);
        buf.writeDouble(xenonReactivityPcmSpread);
        buf.writeDouble(xenonIodineState.xenonConcentration());
        buf.writeDouble(xenonDeadTimeRemainingSeconds);
        buf.writeVarInt(coreMapCells.size());
        for (CoreMapCell cell : coreMapCells) {
            buf.writeVarInt(cell.dx);
            buf.writeVarInt(cell.dz);
            buf.writeEnumValue(cell.kind);
            buf.writeByte(cell.depletionPercent);
            buf.writeByte(cell.rodLevel);
        }
        buf.writeVarInt(controlRodSummaries.size());
        for (ControlRodSummary summary : controlRodSummaries) {
            buf.writeVarInt(summary.dx);
            buf.writeVarInt(summary.dz);
            buf.writeVarInt(summary.bankId);
            buf.writeDouble(summary.insertionDepth);
        }
    }

    private void readOperatingStatus(@NotNull PacketBuffer buf) {
        ReactorOperatingStage stage = buf.readEnumValue(ReactorOperatingStage.class);
        ScramReason reason = buf.readBoolean() ? buf.readEnumValue(ScramReason.class) : null;
        operatingState = new ReactorOperatingState(stage, reason);
        coolantMassKg = buf.readDouble();
        coolantInjectionActive = buf.readBoolean();
        ratedCoolantMassKg = buf.readDouble();
        pressureMPa = buf.readDouble();
        pressurizationActive = buf.readBoolean();
        depressurizationActive = buf.readBoolean();
        temperatureKelvin = buf.readDouble();
        fuelTemperatureKelvin = buf.readDouble();
        meltedFuelMassKg = buf.readDouble();
        coreFuelMassAtMeltdownStartKg = buf.readDouble();
        primaryVoidFraction = buf.readDouble();
        severeAccidentState = new SevereAccidentState(buf.readEnumValue(SevereAccidentStage.class), buf.readDouble());
        boronPpm = buf.readDouble();
        totalPowerConsumptionEuPerTick = buf.readLong();
        primaryCirculationFlowMb = buf.readVarInt();
        boronDilutionActive = buf.readBoolean();
        boronConcentrationActive = buf.readBoolean();
        autoControlSequencer.setActive(buf.readBoolean());
        housingMismatch = buf.readBoolean();
        lengthMismatch = buf.readBoolean();
        structureBreachRecorded = buf.readBoolean();
        startupBlocker = buf.readBoolean() ? buf.readEnumValue(ReactorStartupRequirements.Blocker.class) : null;
        loadedFuelAssemblyCountSynced = buf.readVarInt();
        loadedControlRodClusterCountSynced = buf.readVarInt();
        rcpRunning = buf.readBoolean();
        rhrInService = buf.readBoolean();
        rhrNotice = buf.readEnumValue(RhrNotice.class);
        rhrHeatRemovalWatts = buf.readDouble();
        rhrCoolingInletKelvin = buf.readDouble();
        rhrCoolingOutletKelvin = buf.readDouble();
        rhrCoolingFlowKgPerSecond = buf.readDouble();
        rhrOutletCapped = buf.readBoolean();
        rhrNoPower = buf.readBoolean();
        rhrNoCoolingWater = buf.readBoolean();
        refuelingHold = buf.readBoolean();
        refuelingHoldReason = buf.readBoolean() ? buf.readEnumValue(RefuelingEvaluator.HoldReason.class) : null;
        subcriticalMultiplication = buf.readDouble();
        refuelingBaselineMultiplication = buf.readDouble();
        operatingMode = buf.readVarInt();
        refuelChecklistLine = buf.readString(256);
        refuelDetailLine = buf.readString(256);
        refuelingBaselinePopulation = buf.readDouble();
        reactorPeriodSeconds = buf.readDouble();
        displayDelayedNeutronFraction = buf.readDouble();
        sourceNeutronsPerSecond = buf.readDouble();
        // Display-only on the client: just enough of each state to back this class's own getters, not a full
        // reconstruction (the client never runs updatePhysicsAmplitude() itself, so precursor/iodine detail is
        // never needed here).
        pointKineticsState = new PointKineticsState(buf.readDouble(), new double[0]);
        totalReactivity = buf.readDouble();
        shapeReactivityPcm = buf.readDouble();
        localDopplerReactivityPcm = buf.readDouble();
        localModeratorTemperatureReactivityPcm = buf.readDouble();
        dopplerReactivityPcmSpread = buf.readDouble();
        xenonReactivityPcmSpread = buf.readDouble();
        xenonIodineState = new XenonIodineState(0.0, buf.readDouble());
        xenonDeadTimeRemainingSeconds = buf.readDouble();
        int cellCount = buf.readVarInt();
        List<CoreMapCell> cells = new ArrayList<>(cellCount);
        for (int i = 0; i < cellCount; i++) {
            cells.add(new CoreMapCell(buf.readVarInt(), buf.readVarInt(), buf.readEnumValue(CellKind.class),
                    buf.readByte(), buf.readByte()));
        }
        coreMapCells = cells;
        int summaryCount = buf.readVarInt();
        List<ControlRodSummary> summaries = new ArrayList<>(summaryCount);
        for (int i = 0; i < summaryCount; i++) {
            summaries.add(new ControlRodSummary(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                    buf.readDouble()));
        }
        controlRodSummaries = summaries;
    }

    @Override
    public void receiveCustomData(int dataId, @NotNull PacketBuffer buf) {
        super.receiveCustomData(dataId, buf);
        if (dataId == UPDATE_OPERATING_STATUS) {
            readOperatingStatus(buf);
        }
    }

    @Override
    public void writeInitialSyncData(@NotNull PacketBuffer buf) {
        super.writeInitialSyncData(buf);
        writeOperatingStatus(buf);
    }

    @Override
    public void receiveInitialSyncData(@NotNull PacketBuffer buf) {
        super.receiveInitialSyncData(buf);
        readOperatingStatus(buf);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        data.setString("OperatingStage", operatingState.stage.name());
        if (operatingState.lastScramReason != null) {
            data.setString("LastScramReason", operatingState.lastScramReason.name());
        }
        data.setDouble("CoolantMassKg", coolantMassKg);
        data.setBoolean("CoolantInjectionActive", coolantInjectionActive);
        data.setDouble("PressureMPa", pressureMPa);
        data.setBoolean("PressurizationActive", pressurizationActive);
        data.setBoolean("DepressurizationActive", depressurizationActive);
        data.setDouble("TemperatureKelvin", temperatureKelvin);
        data.setDouble("FuelTemperatureKelvin", fuelTemperatureKelvin);
        data.setDouble("MeltedFuelMassKg", meltedFuelMassKg);
        data.setDouble("CoreFuelMassAtMeltdownStartKg", coreFuelMassAtMeltdownStartKg);
        data.setDouble("PrimaryVoidFraction", primaryVoidFraction);
        data.setString("SevereAccidentStage", severeAccidentState.stage.name());
        data.setDouble("CoreMeltFraction", severeAccidentState.coreMeltFraction);
        data.setDouble("BoronPpm", boronPpm);
        data.setBoolean("BoronDilutionActive", boronDilutionActive);
        data.setBoolean("BoronConcentrationActive", boronConcentrationActive);
        data.setDouble("RelativeNeutronPopulation", pointKineticsState.relativeNeutronPopulation);
        NBTTagList precursorList = new NBTTagList();
        for (double precursor : pointKineticsState.precursorConcentrations) {
            precursorList.appendTag(new NBTTagDouble(precursor));
        }
        data.setTag("PrecursorConcentrations", precursorList);
        data.setDouble("TotalReactivity", totalReactivity);
        data.setDouble("IodineConcentration", xenonIodineState.iodineConcentration());
        data.setDouble("XenonConcentration", xenonIodineState.xenonConcentration());
        data.setBoolean("StructureBreachRecorded", structureBreachRecorded);
        data.setBoolean("RcpRunning", rcpRunning);
        data.setBoolean("RhrInService", rhrInService);
        data.setString("RhrNotice", rhrNotice.name());
        data.setBoolean("RefuelingHold", refuelingHold);
        if (refuelingHoldReason != null) data.setString("RefuelingHoldReason", refuelingHoldReason.name());
        data.setDouble("RefuelingBaselineM", refuelingBaselineMultiplication);
        data.setDouble("RefuelingBaselinePopulation", refuelingBaselinePopulation);
        data.setBoolean("EverCritical", everCritical);
        data.setLong("SubcriticalSinceWorldTick", subcriticalSinceWorldTick);
        return data;
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        ReactorOperatingStage stage = data.hasKey("OperatingStage") ?
                ReactorOperatingStage.valueOf(data.getString("OperatingStage")) : ReactorOperatingStage.SHUTDOWN;
        ScramReason reason = data.hasKey("LastScramReason") ? ScramReason.valueOf(data.getString("LastScramReason")) :
                null;
        operatingState = new ReactorOperatingState(stage, reason);
        coolantMassKg = data.getDouble("CoolantMassKg");
        coolantInjectionActive = data.getBoolean("CoolantInjectionActive");
        // Stage 1-2 (2026-11-21): worlds saved before the atmospheric floor existed may hold 0 (vacuum).
        pressureMPa = Math.max(data.getDouble("PressureMPa"), ATMOSPHERIC_PRESSURE_MPA);
        pressurizationActive = data.getBoolean("PressurizationActive");
        depressurizationActive = data.getBoolean("DepressurizationActive");
        temperatureKelvin = data.hasKey("TemperatureKelvin") ? data.getDouble("TemperatureKelvin") : 300.0;
        fuelTemperatureKelvin = data.hasKey("FuelTemperatureKelvin") ? data.getDouble("FuelTemperatureKelvin") : 300.0;
        meltedFuelMassKg = data.getDouble("MeltedFuelMassKg");
        coreFuelMassAtMeltdownStartKg = data.getDouble("CoreFuelMassAtMeltdownStartKg");
        primaryVoidFraction = data.getDouble("PrimaryVoidFraction");
        SevereAccidentStage severeAccidentStage = data.hasKey("SevereAccidentStage") ?
                SevereAccidentStage.valueOf(data.getString("SevereAccidentStage")) : SevereAccidentStage.NORMAL;
        severeAccidentState = new SevereAccidentState(severeAccidentStage, data.getDouble("CoreMeltFraction"));
        boronPpm = data.getDouble("BoronPpm");
        boronDilutionActive = data.getBoolean("BoronDilutionActive");
        boronConcentrationActive = data.getBoolean("BoronConcentrationActive");
        NBTTagList precursorList = data.getTagList("PrecursorConcentrations", Constants.NBT.TAG_DOUBLE);
        double[] precursorConcentrations = new double[precursorList.tagCount()];
        for (int i = 0; i < precursorConcentrations.length; i++) {
            precursorConcentrations[i] = precursorList.getDoubleAt(i);
        }
        pointKineticsState = new PointKineticsState(data.getDouble("RelativeNeutronPopulation"),
                precursorConcentrations);
        totalReactivity = data.getDouble("TotalReactivity");
        xenonIodineState = new XenonIodineState(data.getDouble("IodineConcentration"),
                data.getDouble("XenonConcentration"));
        structureBreachRecorded = data.getBoolean("StructureBreachRecorded");
        rcpRunning = !data.hasKey("RcpRunning") || data.getBoolean("RcpRunning");
        rhrInService = data.getBoolean("RhrInService");
        rhrNotice = data.hasKey("RhrNotice") ? RhrNotice.valueOf(data.getString("RhrNotice")) : RhrNotice.NONE;
        refuelingHold = data.getBoolean("RefuelingHold");
        refuelingHoldReason = data.hasKey("RefuelingHoldReason") ?
                RefuelingEvaluator.HoldReason.valueOf(data.getString("RefuelingHoldReason")) : null;
        refuelingBaselineMultiplication = data.hasKey("RefuelingBaselineM") ? data.getDouble("RefuelingBaselineM") :
                1.0;
        // Worlds saved before stage 1-4c: assume the core has operated and start the wait at the first tick after
        // loading (the safe side) -- see updateSubcriticalClock().
        everCritical = !data.hasKey("EverCritical") || data.getBoolean("EverCritical");
        refuelingBaselinePopulation = data.getDouble("RefuelingBaselinePopulation");
        subcriticalSinceWorldTick = data.hasKey("SubcriticalSinceWorldTick") ?
                data.getLong("SubcriticalSinceWorldTick") : UNKNOWN_SUBCRITICAL_SINCE;
    }

    public MetaTileEntityPressurizedWaterReactor(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId);
    }

    @Override
    public MetaTileEntity createMetaTileEntity(IGregTechTileEntity tileEntity) {
        return new MetaTileEntityPressurizedWaterReactor(metaTileEntityId);
    }

    /** Closes {@link #telemetryLogger} if the block is broken while it's open. */
    @Override
    public void onRemoval() {
        super.onRemoval();
        if (telemetryLogger != null) {
            telemetryLogger.close();
            telemetryLogger = null;
        }
    }

    @Override
    public boolean allowsExtendedFacing() {
        return false;
    }

    @Override
    public boolean allowsFlip() {
        return false;
    }

    @Override
    public void checkStructurePattern() {
        if (!this.isStructureFormed()) {
            reinitializeStructurePattern();
        }
        super.checkStructurePattern();
    }

    @Override
    protected void updateFormedValid() {
        updateChannelCaches();
        double storedEnergyBeforeJoules = computeStoredEnergyJoules();
        updatePhysicsShape();
        updatePhysicsAmplitude();
        updateFuelBurnup();
        updateControlRodWear();
        updateFuelAndCoolantThermal();
        updateCoolantInjection();
        updatePressureAdjustment();
        updateBoronAdjustment();
        energyLedger.add(ReactorEnergyCategory.STORED_CHANGE,
                computeStoredEnergyJoules() - storedEnergyBeforeJoules);
        energyLedger.endTick();
        updateCoreMapCells();
        updateControlRodSummaries();
        updateSyncedCoreSummary();
        updateSubcriticalClock();
        updateRefueling();
        updateAutoControl();
        checkAutomaticStageTransitions();
        checkScramConditions();
        applyScramRodDrop();
        updateSevereAccidentState();
        syncPhysicsDisplayPeriodically();
    }

    /**
     * Fuel + primary coolant internal energy (J) relative to this mod's {@code AMBIENT_TEMPERATURE_KELVIN}
     * reference -- sensible heat of both nodes plus fuel melting latent heat and coolant void latent heat, the
     * same quantities {@link #updateFuelAndCoolantThermal()}/{@link #applyCoolantSubcoolingClamp()} trade
     * temperature for. Read only by {@link #energyLedger}'s per-tick {@link ReactorEnergyCategory#STORED_CHANGE}
     * (stage 0-3 of this mod's reactor physics rework, see this mod's design memory).
     */
    private double computeStoredEnergyJoules() {
        double fuelMassKg = computeTotalCoreFuelMassKg();
        double fuelJoules = fuelMassKg * FuelThermalConstants.SPECIFIC_HEAT_CAPACITY_J_PER_KG_KELVIN *
                (fuelTemperatureKelvin - AMBIENT_TEMPERATURE_KELVIN) +
                meltedFuelMassKg * FuelThermalConstants.LATENT_HEAT_OF_FUSION_J_PER_KG;
        double coolantJoules = liquidEnthalpyJoules(coolantMassKg, temperatureKelvin) +
                primaryVoidFraction * coolantMassKg * SteamProperties.LATENT_HEAT_OF_VAPORIZATION_J_PER_KG;
        return fuelJoules + coolantJoules;
    }

    /** {@code m * c_p * (T - 300 K)} -- see {@link ReactorEnergyCategory}'s own JavaDoc for the convention. */
    private static double liquidEnthalpyJoules(double massKg, double temperatureKelvin) {
        return massKg * CoolantThermalSolver.REPRESENTATIVE_SPECIFIC_HEAT_CAPACITY *
                (temperatureKelvin - AMBIENT_TEMPERATURE_KELVIN);
    }

    /**
     * Drives every control rod toward full insertion at {@code ConfigHolder.reactor.scramRodDropTimeSeconds}
     * while {@link ReactorOperatingStage#TRIPPED} -- a real PWR's rod cluster control assemblies drop by gravity
     * once the drive mechanism releases them on a trip signal (stage 1-1 of this mod's reactor physics rework,
     * 2026-11-21, see this mod's design memory: before this, a trip only changed the stage label and left every
     * rod wherever it was). Deliberately stateless -- "tripped means rods are falling toward 1.0" -- so a world
     * reload mid-drop simply resumes from each rod's own persisted insertion depth.
     */
    private void applyScramRodDrop() {
        if (operatingState.stage != ReactorOperatingStage.TRIPPED) return;
        boolean moved = false;
        // Every port, not just intact clusters: a port's drive state is meaningful even while its insert is a
        // broken rod or absent, and should read "inserted" once the trip clears.
        for (MetaTileEntityFuelAssemblyPort controlRod : cachedAssemblyPorts) {
            double current = controlRod.getInsertionDepth();
            if (current >= 1.0) continue;
            controlRod.setInsertionDepth(ScramRodDrop.advance(current, TICK_SECONDS,
                    ConfigHolder.reactor.scramRodDropTimeSeconds));
            moved = true;
        }
        if (moved) markDirty();
    }

    @Override
    protected void formStructure(PatternMatchContext context) {
        super.formStructure(context);
        housingMismatch = !checkHousingColumns();
        updateChannelCaches();
        lengthMismatch = computeLengthMismatch();
        requestShapeRecompute();
    }

    /**
     * A structure that stops matching while the reactor is running (stage 1-3, 2026-11-21) trips it with
     * {@link ScramReason#STRUCTURE_BREACH}. Once unformed, {@link #updateFormedValid()} no longer runs, so the
     * rods cannot be dropped over time -- every port's rods are forced to full insertion immediately instead.
     * Skipped when part of the vessel is in an unloaded chunk, so a chunk-border unload is not mistaken for a
     * breach.
     */
    @Override
    public void invalidateStructure() {
        if (getWorld() != null && !getWorld().isRemote && isStructureFormed() && isRunningStage() &&
                isVesselAreaLoaded()) {
            for (IMultiblockPart part : getMultiblockParts()) {
                if (part instanceof MetaTileEntityFuelAssemblyPort port) port.setInsertionDepth(1.0);
            }
            structureBreachRecorded = true;
            applyOperatingStateChange(ReactorOperatingEvaluator.scram(operatingState, ScramReason.STRUCTURE_BREACH));
        }
        super.invalidateStructure();
        cachedAssemblyPorts = new ArrayList<>();
        cachedFuelPorts = new ArrayList<>();
        cachedControlRods = new ArrayList<>();
    }

    private boolean isRunningStage() {
        return operatingState.stage == ReactorOperatingStage.STARTUP ||
                operatingState.stage == ReactorOperatingStage.OPERATING ||
                operatingState.stage == ReactorOperatingStage.SHUTTING_DOWN;
    }

    /** Whether every block of the vessel's bounding box is in a loaded chunk. */
    private boolean isVesselAreaLoaded() {
        int radius = diameter / 2;
        BlockPos center = getPos().offset(getFrontFacing().getOpposite(), radius);
        BlockPos from = center.add(-radius, -LAYERS_BELOW_CONTROLLER, -radius);
        BlockPos to = center.add(radius, channelTopYRelative(), radius);
        return getWorld().isAreaLoaded(from, to);
    }

    /**
     * Scans straight down from each fuel assembly port through its channel (down to, but not including, the solid
     * bottom cap) checking that every block in that column is {@code fuel_rod_housing}.
     */
    private boolean checkHousingColumns() {
        if (getWorld() == null) return true;
        int channelBottomY = getPos().getY() + CHANNEL_BOTTOM_Y_RELATIVE;
        for (IMultiblockPart part : getMultiblockParts()) {
            if (part instanceof MetaTileEntityFuelAssemblyPort port &&
                    !isHousingColumnUniform(port.getPos(), channelBottomY, GTNLMetaBlocks.FUEL_ROD_HOUSING)) {
                return false;
            }
        }
        return true;
    }

    private boolean isHousingColumnUniform(@NotNull BlockPos portPos, int channelBottomY, @NotNull Block housingBlock) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(portPos);
        for (int y = portPos.getY() - 1; y >= channelBottomY; y--) {
            pos.setPos(portPos.getX(), y, portPos.getZ());
            if (getWorld().getBlockState(pos).getBlock() != housingBlock) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void configureErrorText(MultiblockUIBuilder builder) {
        super.configureErrorText(builder);
        builder.addCustom((keyList, syncer) -> {
            if (!isStructureFormed()) return;
            if (syncer.syncBoolean(housingMismatch)) {
                keyList.add(KeyUtil.lang(TextFormatting.RED,
                        "gtnuclear.multiblock.pressurized_water_reactor.housing_mismatch"));
            }
            if (syncer.syncBoolean(lengthMismatch)) {
                keyList.add(KeyUtil.lang(TextFormatting.RED,
                        "gtnuclear.multiblock.pressurized_water_reactor.length_mismatch"));
            }
        });
    }

    private static boolean isVesselCasing(@NotNull IBlockState state) {
        return state.getBlock() == GTNLMetaBlocks.REACTOR_VESSEL_CASING;
    }

    /**
     * The controller sits on the horizontal center of the front wall, so scanning straight into the structure
     * (away from the front face) from the controller's own position traces a chord through the circle's center
     * -- i.e. the full diameter.
     */
    private int findDiameter() {
        if (getWorld() == null) return MIN_DIAMETER;
        int distance = 1;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(getPos());
        while (distance <= MAX_DIAMETER && !isVesselCasing(getWorld().getBlockState(pos))) {
            pos.move(getFrontFacing().getOpposite());
            distance++;
        }
        return distance;
    }

    /**
     * Counts consecutive vessel-casing layers above the controller (including the solid top cap) by scanning straight
     * up from the controller's own position.
     */
    private int findLayersAboveController() {
        if (getWorld() == null) return FIXED_LAYERS_ABOVE_CONTROLLER + MIN_FLEXIBLE_MIDDLE_LAYERS;
        int distance = 0;
        int limit = MAX_TOTAL_HEIGHT - LAYERS_BELOW_CONTROLLER - 1;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(getPos());
        while (distance < limit && isVesselCasing(getWorld().getBlockState(pos.move(EnumFacing.UP)))) {
            distance++;
        }
        return distance;
    }

    private static int clampToOddDiameter(int value) {
        int clamped = Math.max(Math.min(value, MAX_DIAMETER), MIN_DIAMETER);
        return clamped % 2 == 0 ? clamped - 1 : clamped;
    }

    private static boolean withinRasterizedCircle(int row, int column, int center, int radius) {
        double dRow = row - center;
        double dColumn = column - center;
        double limit = radius + 0.5;
        return dRow * dRow + dColumn * dColumn < limit * limit;
    }

    private static String withCharAt(@NotNull String source, int index, char replacement) {
        return source.substring(0, index) + replacement + source.substring(index + 1);
    }

    @NotNull
    @Override
    protected BlockPattern createStructurePattern() {
        this.diameter = getWorld() != null ? clampToOddDiameter(findDiameter()) : MIN_DIAMETER;

        int layersAboveController = getWorld() != null ? findLayersAboveController() :
                FIXED_LAYERS_ABOVE_CONTROLLER + MIN_FLEXIBLE_MIDDLE_LAYERS;
        layersAboveController = Math.max(
                Math.min(layersAboveController, MAX_TOTAL_HEIGHT - LAYERS_BELOW_CONTROLLER - 1),
                FIXED_LAYERS_ABOVE_CONTROLLER + MIN_FLEXIBLE_MIDDLE_LAYERS);
        this.flexibleMiddleLayers = Math.min(layersAboveController - FIXED_LAYERS_ABOVE_CONTROLLER,
                MAX_FLEXIBLE_MIDDLE_LAYERS);

        int center = diameter / 2;
        int radius = diameter / 2;

        String[] ringLayer = new String[diameter];
        String[] solidLayer = new String[diameter];
        String[] topLayer = new String[diameter];
        StringBuilder ringRow = new StringBuilder(diameter);
        StringBuilder solidRow = new StringBuilder(diameter);
        StringBuilder topRow = new StringBuilder(diameter);
        int interiorCount = 0;

        for (int row = 0; row < diameter; row++) {
            for (int column = 0; column < diameter; column++) {
                boolean inside = withinRasterizedCircle(row, column, center, radius);
                solidRow.append(inside ? 'B' : ' ');
                if (!inside) {
                    ringRow.append(' ');
                    topRow.append(' ');
                } else {
                    boolean boundary = !withinRasterizedCircle(row - 1, column, center, radius) ||
                            !withinRasterizedCircle(row + 1, column, center, radius) ||
                            !withinRasterizedCircle(row, column - 1, center, radius) ||
                            !withinRasterizedCircle(row, column + 1, center, radius);
                    ringRow.append(boundary ? 'B' : 'A');
                    // Top cap: boundary stays casing wall, every interior cell is a fuel assembly channel slot
                    // ('F': a fuel assembly port or plain casing -- stage 1-3, control rod clusters now live in
                    // the fuel assemblies' guide tubes instead of separate channels).
                    topRow.append(boundary ? 'B' : 'F');
                    if (!boundary) interiorCount++;
                }
            }
            ringLayer[row] = ringRow.toString();
            solidLayer[row] = solidRow.toString();
            topLayer[row] = topRow.toString();
            ringRow.setLength(0);
            solidRow.setLength(0);
            topRow.setLength(0);
        }
        this.interiorCellsPerLayer = interiorCount;

        // Left face center (coolant import); front face center, directly below the controller (maintenance
        // hatch); back face center (energy hatch); right face center (radioactive wastewater drain) -- all on
        // this same fixed "2nd layer from the bottom."
        String[] importRingLayer = ringLayer.clone();
        importRingLayer[center] = withCharAt(importRingLayer[center], 0, 'I');
        importRingLayer[0] = withCharAt(importRingLayer[0], center, 'M');
        importRingLayer[diameter - 1] = withCharAt(importRingLayer[diameter - 1], center, 'E');
        importRingLayer[center] = withCharAt(importRingLayer[center], diameter - 1, 'D');

        // Left face center (boric acid injection); right face center (coolant export).
        String[] exportRingLayer = ringLayer.clone();
        exportRingLayer[center] = withCharAt(exportRingLayer[center], 0, 'X');
        exportRingLayer[center] = withCharAt(exportRingLayer[center], diameter - 1, 'O');

        // Front face, horizontal center (controller), its immediate right neighbor (link hatch) and immediate
        // left neighbor (startup neutron source hatch).
        String[] controllerRingLayer = ringLayer.clone();
        controllerRingLayer[0] = withCharAt(
                withCharAt(withCharAt(controllerRingLayer[0], center, 'S'), center + 1, 'L'), center - 1, 'N');
        // Left/right face centers of the same layer: residual heat removal cooling water inlet ('W') and outlet
        // ('Y') -- stage 1-4a, the non-radioactive cooling side of the built-in RHR heat exchanger.
        controllerRingLayer[center] = withCharAt(withCharAt(controllerRingLayer[center], 0, 'W'), diameter - 1, 'Y');

        // Back face center (pure/distilled water injection, for boron dilution).
        String[] pureWaterRingLayer = ringLayer.clone();
        pureWaterRingLayer[diameter - 1] = withCharAt(pureWaterRingLayer[diameter - 1], center, 'P');

        // Left face center (emergency coolant injection).
        String[] emergencyCoolantRingLayer = ringLayer.clone();
        emergencyCoolantRingLayer[center] = withCharAt(emergencyCoolantRingLayer[center], 0, 'Q');

        // Split the flexible middle budget around the dedicated pure water hatch layer so it sits as close to
        // the true vertical center of the whole structure as the available budget allows.
        int layersAroundWaterHatch = flexibleMiddleLayers - MIN_FLEXIBLE_MIDDLE_LAYERS;
        int lowerRepeat = layersAroundWaterHatch / 2;
        int upperRepeat = layersAroundWaterHatch - lowerRepeat;

        return FactoryBlockPattern.start(RelativeDirection.RIGHT, RelativeDirection.FRONT, RelativeDirection.UP)
                .aisle(solidLayer)
                .aisle(importRingLayer)
                .aisle(controllerRingLayer)
                .aisle(ringLayer).setRepeatable(lowerRepeat)
                .aisle(pureWaterRingLayer)
                .aisle(ringLayer).setRepeatable(upperRepeat)
                .aisle(exportRingLayer)
                .aisle(emergencyCoolantRingLayer)
                .aisle(topLayer)
                .where('S', selfPredicate())
                .where('L', abilities(GTNLMultiblockAbility.REACTOR_LINK_TRANSMITTER).setMinGlobalLimited(1))
                .where('N', abilities(GTNLMultiblockAbility.NEUTRON_SOURCE_PORT).setMinGlobalLimited(1))
                .where('Q', abilities(GTNLMultiblockAbility.EMERGENCY_COOLANT_IMPORT).setMinGlobalLimited(1))
                .where('I', abilities(GTNLMultiblockAbility.REACTOR_COOLANT_IMPORT).setMinGlobalLimited(1))
                .where('M', maintenancePredicate())
                .where('E', abilities(MultiblockAbility.INPUT_ENERGY).setMinGlobalLimited(1))
                .where('D', abilities(GTNLMultiblockAbility.RADIOACTIVE_WASTEWATER_EXPORT).setMinGlobalLimited(1))
                .where('O', abilities(GTNLMultiblockAbility.REACTOR_COOLANT_EXPORT).setMinGlobalLimited(1))
                .where('X', abilities(GTNLMultiblockAbility.BORIC_ACID_IMPORT).setMinGlobalLimited(1))
                .where('P', abilities(GTNLMultiblockAbility.PURE_WATER_IMPORT).setMinGlobalLimited(1))
                .where('W', abilities(GTNLMultiblockAbility.RHR_COOLING_WATER_IMPORT).setMinGlobalLimited(1))
                .where('Y', abilities(GTNLMultiblockAbility.RHR_COOLING_WATER_EXPORT).setMinGlobalLimited(1))
                .where('F', abilities(GTNLMultiblockAbility.FUEL_ASSEMBLY_PORT)
                        .setMinGlobalLimited(ReactorStartupRequirements.MIN_CONTROL_ROD_CLUSTERS)
                        .or(states(GTNLMetaBlocks.REACTOR_VESSEL_CASING
                                .getState(ReactorVesselCasing.CasingType.PWR_REACTOR_VESSEL))))
                .where('A', any())
                .where('B',
                        states(GTNLMetaBlocks.REACTOR_VESSEL_CASING
                                .getState(ReactorVesselCasing.CasingType.PWR_REACTOR_VESSEL)))
                .where(' ', any())
                .build();
    }

    @SideOnly(Side.CLIENT)
    @Override
    public ICubeRenderer getBaseTexture(IMultiblockPart sourcePart) {
        return GTNLTextures.PWR_REACTOR_VESSEL_CASING;
    }

    @SideOnly(Side.CLIENT)
    @Override
    @NotNull
    protected ICubeRenderer getFrontOverlay() {
        return GTNLTextures.PWR_FISSION_REACTOR_OVERLAY;
    }

    @Override
    public void renderMetaTileEntity(CCRenderState renderState, Matrix4 translation, IVertexOperation[] pipeline) {
        super.renderMetaTileEntity(renderState, translation, pipeline);

        this.getFrontOverlay().renderOrientedState(renderState, translation, pipeline, getFrontFacing(),
                isStructureFormed(), true);
    }

    @Override
    public boolean shouldShowVoidingModeButton() {
        return false;
    }
}
