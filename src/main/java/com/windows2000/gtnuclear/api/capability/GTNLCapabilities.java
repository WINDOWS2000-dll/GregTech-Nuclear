package com.windows2000.gtnuclear.api.capability;

import gregtech.api.capability.SimpleCapabilityManager;
import gregtech.common.pipelike.optical.BlockOpticalPipe;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;

/** This mod's own Forge capability constants, mirroring GTST's {@code GregtechTileCapabilities}. */
public final class GTNLCapabilities {

    @CapabilityInject(IReactorLinkHatch.class)
    public static Capability<IReactorLinkHatch> CAPABILITY_REACTOR_LINK = null;

    private GTNLCapabilities() {}

    /** Must run before any hatch queries {@link #CAPABILITY_REACTOR_LINK} -- called from {@code CoreModule#preInit}. */
    public static void init() {
        SimpleCapabilityManager.registerCapabilityWithNoDefault(IReactorLinkHatch.class);
        // Forge's CapabilityManager.register(...) injects @CapabilityInject fields synchronously, so
        // CAPABILITY_REACTOR_LINK is already non-null here. Without this, GTST's own Optical Cable
        // (BlockOpticalPipe#canPipeConnectToBlock) only recognizes its own two built-in capability types and
        // would never physically connect to a MetaTileEntityReactorLinkHatch -- see this mod's design memory,
        // 2026-09-20, for the user-reported symptom this fixes.
        BlockOpticalPipe.registerConnectableCapability(CAPABILITY_REACTOR_LINK);
    }
}
