package com.windows2000.gtnuclear.common.util;

import net.minecraft.util.math.BlockPos;

import com.windows2000.gtnuclear.api.nuclear.thermal.EnergyLedger;
import com.windows2000.gtnuclear.api.util.GTNLLogger;
import com.windows2000.gtnuclear.common.metatileentities.multi.MetaTileEntityPressurizedWaterReactor;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Experimental (2026-09-24, user request for balance-tuning work) CSV telemetry dump for a running reactor,
 * written once per automatic-rod-control cycle (see {@code ConfigHolder.reactor.autoControlIntervalTicks}).
 * Opened lazily on {@code MetaTileEntityPressurizedWaterReactor#updateAutoControl()}'s own first call, closed
 * on the reactor's {@code onRemoval()} -- independent of whether automatic control itself is actually toggled on
 * (2026-09-29, real-playtest-driven, see this mod's design memory: tying the logger's own lifecycle to the
 * auto-control toggle meant switching control off to make a manual adjustment silently lost telemetry, and
 * switching back on fragmented one continuous session across multiple files). Mirrors
 * {@code HeatExchangerTelemetryLogger}'s own always-on-once-formed lifecycle.
 */
public final class ReactorTelemetryLogger implements AutoCloseable {

    private static final DateTimeFormatter FILE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final PrintWriter writer;

    private ReactorTelemetryLogger(@NotNull PrintWriter writer) {
        this.writer = writer;
        writer.println("elapsedSeconds,stage,powerRelative,reactivityPcm,shapeReactivityPcm,xenonPcm,fuelTempK," +
                "coolantTempK,pressureMPa,coolantMassKg,voidFractionPercent,boronPpm,boronStatus," +
                "avgRodInsertionPercent,severeAccidentStage,autoControlAction,moderatorTemperatureReactivityPcm," +
                "dopplerReactivityPcm,totalPowerConsumptionEuPerTick,primaryCirculationFlowMb," +
                "neutronSourceSeedDebug,hasPointKineticsConstantsDebug,hasCmfdResultDebug,powerRelativeExactDebug," +
                "ledgerTicks,eFissionJ,eRcpJ,eImportJ,eMakeupInJ,eExportJ,eMakeupOutJ,eStoredDeltaJ,eResidualJ," +
                "eBandRoundingJ,fuelAssemblies,rodClusters,rcpRunning,rhrInService,rhrHeatRemovalW,rhrCoolingFlowKgPerS," +
                "rhrCoolingOutletK,eRhrJ,eRhrPumpJ,operatingMode,multiplicationM,refuelingHold,refuelingHoldReason");
        writer.flush();
    }

    /**
     * @param pos the reactor controller's position, used only to make the log's filename identifiable when
     *            multiple reactors are being tuned in the same session
     */
    @NotNull
    public static ReactorTelemetryLogger open(@NotNull BlockPos pos) {
        File dir = new File("logs/gtnuclear");
        if (!dir.exists() && !dir.mkdirs()) {
            GTNLLogger.logger.warn("Could not create reactor telemetry log directory: " + dir.getAbsolutePath());
        }
        File file = new File(dir, "reactor_" + pos.getX() + "_" + pos.getY() + "_" + pos.getZ() + "_" +
                FILE_TIMESTAMP.format(LocalDateTime.now()) + ".csv");
        try {
            PrintWriter writer = new PrintWriter(new FileWriter(file));
            GTNLLogger.logger.info("Reactor telemetry logging started: " + file.getAbsolutePath());
            return new ReactorTelemetryLogger(writer);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to open reactor telemetry log file: " + file.getAbsolutePath(),
                    e);
        }
    }

    public void logRow(double elapsedSeconds, @NotNull MetaTileEntityPressurizedWaterReactor reactor,
                       @NotNull String autoControlAction) {
        double avgRodInsertionPercent = reactor.getControlRodSummaries().stream()
                .mapToDouble(summary -> summary.insertionDepth() * 100.0)
                .average().orElse(0.0);
        String boronStatus = reactor.isBoronDilutionActive() ? "DILUTING" :
                reactor.isBoronConcentrationActive() ? "CONCENTRATING" : "IDLE";
        EnergyLedger<ReactorEnergyCategory> ledger = reactor.getEnergyLedger();
        writer.printf(
                "%.2f,%s,%.6f,%.1f,%.1f,%.1f,%.1f,%.1f,%.2f,%.2f,%.2f,%.1f,%s,%.2f,%s,%s,%.1f,%.1f,%d,%d,%.9f,%s,%s,%.20e," +
                        "%d,%.6e,%.6e,%.6e,%.6e,%.6e,%.6e,%.6e,%.6e,%.6e,%d,%d,%s,%s,%.6e,%.2f,%.1f,%.6e,%.6e,%d,%.4f,%s,%s%n",
                elapsedSeconds,
                reactor.getOperatingState().stage,
                reactor.getRelativeNeutronPopulation(),
                reactor.getTotalReactivity() * 1.0e5,
                reactor.getShapeReactivityPcm(),
                reactor.getXenonReactivityWorth() * 1.0e5,
                reactor.getFuelTemperatureKelvin(),
                reactor.getTemperatureKelvin(),
                reactor.getPressureMPa(),
                reactor.getCoolantMassKg(),
                reactor.getPrimaryVoidFraction() * 100.0,
                reactor.getBoronPpm(),
                boronStatus,
                avgRodInsertionPercent,
                reactor.getSevereAccidentState().stage,
                autoControlAction,
                reactor.getModeratorTemperatureReactivityPcm(),
                reactor.getDopplerReactivityPcm(),
                reactor.getTotalPowerConsumptionEuPerTick(),
                reactor.getPrimaryCirculationFlowMb(),
                reactor.getNeutronSourceSeedForDebug(),
                reactor.hasPointKineticsConstantsForDebug(),
                reactor.hasCmfdResultForDebug(),
                reactor.getRelativeNeutronPopulation(),
                ledger.ticks(),
                ledger.get(ReactorEnergyCategory.FISSION),
                ledger.get(ReactorEnergyCategory.RCP),
                ledger.get(ReactorEnergyCategory.IMPORT),
                ledger.get(ReactorEnergyCategory.MAKEUP_IN),
                ledger.get(ReactorEnergyCategory.EXPORT),
                ledger.get(ReactorEnergyCategory.MAKEUP_OUT),
                ledger.get(ReactorEnergyCategory.STORED_CHANGE),
                ledger.residualJoules(ReactorEnergyCategory.values()),
                ledger.get(ReactorEnergyCategory.BAND_ROUNDING),
                reactor.getLoadedFuelAssemblyCount(),
                reactor.getLoadedControlRodClusterCount(),
                reactor.isRcpRunning(),
                reactor.isRhrInService(),
                reactor.getRhrHeatRemovalWatts(),
                reactor.getRhrCoolingFlowKgPerSecond(),
                reactor.getRhrCoolingOutletKelvin(),
                ledger.get(ReactorEnergyCategory.RHR),
                ledger.get(ReactorEnergyCategory.RHR_PUMP),
                reactor.getOperatingMode(),
                reactor.getSubcriticalMultiplication(),
                reactor.isRefuelingHold(),
                reactor.getRefuelingHoldReason() != null ? reactor.getRefuelingHoldReason().name() : "NONE");
        writer.flush();
    }

    @Override
    public void close() {
        writer.close();
    }
}
