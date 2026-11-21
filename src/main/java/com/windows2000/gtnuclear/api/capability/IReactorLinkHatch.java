package com.windows2000.gtnuclear.api.capability;

import com.windows2000.gtnuclear.common.metatileentities.multi.MetaTileEntityPressurizedWaterReactor;
import org.jetbrains.annotations.Nullable;

/**
 * Exposed by {@code MetaTileEntityReactorLinkHatch} on its front face so a paired hatch across an Optical Cable
 * network can resolve the reactor on the other end -- mirrors GTST's own {@code IDataAccessHatch}/
 * {@code IOpticalComputationProvider} pattern (see {@code MetaTileEntityComputationHatch}), reusing the same
 * Optical Cable network infrastructure without any GTST-side changes (its pipe network routing is generic over
 * any Forge {@code Capability<T>}, not hardcoded to GT's own two capability types).
 * <p>
 * Both ends resolve the same reactor reference: the transmitter (reactor-side) returns its own controller
 * directly, while the receiver (console-side) follows its front-facing Optical Cable across the network to the
 * transmitter and delegates to it -- so callers never need to care which end they're holding.
 */
public interface IReactorLinkHatch {

    /**
     * True if this hatch is the reactor-side (transmitter) end of the link, false for the console-side (receiver) end.
     */
    boolean isTransmitter();

    /**
     * @return the paired reactor, if the link is currently connected end-to-end and the reactor structure is
     *         formed; null otherwise.
     */
    @Nullable
    MetaTileEntityPressurizedWaterReactor getReactor();

    /**
     * @return true if a hatch of the opposite role ({@link #isTransmitter()}) is reachable across this hatch's
     *         front-facing Optical Cable network -- i.e. the physical link is connected, regardless of whether
     *         the peer's own multiblock structure happens to be formed right now. Used by the reactor side (see
     *         this mod's design memory, 2026-09-17: "コンソール接続の喪失" SCRAM trigger) to detect console-link
     *         loss; server-side only, called from {@code updateFormedValid()}.
     */
    boolean hasActiveLink();
}
