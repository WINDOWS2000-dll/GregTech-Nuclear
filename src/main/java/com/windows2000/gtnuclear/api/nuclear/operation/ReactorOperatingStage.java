package com.windows2000.gtnuclear.api.nuclear.operation;

/**
 * The reactor's coarse operating mode -- see this mod's design memory (2026-09-17, startup/shutdown/SCRAM state
 * transitions) for the full narrative this enum encodes. Deliberately does not distinguish "criticality just
 * reached" from "at rated power": both are just points along the continuous power rise that
 * {@link com.windows2000.gtnuclear.api.nuclear.neutronics.PointKineticsSolver} already models once
 * {@link #OPERATING} begins -- no separate discrete state is needed for that.
 */
public enum ReactorOperatingStage {
    /** Fully shut down. Fuel handling needs {@link #REFUELING} since stage 1-4c (2026-11-27). */
    SHUTDOWN,
    /** Coolant/pressure/temperature/boron are being brought up and rods withdrawn; not yet self-sustaining. */
    STARTUP,
    /** Critical and running, anywhere from just-critical through rated power. */
    OPERATING,
    /** A planned shutdown is in progress (rods being inserted, boron being concentrated, depressurizing/cooling). */
    SHUTTING_DOWN,
    /** SCRAM has fired; all rods inserted. Only a manual console reset can leave this stage. */
    TRIPPED,
    /**
     * Mode 6 (stage 1-4c, 2026-11-27): entered from SHUTDOWN once every refueling start condition holds (see
     * {@link RefuelingEvaluator}); the only stage in which fuel assembly ports open. Rod drives are disconnected
     * (held fully inserted) and startup, boron dilution and pressurization are refused. Not a running stage: SCRAM
     * triggers and structure breaches do not trip it.
     */
    REFUELING
}
