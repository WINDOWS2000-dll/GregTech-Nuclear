package com.windows2000.gtnuclear.api.nuclear.operation;

/**
 * Control rod insertion during a reactor trip (stage 1-1 of this mod's reactor physics rework, 2026-11-21, see
 * this mod's design memory). A real PWR's rod cluster control assemblies are held by electromagnetic drive
 * mechanisms; a trip signal releases them and they fall into the core under gravity, with technical
 * specifications requiring full insertion within roughly 2.2-2.7 seconds. Modeled here as a constant-speed drop
 * over {@code dropTimeSeconds} -- the cosine-shaped rod worth curve already turns that into the characteristic
 * S-shaped reactivity insertion, so a more detailed fall profile adds nothing visible at this fidelity.
 * <p>
 * Pure and stateless: the caller simply applies it every tick for as long as the reactor stays tripped.
 */
public final class ScramRodDrop {

    private ScramRodDrop() {}

    /**
     * @param insertionDepth  current insertion depth (0 = fully withdrawn, 1 = fully inserted)
     * @param dtSeconds       time step
     * @param dropTimeSeconds time for a fully withdrawn rod to reach full insertion; {@code <= 0} means instant
     * @return the new insertion depth, never exceeding 1.0
     */
    public static double advance(double insertionDepth, double dtSeconds, double dropTimeSeconds) {
        double current = Math.max(0.0, Math.min(1.0, insertionDepth));
        if (dropTimeSeconds <= 0.0) return 1.0;
        return Math.min(1.0, current + Math.max(0.0, dtSeconds) / dropTimeSeconds);
    }
}
