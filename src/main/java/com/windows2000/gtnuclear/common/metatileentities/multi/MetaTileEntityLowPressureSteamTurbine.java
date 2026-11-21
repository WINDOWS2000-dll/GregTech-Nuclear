package com.windows2000.gtnuclear.common.metatileentities.multi;

import gregtech.api.GTValues;
import gregtech.api.capability.IEnergyContainer;
import gregtech.api.capability.impl.EnergyContainerList;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.interfaces.IGregTechTileEntity;
import gregtech.api.metatileentity.multiblock.IMultiblockPart;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.metatileentity.multiblock.MultiblockWithDisplayBase;
import gregtech.api.metatileentity.multiblock.ProgressBarMultiblock;
import gregtech.api.metatileentity.multiblock.ui.MultiblockUIBuilder;
import gregtech.api.metatileentity.multiblock.ui.TemplateBarBuilder;
import gregtech.api.mui.GTGuiTextures;
import gregtech.api.mui.sync.FixedIntArraySyncValue;
import gregtech.api.pattern.BlockPattern;
import gregtech.api.pattern.FactoryBlockPattern;
import gregtech.api.unification.material.Materials;
import gregtech.api.util.GTUtility;
import gregtech.api.util.KeyUtil;
import gregtech.api.util.TextFormattingUtil;
import gregtech.client.renderer.ICubeRenderer;
import gregtech.client.renderer.texture.Textures;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Matrix4;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.screen.RichTooltip;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.StringSyncValue;
import com.windows2000.gtnuclear.api.metatileentity.multiblock.GTNLMultiblockAbility;
import com.windows2000.gtnuclear.api.nuclear.power.SteamTurbineOutput;
import com.windows2000.gtnuclear.api.unification.materials.GTNLMaterials;
import com.windows2000.gtnuclear.client.renderer.texture.GTNLTextures;
import com.windows2000.gtnuclear.common.ConfigHolder;
import com.windows2000.gtnuclear.common.blocks.GTNLMetaBlocks;
import com.windows2000.gtnuclear.common.blocks.generator.TurbineCasing;
import com.windows2000.gtnuclear.common.metatileentities.multiblockpart.MetaTileEntityTurbineRotorHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.UnaryOperator;

import static gregtech.api.unification.material.Materials.Steel;

/**
 * The secondary loop's low-pressure turbine stage: consumes {@code LowPressureSteam} (the high-pressure stage's
 * own exhaust, see {@link MetaTileEntityHighPressureSteamTurbine}'s own JavaDoc), generates its own share of EU
 * via {@link SteamTurbineOutput}, and exhausts final condensate (plain water, piped back toward feedwater by
 * hand -- no logical link, same convention as every other inter-multiblock connection in this mod).
 * <p>
 * <b>This shape is a placeholder</b>, same precedent as {@code MetaTileEntityHeatExchanger}/
 * {@code MetaTileEntityHighPressureSteamTurbine}.
 */
public class MetaTileEntityLowPressureSteamTurbine extends MultiblockWithDisplayBase
                                                   implements ProgressBarMultiblock {

    private static final double TICK_SECONDS = 1.0 / 20.0;
    /** Mirrors GT's own {@code MetaTileEntityLargeTurbine#MIN_DURABILITY_TO_WARN}. */
    private static final int MIN_DURABILITY_TO_WARN = 10;

    /**
     * Whether this turbine actually generated power this tick -- see {@link #isActive()}/{@link #updateFormedValid()}.
     */
    private boolean active = false;
    /**
     * What {@link #produceThisTick} actually delivered last tick -- drives {@link #configureDisplayText}'s
     * real-time production line, mirroring GT's own {@code MetaTileEntityLargeTurbine#lastDeliveredEUt}.
     */
    private long lastDeliveredEUt = 0;

    public MetaTileEntityLowPressureSteamTurbine(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId);
    }

    @Override
    public MetaTileEntity createMetaTileEntity(IGregTechTileEntity tileEntity) {
        return new MetaTileEntityLowPressureSteamTurbine(metaTileEntityId);
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
        return FactoryBlockPattern.start()
                .aisle("CFPP P C", "CFVMFLFC", "CFPP P C")
                .aisle("CCCCFCFC", "OGGGCGCR", "CCCCFCFC")
                .aisle("CFPP P C", "CFCSFIFC", "CFPP P C")
                .where('S', selfPredicate())
                .where('M', maintenancePredicate())
                .where('L', abilities(MultiblockAbility.IMPORT_FLUIDS))
                .where('I', abilities(GTNLMultiblockAbility.LOW_PRESSURE_STEAM_IMPORT))
                .where('V', abilities(GTNLMultiblockAbility.CONDENSATE_EXPORT))
                .where('R', abilities(GTNLMultiblockAbility.TURBINE_ROTOR_SLOT))
                .where('O', abilities(MultiblockAbility.OUTPUT_ENERGY))
                .where('C', states(GTNLMetaBlocks.TURBINE_CASING
                        .getState(TurbineCasing.CasingType.TURBINE_LP_CASING)))
                .where('P', states(GTNLMetaBlocks.TURBINE_CASING
                        .getState(TurbineCasing.CasingType.TURBINE_LP_PIPE_CASING)))
                .where('G', states(GTNLMetaBlocks.TURBINE_CASING
                        .getState(TurbineCasing.CasingType.TURBINE_LP_GEARBOX_CASING)))
                .where('F', frames(Steel))
                .where(" ", air())
                .build();
    }
    // spotless:on

    @SideOnly(Side.CLIENT)
    @Override
    public ICubeRenderer getBaseTexture(IMultiblockPart sourcePart) {
        return Textures.SOLID_STEEL_CASING;
    }

    @SideOnly(Side.CLIENT)
    @NotNull
    @Override
    protected ICubeRenderer getFrontOverlay() {
        return GTNLTextures.LP_TURBINE_OVERLAY;
    }

    @Override
    public void renderMetaTileEntity(CCRenderState renderState, Matrix4 translation, IVertexOperation[] pipeline) {
        super.renderMetaTileEntity(renderState, translation, pipeline);

        this.getFrontOverlay().renderOrientedState(renderState, translation, pipeline, getFrontFacing(),
                this.isActive(), true);
    }

    /** Whether this turbine actually generated power last tick -- see {@link #updateFormedValid()}. */
    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    protected void updateFormedValid() {
        MetaTileEntityTurbineRotorHolder rotorHolder = findRotorHolder();
        active = produceThisTick(rotorHolder);
        if (rotorHolder != null) rotorHolder.setActiveThisTick(active);
    }

    /** Otherwise a stale {@link #lastDeliveredEUt}/{@link #active} would survive indefinitely while unformed. */
    @Override
    public void invalidateStructure() {
        super.invalidateStructure();
        active = false;
        lastDeliveredEUt = 0;
    }

    /** @return whether this tick actually generated power -- see {@link #setActiveThisTick}'s own caller. */
    private boolean produceThisTick(MetaTileEntityTurbineRotorHolder rotorHolder) {
        double rotorPowerPercent = rotorHolder != null ? rotorHolder.getRotorPowerPercent() : 0.0;
        double rotorEfficiencyPercent = rotorHolder != null ? rotorHolder.getRotorEfficiencyPercent() : 0.0;
        if (rotorPowerPercent <= 0.0) {
            lastDeliveredEUt = 0;
            return false;
        }

        double maxThroughputMbPerTick = SteamTurbineOutput.maxThroughputMbPerTick(
                ConfigHolder.reactor.lowPressureTurbineBaseThroughputMbPerTick, rotorPowerPercent);
        int budgetMb = Math.min((int) Math.round(maxThroughputMbPerTick), computeCondensateExportHeadroomMb());
        if (budgetMb <= 0) {
            lastDeliveredEUt = 0;
            return false;
        }

        int consumedMb = drainLowPressureSteam(budgetMb);
        if (consumedMb <= 0) {
            lastDeliveredEUt = 0;
            return false;
        }

        double stageEfficiencyFraction = computeStageEfficiencyFraction();
        double electricalOutputWatts = SteamTurbineOutput.electricalOutputWatts(consumedMb, stageEfficiencyFraction,
                rotorEfficiencyPercent, TICK_SECONDS);
        long euPerTick = Math.round(SteamTurbineOutput.euPerTick(electricalOutputWatts,
                ConfigHolder.reactor.euPerTickToWattsConversionGeneration));
        if (euPerTick > 0) {
            IEnergyContainer energyContainer = new EnergyContainerList(getAbilities(MultiblockAbility.OUTPUT_ENERGY));
            energyContainer.addEnergy(euPerTick);
        }
        lastDeliveredEUt = euPerTick;

        exportCondensate(consumedMb);
        if (rotorHolder != null && getOffsetTimer() % 20 == 0) {
            // Matches GT's own MetaTileEntityRotorHolder#update() exactly: a flat 1 durability point (plus one per
            // active maintenance problem) every 20 ticks while generating, not tied to mass flow -- see
            // IMaintenance#getNumMaintenanceProblems, already inherited via MultiblockWithDisplayBase.
            rotorHolder.damageRotor(1 + getNumMaintenanceProblems());
        }
        markDirty();
        return euPerTick > 0;
    }

    /** Shared by {@link #produceThisTick}'s real throughput and {@link #computeTargetEuPerTick}'s theoretical max. */
    private double computeStageEfficiencyFraction() {
        return SteamTurbineOutput.stageEfficiencyFraction(
                ConfigHolder.reactor.turbineThermalEfficiency,
                GTNLMaterials.LowPressureSteam.getFluid().getTemperature(),
                ConfigHolder.reactor.turbineCondenserTemperatureKelvin,
                GTNLMaterials.HighPressureSteam.getFluid().getTemperature(),
                ConfigHolder.reactor.turbineCondenserTemperatureKelvin);
    }

    /**
     * @return the EU/t this turbine's currently loaded rotor could deliver given unlimited steam -- analogous to
     *         GT's own {@code MetaTileEntityLargeTurbine#getTargetVoltage()}, used both for the "Max EU/t" line and
     *         to clamp the real-time "Output EU/t" line in {@link #configureDisplayText}.
     */
    private long computeTargetEuPerTick() {
        MetaTileEntityTurbineRotorHolder rotorHolder = findRotorHolder();
        if (rotorHolder == null) return 0;
        double rotorPowerPercent = rotorHolder.getRotorPowerPercent();
        double rotorEfficiencyPercent = rotorHolder.getRotorEfficiencyPercent();
        if (rotorPowerPercent <= 0.0) return 0;

        double maxThroughputMbPerTick = SteamTurbineOutput.maxThroughputMbPerTick(
                ConfigHolder.reactor.lowPressureTurbineBaseThroughputMbPerTick, rotorPowerPercent);
        double electricalOutputWatts = SteamTurbineOutput.electricalOutputWatts(maxThroughputMbPerTick,
                computeStageEfficiencyFraction(), rotorEfficiencyPercent, TICK_SECONDS);
        return Math.round(SteamTurbineOutput.euPerTick(electricalOutputWatts,
                ConfigHolder.reactor.euPerTickToWattsConversionGeneration));
    }

    @Override
    protected void configureDisplayText(MultiblockUIBuilder builder) {
        long target = computeTargetEuPerTick();
        builder.setWorkingStatus(true, isActive())
                .addEnergyProductionLine(target, target)
                .addCustom((keyList, syncer) -> {
                    long current = syncer.syncLong(Math.min(lastDeliveredEUt, target));
                    String energyFormatted = TextFormattingUtil.formatNumbers(current);
                    var voltageName = KeyUtil.string(GTValues.VOCNF[GTUtility.getFloorTierByVoltage(current)]);
                    keyList.add(KeyUtil.lang(TextFormatting.GRAY,
                            "gregtech.multiblock.turbine.energy_per_tick", energyFormatted, voltageName));
                })
                .addWorkingStatusLine();
    }

    @Override
    public int getProgressBarCount() {
        return 3;
    }

    @Override
    public void registerBars(List<UnaryOperator<TemplateBarBuilder>> bars, PanelSyncManager syncManager) {
        FixedIntArraySyncValue fuelValue = new FixedIntArraySyncValue(this::getFuelAmount, null);
        StringSyncValue fuelNameValue = new StringSyncValue(() -> {
            FluidStack stack = firstNonEmptyFuelTank();
            if (stack == null) return null;
            Fluid fluid = stack.getFluid();
            return fluid == null ? null : fluid.getName();
        });
        syncManager.syncValue("fuel_amount", fuelValue);
        syncManager.syncValue("fuel_name", fuelNameValue);

        IntSyncValue rotorSpeedValue = new IntSyncValue(() -> {
            MetaTileEntityTurbineRotorHolder rotorHolder = findRotorHolder();
            return rotorHolder != null ? rotorHolder.getCurrentSpeed() : 0;
        });
        IntSyncValue rotorMaxSpeedValue = new IntSyncValue(MetaTileEntityTurbineRotorHolder::getMaxSpeed);

        syncManager.syncValue("rotor_speed", rotorSpeedValue);
        syncManager.syncValue("rotor_max_speed", rotorMaxSpeedValue);
        IntSyncValue durabilityValue = new IntSyncValue(() -> {
            MetaTileEntityTurbineRotorHolder rotorHolder = findRotorHolder();
            return rotorHolder != null ? rotorHolder.getRotorDurabilityPercent() : 0;
        });
        IntSyncValue efficiencyValue = new IntSyncValue(() -> {
            MetaTileEntityTurbineRotorHolder rotorHolder = findRotorHolder();
            return rotorHolder != null ? (int) Math.round(rotorHolder.getRotorEfficiencyPercent()) : 0;
        });

        syncManager.syncValue("rotor_durability", durabilityValue);
        syncManager.syncValue("rotor_efficiency", efficiencyValue);

        bars.add(barTest -> barTest
                .progress(() -> fuelValue.getValue(1) == 0 ? 0 :
                        1.0 * fuelValue.getValue(0) / fuelValue.getValue(1))
                .texture(GTGuiTextures.PROGRESS_BAR_LCE_FUEL)
                .tooltipBuilder(t -> createFuelTooltip(t, fuelValue, fuelNameValue)));

        bars.add(barTest -> barTest
                .progress(() -> rotorMaxSpeedValue.getIntValue() == 0 ? 0 :
                        1.0 * rotorSpeedValue.getIntValue() / rotorMaxSpeedValue.getIntValue())
                .texture(GTGuiTextures.PROGRESS_BAR_TURBINE_ROTOR_SPEED)
                .tooltipBuilder(t -> {
                    if (isStructureFormed()) {
                        int speed = rotorSpeedValue.getIntValue();
                        int maxSpeed = rotorMaxSpeedValue.getIntValue();

                        t.addLine(KeyUtil.lang("gregtech.multiblock.turbine.rotor_speed",
                                getSpeedFormat(maxSpeed, speed), speed, maxSpeed));
                    } else {
                        t.addLine(IKey.lang("gregtech.multiblock.invalid_structure"));
                    }
                }));

        bars.add(barTest -> barTest
                .progress(() -> durabilityValue.getIntValue() / 100.0)
                .texture(GTGuiTextures.PROGRESS_BAR_TURBINE_ROTOR_DURABILITY)
                .tooltipBuilder(t -> {
                    if (isStructureFormed()) {
                        if (efficiencyValue.getIntValue() <= 0) {
                            t.addLine(IKey.lang("gregtech.multiblock.turbine.no_rotor"));
                        } else {
                            int durability = durabilityValue.getIntValue();
                            if (durability > 40) {
                                t.addLine(IKey.lang("gregtech.multiblock.turbine.rotor_durability.high",
                                        durability));
                            } else if (durability > MIN_DURABILITY_TO_WARN) {
                                t.addLine(IKey.lang("gregtech.multiblock.turbine.rotor_durability.medium",
                                        durability));
                            } else {
                                t.addLine(IKey.lang("gregtech.multiblock.turbine.rotor_durability.low",
                                        durability));
                            }
                        }
                    } else {
                        t.addLine(IKey.lang("gregtech.multiblock.invalid_structure"));
                    }
                }));
    }

    /** Mirrors GT's own {@code MetaTileEntityLargeTurbine#getSpeedFormat}. */
    private static @NotNull TextFormatting getSpeedFormat(int maxSpeed, int speed) {
        float percent = maxSpeed == 0 ? 0 : 1.0f * speed / maxSpeed;
        if (percent < 0.4) {
            return TextFormatting.RED;
        } else if (percent < 0.8) {
            return TextFormatting.YELLOW;
        } else {
            return TextFormatting.GREEN;
        }
    }

    private void createFuelTooltip(RichTooltip tooltip, FixedIntArraySyncValue amounts,
                                   StringSyncValue fuelNameValue) {
        if (isStructureFormed()) {
            Fluid fluid = fuelNameValue.getStringValue() == null ? null :
                    FluidRegistry.getFluid(fuelNameValue.getStringValue());
            if (fluid == null) {
                tooltip.addLine(IKey.lang("gregtech.multiblock.large_combustion_engine.fuel_none"));
            } else {
                tooltip.addLine(
                        IKey.lang("gregtech.multiblock.large_combustion_engine.fuel_amount", amounts.getValue(0),
                                amounts.getValue(1), fluid.getLocalizedName(new FluidStack(fluid, 1))));
            }
        } else {
            tooltip.addLine(IKey.lang("gregtech.multiblock.invalid_structure"));
        }
    }

    /** @return whatever {@code LowPressureSteam} currently sits in the LP import tanks, or {@code null}. */
    @Nullable
    private FluidStack firstNonEmptyFuelTank() {
        for (IFluidTank tank : getAbilities(GTNLMultiblockAbility.LOW_PRESSURE_STEAM_IMPORT)) {
            FluidStack contents = tank.getFluid();
            if (contents != null && contents.amount > 0) return contents;
        }
        return null;
    }

    /** @return an array of [steam stored, steam capacity] summed across all LP import tanks. */
    private int[] getFuelAmount() {
        int storedMb = 0;
        int capacityMb = 0;
        for (IFluidTank tank : getAbilities(GTNLMultiblockAbility.LOW_PRESSURE_STEAM_IMPORT)) {
            FluidStack stack = tank.getFluid();
            storedMb += stack != null ? stack.amount : 0;
            capacityMb += tank.getCapacity();
        }
        return new int[] { storedMb, capacityMb };
    }

    private MetaTileEntityTurbineRotorHolder findRotorHolder() {
        for (IMultiblockPart part : getMultiblockParts()) {
            if (part instanceof MetaTileEntityTurbineRotorHolder rotorHolder) return rotorHolder;
        }
        return null;
    }

    private int drainLowPressureSteam(int budgetMb) {
        int drainedMb = 0;
        for (IFluidTank tank : getAbilities(GTNLMultiblockAbility.LOW_PRESSURE_STEAM_IMPORT)) {
            if (drainedMb >= budgetMb) break;
            FluidStack drained = tank.drain(budgetMb - drainedMb, true);
            if (drained != null) drainedMb += drained.amount;
        }
        return drainedMb;
    }

    private int computeCondensateExportHeadroomMb() {
        int headroomMb = 0;
        for (IFluidTank tank : getAbilities(GTNLMultiblockAbility.CONDENSATE_EXPORT)) {
            FluidStack existing = tank.getFluid();
            headroomMb += tank.getCapacity() - (existing != null ? existing.amount : 0);
        }
        return headroomMb;
    }

    /** Mass-conserved through the turbine; final exhaust is plain water, same as feedwater's own convention. */
    private void exportCondensate(int mb) {
        int filledMb = 0;
        for (IFluidTank tank : getAbilities(GTNLMultiblockAbility.CONDENSATE_EXPORT)) {
            if (filledMb >= mb) break;
            filledMb += tank.fill(new FluidStack(Materials.Water.getFluid(), mb - filledMb), true);
        }
    }
}
