package com.windows2000.gtnuclear.common.metatileentities.multi;

import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.interfaces.IGregTechTileEntity;
import gregtech.api.metatileentity.multiblock.IMultiblockPart;
import gregtech.api.metatileentity.multiblock.MultiblockWithDisplayBase;
import gregtech.api.metatileentity.multiblock.ProgressBarMultiblock;
import gregtech.api.metatileentity.multiblock.ui.MultiblockUIBuilder;
import gregtech.api.metatileentity.multiblock.ui.TemplateBarBuilder;
import gregtech.api.mui.GTGuiTextures;
import gregtech.api.pattern.BlockPattern;
import gregtech.api.pattern.FactoryBlockPattern;
import gregtech.api.util.KeyUtil;
import gregtech.api.util.RelativeDirection;
import gregtech.api.util.TextFormattingUtil;
import gregtech.client.renderer.ICubeRenderer;
import gregtech.client.renderer.texture.Textures;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.windows2000.gtnuclear.api.metatileentity.multiblock.GTNLMultiblockAbility;
import com.windows2000.gtnuclear.api.nuclear.thermal.CoolantThermalSolver;
import com.windows2000.gtnuclear.api.nuclear.thermal.EnergyLedger;
import com.windows2000.gtnuclear.api.nuclear.thermal.SteamProperties;
import com.windows2000.gtnuclear.api.nuclear.thermal.WaterSaturationCurve;
import com.windows2000.gtnuclear.api.unification.materials.GTNLMaterials;
import com.windows2000.gtnuclear.common.ConfigHolder;
import com.windows2000.gtnuclear.common.blocks.GTNLMetaBlocks;
import com.windows2000.gtnuclear.common.blocks.reactor.ReactorVesselCasing;
import com.windows2000.gtnuclear.common.util.HeatExchangerEnergyCategory;
import com.windows2000.gtnuclear.common.util.HeatExchangerTelemetryLogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.UnaryOperator;

/**
 * The secondary loop's heat exchanger (steam generator): a self-contained multiblock with no relationship to any
 * particular reactor beyond whatever is physically piped into it -- see this mod's design memory (2026-09-22):
 * primary coolant circulates via real fluid pipes (its own {@code REACTOR_COOLANT_IMPORT}/{@code
 * REACTOR_COOLANT_EXPORT} hatches, the very same hatch types the reactor itself uses, reused as-is rather than a
 * new hatch class) rather than a logical Optical Cable link, exactly like a real steam generator "not knowing"
 * which reactor it's attached to.
 * <p>
 * <b>This shape is a placeholder</b> (small fixed 3x3x3 box), matching {@code MetaTileEntityControlConsole}'s own
 * "minimal functional skeleton, exterior deferred" precedent -- reuses {@code GTNLMetaBlocks.REACTOR_VESSEL_CASING}
 * as filler and GTST's own {@code Textures.ROBUST_TUNGSTENSTEEL_CASING} for rendering, no dedicated assets.
 */
public class MetaTileEntityHeatExchanger extends MultiblockWithDisplayBase implements ProgressBarMultiblock {

    private static final double TICK_SECONDS = 1.0 / 20.0;
    /** See {@code MetaTileEntityPressurizedWaterReactor#AMBIENT_TEMPERATURE_KELVIN}'s own JavaDoc. */
    private static final double AMBIENT_TEMPERATURE_KELVIN = 300.0;
    /** See {@link #coolPrimaryAndPumpBack()}'s own JavaDoc for why this guards against floating-point residue. */
    private static final double MIN_MEANINGFUL_PRIMARY_MASS_KG = 1.0e-6;

    private static final int UPDATE_THERMAL_STATE = 210;
    private static final int THERMAL_SYNC_INTERVAL_TICKS = 20;
    private int thermalSyncTicks = 0;

    /**
     * Experimental (2026-09-25, balance-tuning tooling): once-per-second CSV telemetry, opened lazily the first
     * time {@link #updateFormedValid()} runs -- see {@link HeatExchangerTelemetryLogger}'s own JavaDoc for why
     * this has no player-facing toggle (unlike the reactor's own analogous logger).
     */
    @Nullable
    private HeatExchangerTelemetryLogger telemetryLogger;
    private int telemetryTicksUntilNextLog = 0;
    /**
     * Stage 0-3 of this mod's reactor physics rework (2026-11-21, see this mod's design memory): per-category
     * energy bookkeeping over one telemetry interval, logged by {@link #telemetryLogger} and then reset -- see
     * {@link EnergyLedger}'s own JavaDoc.
     */
    private final EnergyLedger<HeatExchangerEnergyCategory> energyLedger = new EnergyLedger<>(
            HeatExchangerEnergyCategory.class);
    private double telemetryElapsedSeconds = 0.0;
    private static final int TELEMETRY_INTERVAL_TICKS = 20;

    /**
     * Heat exchanger's own primary-side lumped state -- fed by {@code REACTOR_COOLANT_IMPORT}, see
     * {@link #receivePrimaryCoolant()}.
     */
    private double primarySideTemperatureKelvin = 300.0;
    private double primarySideMassKg = 0.0;
    /** Secondary loop's own lumped state -- see {@link #updateSecondarySide(double)}. */
    private double secondaryWaterTemperatureKelvin = 300.0;
    private double secondaryWaterMassKg = 0.0;

    /** Whether this tick actually removed any heat from the primary side -- see {@link #isActive()}. */
    private boolean active = false;
    /**
     * What {@link #coolPrimaryAndPumpBack()} actually removed last tick (W) -- drives
     * {@link #configureDisplayText}'s real-time heat-transfer line, same "frozen last-tick value, not re-derived
     * client-side" convention as the turbines' own {@code lastDeliveredEUt}.
     */
    private double lastQRemovedWatts = 0.0;
    /** What {@link #updateSecondarySide(double)} actually exported to steam last tick (mB) -- same convention. */
    private int lastSteamOutputMb = 0;

    /** Read by this class's own display. */
    public double getPrimarySideTemperatureKelvin() {
        return primarySideTemperatureKelvin;
    }

    /** Read by {@link HeatExchangerTelemetryLogger} -- see that class's own JavaDoc. */
    public double getPrimarySideMassKg() {
        return primarySideMassKg;
    }

    /** Read by this class's own display. */
    public double getSecondaryWaterTemperatureKelvin() {
        return secondaryWaterTemperatureKelvin;
    }

    /** Read by {@link HeatExchangerTelemetryLogger} -- see that class's own JavaDoc. */
    public double getSecondaryWaterMassKg() {
        return secondaryWaterMassKg;
    }

    public MetaTileEntityHeatExchanger(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId);
    }

    @Override
    public MetaTileEntity createMetaTileEntity(IGregTechTileEntity tileEntity) {
        return new MetaTileEntityHeatExchanger(metaTileEntityId);
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
    public boolean shouldShowVoidingModeButton() {
        return false;
    }

    // spotless:off
    @NotNull
    @Override
    protected BlockPattern createStructurePattern() {
        return FactoryBlockPattern.start(RelativeDirection.RIGHT, RelativeDirection.DOWN, RelativeDirection.BACK)
                .aisle("CCC", "ECV", "CCC")
                .aisle("CCC", "ICF", "CCC")
                .aisle("CCC", "CSC", "CMC")
                .where('S', selfPredicate())
                .where('M', maintenancePredicate())
                .where('I', abilities(GTNLMultiblockAbility.REACTOR_COOLANT_IMPORT))
                .where('E', abilities(GTNLMultiblockAbility.REACTOR_COOLANT_EXPORT))
                .where('F', abilities(GTNLMultiblockAbility.FEEDWATER_IMPORT))
                .where('V', abilities(GTNLMultiblockAbility.HIGH_PRESSURE_STEAM_EXPORT))
                .where('C', states(GTNLMetaBlocks.REACTOR_VESSEL_CASING
                        .getState(ReactorVesselCasing.CasingType.PWR_REACTOR_VESSEL)))
                .build();
    }
    // spotless:on

    @SideOnly(Side.CLIENT)
    @Override
    public ICubeRenderer getBaseTexture(IMultiblockPart sourcePart) {
        return Textures.ROBUST_TUNGSTENSTEEL_CASING;
    }

    /**
     * Receives hot primary coolant, exchanges heat with the secondary side (Stage 3), and pumps cooled primary
     * coolant back out -- see this class's own JavaDoc for why this reactor-independent multiblock can do all of
     * this from only its own hatches' contents, with no reactor reference at all.
     */
    @Override
    protected void updateFormedValid() {
        double storedEnergyBeforeJoules = computeStoredEnergyJoules();
        receivePrimaryCoolant();
        double qRemovedWatts = coolPrimaryAndPumpBack();
        updateSecondarySide(qRemovedWatts);
        energyLedger.add(HeatExchangerEnergyCategory.STORED_CHANGE,
                computeStoredEnergyJoules() - storedEnergyBeforeJoules);
        energyLedger.endTick();
        syncThermalStatePeriodically();
        logTelemetryPeriodically(qRemovedWatts);
        lastQRemovedWatts = qRemovedWatts;
        active = qRemovedWatts > 0;
    }

    /** Whether this heat exchanger actually removed heat from the primary side last tick. */
    @Override
    public boolean isActive() {
        return active;
    }

    /** Otherwise a stale {@link #lastQRemovedWatts}/{@link #lastSteamOutputMb}/{@link #active} would persist. */
    @Override
    public void invalidateStructure() {
        super.invalidateStructure();
        active = false;
        lastQRemovedWatts = 0.0;
        lastSteamOutputMb = 0;
    }

    /**
     * No GT precedent to mirror directly (this multiblock has no recipe logic at all, see this class's own
     * JavaDoc) -- lines follow the same "working status + real-time custom data" shape as
     * {@code MetaTileEntityLargeBoiler#configureDisplayText} (GTST's own closest analogue: a plain-tick-loop,
     * no-StateMachine thermal multiblock), reporting both loop's own temperature/mass and this tick's actual
     * heat-transfer/steam-output rates.
     */
    @Override
    protected void configureDisplayText(MultiblockUIBuilder builder) {
        builder.setWorkingStatus(true, isActive())
                .addCustom((keyList, syncer) -> {
                    double primaryTemperature = syncer.syncDouble(primarySideTemperatureKelvin);
                    double primaryMass = syncer.syncDouble(primarySideMassKg);
                    keyList.add(KeyUtil.lang(TextFormatting.GRAY, "gtnuclear.multiblock.heat_exchanger.primary_coolant",
                            TextFormattingUtil.formatNumbers(primaryTemperature),
                            TextFormattingUtil.formatNumbers(primaryMass)));
                })
                .addCustom((keyList, syncer) -> {
                    double secondaryTemperature = syncer.syncDouble(secondaryWaterTemperatureKelvin);
                    double secondaryMass = syncer.syncDouble(secondaryWaterMassKg);
                    keyList.add(KeyUtil.lang(TextFormatting.GRAY,
                            "gtnuclear.multiblock.heat_exchanger.secondary_water",
                            TextFormattingUtil.formatNumbers(secondaryTemperature),
                            TextFormattingUtil.formatNumbers(secondaryMass)));
                })
                .addCustom((keyList, syncer) -> {
                    double heatTransferMw = syncer.syncDouble(lastQRemovedWatts) / 1.0e6;
                    keyList.add(KeyUtil.lang(TextFormatting.GRAY, "gtnuclear.multiblock.heat_exchanger.heat_transfer",
                            TextFormattingUtil.formatNumbers(heatTransferMw)));
                })
                .addCustom((keyList, syncer) -> {
                    int steamOutput = syncer.syncInt(lastSteamOutputMb);
                    keyList.add(KeyUtil.lang(TextFormatting.GRAY, "gtnuclear.multiblock.heat_exchanger.steam_output",
                            TextFormattingUtil.formatNumbers(steamOutput)));
                })
                .addWorkingStatusLine();
    }

    @Override
    public int getProgressBarCount() {
        return 3;
    }

    @Override
    public void registerBars(List<UnaryOperator<TemplateBarBuilder>> bars, PanelSyncManager syncManager) {
        IntSyncValue primaryFillValue = new IntSyncValue(this::getPrimaryFillPercent);
        syncManager.syncValue("primary_fill", primaryFillValue);

        IntSyncValue secondaryFillValue = new IntSyncValue(this::getSecondaryFillPercent);
        syncManager.syncValue("secondary_fill", secondaryFillValue);

        IntSyncValue steamFillValue = new IntSyncValue(this::getSteamExportFillPercent);
        syncManager.syncValue("steam_fill", steamFillValue);

        bars.add(bar -> bar
                .progress(() -> primaryFillValue.getIntValue() / 100.0)
                .texture(GTGuiTextures.PROGRESS_BAR_LCE_FUEL));

        bars.add(bar -> bar
                .progress(() -> secondaryFillValue.getIntValue() / 100.0)
                .texture(GTGuiTextures.PROGRESS_BAR_LCE_LUBRICANT));

        bars.add(bar -> bar
                .progress(() -> steamFillValue.getIntValue() / 100.0)
                .texture(GTGuiTextures.PROGRESS_BAR_LCE_OXYGEN));
    }

    /** @return {@link #primarySideMassKg}'s fill ratio against its own rated cap, as a 0-100 percent. */
    private int getPrimaryFillPercent() {
        double ratedMassKg = ConfigHolder.reactor.heatExchangerPrimarySideRatedMassKg;
        return ratedMassKg > 0 ? (int) Math.round(100.0 * Math.min(1.0, primarySideMassKg / ratedMassKg)) : 0;
    }

    /** @return {@link #secondaryWaterMassKg}'s fill ratio against its own rated cap, as a 0-100 percent. */
    private int getSecondaryFillPercent() {
        double ratedMassKg = ConfigHolder.reactor.secondaryWaterRatedMassKg;
        return ratedMassKg > 0 ? (int) Math.round(100.0 * Math.min(1.0, secondaryWaterMassKg / ratedMassKg)) : 0;
    }

    /** @return the HP steam export tank's current fill ratio as a 0-100 percent, or 0 with no tank capacity. */
    private int getSteamExportFillPercent() {
        int storedMb = 0;
        int capacityMb = 0;
        for (IFluidTank tank : getAbilities(GTNLMultiblockAbility.HIGH_PRESSURE_STEAM_EXPORT)) {
            FluidStack stack = tank.getFluid();
            storedMb += stack != null ? stack.amount : 0;
            capacityMb += tank.getCapacity();
        }
        return capacityMb > 0 ? (int) Math.round(100.0 * storedMb / capacityMb) : 0;
    }

    /** See {@link #telemetryLogger}'s own JavaDoc. */
    private void logTelemetryPeriodically(double qRemovedWatts) {
        if (telemetryLogger == null) {
            telemetryLogger = HeatExchangerTelemetryLogger.open(getPos());
        }
        if (telemetryTicksUntilNextLog-- > 0) return;
        telemetryTicksUntilNextLog = TELEMETRY_INTERVAL_TICKS;
        telemetryElapsedSeconds += TELEMETRY_INTERVAL_TICKS * TICK_SECONDS;
        telemetryLogger.logRow(telemetryElapsedSeconds, this, qRemovedWatts);
        energyLedger.reset();
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

    /**
     * Drains {@code REACTOR_COOLANT_IMPORT}, mixing whatever arrives into {@link #primarySideMassKg}/
     * {@link #primarySideTemperatureKelvin} by mass-weighted temperature average. Temperature is read via
     * {@link Fluid#getTemperature()} (a discrete, per-type property -- see
     * {@code ReactorCoolantTemperatureBands}'s own JavaDoc for why the primary loop's fluid transport is
     * quantized into several discrete-temperature fluids rather than a single fluid carrying a continuous NBT
     * tag), not NBT. Capped at {@code ConfigHolder.reactor.heatExchangerPrimarySideRatedMassKg} -- see that
     * field's own JavaDoc for why (mirrors the reactor's own analogous cap in
     * {@code MetaTileEntityPressurizedWaterReactor#updatePrimaryCirculation}).
     */
    private void receivePrimaryCoolant() {
        int budgetMb = (int) Math.round(ConfigHolder.reactor.primaryCirculationRateKgPerSecond * TICK_SECONDS *
                1000.0);
        double headroomKg = Math.max(0.0, ConfigHolder.reactor.heatExchangerPrimarySideRatedMassKg - primarySideMassKg);
        budgetMb = Math.min(budgetMb, (int) Math.round(headroomKg * 1000.0));
        if (budgetMb <= 0) return;

        int drainedMb = 0;
        for (IFluidTank tank : getAbilities(GTNLMultiblockAbility.REACTOR_COOLANT_IMPORT)) {
            if (drainedMb >= budgetMb) break;
            FluidStack drained = tank.drain(budgetMb - drainedMb, true);
            if (drained == null || drained.amount <= 0) continue;
            double drainedTemperatureKelvin = drained.getFluid().getTemperature();
            double drainedMassKg = drained.amount / 1000.0;
            energyLedger.add(HeatExchangerEnergyCategory.PRIMARY_IMPORT,
                    liquidEnthalpyJoules(drainedMassKg, drainedTemperatureKelvin));
            primarySideTemperatureKelvin = CoolantThermalSolver.mixTemperatures(primarySideMassKg,
                    primarySideTemperatureKelvin, drainedMassKg, drainedTemperatureKelvin);
            primarySideMassKg += drainedMassKg;
            drainedMb += drained.amount;
        }
        if (drainedMb > 0) markDirty();
    }

    /**
     * Cools {@link #primarySideTemperatureKelvin} against the secondary side's own current temperature via
     * {@code ConfigHolder.reactor.heatExchangerUAWattsPerKelvin}, then -- only while there is actually heat to
     * send back ({@link #primarySideTemperatureKelvin} above ambient, mirroring
     * {@code MetaTileEntityPressurizedWaterReactor#updatePrimaryCirculation}'s own gating and its rationale --
     * pumping cold coolant back out serves no purpose and only risks the same "coolant vanishes" playtest report
     * this gate fixes there) pumps the cooled primary coolant back out via {@code REACTOR_COOLANT_EXPORT}, using
     * {@link GTNLMaterials#nearestReactorCoolantBandFluid} to pick the outgoing fluid.
     * <p>
     * While {@link #secondaryWaterTemperatureKelvin} is still sub-cooled, the UA-based heat removal above is
     * additionally capped by {@code ConfigHolder.reactor.heatExchangerMaxSecondaryHeatupRateKelvinPerSecond} --
     * see that field's own JavaDoc for why {@code heatExchangerUAWattsPerKelvin} alone (sized for full rated
     * fission power) otherwise pins primary coolant just a few Kelvin above the secondary side indefinitely
     * during pre-criticality heatup, when only weak pump friction heat is available. The cap is lifted entirely
     * once the secondary side reaches saturation, so it never throttles genuine full-power steam production.
     *
     * @return heat removed from the primary side this tick (W), the secondary side's own heat input.
     */
    private double coolPrimaryAndPumpBack() {
        if (primarySideMassKg < MIN_MEANINGFUL_PRIMARY_MASS_KG) {
            // No coolant actually present -- without this guard, a floating-point residue (receivePrimaryCoolant()
            // caps import by rounding to the nearest mb; the export below can round-trip a hair past zero) would
            // still compute and return a nonzero qRemovedWatts purely from a now-stale primarySideTemperatureKelvin
            // with nothing backing it, heating the secondary side with heat that was never actually removed from
            // any real coolant. Confirmed via a real playtest, 2026-09-26: primarySideMassKg stuck at -0.00 kg for
            // over 1000 seconds (CoolantThermalSolver.advance()'s own heatCapacity<=0 guard froze
            // primarySideTemperatureKelvin, but did not stop this method from still reporting ~21 MW removed every
            // tick) while the reactor's own coolant was still below AMBIENT_TEMPERATURE_KELVIN and sending nothing.
            primarySideMassKg = 0.0;
            return 0.0;
        }
        double qRemovedWatts = ConfigHolder.reactor.heatExchangerUAWattsPerKelvin *
                Math.max(0.0, primarySideTemperatureKelvin - secondaryWaterTemperatureKelvin);
        double saturationTemperatureKelvin = WaterSaturationCurve.saturationTemperature(
                ConfigHolder.reactor.secondaryPressureMPa * 1.0e6);
        if (secondaryWaterTemperatureKelvin < saturationTemperatureKelvin) {
            double maxSubcooledAbsorptionWatts = secondaryWaterMassKg *
                    CoolantThermalSolver.REPRESENTATIVE_SPECIFIC_HEAT_CAPACITY *
                    ConfigHolder.reactor.heatExchangerMaxSecondaryHeatupRateKelvinPerSecond;
            qRemovedWatts = Math.min(qRemovedWatts, maxSubcooledAbsorptionWatts);
        }
        primarySideTemperatureKelvin = CoolantThermalSolver.advance(primarySideTemperatureKelvin, -qRemovedWatts,
                primarySideMassKg, CoolantThermalSolver.REPRESENTATIVE_SPECIFIC_HEAT_CAPACITY, TICK_SECONDS);
        energyLedger.add(HeatExchangerEnergyCategory.TRANSFER, qRemovedWatts * TICK_SECONDS);

        if (primarySideTemperatureKelvin > AMBIENT_TEMPERATURE_KELVIN) {
            int budgetMb = (int) Math.round(ConfigHolder.reactor.primaryCirculationRateKgPerSecond * TICK_SECONDS *
                    1000.0);
            int pumpedMb = Math.min(budgetMb, (int) Math.round(primarySideMassKg * 1000.0));
            if (pumpedMb > 0) {
                Fluid outgoingFluid = GTNLMaterials.nearestReactorCoolantBandFluid(primarySideTemperatureKelvin);
                int filledMb = 0;
                for (IFluidTank tank : getAbilities(GTNLMultiblockAbility.REACTOR_COOLANT_EXPORT)) {
                    if (filledMb >= pumpedMb) break;
                    filledMb += tank.fill(new FluidStack(outgoingFluid, pumpedMb - filledMb), true);
                }
                if (filledMb > 0) {
                    double exportedMassKg = filledMb / 1000.0;
                    energyLedger.add(HeatExchangerEnergyCategory.PRIMARY_EXPORT,
                            liquidEnthalpyJoules(exportedMassKg, primarySideTemperatureKelvin));
                    energyLedger.add(HeatExchangerEnergyCategory.BAND_ROUNDING,
                            liquidEnthalpyJoules(exportedMassKg, primarySideTemperatureKelvin) -
                                    liquidEnthalpyJoules(exportedMassKg, outgoingFluid.getTemperature()));
                    primarySideMassKg -= exportedMassKg;
                    markDirty();
                }
            }
        }
        return qRemovedWatts;
    }

    /**
     * Tops up {@link #secondaryWaterMassKg} from the feedwater import hatch, then applies {@code qInputWatts} of
     * heat (received from {@link #coolPrimaryAndPumpBack()}) -- below {@link WaterSaturationCurve}'s own
     * saturation temperature at {@code ConfigHolder.reactor.secondaryPressureMPa}, this just raises
     * {@link #secondaryWaterTemperatureKelvin}; once at saturation, excess energy instead boils water into
     * {@link GTNLMaterials#HighPressureSteam}, capped by the export hatch's own headroom (checked first, mirroring
     * {@code MetaTileEntityPressurizedWaterReactor#updateBoronAdjustment}'s "check output headroom before
     * committing" pattern) so backpressure simply pauses boiling rather than destroying mass. The third instance
     * of this mod's phase-change-plateau pattern (fuel melting, primary voiding, and this).
     * <p>
     * Steam output uses the same "1000 mB = 1 kg" convention as every other fluid in this mod (not a
     * pressure-dependent real steam density), for consistency with how {@link #secondaryWaterMassKg}/primary
     * coolant/boron solution etc. are all already tracked.
     */
    private void updateSecondarySide(double qInputWatts) {
        replenishFeedwater();

        double saturationTemperatureKelvin = WaterSaturationCurve.saturationTemperature(
                ConfigHolder.reactor.secondaryPressureMPa * 1.0e6);
        double candidateKelvin = CoolantThermalSolver.advance(secondaryWaterTemperatureKelvin, qInputWatts,
                secondaryWaterMassKg, CoolantThermalSolver.REPRESENTATIVE_SPECIFIC_HEAT_CAPACITY, TICK_SECONDS);
        if (candidateKelvin < saturationTemperatureKelvin || secondaryWaterMassKg <= 0) {
            secondaryWaterTemperatureKelvin = candidateKelvin;
            lastSteamOutputMb = 0;
            return;
        }

        double excessJoules = (candidateKelvin - saturationTemperatureKelvin) * secondaryWaterMassKg *
                CoolantThermalSolver.REPRESENTATIVE_SPECIFIC_HEAT_CAPACITY;
        double desiredEvaporationKg = Math.min(secondaryWaterMassKg,
                excessJoules / SteamProperties.LATENT_HEAT_OF_VAPORIZATION_J_PER_KG);
        int desiredMb = (int) Math.round(desiredEvaporationKg * 1000.0);
        int actualMb = Math.min(desiredMb, computeSteamExportHeadroomMb());

        secondaryWaterTemperatureKelvin = saturationTemperatureKelvin;
        if (actualMb <= 0) {
            lastSteamOutputMb = 0;
            return;
        }

        int filledMb = 0;
        for (IFluidTank tank : getAbilities(GTNLMultiblockAbility.HIGH_PRESSURE_STEAM_EXPORT)) {
            if (filledMb >= actualMb) break;
            filledMb += tank.fill(new FluidStack(GTNLMaterials.HighPressureSteam.getFluid(), actualMb - filledMb),
                    true);
        }
        lastSteamOutputMb = filledMb;
        if (filledMb > 0) {
            double steamMassKg = filledMb / 1000.0;
            energyLedger.add(HeatExchangerEnergyCategory.STEAM, liquidEnthalpyJoules(steamMassKg,
                    saturationTemperatureKelvin) + steamMassKg * SteamProperties.LATENT_HEAT_OF_VAPORIZATION_J_PER_KG);
            secondaryWaterMassKg -= steamMassKg;
            markDirty();
        }
    }

    private int computeSteamExportHeadroomMb() {
        int headroomMb = 0;
        for (IFluidTank tank : getAbilities(GTNLMultiblockAbility.HIGH_PRESSURE_STEAM_EXPORT)) {
            FluidStack existing = tank.getFluid();
            headroomMb += tank.getCapacity() - (existing != null ? existing.amount : 0);
        }
        return headroomMb;
    }

    /**
     * Drains {@code FEEDWATER_IMPORT}, mixing incoming (ambient-temperature) feedwater into
     * {@link #secondaryWaterMassKg}/{@link #secondaryWaterTemperatureKelvin} by mass-weighted average -- a
     * level-control abstraction, always topping up rather than requiring an operator command. The intake budget
     * is the larger of (a) the deficit below {@code ConfigHolder.reactor.secondaryWaterRatedMassKg} and (b)
     * {@code ConfigHolder.reactor.feedwaterTrickleRateKgPerSecond} -- see that field's own JavaDoc for why a
     * nonzero floor is needed even at/above the rated mass (a real playtest, 2026-09-26, found that without it,
     * a secondary side that had ever gotten hotter than the primary loop's current temperature could never cool
     * back down once at its mass cap). Any mass drained beyond the rated cap is discarded immediately after
     * mixing (a blowdown abstraction) -- since the tank is treated as a single well-mixed body, discarding a
     * slice of it after mixing does not change the resulting temperature.
     */
    private void replenishFeedwater() {
        double ratedMassKg = ConfigHolder.reactor.secondaryWaterRatedMassKg;
        double deficitKg = Math.max(0.0, ratedMassKg - secondaryWaterMassKg);
        double trickleKg = ConfigHolder.reactor.feedwaterTrickleRateKgPerSecond * TICK_SECONDS;
        int budgetMb = (int) Math.round(Math.max(deficitKg, trickleKg) * 1000.0);
        if (budgetMb <= 0) return;

        int drainedMb = 0;
        for (IFluidTank tank : getAbilities(GTNLMultiblockAbility.FEEDWATER_IMPORT)) {
            if (drainedMb >= budgetMb) break;
            FluidStack drained = tank.drain(budgetMb - drainedMb, true);
            if (drained != null) drainedMb += drained.amount;
        }
        if (drainedMb <= 0) return;

        double incomingMassKg = drainedMb / 1000.0;
        energyLedger.add(HeatExchangerEnergyCategory.FEEDWATER,
                liquidEnthalpyJoules(incomingMassKg, FEEDWATER_TEMPERATURE_KELVIN));
        secondaryWaterTemperatureKelvin = CoolantThermalSolver.mixTemperatures(secondaryWaterMassKg,
                secondaryWaterTemperatureKelvin, incomingMassKg, FEEDWATER_TEMPERATURE_KELVIN);
        double mixedMassKg = secondaryWaterMassKg + incomingMassKg;
        double blowdownKg = Math.max(0.0, mixedMassKg - ratedMassKg);
        energyLedger.add(HeatExchangerEnergyCategory.BLOWDOWN,
                liquidEnthalpyJoules(blowdownKg, secondaryWaterTemperatureKelvin));
        secondaryWaterMassKg = Math.min(ratedMassKg, mixedMassKg);
        markDirty();
    }

    /** The temperature this class has always assumed for incoming feedwater (previously an inline literal). */
    private static final double FEEDWATER_TEMPERATURE_KELVIN = 300.0;

    /**
     * Primary-side + secondary-side internal energy (J) relative to {@link #AMBIENT_TEMPERATURE_KELVIN} -- read
     * only by {@link #energyLedger}'s per-tick {@link HeatExchangerEnergyCategory#STORED_CHANGE}.
     */
    private double computeStoredEnergyJoules() {
        return liquidEnthalpyJoules(primarySideMassKg, primarySideTemperatureKelvin) +
                liquidEnthalpyJoules(secondaryWaterMassKg, secondaryWaterTemperatureKelvin);
    }

    /** {@code m * c_p * (T - 300 K)} -- same convention as the reactor's own ledger. */
    private static double liquidEnthalpyJoules(double massKg, double temperatureKelvin) {
        return massKg * CoolantThermalSolver.REPRESENTATIVE_SPECIFIC_HEAT_CAPACITY *
                (temperatureKelvin - AMBIENT_TEMPERATURE_KELVIN);
    }

    /** Read by {@link HeatExchangerTelemetryLogger} -- see {@link #energyLedger}'s own JavaDoc. */
    @NotNull
    public EnergyLedger<HeatExchangerEnergyCategory> getEnergyLedger() {
        return energyLedger;
    }

    /** Same "periodic timer, not change-triggered" sync pattern as the reactor's own display fields. */
    private void syncThermalStatePeriodically() {
        if (thermalSyncTicks-- > 0) return;
        thermalSyncTicks = THERMAL_SYNC_INTERVAL_TICKS;
        writeCustomData(UPDATE_THERMAL_STATE, this::writeThermalState);
    }

    private void writeThermalState(@NotNull PacketBuffer buf) {
        buf.writeDouble(primarySideTemperatureKelvin);
        buf.writeDouble(primarySideMassKg);
        buf.writeDouble(secondaryWaterTemperatureKelvin);
        buf.writeDouble(secondaryWaterMassKg);
    }

    private void readThermalState(@NotNull PacketBuffer buf) {
        primarySideTemperatureKelvin = buf.readDouble();
        primarySideMassKg = buf.readDouble();
        secondaryWaterTemperatureKelvin = buf.readDouble();
        secondaryWaterMassKg = buf.readDouble();
    }

    @Override
    public void receiveCustomData(int dataId, @NotNull PacketBuffer buf) {
        super.receiveCustomData(dataId, buf);
        if (dataId == UPDATE_THERMAL_STATE) {
            readThermalState(buf);
        }
    }

    @Override
    public void writeInitialSyncData(@NotNull PacketBuffer buf) {
        super.writeInitialSyncData(buf);
        writeThermalState(buf);
    }

    @Override
    public void receiveInitialSyncData(@NotNull PacketBuffer buf) {
        super.receiveInitialSyncData(buf);
        readThermalState(buf);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        data.setDouble("PrimarySideTemperatureKelvin", primarySideTemperatureKelvin);
        data.setDouble("PrimarySideMassKg", primarySideMassKg);
        data.setDouble("SecondaryWaterTemperatureKelvin", secondaryWaterTemperatureKelvin);
        data.setDouble("SecondaryWaterMassKg", secondaryWaterMassKg);
        return data;
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        primarySideTemperatureKelvin = data.hasKey("PrimarySideTemperatureKelvin") ?
                data.getDouble("PrimarySideTemperatureKelvin") : 300.0;
        primarySideMassKg = data.getDouble("PrimarySideMassKg");
        secondaryWaterTemperatureKelvin = data.hasKey("SecondaryWaterTemperatureKelvin") ?
                data.getDouble("SecondaryWaterTemperatureKelvin") : 300.0;
        secondaryWaterMassKg = data.getDouble("SecondaryWaterMassKg");
    }
}
