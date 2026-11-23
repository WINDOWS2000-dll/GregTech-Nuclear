package com.windows2000.gtnuclear.api.nuclear.thermal;

/**
 * Water's liquid-vapor saturation curve via the IAPWS-95 "Auxiliary Equation for the Saturation Pressure" (the
 * Wagner equation), valid from the triple point (273.16 K) to the critical point (647.096 K / 22.064 MPa) -- the
 * entire range relevant to a PWR's primary coolant (typically ~570-600 K at ~15.5 MPa during normal operation,
 * kept well subcooled below saturation by design).
 * <p>
 * Reference: W. Wagner and A. Pruss, "The IAPWS Formulation 1995 for the Thermodynamic Properties of Ordinary
 * Water Substance for General and Scientific Use", J. Phys. Chem. Ref. Data, 31, 387 (2002).
 */
public final class WaterSaturationCurve {

    /** Critical temperature of water (K). */
    public static final double CRITICAL_TEMPERATURE_KELVIN = 647.096;
    /** Critical pressure of water (Pa). */
    public static final double CRITICAL_PRESSURE_PASCALS = 22.064e6;
    /** Triple point temperature of water (K) -- the lower validity bound of this correlation. */
    public static final double TRIPLE_POINT_TEMPERATURE_KELVIN = 273.16;
    /**
     * Standard atmospheric pressure (absolute). Added 2026-11-21 (stage 1-2 of this mod's reactor physics rework,
     * see this mod's design memory): every pressure this mod feeds into {@link #saturationTemperature} is absolute,
     * and a depressurized/vented loop sits at atmospheric pressure (saturation ~373 K), not vacuum -- before this
     * floor existed, the reactor's own "depressurized" 0 MPa was treated as vacuum, putting saturation at the
     * triple point (273.16 K) so that injecting 300 K water into a depressurized reactor spuriously "boiled" it
     * (confirmed via real playtest telemetry: coolant pinned at 273.2 K with 38-63% void during a fill).
     */
    public static final double ATMOSPHERIC_PRESSURE_PASCALS = 101325.0;

    private static final double A1 = -7.85951783;
    private static final double A2 = 1.84408259;
    private static final double A3 = -11.7866497;
    private static final double A4 = 22.6807411;
    private static final double A5 = -15.9618719;
    private static final double A6 = 1.80122502;

    private static final double BISECTION_TOLERANCE_KELVIN = 1e-4;

    private WaterSaturationCurve() {}

    /** @return the saturation pressure (Pa) at the given temperature (K), via the closed-form Wagner equation. */
    public static double saturationPressure(double temperatureKelvin) {
        double theta = 1 - temperatureKelvin / CRITICAL_TEMPERATURE_KELVIN;
        double exponent = (CRITICAL_TEMPERATURE_KELVIN / temperatureKelvin) *
                (A1 * theta + A2 * Math.pow(theta, 1.5) + A3 * Math.pow(theta, 3) + A4 * Math.pow(theta, 3.5) +
                        A5 * Math.pow(theta, 4) + A6 * Math.pow(theta, 7.5));
        return CRITICAL_PRESSURE_PASCALS * Math.exp(exponent);
    }

    /**
     * @return the saturation temperature (K) at the given pressure (Pa), found via bisection since the Wagner
     *         equation has no closed-form inverse. {@link #isBoiling} avoids this bisection where only a
     *         boolean phase check (not the numeric saturation temperature itself) is needed.
     */
    public static double saturationTemperature(double pressurePascals) {
        double lower = TRIPLE_POINT_TEMPERATURE_KELVIN;
        double upper = CRITICAL_TEMPERATURE_KELVIN;
        // saturationPressure is monotonically increasing with temperature over the valid range.
        while (upper - lower > BISECTION_TOLERANCE_KELVIN) {
            double mid = (lower + upper) / 2;
            if (saturationPressure(mid) < pressurePascals) {
                lower = mid;
            } else {
                upper = mid;
            }
        }
        return (lower + upper) / 2;
    }

    /**
     * @return true if liquid water at the given temperature (K) and pressure (Pa) is at or above its saturation
     *         point (boiling/flashing to steam) rather than subcooled. Equivalent to
     *         {@code temperatureKelvin >= saturationTemperature(pressurePascals)} but computed via the direct
     *         (non-bisecting) direction of the Wagner equation for efficiency in a per-tick check.
     */
    public static boolean isBoiling(double temperatureKelvin, double pressurePascals) {
        return saturationPressure(temperatureKelvin) >= pressurePascals;
    }
}
