package com.windows2000.gtnuclear.api.nuclear.neutronics;

/**
 * Derived rate constants for {@link XenonIodineSolver}, built from three inputs: the game's configured xenon dead
 * time and equilibrium xenon worth (both player/config-facing, see {@code ConfigHolder.reactor}), and two fixed
 * real-nuclear-data ratios that are not configurable (analogous to how this mod's decay branching ratios aren't
 * configurable elsewhere):
 * <ul>
 * <li>{@link #DIRECT_XENON_FRACTION} -- the fraction of Xe-135 production that comes directly from fission
 * rather than via I-135 decay, derived from real U-235 thermal fission yields
 * (independent yields: I-135 ~6.39%, Xe-135 ~0.237%; fraction = 0.237/(0.237+6.39)).</li>
 * <li>{@link #BURNUP_TO_DECAY_RATIO_AT_RATED_POWER} -- how much faster Xe-135 is removed by neutron absorption
 * ("burnup") than by radioactive decay at full rated power, derived from Xe-135's real thermal absorption
 * cross-section (~2.6e6 barns) and a representative full-power thermal flux (~3e13 n/cm^2/s).</li>
 * </ul>
 * These two ratios are scale-invariant under the {@link XenonDeadTimeCalibration} time-scaling (they depend only on
 * ratios of physical quantities, not on the absolute magnitude of the decay constants), so they stay physically
 * meaningful regardless of the configured dead time.
 * <p>
 * Rather than modeling xenon concentration as a physical atom density (which would need core-averaged macroscopic
 * cross-sections this mod does not have for an arbitrary player-built reactor), xenon concentration here is
 * defined directly in reactivity-worth units: at full-power (n=1) equilibrium, {@code xenonConcentration} equals
 * exactly {@code equilibriumXenonWorth}. This is a legitimate rescaling of the underlying linear ODEs (see this
 * mod's design memory for the derivation) that avoids fabricating precision this mod cannot actually back with
 * real core physics data, while still preserving the physically-correct transient shape (the iodine-pit
 * peak/timing) via the two ratios above.
 */
public final class XenonIodineConstants {

    /** Fraction of Xe-135 production that comes directly from fission rather than via I-135 decay. */
    public static final double DIRECT_XENON_FRACTION = 0.0357;
    /** Ratio of Xe-135's neutron-absorption ("burnup") removal rate to its radioactive decay rate at rated power. */
    public static final double BURNUP_TO_DECAY_RATIO_AT_RATED_POWER = 3.7;

    public final double iodineDecayConstant;
    public final double xenonDecayConstant;
    public final double xenonBurnupCoefficient;
    public final double iodineProductionRate;
    public final double xenonProductionRate;

    public XenonIodineConstants(double configuredDeadTimeSeconds, double equilibriumXenonWorth) {
        double scale = XenonDeadTimeCalibration.scalingFactorFor(configuredDeadTimeSeconds);
        this.iodineDecayConstant = scale * XenonDeadTimeCalibration.REAL_IODINE_DECAY_CONSTANT;
        this.xenonDecayConstant = scale * XenonDeadTimeCalibration.REAL_XENON_DECAY_CONSTANT;
        this.xenonBurnupCoefficient = BURNUP_TO_DECAY_RATIO_AT_RATED_POWER * xenonDecayConstant;

        double totalProductionAtRatedPower = equilibriumXenonWorth * (xenonDecayConstant + xenonBurnupCoefficient);
        this.xenonProductionRate = DIRECT_XENON_FRACTION * totalProductionAtRatedPower;
        this.iodineProductionRate = (1 - DIRECT_XENON_FRACTION) * totalProductionAtRatedPower;
    }
}
