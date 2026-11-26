package com.windows2000.gtnuclear.api.nuclear.thermal;

/**
 * The heat exchanger's secondary-side pressure setpoint ramp (2026-11-26, stage 1-4b of this mod's reactor physics
 * rework, decision "R1", see this mod's design memory). The operator chooses a target; the effective setpoint (the
 * pressure the steam dump holds, which sets the boiling temperature) follows it -- immediately when raised, but when
 * lowered only so fast that its saturation temperature falls by at most {@code maxCooldownKelvinPerHour}. This is
 * how a real PWR is cooled from hot standby toward RHR entry: the steam dump in pressure mode, ramped so the
 * reactor coolant cooldown rate stays within its Technical Specification limit (55.6 K/h, 100 F/h).
 * <p>
 * The effective setpoint is tracked as its <b>saturation temperature</b>, not as a pressure:
 * {@link WaterSaturationCurve}'s pressure and temperature functions are not exact inverses of each other, and
 * converting back and forth every tick accumulated a ~4% error in the ramp rate over an hour.
 */
public final class SecondaryPressureControl {

    /** The lowest setpoint: the secondary side vents to atmosphere, it is never drawn into a vacuum. */
    public static final double MIN_SETPOINT_MPA = WaterSaturationCurve.ATMOSPHERIC_PRESSURE_PASCALS / 1.0e6;

    /** Saturation temperature at {@link #MIN_SETPOINT_MPA}. */
    public static final double MIN_SETPOINT_SATURATION_KELVIN = WaterSaturationCurve.saturationTemperature(
            WaterSaturationCurve.ATMOSPHERIC_PRESSURE_PASCALS);

    private SecondaryPressureControl() {}

    /** Saturation temperature of a setpoint given in MPa, clamped to atmospheric. */
    public static double saturationKelvinOf(double setpointMPa) {
        return WaterSaturationCurve.saturationTemperature(Math.max(setpointMPa, MIN_SETPOINT_MPA) * 1.0e6);
    }

    /**
     * @return the next tick's effective setpoint, as a saturation temperature (K), moving from
     *         {@code effectiveKelvin} toward {@code targetKelvin}
     */
    public static double rampSetpointSaturationKelvin(double effectiveKelvin, double targetKelvin,
                                                      double maxCooldownKelvinPerHour, double dtSeconds) {
        double target = Math.max(targetKelvin, MIN_SETPOINT_SATURATION_KELVIN);
        if (target >= effectiveKelvin) return target;
        if (maxCooldownKelvinPerHour <= 0) return effectiveKelvin;
        return Math.max(target, effectiveKelvin - maxCooldownKelvinPerHour * dtSeconds / 3600.0);
    }
}
