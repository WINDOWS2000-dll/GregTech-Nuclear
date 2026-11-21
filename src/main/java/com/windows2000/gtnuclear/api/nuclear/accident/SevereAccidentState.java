package com.windows2000.gtnuclear.api.nuclear.accident;

import org.jetbrains.annotations.NotNull;

/**
 * Current severe accident stage plus, once melting has begun, the fraction (0-1) of the core's total fuel
 * inventory that has become corium -- see {@link SevereAccidentStage} for the overall progression.
 */
public final class SevereAccidentState {

    @NotNull
    public final SevereAccidentStage stage;
    /**
     * Fraction (0-1) of the core's total fuel inventory that has become corium; 0 before
     * {@link SevereAccidentStage#MELTDOWN}.
     */
    public final double coreMeltFraction;

    public SevereAccidentState(@NotNull SevereAccidentStage stage, double coreMeltFraction) {
        this.stage = stage;
        this.coreMeltFraction = coreMeltFraction;
    }

    @NotNull
    public static SevereAccidentState normal() {
        return new SevereAccidentState(SevereAccidentStage.NORMAL, 0.0);
    }

    /**
     * @return the explosion's magnitude (linear in melted fuel mass and {@link #coreMeltFraction} -- see
     *         {@link SevereAccidentConstants}'s own JavaDoc for why this is a gameplay-balance formula, not a
     *         physically-derived one). Only meaningful once {@link #stage} is {@link SevereAccidentStage#EXPLOSION}.
     */
    public double explosionMagnitude(double meltedFuelMassKg, @NotNull SevereAccidentConstants constants) {
        return constants.explosionMagnitudeCoefficient * meltedFuelMassKg * coreMeltFraction;
    }
}
