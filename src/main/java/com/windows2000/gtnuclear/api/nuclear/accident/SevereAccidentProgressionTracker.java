package com.windows2000.gtnuclear.api.nuclear.accident;

import com.windows2000.gtnuclear.common.ConfigHolder;
import org.jetbrains.annotations.NotNull;

/**
 * Advances a reactor's {@link SevereAccidentState} by one tick via the already-pure {@link SevereAccidentEvaluator}
 * (2026-10-06, extracted from {@code MetaTileEntityPressurizedWaterReactor#updateSevereAccidentState} as the
 * second step of that class's own decomposition -- see this mod's design memory). Every sensor read goes
 * through the narrow {@link AccidentHost} interface, so this class has no dependency on the concrete reactor
 * multiblock. Deliberately does not fire the actual in-world explosion itself (that needs real
 * {@code World}/{@code BlockPos} access, which cannot be made pure) -- {@link #advance} only returns the next
 * {@link SevereAccidentState}; the host compares its {@code stage} against the state passed in to detect a new
 * crossing into {@link SevereAccidentStage#EXPLOSION} and fires the explosion itself.
 */
public final class SevereAccidentProgressionTracker {

    /**
     * {@code abnormalCoolingOrControl} combines the same LOCA/structural-damage signals SCRAM already reacts to
     * plus void fraction exceeding a threshold -- SCRAM tripping does not by itself resolve an ongoing LOCA/void
     * condition (decay heat continues after shutdown), so this check is deliberately independent of the
     * reactor's own operating state, matching real accident progression. Melt progression itself (the melted
     * fuel mass) already happens in the host's own thermal step; this method only advances the staging built on
     * top of it.
     * <p>
     * Once melting has ever started ({@link AccidentHost#getMeltedFuelMassKg()} &gt; 0), the meltdown-fraction
     * denominator passed to {@link SevereAccidentEvaluator#advance} is the stable
     * {@link AccidentHost#getCoreFuelMassAtMeltdownStartKg()}, not a freshly recomputed
     * {@link AccidentHost#computeTotalCoreFuelMassKg()} -- see that method's own JavaDoc for why using the live
     * value there was a real bug (reloading a small amount of fresh fuel after a meltdown could look like the
     * new fuel was already almost entirely melted). Before any melting has occurred the live value is used,
     * though it is not actually consulted by the evaluator outside the {@code MELTDOWN} case anyway.
     */
    @NotNull
    public SevereAccidentState advance(@NotNull AccidentHost host, @NotNull SevereAccidentState current) {
        double ratedCoolantMassKg = host.computeRatedCoolantMassKg();
        boolean isLoca = LOCADetector.isLOCA(host.getCoolantMassKg(), ratedCoolantMassKg,
                ConfigHolder.reactor.locaCoolantFraction);
        boolean voidAbnormal = host.getPrimaryVoidFraction() > ConfigHolder.reactor.severeAccidentVoidFractionThreshold;
        boolean abnormalCoolingOrControl = isLoca || host.isFuelControlRodHousingMismatch() || voidAbnormal;
        // MELTDOWN-stage corium still submerged in coolant risks a steam explosion on contact -- a deliberate
        // simplification of far more complex real energetics, same spirit as SevereAccidentConstants' own
        // explosion-magnitude formula.
        boolean coolantContactWithCorium = host.getCoolantMassKg() > 0;
        double totalCoreFuelMassKgForEvaluator = host.getMeltedFuelMassKg() > 0 ?
                host.getCoreFuelMassAtMeltdownStartKg() : host.computeTotalCoreFuelMassKg();

        SevereAccidentConstants accidentConstants = new SevereAccidentConstants(
                ConfigHolder.reactor.explosionMeltFractionThreshold,
                ConfigHolder.reactor.explosionMagnitudeCoefficient);
        return SevereAccidentEvaluator.advance(current, abnormalCoolingOrControl, host.getFuelTemperatureKelvin(),
                host.getMeltedFuelMassKg(), totalCoreFuelMassKgForEvaluator, coolantContactWithCorium,
                accidentConstants);
    }
}
