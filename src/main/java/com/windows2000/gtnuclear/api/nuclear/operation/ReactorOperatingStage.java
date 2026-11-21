package com.windows2000.gtnuclear.api.nuclear.operation;

/**
 * The reactor's coarse operating mode -- see this mod's design memory (2026-09-17, startup/shutdown/SCRAM state
 * transitions) for the full narrative this enum encodes. Deliberately does not distinguish "criticality just
 * reached" from "at rated power": both are just points along the continuous power rise that
 * {@link com.windows2000.gtnuclear.api.nuclear.neutronics.PointKineticsSolver} already models once
 * {@link #OPERATING} begins -- no separate discrete state is needed for that.
 */
public enum ReactorOperatingStage {
    /** Fully shut down; maintenance (fuel/control-rod item swaps) is only allowed in this stage. */
    SHUTDOWN,
    /** Coolant/pressure/temperature/boron are being brought up and rods withdrawn; not yet self-sustaining. */
    STARTUP,
    /** Critical and running, anywhere from just-critical through rated power. */
    OPERATING,
    /** A planned shutdown is in progress (rods being inserted, boron being concentrated, depressurizing/cooling). */
    SHUTTING_DOWN,
    /** SCRAM has fired; all rods inserted. Only a manual console reset can leave this stage. */
    TRIPPED
}
