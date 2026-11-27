package com.windows2000.gtnuclear.api.nuclear.neutronics;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Physical neutron source strengths for the point kinetics source term (2026-11-27, stage 2-1 of this mod's reactor
 * physics rework, see this mod's design memory): the startup source's Cf-252 and the fuel's own spontaneous
 * fission ("intrinsic source"), both in neutrons per second, plus the conversion into the relative units
 * {@link PointKineticsSolver} works in.
 * <p>
 * Relative conversion: point kinetics tracks the neutron population relative to rated power. At rated power the
 * core holds {@code N0 = nu * F0 * Lambda} neutrons ({@code F0} fissions/s, each releasing {@code nu} neutrons that
 * live {@code Lambda} on average), so a source of {@code S} neutrons/s adds {@code S / N0} per second in relative
 * units. A subcritical core then settles at {@code n = S_rel * Lambda / -rho}.
 */
public final class NeutronSourceStrength {

    /** Recoverable energy per fission, ~200 MeV (J). */
    public static final double ENERGY_PER_FISSION_JOULES = 200.0 * 1.602176634e-13;
    /**
     * Neutrons per fission used for the relative conversion -- U-235's thermal value, an interim constant until
     * per-nuclide nu arrives with the group constant rework (stages 3-4).
     */
    public static final double NEUTRONS_PER_FISSION = 2.43;
    private static final double AVOGADRO = 6.02214076e23;
    private static final double SECONDS_PER_YEAR = 365.25 * 86400.0;

    /** Cf-252: real half-life 2.645 y, spontaneous fission branch 3.09%, 3.77 neutrons per spontaneous fission. */
    public static final double CF252_HALF_LIFE_YEARS = 2.645;
    public static final double CF252_SPONTANEOUS_FISSION_BRANCH = 0.0309;
    public static final double CF252_NEUTRONS_PER_SPONTANEOUS_FISSION = 3.77;
    public static final double CF252_MOLAR_MASS_GRAMS = 252.08;

    /**
     * Spontaneous fission neutron yields (neutrons per second per gram) of the fuel nuclides this mod tracks, keyed
     * by element symbol. Source: D. Reilly et al., "Passive Nondestructive Assay of Nuclear Materials"
     * (NUREG/CR-5550, 1991), Table 11-1. Nuclides not listed (or not yet tracked by burnup, e.g. Pu-238, Pu-242,
     * Cm-244) contribute nothing; Pu-240 is what makes burned fuel a noticeable source.
     */
    public static final Map<String, Double> SPONTANEOUS_FISSION_NEUTRONS_PER_SECOND_PER_GRAM = Map.of(
            "U-235", 2.99e-4,
            "U-236", 5.49e-3,
            "U-238", 1.36e-2,
            "Pu-239", 2.18e-2,
            "Pu-240", 1.02e3,
            "Pu-241", 5.0e-2);

    private NeutronSourceStrength() {}

    /**
     * Neutron emission of {@code grams} of Cf-252, from its <b>real</b> decay constant -- the item's own depletion
     * runs on this mod's separate compressed clock, but using that compressed constant here would inflate the
     * strength by the compression factor.
     */
    public static double cf252NeutronsPerSecond(double grams) {
        if (grams <= 0) return 0.0;
        double atoms = grams / CF252_MOLAR_MASS_GRAMS * AVOGADRO;
        double decayConstant = Math.log(2) / (CF252_HALF_LIFE_YEARS * SECONDS_PER_YEAR);
        return atoms * decayConstant * CF252_SPONTANEOUS_FISSION_BRANCH * CF252_NEUTRONS_PER_SPONTANEOUS_FISSION;
    }

    /** Spontaneous fission neutrons/s of a fuel inventory given as grams per element symbol. */
    public static double intrinsicNeutronsPerSecond(@NotNull Map<String, Double> gramsBySymbol) {
        double total = 0.0;
        for (Map.Entry<String, Double> entry : gramsBySymbol.entrySet()) {
            Double yield = SPONTANEOUS_FISSION_NEUTRONS_PER_SECOND_PER_GRAM.get(entry.getKey());
            if (yield != null && entry.getValue() > 0) total += yield * entry.getValue();
        }
        return total;
    }

    /**
     * @return the source in point kinetics' relative units (per second) for a core of the given rated thermal power
     *         and prompt neutron generation time
     */
    public static double relativeRate(double neutronsPerSecond, double ratedThermalPowerWatts,
                                      double promptNeutronLifetime) {
        if (neutronsPerSecond <= 0 || ratedThermalPowerWatts <= 0 || promptNeutronLifetime <= 0) return 0.0;
        double ratedFissionsPerSecond = ratedThermalPowerWatts / ENERGY_PER_FISSION_JOULES;
        double ratedPopulation = NEUTRONS_PER_FISSION * ratedFissionsPerSecond * promptNeutronLifetime;
        return neutronsPerSecond / ratedPopulation;
    }
}
