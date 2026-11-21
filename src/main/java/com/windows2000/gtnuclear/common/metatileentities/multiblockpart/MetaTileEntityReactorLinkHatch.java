package com.windows2000.gtnuclear.common.metatileentities.multiblockpart;

import gregtech.api.GTValues;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.interfaces.IGregTechTileEntity;
import gregtech.api.metatileentity.multiblock.AbilityInstances;
import gregtech.api.metatileentity.multiblock.IMultiblockAbilityPart;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.metatileentity.multiblock.MultiblockControllerBase;
import gregtech.api.pipenet.IRoutePath;
import gregtech.client.renderer.texture.Textures;
import gregtech.common.metatileentities.multi.multiblockpart.MetaTileEntityMultiblockPart;
import gregtech.common.pipelike.optical.net.OpticalPipeNet;
import gregtech.common.pipelike.optical.tile.TileEntityOpticalPipe;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;

import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Matrix4;
import com.windows2000.gtnuclear.api.capability.GTNLCapabilities;
import com.windows2000.gtnuclear.api.capability.IReactorLinkHatch;
import com.windows2000.gtnuclear.api.metatileentity.multiblock.GTNLMultiblockAbility;
import com.windows2000.gtnuclear.common.metatileentities.multi.MetaTileEntityPressurizedWaterReactor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * One concrete class for both ends of the reactor-console link: the transmitter (reactor-side, sits at the
 * reactor's own 'L' pattern slot) and the receiver (console-side). Mirrors GTST's own
 * {@code MetaTileEntityComputationHatch} pattern almost exactly -- the transmitter resolves its own
 * {@link #getController()} directly, while the receiver looks at its front-facing neighbor for an
 * {@code Optical Cable} pipe and queries {@link GTNLCapabilities#CAPABILITY_REACTOR_LINK} across the network.
 * <p>
 * <b>Server-side only.</b> {@link #resolvePeer()} calls {@link TileEntityOpticalPipe#getOpticalPipeNet()}
 * directly (bypassing {@code TileEntityOpticalPipe#getCapabilityInternal}, which -- discovered 2026-09-20 after
 * a user report that the console never left "NO REACTOR LINKED" even once the physical pipe connection worked --
 * is hardcoded to only route GTST's own two built-in capability types, {@code CAPABILITY_DATA_ACCESS}/
 * {@code CABABILITY_COMPUTATION_PROVIDER}; a naive {@code tileEntity.getCapability(CAPABILITY_REACTOR_LINK, ...)}
 * call on the neighboring pipe silently falls through to {@code super.getCapabilityInternal} and never finds
 * anything). {@code OpticalPipeNet.getNetData(BlockPos, EnumFacing)} and the inherited
 * {@link IRoutePath#getTargetCapability} default method are both already fully generic public API, so this needed
 * no further GTST changes -- only the earlier {@code BlockOpticalPipe.canPipeConnectToBlock} registry addition
 * (a separate, physical-connection-only check) remains necessary. {@code OpticalPipeNet.getOpticalPipeNet()}
 * returns null on the client ({@code world.isRemote}), so {@link #getReactor()} and {@link #hasActiveLink()} are
 * only ever safe to call from server-side code -- see {@code MetaTileEntityControlConsole}'s own JavaDoc for how
 * the console resolves and explicitly syncs the result for its client-side screen part to read.
 */
public class MetaTileEntityReactorLinkHatch extends MetaTileEntityMultiblockPart
                                            implements IMultiblockAbilityPart<IReactorLinkHatch>, IReactorLinkHatch {

    private final boolean isTransmitter;

    public MetaTileEntityReactorLinkHatch(ResourceLocation metaTileEntityId, boolean isTransmitter) {
        super(metaTileEntityId, GTValues.LV);
        this.isTransmitter = isTransmitter;
    }

    @Override
    public MetaTileEntity createMetaTileEntity(IGregTechTileEntity tileEntity) {
        return new MetaTileEntityReactorLinkHatch(metaTileEntityId, isTransmitter);
    }

    @Override
    public boolean isTransmitter() {
        return this.isTransmitter;
    }

    /**
     * On the transmitter (reactor-side) end, resolves directly via {@link #getController()}. On the receiver
     * (console-side) end, follows the front-facing Optical Cable network to the transmitter and delegates to its
     * own {@code getReactor()} -- so callers on either end can use this one method regardless of which side they
     * hold. Server-side only, see this class's own JavaDoc.
     */
    @Nullable
    @Override
    public MetaTileEntityPressurizedWaterReactor getReactor() {
        if (isTransmitter()) {
            MultiblockControllerBase controller = getController();
            return controller instanceof MetaTileEntityPressurizedWaterReactor reactor &&
                    controller.isStructureFormed() ? reactor : null;
        }
        IReactorLinkHatch peer = resolvePeer();
        return peer != null && peer.isTransmitter() ? peer.getReactor() : null;
    }

    /**
     * Symmetric for both roles: walks this hatch's own front-facing Optical Cable network looking for a hatch of
     * the <i>opposite</i> role. Requires {@link #getCapability} to expose {@code CAPABILITY_REACTOR_LINK} on
     * both roles (not just the transmitter, as the original {@link #getReactor()}-only design needed) so the
     * transmitter side can discover a receiver the same way the receiver already discovers a transmitter.
     * Server-side only, see this class's own JavaDoc.
     */
    @Override
    public boolean hasActiveLink() {
        IReactorLinkHatch peer = resolvePeer();
        return peer != null && peer.isTransmitter() != this.isTransmitter();
    }

    /**
     * Walks this hatch's own front-facing Optical Cable network for a hatch of either role -- see this class's
     * own JavaDoc for why this goes through {@code OpticalPipeNet.getNetData}/{@code IRoutePath#getTargetCapability}
     * directly instead of a plain {@code neighborPipe.getCapability(...)} call.
     */
    @Nullable
    private IReactorLinkHatch resolvePeer() {
        TileEntity tileEntity = getNeighbor(getFrontFacing());
        if (!(tileEntity instanceof TileEntityOpticalPipe pipe)) return null;
        OpticalPipeNet net = pipe.getOpticalPipeNet();
        if (net == null) return null;
        IRoutePath<TileEntityOpticalPipe> route = net.getNetData(pipe.getPipePos(), getFrontFacing().getOpposite());
        return route == null ? null : route.getTargetCapability(GTNLCapabilities.CAPABILITY_REACTOR_LINK);
    }

    @Override
    protected boolean openGUIOnRightClick() {
        return false;
    }

    @Override
    public boolean canPartShare() {
        return false;
    }

    @Override
    public void renderMetaTileEntity(CCRenderState renderState, Matrix4 translation, IVertexOperation[] pipeline) {
        super.renderMetaTileEntity(renderState, translation, pipeline);
        if (shouldRenderOverlay()) {
            // Reuses GT's own optical data access hatch overlay as a placeholder pending a dedicated texture.
            Textures.OPTICAL_DATA_ACCESS_HATCH.renderSided(getFrontFacing(), renderState, translation, pipeline);
        }
    }

    @Override
    public MultiblockAbility<IReactorLinkHatch> getAbility() {
        return isTransmitter() ? GTNLMultiblockAbility.REACTOR_LINK_TRANSMITTER :
                GTNLMultiblockAbility.REACTOR_LINK_RECEIVER;
    }

    @Override
    public void registerAbilities(@NotNull AbilityInstances abilityInstances) {
        abilityInstances.add(this);
    }

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing side) {
        // Exposed on both roles (not just the transmitter) so hasActiveLink() can discover a peer of either role
        // -- getReactor() only ever needs the transmitter's own exposure, but the receiver now needs to be
        // discoverable too.
        if (side == getFrontFacing() && capability == GTNLCapabilities.CAPABILITY_REACTOR_LINK) {
            return GTNLCapabilities.CAPABILITY_REACTOR_LINK.cast(this);
        }
        return super.getCapability(capability, side);
    }
}
