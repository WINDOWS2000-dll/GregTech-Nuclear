package com.windows2000.gtnuclear.common.metatileentities.multiblockpart;

import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.interfaces.IGregTechTileEntity;
import gregtech.api.metatileentity.multiblock.AbilityInstances;
import gregtech.api.metatileentity.multiblock.IMultiblockAbilityPart;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.metatileentity.multiblock.MultiblockControllerBase;
import gregtech.client.utils.RenderUtil;
import gregtech.common.metatileentities.multi.multiblockpart.MetaTileEntityMultiblockPart;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import com.windows2000.gtnuclear.api.metatileentity.multiblock.GTNLMultiblockAbility;
import com.windows2000.gtnuclear.api.nuclear.accident.SevereAccidentStage;
import com.windows2000.gtnuclear.api.nuclear.operation.ReactorOperatingStage;
import com.windows2000.gtnuclear.api.nuclear.operation.ReactorOperatingState;
import com.windows2000.gtnuclear.api.util.GTNLLogger;
import com.windows2000.gtnuclear.common.ConfigHolder;
import com.windows2000.gtnuclear.common.metatileentities.multi.MetaTileEntityControlConsole;
import com.windows2000.gtnuclear.common.metatileentities.multi.MetaTileEntityPressurizedWaterReactor;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

/**
 * The control console's read-only telemetry display, inspired by (but not built on top of) GTST's own
 * {@code MetaTileEntityMonitorScreen} -- no plugin item slot, no cover-based data reading: this part always
 * shows the same built-in reactor telemetry view for whichever reactor is currently reachable through the
 * console's own
 * {@link com.windows2000.gtnuclear.api.metatileentity.multiblock.GTNLMultiblockAbility#REACTOR_LINK_RECEIVER}
 * hatch (see {@link MetaTileEntityControlConsole}'s own JavaDoc for how that link is resolved server-side and
 * synced -- this part only ever reads the already-resolved {@code getLinkedReactorPos()} result).
 * <p>
 * Most telemetry fields shown here are still placeholders (0 / "--"): the neutron/thermal physics engine is not
 * wired into {@link MetaTileEntityPressurizedWaterReactor} yet (a separate, later phase -- see this mod's design
 * memory). What already works: structure-formed status, the fuel/control-rod housing mismatch flag, and a core
 * map colored by each control rod controller's current {@code getInsertionDepth()} (fuel ports are shown in a
 * fixed color for now, since per-channel fuel temperature does not exist yet either).
 * <p>
 * <b>Rendering is driven by the console controller, not this part itself.</b> GTST's own
 * {@code MetaTileEntityCentralMonitor} (not {@code MetaTileEntityMonitorScreen}) is the one that implements
 * {@code IFastRenderMetaTileEntity}, iterating its own child screens and rendering each one in a single call --
 * putting that interface on the screen part instead (an earlier revision of this class) meant
 * {@code renderMetaTileEntity(double,double,double,float)} was simply never invoked, confirmed by a diagnostic
 * log line that never fired even though GTST's own Central Monitor rendered correctly in the same test. This
 * class instead exposes a plain {@link #renderScreen(double, double, double, float)} method that
 * {@code MetaTileEntityControlConsole} calls directly, mirroring that working pattern exactly.
 */
public class MetaTileEntityConsoleScreen extends MetaTileEntityMultiblockPart
                                         implements IMultiblockAbilityPart<MetaTileEntityConsoleScreen> {

    private static final int COLOR_BACKGROUND = 0xFF000000;
    private static final int COLOR_FRAME = 0xFF80C0FF;
    private static final int COLOR_TEXT = 0xFFFFFFFF;
    private static final int COLOR_WARNING = 0xFFFF4040;
    private static final int COLOR_FUEL = 0xFF3070FF;
    /** A depleted fuel rod's own end of the gradient -- reuses the same red as a fully-inserted control rod. */
    private static final int COLOR_FUEL_DEPLETED = 0xFFFF4040;
    private static final int COLOR_CONTROL_ROD_WITHDRAWN = 0xFF40FF40;
    private static final int COLOR_CONTROL_ROD_INSERTED = 0xFFFF4040;
    /** Same brightness as {@link #COLOR_TEXT} (2026-09-21 feedback: the original dim gray was too subtle). */
    private static final int COLOR_GRID = COLOR_TEXT;
    /** Core map: an interior cell with plain casing (no port). */
    private static final int COLOR_CASING_CELL = 0xFF303030;
    /** Core map: a fuel assembly port without fuel. */
    private static final int COLOR_EMPTY_PORT = 0xFF909090;

    /**
     * Shrinks glyph size (and, proportionally, line spacing) relative to the panel-size-driven {@code scale} used
     * elsewhere -- 2026-09-21 playtest feedback: at the original 1:1 ratio, ~9 lines of telemetry text filled
     * almost the entire panel, leaving no room for {@link #renderCoreMap}. Decoupled from the core map's own
     * sizing, which already fills whatever area it's given.
     */
    private static final float TEXT_SCALE_FACTOR = 0.6f;

    /**
     * Which subset of telemetry a given panel shows -- see {@code MetaTileEntityControlConsole#renderScreenGroup}
     * for how a panel's role is decided (the fixed 5/9/5-wide three-panel split maps to
     * STATUS/CORE_MAP/PHYSICS respectively; any other panel shape/grouping uses FULL, the original combined
     * layout). Introduced 2026-09-21 alongside the text scale reduction, both from the same playtest feedback:
     * cramming everything onto every panel left no room to make the core map genuinely legible.
     */
    public enum PanelRole {
        FULL,
        STATUS,
        CORE_MAP,
        PHYSICS
    }

    /** Temporary diagnostic flag (see this mod's design memory), logs only the first ever call in the session. */
    private static volatile boolean loggedFirstRenderCall = false;

    public MetaTileEntityConsoleScreen(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId, 1);
    }

    @Override
    public MetaTileEntity createMetaTileEntity(IGregTechTileEntity tileEntity) {
        return new MetaTileEntityConsoleScreen(metaTileEntityId);
    }

    @Override
    protected boolean openGUIOnRightClick() {
        return false;
    }

    @Override
    public boolean hasFrontFacing() {
        return true;
    }

    @Override
    public MultiblockAbility<MetaTileEntityConsoleScreen> getAbility() {
        return GTNLMultiblockAbility.CONSOLE_SCREEN;
    }

    @Override
    public void registerAbilities(@NotNull AbilityInstances abilityInstances) {
        abilityInstances.add(this);
    }

    /**
     * Resolves the reactor reachable through this console's own receiver link hatch, if any. Reads
     * {@code MetaTileEntityControlConsole#getLinkedReactorPos()} directly -- an explicitly-synced field the
     * console resolves server-side every tick (see that class's own JavaDoc for why: the underlying
     * {@code MetaTileEntityReactorLinkHatch#getReactor()} chain is itself server-only, so this part must never
     * call it directly and must only ever read the console's already-resolved, already-synced result).
     */
    private MetaTileEntityPressurizedWaterReactor getLinkedReactor() {
        if (getWorld() == null) return null;
        MultiblockControllerBase controller = getController();
        if (!(controller instanceof MetaTileEntityControlConsole console)) return null;
        BlockPos reactorPos = console.getLinkedReactorPos();
        if (reactorPos == null) return null;
        TileEntity tileEntity = getWorld().getTileEntity(reactorPos);
        if (!(tileEntity instanceof IGregTechTileEntity gregTechTileEntity)) return null;
        MetaTileEntity metaTileEntity = gregTechTileEntity.getMetaTileEntity();
        return metaTileEntity instanceof MetaTileEntityPressurizedWaterReactor reactor &&
                reactor.isStructureFormed() ? reactor : null;
    }

    /**
     * Draws the merged panel for a whole group of same-facing, coplanar screens, given the render-relative world
     * position of the group's own "top-left" corner block (in local panel space -- see
     * {@code MetaTileEntityControlConsole#renderScreenGroup} for how that corner and {@code widthBlocks}/
     * {@code heightBlocks} are computed from the group's world positions). A single-block screen is simply the
     * {@code widthBlocks == heightBlocks == 1} case. Called directly by {@code MetaTileEntityControlConsole}'s
     * own {@code IFastRenderMetaTileEntity} implementation -- see this class's own JavaDoc for why the interface
     * isn't implemented here.
     */
    @SideOnly(Side.CLIENT)
    public void renderScreen(double x, double y, double z, float partialTicks, @NotNull EnumFacing facing,
                             int widthBlocks, int heightBlocks, @NotNull PanelRole role) {
        if (getWorld() == null) return;
        if (!loggedFirstRenderCall) {
            loggedFirstRenderCall = true;
            GTNLLogger.logger.info(
                    "MetaTileEntityConsoleScreen.renderScreen() first call: pos={}, facing={}, xyz=({},{},{}), size={}x{}",
                    getPos(), facing, x, y, z, widthBlocks, heightBlocks);
        }

        GlStateManager.pushMatrix();
        try {
            // Mirrors GTST's own MetaTileEntityCentralMonitor screen-rendering setup exactly (verified against
            // GTST's own source, not a decompile): moveToFace() lands exactly on the outward face center of the
            // group's corner block, and rotateToFace(facing, UP) orients it right-side up for a player standing
            // in front of it. This mod's own earlier hand-derived rotation+offset+sign-flip chain got the
            // in-plane orientation wrong and, once patched with ad-hoc compensations, ended up positioning
            // content far outside the panel -- using GTST's own proven helpers instead of re-deriving the same
            // math by hand avoids both classes of bug.
            RenderUtil.moveToFace(x, y, z, facing);
            RenderUtil.rotateToFace(facing, EnumFacing.UP);
            GlStateManager.disableLighting();
            // Defensive: the "fast render" dispatch path (Forge's hasFastRenderer()) does not itself disable
            // backface culling the way the classic TESR path does, so a quad that ends up back-facing would
            // otherwise be silently discarded -- invisible with no error, matching the symptom seen here (render
            // call confirmed firing via logging, nothing visible) before this was added.
            GlStateManager.disableCull();

            // The merged panel spans widthBlocks x heightBlocks in local units, one unit per block, with the
            // corner block's own face center at local (0, 0). It fills the full block face edge-to-edge (no gap
            // to the surrounding casing) -- only the text/content placement below keeps its own small inset
            // (`padding`) so glyphs don't touch the frame border.
            float scale = Math.min(widthBlocks, heightBlocks);
            float padding = 0.05f * scale;
            float aLeft = -0.5f;
            float aRight = widthBlocks - 0.5f;
            float bTop = -0.5f;
            float bBottom = heightBlocks - 0.5f;
            float borderThickness = 0.01f * scale;

            // Positive Z here matches GTST's own renderRect calls in the same reference method -- after
            // rotateToFace(), local +Z is the side the player stands on (outward), so a small positive value
            // pushes each layer just off the block's own face instead of z-fighting/clipping against it.
            RenderUtil.renderRect(aLeft, bTop, aRight - aLeft, bBottom - bTop, 0.001f, COLOR_BACKGROUND);
            RenderUtil.renderRect(aLeft, bTop, aRight - aLeft, borderThickness, 0.002f, COLOR_FRAME);
            RenderUtil.renderRect(aLeft, bBottom - borderThickness, aRight - aLeft, borderThickness, 0.002f,
                    COLOR_FRAME);
            RenderUtil.renderRect(aLeft, bTop, borderThickness, bBottom - bTop, 0.002f, COLOR_FRAME);
            RenderUtil.renderRect(aRight - borderThickness, bTop, borderThickness, bBottom - bTop, 0.002f,
                    COLOR_FRAME);

            // RenderUtil.renderRect() always disableBlend()s itself as its own cleanup, leaving blending off by
            // the time we get here -- FontRenderer's own glyph rendering needs it back on, or its alpha-keyed
            // glyph texture doesn't composite correctly and the text doesn't show at all.
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

            MetaTileEntityPressurizedWaterReactor reactor = getLinkedReactor();
            if (reactor == null) {
                renderTextOnPanel(aLeft + padding, bTop + 2 * padding, COLOR_WARNING, "NO REACTOR LINKED",
                        scale * TEXT_SCALE_FACTOR);
            } else {
                renderTelemetry(reactor, aLeft, aRight, bTop, bBottom, padding, scale, role);
            }

            GlStateManager.enableLighting();
            GlStateManager.enableCull();
        } catch (Exception exception) {
            GTNLLogger.logger.error("MetaTileEntityConsoleScreen.renderScreen() threw", exception);
        } finally {
            GlStateManager.popMatrix();
        }
    }

    /**
     * Dispatches on {@code role} (see that enum's own JavaDoc): STATUS/PHYSICS render only their own subset of
     * text lines with no core map; CORE_MAP renders no text at all, just the map filling the whole panel; FULL
     * reproduces the original combined layout (all lines, then the core map in whatever space is left) for any
     * panel that isn't part of the dedicated three-way split.
     */
    private void renderTelemetry(@NotNull MetaTileEntityPressurizedWaterReactor reactor, float aLeft, float aRight,
                                 float bTop, float bBottom, float padding, float scale, @NotNull PanelRole role) {
        if (role == PanelRole.CORE_MAP) {
            renderCoreMap(reactor, aLeft + padding, bTop + padding, aRight - aLeft - 2 * padding,
                    bBottom - bTop - 2 * padding);
            return;
        }

        float textScale = scale * TEXT_SCALE_FACTOR;
        List<String> lines = role == PanelRole.PHYSICS ? physicsLines(reactor) : statusLines(reactor);
        if (role == PanelRole.FULL) lines.addAll(physicsLines(reactor));

        float b = bTop + 2 * padding;
        for (String line : lines) {
            int color = line.contains("MISMATCH") || line.startsWith("SCRAM") || line.startsWith("BREACH") ||
                    line.contains("WARNING") || line.contains("ISOLATED") || line.contains("REFUSED") ||
                    line.contains("NO POWER") || line.contains("NO COOLING") ?
                            COLOR_WARNING : COLOR_TEXT;
            renderTextOnPanel(aLeft + padding, b, color, line, textScale);
            b += 0.06f * textScale;
        }

        if (role == PanelRole.FULL) {
            renderCoreMap(reactor, aLeft + padding, b + 0.03f * textScale, aRight - aLeft - 2 * padding,
                    bBottom - padding - (b + 0.03f * textScale));
        }
    }

    @NotNull
    private static List<String> statusLines(@NotNull MetaTileEntityPressurizedWaterReactor reactor) {
        ReactorOperatingState operatingState = reactor.getOperatingState();
        List<String> lines = new ArrayList<>();
        lines.add("STRUCTURE: " + (reactor.isStructureFormed() ? "FORMED" : "NOT FORMED"));
        lines.add("HOUSING: " + (reactor.isHousingMismatch() ? "MISMATCH" : "OK"));
        if (reactor.isLengthMismatch()) lines.add("LENGTH: MISMATCH");
        if (reactor.isStructureBreachRecorded()) lines.add("BREACH: STRUCTURE BROKEN WHILE RUNNING");
        lines.add("ASSEMBLIES: " + reactor.getLoadedFuelAssemblyCount() + " fuel / " +
                reactor.getLoadedControlRodClusterCount() + " rods");
        lines.add("STAGE: " + operatingState.stage);
        if (operatingState.stage == ReactorOperatingStage.SHUTDOWN && reactor.getSyncedStartupBlocker() != null) {
            lines.add("STARTUP BLOCKED: " + reactor.getSyncedStartupBlocker());
        }
        if (operatingState.stage == ReactorOperatingStage.TRIPPED) {
            lines.add("SCRAM: " + operatingState.lastScramReason);
        }
        // Real synced values. Pressure remains a plain operator-managed status value (not physically coupled to
        // temperature/boiling) pending a real secondary-side-driven pressure model. Displayed in mb (1000 mb =
        // 1 kg, matching MetaTileEntityPressurizedWaterReactor#updateCoolantInjection's own conversion) to match
        // the fluid-hatch capacities (also in mb) players compare this against, even though
        // coolantMassKg/ratedCoolantMassKg themselves stay kg internally for the physics calculations.
        lines.add("COOLANT: " + String.format("%.0f", reactor.getCoolantMassKg() * 1000.0) + " / " +
                String.format("%.0f", reactor.getRatedCoolantMassKg() * 1000.0) + " mb" +
                (reactor.isCoolantInjectionActive() ? " (INJECTING)" : ""));
        // Added 2026-09-28 (user request): the reactor's own current primary circulation flow -- the same mb
        // convention as COOLANT above, but per-tick (a real RCP's own instantaneous flow rate reading) rather
        // than total inventory.
        lines.add("FLOW: " + reactor.getPrimaryCirculationFlowMb() + " mb/t");
        lines.add("PRESSURE: " + String.format("%.2f", reactor.getPressureMPa()) + " MPa" +
                (reactor.isPressurizationActive() ? " (PRESSURIZING)" :
                        reactor.isDepressurizationActive() ? " (DEPRESSURIZING)" : ""));
        lines.add("TEMP: " + String.format("%.0f", reactor.getTemperatureKelvin()) + " K");
        lines.add("FUEL TEMP: " + String.format("%.0f", reactor.getFuelTemperatureKelvin()) + " K");
        if (reactor.getPrimaryVoidFraction() > 0) {
            lines.add("VOID: " + String.format("%.1f", reactor.getPrimaryVoidFraction() * 100.0) + "%");
        }
        lines.add("BORON: " + String.format("%.0f", reactor.getBoronPpm()) + " ppm" +
                (reactor.isBoronDilutionActive() ? " (DILUTING)" :
                        reactor.isBoronConcentrationActive() ? " (CONCENTRATING)" : ""));
        // Added 2026-09-28 (user request): the reactor's own current power draw (baseline instrumentation +
        // coolant pump combined, see MetaTileEntityPressurizedWaterReactor#updateReactorPowerConsumption).
        lines.add("POWER USE: " + reactor.getTotalPowerConsumptionEuPerTick() + " EU/t");
        lines.add("RCP: " + (reactor.isRcpRunning() ? "RUNNING" : "STOPPED"));
        lines.add(rhrLine(reactor));
        if (reactor.isRhrInService() && reactor.isRhrOutletCapped()) {
            lines.add("RHR WARNING: COOLING WATER TEMP LIMIT");
        }
        if (reactor.isAutoRodControlActive()) {
            lines.add("AUTO CONTROL: ON");
        }
        if (reactor.getSevereAccidentState().stage != SevereAccidentStage.NORMAL) {
            lines.add("!! " + reactor.getSevereAccidentState().stage + ": " +
                    String.format("%.0f", reactor.getSevereAccidentState().coreMeltFraction * 100.0) + "% !!");
        }
        return lines;
    }

    /** Stage 1-4a: residual heat removal status, heat removed and cooling water temperatures. */
    @NotNull
    private static String rhrLine(@NotNull MetaTileEntityPressurizedWaterReactor reactor) {
        if (!reactor.isRhrInService()) {
            return switch (reactor.getRhrNotice()) {
                case ISOLATED_OVERPRESSURE -> "RHR: ISOLATED (OVERPRESSURE)";
                case START_REFUSED_TEMPERATURE -> "RHR: STANDBY (START REFUSED: TEMP)";
                case START_REFUSED_PRESSURE -> "RHR: STANDBY (START REFUSED: PRESS)";
                default -> "RHR: STANDBY";
            };
        }
        if (reactor.isRhrNoPower()) return "RHR: IN SERVICE (NO POWER)";
        if (reactor.isRhrNoCoolingWater()) return "RHR: IN SERVICE (NO COOLING WATER)";
        return String.format("RHR: %.1f MW, CW %.0f->%.0f K", reactor.getRhrHeatRemovalWatts() / 1.0e6,
                reactor.getRhrCoolingInletKelvin(), reactor.getRhrCoolingOutletKelvin());
    }

    @NotNull
    private static List<String> physicsLines(@NotNull MetaTileEntityPressurizedWaterReactor reactor) {
        // Real synced values from the neutron physics tick loop (CMFD shape + point kinetics amplitude + xenon).
        List<String> lines = new ArrayList<>();
        lines.add("POWER: " + String.format("%.4f", reactor.getRelativeNeutronPopulation()) + " (rel.)");
        lines.add("REACTIVITY: " + String.format("%.0f", reactor.getTotalReactivity() * 1.0e5) + " pcm");
        lines.add("SHAPE: " + String.format("%.0f", reactor.getShapeReactivityPcm()) + " pcm");
        lines.add("XENON: " + String.format("%.0f", reactor.getXenonReactivityWorth() * 1.0e5) + " pcm");
        lines.add("MTC: " + String.format("%.0f", reactor.getModeratorTemperatureReactivityPcm()) + " pcm");
        lines.add("DOPPLER: " + String.format("%.0f", reactor.getDopplerReactivityPcm()) + " pcm");
        // Debug-only spatial-resolution indicators -- see getDopplerReactivityPcmSpread()'s own JavaDoc. Both
        // read 0 whenever every fuel port's local conditions coincide (indistinguishable from the old
        // reactor-wide-scalar model by looking at XENON/DOPPLER above alone), so a nonzero value here is the
        // actual evidence that node-level local feedback is doing something.
        lines.add("XE SPREAD: " + String.format("%.0f", reactor.getXenonReactivityPcmSpread()) + " pcm");
        lines.add("DOPPLER SPREAD: " + String.format("%.0f", reactor.getDopplerReactivityPcmSpread()) + " pcm");
        if (reactor.getPrimaryVoidFraction() > 0) {
            double voidReactivityPcm = ConfigHolder.reactor.voidReactivityCoefficientPcm *
                    reactor.getPrimaryVoidFraction();
            lines.add("VOID: " + String.format("%.0f", voidReactivityPcm) + " pcm");
        }
        double deadTimeRemaining = reactor.getXenonDeadTimeRemainingSeconds();
        if (deadTimeRemaining > 0) {
            lines.add("XENON PIT: ~" + String.format("%.0f", deadTimeRemaining / 60.0) + " min remaining");
        }
        return lines;
    }

    /**
     * Thin wrapper around {@link RenderUtil#renderText} matching GTST's own {@code TextPluginBehavior} call
     * pattern exactly (verified against GTST's own source): it passes plain panel-space coordinates straight
     * through, with no extra rotation or sign flips -- the frame/background rects above already establish a
     * coordinate frame where this "just works", so no compensation belongs here. {@code z} is a small positive
     * value, larger than the frame/background rects' own so text always draws in front of them. {@code scale}
     * enlarges the font (and by extension the glyph advance) proportionally to how many blocks the merged panel
     * spans, so text fills a multi-block panel instead of staying pinned at single-block size.
     */
    private static void renderTextOnPanel(float x, float y, int color, @NotNull String text, float scale) {
        RenderUtil.renderText(x, y, 0.004f, scale / 128f, color, text, false);
    }

    /**
     * One square per interior channel cell (stage 1-3): plain casing dark gray, an empty port light gray, a fuel
     * assembly blue-to-red by depletion; a loaded control rod cluster draws a smaller inner square colored
     * green-to-red by insertion depth on top of its fuel assembly. Reads
     * {@code reactor.getCoreMapCells()} -- an explicitly server-resolved-and-synced snapshot, not
     * {@code getMultiblockParts()} directly (that list is server-only bookkeeping, always empty on the client --
     * see {@link MetaTileEntityPressurizedWaterReactor.CoreMapCell}'s own JavaDoc for this mod's umpteenth run-in
     * with that exact pitfall, confirmed here too: the core map silently rendered nothing until this was fixed).
     */
    private void renderCoreMap(@NotNull MetaTileEntityPressurizedWaterReactor reactor, float areaX, float areaY,
                               float areaWidth, float areaHeight) {
        List<MetaTileEntityPressurizedWaterReactor.CoreMapCell> cells = reactor.getCoreMapCells();
        if (cells.isEmpty()) return;
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
        for (MetaTileEntityPressurizedWaterReactor.CoreMapCell cell : cells) {
            minX = Math.min(minX, cell.dx());
            maxX = Math.max(maxX, cell.dx());
            minZ = Math.min(minZ, cell.dz());
            maxZ = Math.max(maxZ, cell.dz());
        }
        float spanX = Math.max(maxX - minX, 1);
        float spanZ = Math.max(maxZ - minZ, 1);
        // Shrunk below the tightest fit (2026-09-21 playtest feedback: at 1:1 it looked oversized/cramped against
        // the panel edge) and centered in the available area, rather than anchored to its top-left corner.
        float cellSize = Math.min(areaWidth / (spanX + 1), areaHeight / (spanZ + 1)) * 0.7f;
        float gridWidth = (spanX + 1) * cellSize;
        float gridHeight = (spanZ + 1) * cellSize;
        float originX = areaX + (areaWidth - gridWidth) / 2f;
        float originY = areaY + (areaHeight - gridHeight) / 2f;

        renderGridLines(cells, minX, minZ, originX, originY, cellSize);

        float cellSquareSize = cellSize * 0.85f;
        float cellSquareMargin = (cellSize - cellSquareSize) / 2f;
        for (MetaTileEntityPressurizedWaterReactor.CoreMapCell cell : cells) {
            int color = switch (cell.kind()) {
                case CASING -> COLOR_CASING_CELL;
                case EMPTY_PORT -> COLOR_EMPTY_PORT;
                case FUEL -> lerpColor(COLOR_FUEL, COLOR_FUEL_DEPLETED, cell.depletionPercent() / 100f);
            };
            // Centered within its own cellSize x cellSize box (equal margin on all four sides), not anchored to
            // the box's top-left corner -- 2026-09-21 feedback: an off-center square put the grid line (drawn at
            // the box boundary) noticeably closer to one neighbor than the other instead of bisecting the gap.
            float cx = originX + (cell.dx() - minX) * cellSize + cellSquareMargin;
            float cz = originY + (cell.dz() - minZ) * cellSize + cellSquareMargin;
            RenderUtil.renderRect(cx, cz, cellSquareSize, cellSquareSize, 0.003f, color);
            if (cell.hasRod()) {
                float rodSize = cellSquareSize * 0.5f;
                float rodMargin = (cellSquareSize - rodSize) / 2f;
                int rodColor = lerpColor(COLOR_CONTROL_ROD_WITHDRAWN, COLOR_CONTROL_ROD_INSERTED,
                        cell.rodLevel() / 100f);
                RenderUtil.renderRect(cx + rodMargin, cz + rodMargin, rodSize, rodSize, 0.0035f, rodColor);
            }
        }
    }

    /**
     * Draws each existing cell's own border (four thin edges), rather than a full rectangular grid over the
     * bounding box -- 2026-09-21 playtest feedback: a full-rectangle grid drew lines through empty corners the
     * checkerboard/diamond-shaped core never actually occupies, whereas per-cell borders naturally trace the
     * reactor's real footprint (an edge only exists where a real cell's own boundary is), and a border shared by
     * two adjacent cells lands exactly on the line between their centers since both draw the same edge. Uses
     * {@link #COLOR_TEXT}'s own brightness (2026-09-21 feedback: the original dim gray was too subtle) and draws
     * behind the cell squares themselves ({@code z} smaller than {@link #renderCoreMap}'s own cell rects).
     */
    private static void renderGridLines(@NotNull List<MetaTileEntityPressurizedWaterReactor.CoreMapCell> cells,
                                        int minX, int minZ, float originX, float originY, float cellSize) {
        float thickness = Math.max(cellSize * 0.04f, 0.0005f);
        for (MetaTileEntityPressurizedWaterReactor.CoreMapCell cell : cells) {
            float cx = originX + (cell.dx() - minX) * cellSize;
            float cz = originY + (cell.dz() - minZ) * cellSize;
            RenderUtil.renderRect(cx, cz, cellSize, thickness, 0.0025f, COLOR_GRID);
            RenderUtil.renderRect(cx, cz + cellSize - thickness, cellSize, thickness, 0.0025f, COLOR_GRID);
            RenderUtil.renderRect(cx, cz, thickness, cellSize, 0.0025f, COLOR_GRID);
            RenderUtil.renderRect(cx + cellSize - thickness, cz, thickness, cellSize, 0.0025f, COLOR_GRID);
        }
    }

    private static int lerpColor(int from, int to, float fraction) {
        float clamped = Math.max(0.0f, Math.min(1.0f, fraction));
        int a = lerpChannel(from >> 24, to >> 24, clamped);
        int r = lerpChannel(from >> 16, to >> 16, clamped);
        int g = lerpChannel(from >> 8, to >> 8, clamped);
        int b = lerpChannel(from, to, clamped);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int lerpChannel(int from, int to, float fraction) {
        int fromChannel = from & 0xFF;
        int toChannel = to & 0xFF;
        return (fromChannel + Math.round((toChannel - fromChannel) * fraction)) & 0xFF;
    }

    @Override
    public boolean showToolUsages() {
        return false;
    }
}
