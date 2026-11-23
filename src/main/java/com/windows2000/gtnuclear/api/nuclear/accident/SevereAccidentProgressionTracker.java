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
        // Stage 1-2 (2026-11-21, see this mod's design memory, S3): loss of cooling/control can only heat a core
        // that actually holds fuel AND currently produces heat. Without this gate, an empty reactor -- or a cold,
        // fueled, zero-power reactor in the middle of its initial fill (still "LOCA" by inventory) -- was declared
        // UNCONTROLLED_HEATING (confirmed via real playtest telemetry, 2026-09-29). Decay heat (stage 2) will plug
        // into hasCoreHeatSource(), so a freshly SCRAMmed core that loses coolant still escalates as intended.
        boolean heatGeneratingCore = host.computeTotalCoreFuelMassKg() > 0 && host.hasCoreHeatSource();
        boolean abnormalCoolingOrControl = heatGeneratingCore &&
                (isLoca || host.isHousingMismatch() || voidAbnormal);
        // Stage 1-2 (2026-11-21, see this mod's design memory, S1): the previous "any coolant at all means molten
        // fuel/coolant contact" criterion escalated every MELTDOWN straight to EXPLOSION on the very next tick
        // whenever a single kilogram of coolant remained. Disabled until the severe-accident design is revisited
        // (break modeling, melt relocation, steam explosion conditions); until then only the melt-fraction
        // threshold can trigger an explosion. SevereAccidentEvaluator's own contact branch is kept as-is.
        boolean coolantContactWithCorium = false;
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
