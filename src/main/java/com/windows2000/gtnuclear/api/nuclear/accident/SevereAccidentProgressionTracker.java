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
     * The stage follows the fuel itself (stage 2-3b, decision O2 -- see {@link SevereAccidentEvaluator}); melt
     * progression (the melted fuel mass) happens in the host's own thermal step, and this method only advances the
     * staging built on top of it, independent of the reactor's operating state (decay heat continues after a SCRAM).
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
        // Stage 2-3b (2026-11-28, decision O2): the stage follows the fuel's own temperature and melting, so the old
        // cause gate (LOCA / void / housing mismatch) and the stage 1-2 "heat-generating core" flag are gone -- an
        // empty
        // or cold core never heats, and a SCRAMmed core that loses cooling heats on its decay heat and escalates.
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
        return SevereAccidentEvaluator.advance(current, host.getFuelTemperatureKelvin(),
                host.getMeltedFuelMassKg(), totalCoreFuelMassKgForEvaluator, coolantContactWithCorium,
                accidentConstants);
    }
}
