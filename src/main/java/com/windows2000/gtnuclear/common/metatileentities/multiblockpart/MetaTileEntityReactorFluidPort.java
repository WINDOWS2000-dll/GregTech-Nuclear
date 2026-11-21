package com.windows2000.gtnuclear.common.metatileentities.multiblockpart;

import gregtech.api.GTValues;
import gregtech.api.capability.impl.FluidTankList;
import gregtech.api.capability.impl.NotifiableFluidTank;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.interfaces.IGregTechTileEntity;
import gregtech.api.metatileentity.multiblock.AbilityInstances;
import gregtech.api.metatileentity.multiblock.IMultiblockAbilityPart;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.mui.GTGuis;
import gregtech.client.renderer.texture.Textures;
import gregtech.client.renderer.texture.cube.SimpleOverlayRenderer;
import gregtech.common.metatileentities.multi.multiblockpart.MetaTileEntityMultiblockNotifiablePart;
import gregtech.common.mui.widget.GTFluidSlot;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;

import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Matrix4;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * A single-fluid, hard-locked import/export hatch for the PWR reactor's various dedicated ports (coolant, boric
 * acid, pure/distilled water, emergency coolant, radioactive wastewater -- see this mod's design memory for the
 * full hatch layout). One concrete class parameterized by direction/allowed fluid/ability/capacity rather than a
 * subclass per port, since all six are structurally identical.
 * <p>
 * Unlike GT's own {@code MetaTileEntityFluidHatch}, the accepted fluid is fixed at construction (not a
 * player-configurable "lock", and enforced for <i>both</i> import and export directions -- GT's own lock feature
 * only applies to export hatches) and there is no tier-based capacity scaling or ghost-circuit item handling,
 * since none of that applies to a single-purpose reactor port.
 */
public class MetaTileEntityReactorFluidPort extends MetaTileEntityMultiblockNotifiablePart
                                            implements IMultiblockAbilityPart<IFluidTank> {

    private final MultiblockAbility<IFluidTank> ability;
    private final Set<Fluid> allowedFluids;
    private final int capacity;
    protected final NotifiableFluidTank fluidTank;

    public MetaTileEntityReactorFluidPort(ResourceLocation metaTileEntityId, boolean isExportHatch,
                                          MultiblockAbility<IFluidTank> ability, Fluid allowedFluid, int capacity) {
        this(metaTileEntityId, isExportHatch, ability, Collections.singleton(allowedFluid), capacity);
    }

    /**
     * As the single-fluid constructor, but accepting any of {@code allowedFluids} -- used by the primary coolant
     * ports, which must accept any of {@code GTNLMaterials#ReactorCoolantWaterBands} (see
     * {@code com.windows2000.gtnuclear.api.nuclear.thermal.ReactorCoolantTemperatureBands}'s own JavaDoc for why
     * the primary loop's fluid is quantized into multiple discrete-temperature fluids rather than a single one).
     */
    public MetaTileEntityReactorFluidPort(ResourceLocation metaTileEntityId, boolean isExportHatch,
                                          MultiblockAbility<IFluidTank> ability, Collection<Fluid> allowedFluids,
                                          int capacity) {
        super(metaTileEntityId, GTValues.IV, isExportHatch);
        this.ability = ability;
        this.allowedFluids = new LinkedHashSet<>(allowedFluids);
        this.capacity = capacity;
        this.fluidTank = createFluidTank();
        // createImportFluidHandler()/createExportFluidHandler() already ran once during super(...), before
        // fluidTank was assigned, so the pipe-facing import/export handlers built at that point wrapped a null
        // tank. Re-running it now (mirroring GT's own MetaTileEntityFluidHatch, which does the same for the same
        // reason) rebuilds them against the real instance.
        initializeInventory();
    }

    private NotifiableFluidTank createFluidTank() {
        return new NotifiableFluidTank(capacity, this, isExportHatch) {

            @Override
            public int fillInternal(FluidStack resource, boolean doFill) {
                if (resource == null || !allowedFluids.contains(resource.getFluid())) return 0;
                return super.fillInternal(resource, doFill);
            }
        };
    }

    @Override
    public MetaTileEntity createMetaTileEntity(IGregTechTileEntity tileEntity) {
        return new MetaTileEntityReactorFluidPort(metaTileEntityId, isExportHatch, ability, allowedFluids, capacity);
    }

    @Override
    protected FluidTankList createImportFluidHandler() {
        return isExportHatch ? new FluidTankList(false) : new FluidTankList(false, fluidTank);
    }

    @Override
    protected FluidTankList createExportFluidHandler() {
        return isExportHatch ? new FluidTankList(false, fluidTank) : new FluidTankList(false);
    }

    @Override
    public void update() {
        super.update();
        if (!getWorld().isRemote) {
            if (isExportHatch) {
                pushFluidsIntoNearbyHandlers(getFrontFacing());
            } else {
                pullFluidsFromNearbyHandlers(getFrontFacing());
            }
        }
    }

    @Override
    public @NotNull List<MultiblockAbility<?>> getAbilities() {
        return Collections.singletonList(ability);
    }

    @Override
    public void registerAbilities(@NotNull AbilityInstances abilityInstances) {
        abilityInstances.add(fluidTank);
    }

    @Override
    public void renderMetaTileEntity(CCRenderState renderState, Matrix4 translation, IVertexOperation[] pipeline) {
        super.renderMetaTileEntity(renderState, translation, pipeline);
        if (shouldRenderOverlay()) {
            SimpleOverlayRenderer pipeOverlay = isExportHatch ? Textures.PIPE_OUT_OVERLAY : Textures.PIPE_IN_OVERLAY;
            pipeOverlay.renderSided(getFrontFacing(), renderState, translation, pipeline);
            SimpleOverlayRenderer portOverlay = isExportHatch ? Textures.FLUID_HATCH_OUTPUT_OVERLAY :
                    Textures.FLUID_HATCH_INPUT_OVERLAY;
            portOverlay.renderSided(getFrontFacing(), renderState, translation, pipeline);
        }
    }

    @Override
    public ModularPanel buildUI(PosGuiData guiData, PanelSyncManager syncManager, UISettings settings) {
        var fluidSyncHandler = GTFluidSlot.sync(fluidTank).showAmountOnSlot(false);
        return GTGuis.createPanel(this, 176, 166)
                .child(IKey.lang(getMetaFullName()).asWidget().pos(6, 6))
                .child(new GTFluidSlot().pos(79, 36).size(18).syncHandler(fluidSyncHandler))
                .bindPlayerInventory();
    }

    @NotNull
    public Set<Fluid> getAllowedFluids() {
        return allowedFluids;
    }
}
