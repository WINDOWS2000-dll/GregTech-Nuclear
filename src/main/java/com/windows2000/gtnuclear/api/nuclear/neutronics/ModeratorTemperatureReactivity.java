package com.windows2000.gtnuclear.api.nuclear.neutronics;

/**
 * Moderator temperature coefficient (MTC) reactivity, modeled as a quadratic (not flat-linear) curve in
 * temperature -- see this mod's design memory (2026-09-28) for why: a real PWR's own MTC is not a constant
 * instantaneous slope across its entire operating temperature range, it grows steadily more negative as
 * temperature rises (near zero, sometimes even slightly positive, at cold/low-boron-diluted conditions; reaching
 * its full quoted "design" value only near hot/operating temperature). Treating a single quoted coefficient (e.g.
 * -30 pcm/K, itself a realistic instantaneous value near hot conditions) as a flat constant applied uniformly from
 * cold shutdown all the way to hot operating temperature therefore over-integrates the cumulative reactivity
 * effect -- a real playtest found this flat-linear approach alone already produced a -7050 pcm burden at 535K, on
 * top of which even a fully-diluted (0 ppm) boron inventory could not reach criticality, whereas a real PWR's own
 * total cold-to-hot temperature defect is typically only on the order of -1000 to -2500 pcm.
 * <p>
 * This instead treats {@code referenceCoefficientPcmPerKelvin} as the INSTANTANEOUS slope of reactivity versus
 * temperature at {@code referenceTemperatureKelvin} (not a flat constant), and assumes that slope itself scales
 * linearly with how far above {@code ambientTemperatureKelvin} the reactor currently is -- i.e.
 * {@code d(rho)/dT = referenceCoefficientPcmPerKelvin * (T - ambient) / (referenceTemperature - ambient)}.
 * Integrating that from {@code ambientTemperatureKelvin} up to {@code temperatureKelvin} yields a quadratic
 * reactivity curve: zero at ambient, growing quadratically (not linearly) toward
 * {@code referenceCoefficientPcmPerKelvin * (referenceTemperature - ambient) / 2} at the reference temperature
 * itself -- exactly half of what a flat-linear model would give at that same point, and proportionally much less
 * at any lower temperature (a real playtest confirmed criticality became reachable again with this curve, at the
 * same coefficient magnitude the flat-linear model could not reach even with boron fully diluted).
 */
public final class ModeratorTemperatureReactivity {

    private ModeratorTemperatureReactivity() {}

    /**
     * @param temperatureKelvin                current coolant (or fuel, for the analogous Doppler use case)
     *                                         temperature
     * @param ambientTemperatureKelvin         reference "zero reactivity contribution" temperature (cold shutdown)
     * @param referenceTemperatureKelvin       temperature at which {@code referenceCoefficientPcmPerKelvin} is the
     *                                         real, quoted instantaneous slope
     * @param referenceCoefficientPcmPerKelvin the instantaneous slope (pcm/K, typically negative) at
     *                                         {@code referenceTemperatureKelvin}
     * @return the reactivity contribution (pcm) at {@code temperatureKelvin}; 0.0 if
     *         {@code referenceTemperatureKelvin <= ambientTemperatureKelvin} (a degenerate configuration with no
     *         valid reference span to scale against)
     */
    public static double reactivityPcm(double temperatureKelvin, double ambientTemperatureKelvin,
                                       double referenceTemperatureKelvin, double referenceCoefficientPcmPerKelvin) {
        double referenceSpanKelvin = referenceTemperatureKelvin - ambientTemperatureKelvin;
        if (referenceSpanKelvin <= 0.0) return 0.0;
        double deltaKelvin = temperatureKelvin - ambientTemperatureKelvin;
        return referenceCoefficientPcmPerKelvin * deltaKelvin * deltaKelvin / (2.0 * referenceSpanKelvin);
    }
}
