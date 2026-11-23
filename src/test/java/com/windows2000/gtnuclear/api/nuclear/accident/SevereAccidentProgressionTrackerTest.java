package com.windows2000.gtnuclear.api.nuclear.accident;

import com.windows2000.gtnuclear.common.ConfigHolder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link SevereAccidentProgressionTracker} against a {@link FakeAccidentHost} -- the stage 1-2 (2026-11-21) gates:
 * no escalation without a heat-generating fueled core, and no instant coolant-contact explosion.
 */
class SevereAccidentProgressionTrackerTest {

    private static final class FakeAccidentHost implements AccidentHost {

        double coolantMassKg = 1000.0;
        double ratedCoolantMassKg = 1000.0;
        double voidFraction = 0.0;
        boolean housingMismatch = false;
        double fuelTemperatureKelvin = 600.0;
        double meltedFuelMassKg = 0.0;
        double coreFuelMassAtMeltdownStartKg = 0.0;
        double totalCoreFuelMassKg = 1000.0;
        boolean heatSource = true;

        @Override
        public double getCoolantMassKg() {
            return coolantMassKg;
        }

        @Override
        public double computeRatedCoolantMassKg() {
            return ratedCoolantMassKg;
        }

        @Override
        public double getPrimaryVoidFraction() {
            return voidFraction;
        }

        @Override
        public boolean isHousingMismatch() {
            return housingMismatch;
        }

        @Override
        public double getFuelTemperatureKelvin() {
            return fuelTemperatureKelvin;
        }

        @Override
        public double getMeltedFuelMassKg() {
            return meltedFuelMassKg;
        }

        @Override
        public double getCoreFuelMassAtMeltdownStartKg() {
            return coreFuelMassAtMeltdownStartKg;
        }

        @Override
        public double computeTotalCoreFuelMassKg() {
            return totalCoreFuelMassKg;
        }

        @Override
        public boolean hasCoreHeatSource() {
            return heatSource;
        }
    }

    private static FakeAccidentHost locaHost() {
        FakeAccidentHost host = new FakeAccidentHost();
        host.coolantMassKg = 0.0;
        return host;
    }

    @Test
    void emptyCoreNeverEscalatesEvenWithLoca() {
        FakeAccidentHost host = locaHost();
        host.totalCoreFuelMassKg = 0.0;

        SevereAccidentState next = new SevereAccidentProgressionTracker().advance(host, SevereAccidentState.normal());

        assertEquals(SevereAccidentStage.NORMAL, next.stage);
    }

    @Test
    void fueledCoreWithoutHeatSourceNeverEscalatesEvenWithLoca() {
        FakeAccidentHost host = locaHost();
        host.heatSource = false;

        SevereAccidentState next = new SevereAccidentProgressionTracker().advance(host, SevereAccidentState.normal());

        assertEquals(SevereAccidentStage.NORMAL, next.stage);
    }

    @Test
    void heatGeneratingCoreWithLocaEscalatesToUncontrolledHeating() {
        FakeAccidentHost host = locaHost();

        SevereAccidentState next = new SevereAccidentProgressionTracker().advance(host, SevereAccidentState.normal());

        assertEquals(SevereAccidentStage.UNCONTROLLED_HEATING, next.stage);
    }

    @Test
    void meltdownWithCoolantPresentDoesNotExplodeBelowMeltFractionThreshold() {
        FakeAccidentHost host = new FakeAccidentHost();
        host.coolantMassKg = 500.0;
        host.coreFuelMassAtMeltdownStartKg = 1000.0;
        host.meltedFuelMassKg = 1000.0 * ConfigHolder.reactor.explosionMeltFractionThreshold * 0.5;
        SevereAccidentState meltdown = new SevereAccidentState(SevereAccidentStage.MELTDOWN, 0.0);

        SevereAccidentState next = new SevereAccidentProgressionTracker().advance(host, meltdown);

        assertEquals(SevereAccidentStage.MELTDOWN, next.stage);
    }

    @Test
    void meltdownStillExplodesAtMeltFractionThreshold() {
        FakeAccidentHost host = new FakeAccidentHost();
        host.coreFuelMassAtMeltdownStartKg = 1000.0;
        host.meltedFuelMassKg = 1000.0 * ConfigHolder.reactor.explosionMeltFractionThreshold;
        SevereAccidentState meltdown = new SevereAccidentState(SevereAccidentStage.MELTDOWN, 0.0);

        SevereAccidentState next = new SevereAccidentProgressionTracker().advance(host, meltdown);

        assertEquals(SevereAccidentStage.EXPLOSION, next.stage);
    }
}
