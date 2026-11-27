package com.windows2000.gtnuclear.common.metatileentities.multi;

import gregtech.api.capability.IEnergyContainer;
import gregtech.api.capability.impl.EnergyContainerList;
import gregtech.api.metatileentity.IFastRenderMetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.interfaces.IGregTechTileEntity;
import gregtech.api.metatileentity.multiblock.IMultiblockPart;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.metatileentity.multiblock.MultiblockWithDisplayBase;
import gregtech.api.metatileentity.multiblock.ui.MultiblockUIFactory;
import gregtech.api.pattern.BlockPattern;
import gregtech.api.pattern.FactoryBlockPattern;
import gregtech.api.pattern.PatternMatchContext;
import gregtech.api.util.RelativeDirection;
import gregtech.client.renderer.ICubeRenderer;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.network.NetworkUtils;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.windows2000.gtnuclear.api.metatileentity.multiblock.GTNLMultiblockAbility;
import com.windows2000.gtnuclear.client.renderer.texture.GTNLTextures;
import com.windows2000.gtnuclear.common.ConfigHolder;
import com.windows2000.gtnuclear.common.blocks.GTNLMetaBlocks;
import com.windows2000.gtnuclear.common.blocks.reactor.ReactorVesselCasing;
import com.windows2000.gtnuclear.common.metatileentities.multiblockpart.MetaTileEntityConsoleScreen;
import com.windows2000.gtnuclear.common.metatileentities.multiblockpart.MetaTileEntityFuelAssemblyPort;
import com.windows2000.gtnuclear.common.metatileentities.multiblockpart.MetaTileEntityReactorLinkHatch;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * The control console: a small, fixed-size multiblock that pairs with exactly one {@code
 * MetaTileEntityPressurizedWaterReactor} through a {@code REACTOR_LINK_RECEIVER} hatch connected via Optical
 * Cable, and displays that reactor's telemetry on its
 * {@link com.windows2000.gtnuclear.common.metatileentities.multiblockpart.MetaTileEntityConsoleScreen}
 * part (see that class's own JavaDoc for what it shows and doesn't show yet).
 * <p>
 * <b>This shape is a placeholder.</b> Per this mod's design memory, the console's actual exterior (the user
 * showed reference images of a large, angled multi-panel control-room wall) will be designed and built by the
 * user directly -- GT's {@code FactoryBlockPattern} can only express a straight rectangular grid, not a bent
 * wall, so that exterior can't be this pattern's job anyway. What this class provides is the minimal functional
 * skeleton: a small fixed 3x3x3 box with the controller, one {@code REACTOR_LINK_RECEIVER} hatch, and one
 * {@code CONSOLE_SCREEN} part, all on the front face. Reusing these two ability symbols is enough to make any
 * future reshaped pattern functional.
 * <p>
 * Implements {@link IFastRenderMetaTileEntity} itself (rather than the screen part doing so) and delegates to
 * each linked {@link MetaTileEntityConsoleScreen}'s own {@code renderScreen(...)} -- mirrors exactly how GTST's
 * own {@code MetaTileEntityCentralMonitor} (not {@code MetaTileEntityMonitorScreen}) drives its child screens'
 * rendering from the controller, one unified per-frame call.
 * <p>
 * <b>Screen positions are tracked in an explicitly-synced field ({@link #screenPositions}), not read via
 * {@link #getAbilities(gregtech.api.metatileentity.multiblock.MultiblockAbility)}.</b>
 * {@code MultiblockControllerBase#checkStructurePattern()} (which populates the ability-instance map
 * {@code getAbilities()} reads from) only ever runs inside {@code if (!getWorld().isRemote)} -- it is pure
 * server-side bookkeeping, never sent to the client. Rendering is always client-side, so querying
 * {@code getAbilities(CONSOLE_SCREEN)} from {@link #renderMetaTileEntity(double, double, double, float)} always
 * returned an empty list (confirmed via diagnostic logging: the render call fired with {@code formed=true} but
 * {@code screens=0}). GTST's own {@code MetaTileEntityCentralMonitor} sidesteps this the same way -- its
 * {@code screens} field is populated independently of the ability system.
 * <p>
 * {@link #receiverLinkHatchPos}/{@link #linkedReactorPos} follow a related but distinct pattern (see this mod's
 * design memory, 2026-09-19/20): {@code MetaTileEntityConsoleScreen} originally resolved its linked reactor via
 * {@code getController().getAbilities(REACTOR_LINK_RECEIVER)} on this class, which is the same server-only
 * bookkeeping and was silently always empty on the client -- confirmed only once a reactor was actually linked
 * and telemetry still never appeared. Note that {@code MetaTileEntityMultiblockPart#getController()} itself
 * (used by every part, including screens and link hatches) is NOT affected by this pitfall -- GTST already syncs
 * a part's own controller reference via its own {@code controllerPos}/{@code SYNC_CONTROLLER} mechanism; only
 * the CONTROLLER's per-ability part lists are server-only. That first fix (syncing {@link #receiverLinkHatchPos}
 * and having the screen call {@code MetaTileEntityReactorLinkHatch#getReactor()} on it directly) turned out to
 * be incomplete: {@code getReactor()}'s network lookup ({@code OpticalPipeNet.getOpticalPipeNet()}) is itself
 * server-only (returns null when {@code world.isRemote}), so calling it from client-side rendering code always
 * failed silently too -- the exact same class of bug one layer deeper. The real fix is this class's own
 * {@link #updateFormedValid()}: resolve the full chain (receiver hatch -&gt; {@code getReactor()} -&gt; reactor
 * position) server-side every tick, same as {@code MetaTileEntityPressurizedWaterReactor#checkScramConditions()}
 * already does for the reverse direction, and sync only the final {@link #linkedReactorPos} result. The screen
 * now reads that synced position directly and never calls anything link-hatch- or network-related itself.
 */
public class MetaTileEntityControlConsole extends MultiblockWithDisplayBase implements IFastRenderMetaTileEntity {

    private static final int UPDATE_SCREEN_POSITIONS = 200;
    private static final int UPDATE_LINKED_REACTOR = 201;

    private final List<BlockPos> screenPositions = new ArrayList<>();
    /**
     * Server-side lookup key only (not synced): which of this console's own parts is its receiver link hatch, so
     * {@link #updateFormedValid()} knows which one to call {@code getReactor()} on. Recomputed by
     * {@link #formStructure(PatternMatchContext)}.
     */
    @Nullable
    private BlockPos receiverLinkHatchPos;
    /** The client-relevant result of resolving {@link #receiverLinkHatchPos} -- see this class's own JavaDoc. */
    @Nullable
    private BlockPos linkedReactorPos;

    /** Read by {@code MetaTileEntityConsoleScreen} to resolve its linked reactor -- see this class's own JavaDoc. */
    @Nullable
    public BlockPos getLinkedReactorPos() {
        return linkedReactorPos;
    }

    public MetaTileEntityControlConsole(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId);
    }

    @Override
    public MetaTileEntity createMetaTileEntity(IGregTechTileEntity tileEntity) {
        return new MetaTileEntityControlConsole(metaTileEntityId);
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
    protected void updateFormedValid() {
        boolean hasPower = updatePowerConsumption();
        BlockPos previous = linkedReactorPos;
        // Added 2026-09-28 (user request): an unpowered console has no linked reactor at all, as far as every
        // downstream consumer is concerned -- both getLinkedReactor() (all command buttons, see
        // addCommandButtons()) and getLinkedReactorPos() (MetaTileEntityConsoleScreen's own status display)
        // derive from this same field, so forcing it null here is the single choke point that disables both a
        // real console's command authority AND its display alike without needing separate plumbing for each.
        linkedReactorPos = hasPower ? resolveLinkedReactorPos() : null;
        if (!Objects.equals(linkedReactorPos, previous)) {
            writeCustomData(UPDATE_LINKED_REACTOR, this::writeLinkedReactorPos);
            markDirty();
        }
    }

    /**
     * Draws a flat, always-on EU/t (2026-09-28, user-specified target) from whatever
     * {@link MultiblockAbility#INPUT_ENERGY} hatch(es) are piped in -- representing the console's own display/
     * control electronics load, distinct from (and much smaller than) the linked reactor's own instrumentation
     * and coolant pump draw (see {@code MetaTileEntityPressurizedWaterReactor#updateReactorPowerConsumption}).
     *
     * @return whether the full desired amount was actually available -- see {@link #updateFormedValid()}'s own
     *         use of this to disable the console entirely while unpowered, mirroring a real control room's
     *         instrumentation going dark without electricity.
     */
    private boolean updatePowerConsumption() {
        IEnergyContainer energyContainer = new EnergyContainerList(getAbilities(MultiblockAbility.INPUT_ENERGY));
        long desiredEuPerTick = Math.round(ConfigHolder.reactor.controlConsoleEuPerTickConsumption);
        if (desiredEuPerTick <= 0) return true;
        long availableEuPerTick = Math.min(desiredEuPerTick, energyContainer.getEnergyStored());
        if (availableEuPerTick <= 0) return false;
        energyContainer.removeEnergy(availableEuPerTick);
        return availableEuPerTick >= desiredEuPerTick;
    }

    @Nullable
    private BlockPos resolveLinkedReactorPos() {
        if (receiverLinkHatchPos == null || getWorld() == null) return null;
        TileEntity tileEntity = getWorld().getTileEntity(receiverLinkHatchPos);
        if (!(tileEntity instanceof IGregTechTileEntity gregTechTileEntity)) return null;
        MetaTileEntity metaTileEntity = gregTechTileEntity.getMetaTileEntity();
        if (!(metaTileEntity instanceof MetaTileEntityReactorLinkHatch hatch)) return null;
        MetaTileEntityPressurizedWaterReactor reactor = hatch.getReactor();
        return reactor != null ? reactor.getPos() : null;
    }

    /**
     * Server-side only, called from the console command buttons' sync-value setters (see
     * {@link #createUIFactory()}) -- resolves the actual live reactor instance from {@link #linkedReactorPos},
     * which {@link #updateFormedValid()} already keeps fresh server-side every tick regardless of side.
     */
    @Nullable
    private MetaTileEntityPressurizedWaterReactor getLinkedReactor() {
        if (linkedReactorPos == null || getWorld() == null) return null;
        TileEntity tileEntity = getWorld().getTileEntity(linkedReactorPos);
        if (!(tileEntity instanceof IGregTechTileEntity gregTechTileEntity)) return null;
        MetaTileEntity metaTileEntity = gregTechTileEntity.getMetaTileEntity();
        return metaTileEntity instanceof MetaTileEntityPressurizedWaterReactor reactor ? reactor : null;
    }

    @Override
    protected void formStructure(PatternMatchContext context) {
        super.formStructure(context);
        screenPositions.clear();
        receiverLinkHatchPos = null;
        for (IMultiblockPart part : getMultiblockParts()) {
            if (part instanceof MetaTileEntityConsoleScreen) {
                screenPositions.add(((MetaTileEntityConsoleScreen) part).getPos());
            } else if (part instanceof MetaTileEntityReactorLinkHatch hatch && !hatch.isTransmitter()) {
                receiverLinkHatchPos = hatch.getPos();
            }
        }
        if (!getWorld().isRemote) {
            writeCustomData(UPDATE_SCREEN_POSITIONS, this::writeScreenPositions);
            markDirty();
        }
    }

    private void writeScreenPositions(@NotNull PacketBuffer buf) {
        buf.writeVarInt(screenPositions.size());
        for (BlockPos pos : screenPositions) {
            buf.writeBlockPos(pos);
        }
    }

    private void readScreenPositions(@NotNull PacketBuffer buf) {
        int size = buf.readVarInt();
        screenPositions.clear();
        for (int i = 0; i < size; i++) {
            screenPositions.add(buf.readBlockPos());
        }
    }

    private void writeLinkedReactorPos(@NotNull PacketBuffer buf) {
        buf.writeBoolean(linkedReactorPos != null);
        if (linkedReactorPos != null) {
            buf.writeBlockPos(linkedReactorPos);
        }
    }

    private void readLinkedReactorPos(@NotNull PacketBuffer buf) {
        linkedReactorPos = buf.readBoolean() ? buf.readBlockPos() : null;
    }

    @Override
    public void receiveCustomData(int dataId, @NotNull PacketBuffer buf) {
        super.receiveCustomData(dataId, buf);
        if (dataId == UPDATE_SCREEN_POSITIONS) {
            readScreenPositions(buf);
        } else if (dataId == UPDATE_LINKED_REACTOR) {
            readLinkedReactorPos(buf);
        }
    }

    @Override
    public void writeInitialSyncData(@NotNull PacketBuffer buf) {
        super.writeInitialSyncData(buf);
        writeScreenPositions(buf);
        writeLinkedReactorPos(buf);
    }

    @Override
    public void receiveInitialSyncData(@NotNull PacketBuffer buf) {
        super.receiveInitialSyncData(buf);
        readScreenPositions(buf);
        readLinkedReactorPos(buf);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        NBTTagList list = new NBTTagList();
        for (BlockPos pos : screenPositions) {
            list.appendTag(NBTUtil.createPosTag(pos));
        }
        data.setTag("ScreenPositions", list);
        if (linkedReactorPos != null) {
            data.setTag("LinkedReactorPos", NBTUtil.createPosTag(linkedReactorPos));
        }
        return data;
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        screenPositions.clear();
        NBTTagList list = data.getTagList("ScreenPositions", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            screenPositions.add(NBTUtil.getPosFromTag(list.getCompoundTagAt(i)));
        }
        linkedReactorPos = data.hasKey("LinkedReactorPos") ?
                NBTUtil.getPosFromTag(data.getCompoundTag("LinkedReactorPos")) : null;
    }

    // spotless:off
    @NotNull
    @Override
    protected BlockPattern createStructurePattern() {
        return FactoryBlockPattern.start(RelativeDirection.RIGHT, RelativeDirection.DOWN, RelativeDirection.BACK)
                .aisle("      CCCCCCCCC      ", "      CCCCCCCCC      ", "      CCCCCCCCC      ", "      CCCCCCCCC      ", "      CCCCCCCCC      ", "      CCCCCCCCC      ", "      CCCCCCCCC      ", "      AAAAAAAAA      ")
                .aisle("CCCCCCCCCCCCCCCCCCCCC", "CCCCCCXXXXXXXXXCCCCCC", "CCCCCCXXXXXXXXXCCCCCC", "CCCCCCXXXXXXXXXCCCCCC", "CCCCCCXXXXXXXXXCCCCCC", "CCCCCCXXXXXXXXXCCCCCC", "CCCCCCCCCCCCCCCCCCCCC", "AAAAAACCCCCCCCCAAAAAA")
                .aisle("CCCCCC         CCCCCC", "CXXXXX         XXXXXC", "CXXXXX         XXXXXC", "CXXXXX         XXXXXC", "CXXXXX         XXXXXC", "CXXXXX         XXXXXC", "CCCCCC         CCCCCC", "CCCCCCCCCCSMCCCCCCCCC")
                .aisle("                     ", "                     ", "                     ", "                     ", "                     ", "                     ", "                     ", "CCCCCC         CCCCCC")
                .aisle("                     ", "                     ", "                     ", "                     ", "                     ", "                     ", "                     ", "CC                 CC")
                .where('S', selfPredicate())
                .where('X', abilities(GTNLMultiblockAbility.CONSOLE_SCREEN))
                .where('M', maintenancePredicate())
                .where('C', states(GTNLMetaBlocks.REACTOR_VESSEL_CASING
                        .getState(ReactorVesselCasing.CasingType.PWR_REACTOR_VESSEL)))
                .where('A', states(GTNLMetaBlocks.REACTOR_VESSEL_CASING
                        .getState(ReactorVesselCasing.CasingType.PWR_REACTOR_VESSEL))
                        .or(abilities(GTNLMultiblockAbility.REACTOR_LINK_RECEIVER).setMinGlobalLimited(1))
                        .or(abilities(MultiblockAbility.INPUT_ENERGY).setMinGlobalLimited(1)))
                .build();
    }
    // spotless:on

    @SideOnly(Side.CLIENT)
    @Override
    public ICubeRenderer getBaseTexture(IMultiblockPart sourcePart) {
        return GTNLTextures.PWR_REACTOR_VESSEL_CASING;
    }

    @Override
    public boolean shouldShowVoidingModeButton() {
        return false;
    }

    /**
     * Stages 1-2 of this mod's startup/shutdown sequence work (2026-09-21): five console write-direction command
     * buttons ({@code beginStartup}/{@code beginShutdown}/{@code manualReset}/manual SCRAM/begin coolant
     * injection), added inside the standard multiblock display screen via
     * {@link MultiblockUIFactory#addScreenChildren} (the only extension point {@code buildUI} exposes -- it is
     * {@code final} on {@code MultiblockWithDisplayBase}, see that class's own source). The four stage-1 buttons
     * enforce no preconditions (a later stage); the coolant injection button's precondition (coolant at or below
     * a configurable fraction of rated) is enforced reactor-side, in
     * {@code MetaTileEntityPressurizedWaterReactor#requestBeginCoolantInjection()}. Every button's action is an
     * unconditional attempt that the reactor-side method itself already treats as a safe no-op when invalid.
     * <p>
     * Each button's action follows this mod's established "route through a {@link BooleanSyncValue} setter"
     * pattern (see {@code MetaTileEntityIsotopeStorage}'s own JavaDoc for why a plain
     * {@code ButtonWidget#onMousePressed} alone never reaches the server) -- the setter runs server-side, where
     * {@link #getLinkedReactor()} is safe to call.
     */
    @Override
    protected MultiblockUIFactory createUIFactory() {
        return super.createUIFactory()
                .setSize(198, 526)
                .setScreenHeight(306)
                .addScreenChildren(this::addCommandButtons);
    }

    /**
     * 2026-09-22 playtest feedback: the original layout (four 44px buttons per row, then several 88-90px ones)
     * wrapped every label longer than ~6 characters onto a second line, visually overlapping the row below it,
     * and the panel's own overall size was never grown alongside {@link #setScreenHeight}, so the tall screen
     * area ran underneath the player inventory slots anchored to the panel's bottom edge. Both are fixed here by
     * brute force, matching the "temporarily expand the UI" request: every command button is now a uniform 90px
     * wide (fits every label used here without wrapping) in a plain 2-per-row grid, and
     * {@link MultiblockUIFactory#setSize} grows the whole panel to comfortably fit the now-taller screen area
     * plus the inventory below it. This is explicitly a stopgap, not the dedicated UI system redesign already
     * flagged as a separate future discussion (see this mod's design memory).
     */
    private void addCommandButtons(@NotNull ParentWidget<?> parent, @NotNull PanelSyncManager syncManager) {
        parent.child(commandButton("startup_trigger", syncManager, () -> {
            MetaTileEntityPressurizedWaterReactor reactor = getLinkedReactor();
            if (reactor != null) reactor.requestStartup();
        }, "gtnuclear.gui.control_console.startup").pos(4, 112).size(90, 16));

        parent.child(commandButton("shutdown_trigger", syncManager, () -> {
            MetaTileEntityPressurizedWaterReactor reactor = getLinkedReactor();
            if (reactor != null) reactor.requestShutdown();
        }, "gtnuclear.gui.control_console.shutdown").pos(98, 112).size(90, 16));

        parent.child(commandButton("reset_trigger", syncManager, () -> {
            MetaTileEntityPressurizedWaterReactor reactor = getLinkedReactor();
            if (reactor != null) reactor.requestManualReset();
        }, "gtnuclear.gui.control_console.reset").pos(4, 130).size(90, 16));

        parent.child(commandButton("scram_trigger", syncManager, () -> {
            MetaTileEntityPressurizedWaterReactor reactor = getLinkedReactor();
            if (reactor != null) reactor.requestManualScram();
        }, "gtnuclear.gui.control_console.scram").pos(98, 130).size(90, 16));

        parent.child(commandButton("coolant_injection_trigger", syncManager, () -> {
            MetaTileEntityPressurizedWaterReactor reactor = getLinkedReactor();
            if (reactor != null) reactor.requestBeginCoolantInjection();
        }, "gtnuclear.gui.control_console.coolant_injection").pos(4, 148).size(90, 16));

        // Stage 3 of this mod's startup/shutdown sequence work (2026-09-21), deliberately provisional -- see
        // ConfigHolder.reactor.targetOperatingPressureMPa's own comment for why.
        parent.child(commandButton("pressurize_trigger", syncManager, () -> {
            MetaTileEntityPressurizedWaterReactor reactor = getLinkedReactor();
            if (reactor != null) reactor.requestBeginPressurization();
        }, "gtnuclear.gui.control_console.pressurize").pos(98, 148).size(90, 16));

        parent.child(commandButton("depressurize_trigger", syncManager, () -> {
            MetaTileEntityPressurizedWaterReactor reactor = getLinkedReactor();
            if (reactor != null) reactor.requestBeginDepressurization();
        }, "gtnuclear.gui.control_console.depressurize").pos(4, 166).size(90, 16));

        // Stage 4 of this mod's startup/shutdown sequence work (2026-09-21) -- toggles, not momentary "begin"
        // triggers, since boron adjustment has no auto-stop target (see MetaTileEntityPressurizedWaterReactor#
        // requestToggleBoronDilution's own JavaDoc).
        parent.child(commandButton("boron_dilution_trigger", syncManager, () -> {
            MetaTileEntityPressurizedWaterReactor reactor = getLinkedReactor();
            if (reactor != null) reactor.requestToggleBoronDilution();
        }, "gtnuclear.gui.control_console.boron_dilution").pos(98, 166).size(90, 16));

        parent.child(commandButton("boron_concentration_trigger", syncManager, () -> {
            MetaTileEntityPressurizedWaterReactor reactor = getLinkedReactor();
            if (reactor != null) reactor.requestToggleBoronConcentration();
        }, "gtnuclear.gui.control_console.boron_concentration").pos(4, 184).size(90, 16));

        // Experimental (2026-09-24, balance-tuning tooling): toggles MetaTileEntityPressurizedWaterReactor's
        // rod-only autopilot + once-per-second CSV telemetry log -- see that method's own JavaDoc.
        parent.child(commandButton("auto_control_trigger", syncManager, () -> {
            MetaTileEntityPressurizedWaterReactor reactor = getLinkedReactor();
            if (reactor != null) reactor.requestToggleAutoRodControl();
        }, "gtnuclear.gui.control_console.auto_control").pos(98, 184).size(90, 16));

        // Stage 1-4a (2026-11-24): reactor coolant pumps and residual heat removal, both toggles -- the reactor
        // side refuses an RCP stop while the core is running and an RHR start outside its interlocks.
        parent.child(commandButton("rcp_trigger", syncManager, () -> {
            MetaTileEntityPressurizedWaterReactor reactor = getLinkedReactor();
            if (reactor != null) reactor.requestToggleRcp();
        }, "gtnuclear.gui.control_console.rcp").pos(4, 202).size(90, 16));

        parent.child(commandButton("rhr_trigger", syncManager, () -> {
            MetaTileEntityPressurizedWaterReactor reactor = getLinkedReactor();
            if (reactor != null) reactor.requestToggleRhr();
        }, "gtnuclear.gui.control_console.rhr").pos(98, 202).size(90, 16));

        // Stage 1-4c (2026-11-27): refueling (Mode 6) start/end toggle and hold resume.
        parent.child(commandButton("refuel_trigger", syncManager, () -> {
            MetaTileEntityPressurizedWaterReactor reactor = getLinkedReactor();
            if (reactor != null) reactor.requestToggleRefueling();
        }, "gtnuclear.gui.control_console.refuel").pos(4, 220).size(90, 16));

        parent.child(commandButton("refuel_resume_trigger", syncManager, () -> {
            MetaTileEntityPressurizedWaterReactor reactor = getLinkedReactor();
            if (reactor != null) reactor.requestResumeRefueling();
        }, "gtnuclear.gui.control_console.refuel_resume").pos(98, 220).size(90, 16));

        addBankNudgeButtons(parent, syncManager);
        addControlRodList(parent, syncManager);
    }

    /**
     * Stage 5 of this mod's startup/shutdown sequence work (2026-09-21): a row of nudge buttons per bank (0-
     * {@link MetaTileEntityFuelAssemblyPort#BANK_COUNT}), not a one-shot "withdraw/insert fully" jump -- see
     * {@code MetaTileEntityPressurizedWaterReactor#requestNudgeBank}'s own JavaDoc for why (matches this mod's
     * existing "no automatic withdrawal button" decision, applied to a whole bank at once). One bank per row
     * (2026-09-22: stacked vertically, not two banks per row, to leave headroom below for the taller layout).
     * Bank 0 (the default "unassigned" bank every controller starts in) gets its own row too (2026-09-22 playtest
     * feedback): without one, a controller never explicitly assigned to bank 1-{@code BANK_COUNT} could not be
     * nudged at all.
     * <p>
     * Temporary addition (2026-09-23, user request): coarser (5%) and finer (0.1%) step buttons alongside the
     * original 1% pair, to make manual criticality approach/fine-tuning practical without needing dozens of
     * clicks -- see {@link #NUDGE_STEP_FRACTIONS}.
     */
    // Scaled down 5x on 2026-11-27 (stage 2-2, decision B) -- see ReactorAutoControlSequencer#LARGE_STEP.
    private static final double[] NUDGE_STEP_FRACTIONS = { 0.01, 0.002, 0.0005 };
    private static final String[] NUDGE_STEP_LABELS = { "1%", ".2%", ".05%" };

    private void addBankNudgeButtons(@NotNull ParentWidget<?> parent, @NotNull PanelSyncManager syncManager) {
        int buttonWidth = 29;
        int gap = 2;
        for (int bankId = 0; bankId <= MetaTileEntityFuelAssemblyPort.BANK_COUNT; bankId++) {
            int y = 244 + bankId * 18;
            int finalBankId = bankId;
            int x = 4;

            for (int i = 0; i < NUDGE_STEP_FRACTIONS.length; i++) {
                double step = NUDGE_STEP_FRACTIONS[i];
                parent.child(literalCommandButton("bank_" + bankId + "_minus_" + NUDGE_STEP_LABELS[i] + "_trigger",
                        syncManager, () -> {
                            MetaTileEntityPressurizedWaterReactor reactor = getLinkedReactor();
                            if (reactor != null) reactor.requestNudgeBank(finalBankId, -step);
                        }, "-" + NUDGE_STEP_LABELS[i]).pos(x, y).size(buttonWidth, 16));
                x += buttonWidth + gap;
            }
            for (int i = NUDGE_STEP_FRACTIONS.length - 1; i >= 0; i--) {
                double step = NUDGE_STEP_FRACTIONS[i];
                parent.child(literalCommandButton("bank_" + bankId + "_plus_" + NUDGE_STEP_LABELS[i] + "_trigger",
                        syncManager, () -> {
                            MetaTileEntityPressurizedWaterReactor reactor = getLinkedReactor();
                            if (reactor != null) reactor.requestNudgeBank(finalBankId, step);
                        }, "+" + NUDGE_STEP_LABELS[i]).pos(x, y).size(buttonWidth, 16));
                x += buttonWidth + gap;
            }
        }
    }

    /**
     * Stage 5's control rod bank-assignment list: one row per control rod controller (position-sorted), clicking
     * a row cycles that controller's bank ({@code 0 -> 1 -> ... -> BANK_COUNT -> 0}). Built once when the GUI
     * opens from whatever {@code reactor.getControlRodSummaries()} snapshot is on hand at that moment (not
     * live-refreshed while the GUI stays open) -- a deliberately provisional placeholder ahead of this mod's
     * planned dedicated UI system redesign (2026-09-21), not meant to be the final control rod management UI.
     */
    private void addControlRodList(@NotNull ParentWidget<?> parent, @NotNull PanelSyncManager syncManager) {
        MetaTileEntityPressurizedWaterReactor reactor = getLinkedReactor();
        List<MetaTileEntityPressurizedWaterReactor.ControlRodSummary> summaries = reactor != null ?
                reactor.getControlRodSummaries() : Collections.emptyList();
        parent.child(new ControlRodListWidget(summaries, syncManager, this::cycleControlRodBank)
                .pos(4, 338).size(182, 70));
    }

    /** Server-side target of the list's row clicks -- see {@link ControlRodListWidget}'s own JavaDoc. */
    private void cycleControlRodBank(int dx, int dz) {
        MetaTileEntityPressurizedWaterReactor reactor = getLinkedReactor();
        if (reactor != null) reactor.requestCycleControlRodBank(dx, dz);
    }

    /**
     * See {@link #addControlRodList}'s own JavaDoc. Mirrors GTST's own {@code WidgetCoverList}/{@code WidgetOreList}.
     */
    private static final class ControlRodListWidget extends ListWidget<IWidget, ControlRodListWidget> {

        ControlRodListWidget(@NotNull List<MetaTileEntityPressurizedWaterReactor.ControlRodSummary> summaries,
                             @NotNull PanelSyncManager syncManager,
                             @NotNull BiConsumer<Integer, Integer> onCycleBank) {
            List<MetaTileEntityPressurizedWaterReactor.ControlRodSummary> sorted = new ArrayList<>(summaries);
            sorted.sort(Comparator.<MetaTileEntityPressurizedWaterReactor.ControlRodSummary>comparingInt(s -> s.dz())
                    .thenComparingInt(s -> s.dx()));
            for (MetaTileEntityPressurizedWaterReactor.ControlRodSummary summary : sorted) {
                int dx = summary.dx();
                int dz = summary.dz();
                String label = String.format("(%d,%d) Bank:%d D:%.1f%%", dx, dz, summary.bankId(),
                        summary.insertionDepth() * 100.0);
                BooleanSyncValue trigger = new BooleanSyncValue(() -> false, val -> {
                    if (val) onCycleBank.accept(dx, dz);
                });
                syncManager.syncValue("bank_cycle_" + dx + "_" + dz, trigger);
                child(new ButtonWidget<>()
                        .size(178, 14)
                        .overlay(IKey.str(label))
                        .onMousePressed(mouseButton -> {
                            trigger.setBoolValue(true);
                            return true;
                        }));
            }
        }
    }

    @NotNull
    private static ButtonWidget<?> commandButton(@NotNull String syncKey, @NotNull PanelSyncManager syncManager,
                                                 @NotNull Runnable action, @NotNull String langKey) {
        return literalCommandButton(syncKey, syncManager, action, IKey.lang(langKey));
    }

    /**
     * As {@link #commandButton}, but with literal (unlocalized) button text via {@link IKey#str} -- used for the
     * stage-5 bank nudge buttons (2026-09-21), whose labels are generated per bank ("Bank 1 -1%" etc.) rather than
     * drawn from a fixed lang-file key.
     */
    @NotNull
    private static ButtonWidget<?> literalCommandButton(@NotNull String syncKey, @NotNull PanelSyncManager syncManager,
                                                        @NotNull Runnable action, @NotNull String label) {
        return literalCommandButton(syncKey, syncManager, action, IKey.str(label));
    }

    @NotNull
    private static ButtonWidget<?> literalCommandButton(@NotNull String syncKey, @NotNull PanelSyncManager syncManager,
                                                        @NotNull Runnable action, @NotNull IKey overlay) {
        // BooleanSyncValue's setter fires twice per press by this framework's own design (see
        // AbstractGenericSyncValue/ValueSyncHandler): once locally where the button was clicked (client), and again
        // when the synced value packet is received on the other side (server), since both setBoolValue() call sites
        // pass setSource=true. Harmless for every other command button here (their actions only mutate this MTE's
        // own fields, and a stray client-side mutation is invisible -- overwritten by the next server sync anyway),
        // but a real playtest (2026-09-26) confirmed it corrupts requestToggleAutoRodControl()'s telemetry CSV: the
        // client and server sides both actually open a real file on disk at the same path, racing each other.
        // Guarding here fixes it for every button in this class at once, not just that one.
        BooleanSyncValue trigger = new BooleanSyncValue(() -> false, val -> {
            if (val && !NetworkUtils.isClient()) action.run();
        });
        syncManager.syncValue(syncKey, trigger);
        return new ButtonWidget<>()
                .size(44, 16)
                .overlay(overlay)
                .onMousePressed(mouseButton -> {
                    trigger.setBoolValue(true);
                    return true;
                });
    }

    @SideOnly(Side.CLIENT)
    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        // Generously padded rather than computed from the (still placeholder, soon to be user-redesigned) exact
        // structure bounds -- isGlobalRenderer() means this box isn't relied on for frustum culling anyway.
        return new AxisAlignedBB(getPos()).grow(16);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public boolean isGlobalRenderer() {
        return true;
    }

    /**
     * The user's actual built console front wall (2026-09-19) is a fixed-width 19-block-wide same-facing run
     * that they want shown as 3 separate fixed-size displays -- 5, 9, and 5 blocks wide -- rather than one huge
     * merged panel. This is deliberately hardcoded to this specific structure (not a generic "always split into
     * N" rule): a same-facing group whose merged width happens to equal the sum of these widths gets sliced into
     * that many independent panels; any other width (e.g. the console's two angled side walls) still renders as
     * one merged panel per {@link #renderMergedPanel}.
     */
    private static final int[] FIXED_PANEL_WIDTHS = { 5, 9, 5 };
    /**
     * Per-panel content role, parallel to {@link #FIXED_PANEL_WIDTHS} (2026-09-21 playtest feedback): left =
     * status text, middle (the largest panel) = core map, right = physics telemetry -- see
     * {@link MetaTileEntityConsoleScreen.PanelRole}'s own JavaDoc.
     */
    private static final MetaTileEntityConsoleScreen.PanelRole[] FIXED_PANEL_ROLES = {
            MetaTileEntityConsoleScreen.PanelRole.STATUS, MetaTileEntityConsoleScreen.PanelRole.CORE_MAP,
            MetaTileEntityConsoleScreen.PanelRole.PHYSICS
    };

    @SideOnly(Side.CLIENT)
    @Override
    public void renderMetaTileEntity(double x, double y, double z, float partialTicks) {
        if (getWorld() == null) return;
        Map<EnumFacing, List<MetaTileEntityConsoleScreen>> groups = new EnumMap<>(EnumFacing.class);
        for (BlockPos screenPos : screenPositions) {
            TileEntity tileEntity = getWorld().getTileEntity(screenPos);
            if (!(tileEntity instanceof IGregTechTileEntity)) continue;
            MetaTileEntity metaTileEntity = ((IGregTechTileEntity) tileEntity).getMetaTileEntity();
            if (!(metaTileEntity instanceof MetaTileEntityConsoleScreen screen)) continue;
            groups.computeIfAbsent(screen.getFrontFacing(), facing -> new ArrayList<>()).add(screen);
        }
        for (Map.Entry<EnumFacing, List<MetaTileEntityConsoleScreen>> entry : groups.entrySet()) {
            renderScreenGroup(x, y, z, partialTicks, entry.getKey(), entry.getValue());
        }
    }

    /**
     * Renders one flat, coplanar wall segment's worth of same-facing screens (see this mod's design memory,
     * 2026-09-19: the user built a bent multi-segment wall of many individual screen blocks and wants each flat
     * segment shown as one or more continuous displays rather than one small panel per block). Screens are
     * grouped by {@link MetaTileEntityConsoleScreen#getFrontFacing()} by the caller. If the group's total merged
     * width matches {@link #FIXED_PANEL_WIDTHS}' sum, it is sliced into that many independent fixed-size panels
     * (see that field's own JavaDoc); otherwise the whole group renders as a single merged panel.
     * <p>
     * The local panel axes used here (and by {@code MetaTileEntityConsoleScreen#renderScreen}) come directly
     * from GTST's own {@code RenderUtil.rotateToFace(facing, EnumFacing.UP)} source (not a decompile -- an
     * earlier bytecode-only reading of this method had NORTH/SOUTH and EAST/WEST's case bodies confused, which
     * would have produced exactly this kind of silently-wrong grouping). With the {@code spin} parameter fixed
     * to {@code UP} (so the in-plane roll term is always 0), working out {@code rotateToFace}'s composed
     * scale+rotate matrices by hand for each horizontal facing gives, writing {@code dx = memberX - referenceX}
     * etc.: NORTH: local (a, b) = (-dx, -dy); SOUTH: (dx, -dy); EAST: (-dz, -dy); WEST: (dz, -dy). Vertical
     * always maps to {@code -dy} (increasing local y is downward, matching {@code FontRenderer}'s own
     * convention); horizontal maps to {@code dx} for north/south-facing walls (which run east-west, constant Z)
     * and to {@code dz} for east/west-facing walls (which run north-south, constant X), with a sign that mirrors
     * between opposite-facing walls -- consistent with two walls facing opposite directions needing mirrored
     * "which way is a viewer's right" conventions.
     */
    @SideOnly(Side.CLIENT)
    private void renderScreenGroup(double x, double y, double z, float partialTicks, @NotNull EnumFacing facing,
                                   @NotNull List<MetaTileEntityConsoleScreen> screens) {
        if (facing.getAxis() == EnumFacing.Axis.Y || screens.size() == 1) {
            // Merging isn't supported for ceiling/floor-mounted screens (rare, not the shape the user built) --
            // and a lone screen in a group is already exactly the single-block case.
            for (MetaTileEntityConsoleScreen screen : screens) {
                BlockPos pos = screen.getPos();
                screen.renderScreen(x + pos.getX() - getPos().getX(), y + pos.getY() - getPos().getY(),
                        z + pos.getZ() - getPos().getZ(), partialTicks, facing, 1, 1,
                        MetaTileEntityConsoleScreen.PanelRole.FULL);
            }
            return;
        }

        BlockPos groupRef = screens.get(0).getPos();
        Map<MetaTileEntityConsoleScreen, Integer> localA = new HashMap<>();
        int minA = Integer.MAX_VALUE, maxA = Integer.MIN_VALUE;
        for (MetaTileEntityConsoleScreen screen : screens) {
            int a = horizontalLocal(facing, screen.getPos(), groupRef);
            localA.put(screen, a);
            minA = Math.min(minA, a);
            maxA = Math.max(maxA, a);
        }

        int fixedTotalWidth = 0;
        for (int width : FIXED_PANEL_WIDTHS) fixedTotalWidth += width;
        if (maxA - minA + 1 == fixedTotalWidth) {
            int boundary = minA;
            for (int i = 0; i < FIXED_PANEL_WIDTHS.length; i++) {
                int width = FIXED_PANEL_WIDTHS[i];
                int rangeMin = boundary;
                int rangeMax = boundary + width - 1;
                List<MetaTileEntityConsoleScreen> subGroup = new ArrayList<>();
                for (MetaTileEntityConsoleScreen screen : screens) {
                    int a = localA.get(screen);
                    if (a >= rangeMin && a <= rangeMax) subGroup.add(screen);
                }
                // Each sub-group resolves its own corner from its own members' actual world positions (below),
                // so the console's middle 9-wide panel -- physically built one block further back than the two
                // 5-wide side panels -- lands at the correct recessed depth automatically, with no separate
                // depth-offset hack needed.
                if (!subGroup.isEmpty()) {
                    renderMergedPanel(x, y, z, partialTicks, facing, subGroup, FIXED_PANEL_ROLES[i]);
                }
                boundary += width;
            }
            return;
        }

        renderMergedPanel(x, y, z, partialTicks, facing, screens, MetaTileEntityConsoleScreen.PanelRole.FULL);
    }

    private static int horizontalLocal(@NotNull EnumFacing facing, @NotNull BlockPos pos, @NotNull BlockPos ref) {
        int dx = pos.getX() - ref.getX();
        int dz = pos.getZ() - ref.getZ();
        return switch (facing) {
            case NORTH -> -dx;
            case SOUTH -> dx;
            case EAST -> -dz;
            case WEST -> dz;
            default -> 0;
        };
    }

    @SideOnly(Side.CLIENT)
    private void renderMergedPanel(double x, double y, double z, float partialTicks, @NotNull EnumFacing facing,
                                   @NotNull List<MetaTileEntityConsoleScreen> screens,
                                   @NotNull MetaTileEntityConsoleScreen.PanelRole role) {
        BlockPos refPos = screens.get(0).getPos();
        int minA = Integer.MAX_VALUE, maxA = Integer.MIN_VALUE, minB = Integer.MAX_VALUE, maxB = Integer.MIN_VALUE;
        for (MetaTileEntityConsoleScreen screen : screens) {
            BlockPos pos = screen.getPos();
            int a = horizontalLocal(facing, pos, refPos);
            int b = -(pos.getY() - refPos.getY());
            minA = Math.min(minA, a);
            maxA = Math.max(maxA, a);
            minB = Math.min(minB, b);
            maxB = Math.max(maxB, b);
        }
        int widthBlocks = maxA - minA + 1;
        int heightBlocks = maxB - minB + 1;

        int cornerDx = switch (facing) {
            case NORTH -> -minA;
            case SOUTH -> minA;
            default -> 0;
        };
        int cornerDz = switch (facing) {
            case EAST -> -minA;
            case WEST -> minA;
            default -> 0;
        };
        int cornerDy = -minB;

        double anchorX = x + (refPos.getX() + cornerDx) - getPos().getX();
        double anchorY = y + (refPos.getY() + cornerDy) - getPos().getY();
        double anchorZ = z + (refPos.getZ() + cornerDz) - getPos().getZ();
        screens.get(0).renderScreen(anchorX, anchorY, anchorZ, partialTicks, facing, widthBlocks, heightBlocks,
                role);
    }
}
