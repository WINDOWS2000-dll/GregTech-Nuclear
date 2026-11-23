package com.windows2000.gtnuclear.common.util;

import net.minecraft.util.math.BlockPos;

import com.windows2000.gtnuclear.api.nuclear.thermal.EnergyLedger;
import com.windows2000.gtnuclear.api.util.GTNLLogger;
import com.windows2000.gtnuclear.common.metatileentities.multi.MetaTileEntityHeatExchanger;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Experimental (2026-09-25, real-playtest-driven, see this mod's design memory) CSV telemetry dump for a running
 * heat exchanger's own internal lumped state -- added specifically because
 * {@link com.windows2000.gtnuclear.common.util.ReactorTelemetryLogger} only ever captures the reactor's own side
 * of the primary loop, which proved insufficient to diagnose a real playtest report of primary coolant
 * temperature plateauing far below its intended operating band (332K observed vs. an intended ~588K) even after
 * ruling out pipe/hatch throughput as the cause -- the heat exchanger's own primary-side/secondary-side state was
 * never directly observable. Unlike that class, this one is not tied to any player-facing toggle (the heat
 * exchanger has no console of its own -- see {@code MetaTileEntityHeatExchanger}'s own JavaDoc on why it's a
 * placeholder shape with no dedicated UI yet); it simply opens the first time the heat exchanger's structure is
 * formed and valid, and closes when the block is removed.
 */
public final class HeatExchangerTelemetryLogger implements AutoCloseable {

    private static final DateTimeFormatter FILE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final PrintWriter writer;

    private HeatExchangerTelemetryLogger(@NotNull PrintWriter writer) {
        this.writer = writer;
        writer.println("elapsedSeconds,primarySideTemperatureKelvin,primarySideMassKg," +
                "secondaryWaterTemperatureKelvin,secondaryWaterMassKg,qRemovedWatts," +
                "ledgerTicks,ePrimaryImportJ,eFeedwaterJ,ePrimaryExportJ,eSteamJ,eBlowdownJ,eStoredDeltaJ," +
                "eResidualJ,eTransferJ,eBandRoundingJ");
        writer.flush();
    }

    /**
     * @param pos the heat exchanger controller's position, used only to make the log's filename identifiable when
     *            multiple heat exchangers are being tuned in the same session
     */
    @NotNull
    public static HeatExchangerTelemetryLogger open(@NotNull BlockPos pos) {
        File dir = new File("logs/gtnuclear");
        if (!dir.exists() && !dir.mkdirs()) {
            GTNLLogger.logger.warn("Could not create heat exchanger telemetry log directory: " +
                    dir.getAbsolutePath());
        }
        File file = new File(dir, "heat_exchanger_" + pos.getX() + "_" + pos.getY() + "_" + pos.getZ() + "_" +
                FILE_TIMESTAMP.format(LocalDateTime.now()) + ".csv");
        try {
            PrintWriter writer = new PrintWriter(new FileWriter(file));
            GTNLLogger.logger.info("Heat exchanger telemetry logging started: " + file.getAbsolutePath());
            return new HeatExchangerTelemetryLogger(writer);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to open heat exchanger telemetry log file: " + file.getAbsolutePath(), e);
        }
    }

    public void logRow(double elapsedSeconds, @NotNull MetaTileEntityHeatExchanger heatExchanger,
                       double qRemovedWatts) {
        EnergyLedger<HeatExchangerEnergyCategory> ledger = heatExchanger.getEnergyLedger();
        writer.printf("%.2f,%.2f,%.2f,%.2f,%.2f,%.1f,%d,%.6e,%.6e,%.6e,%.6e,%.6e,%.6e,%.6e,%.6e,%.6e%n",
                elapsedSeconds,
                heatExchanger.getPrimarySideTemperatureKelvin(),
                heatExchanger.getPrimarySideMassKg(),
                heatExchanger.getSecondaryWaterTemperatureKelvin(),
                heatExchanger.getSecondaryWaterMassKg(),
                qRemovedWatts,
                ledger.ticks(),
                ledger.get(HeatExchangerEnergyCategory.PRIMARY_IMPORT),
                ledger.get(HeatExchangerEnergyCategory.FEEDWATER),
                ledger.get(HeatExchangerEnergyCategory.PRIMARY_EXPORT),
                ledger.get(HeatExchangerEnergyCategory.STEAM),
                ledger.get(HeatExchangerEnergyCategory.BLOWDOWN),
                ledger.get(HeatExchangerEnergyCategory.STORED_CHANGE),
                ledger.residualJoules(HeatExchangerEnergyCategory.values()),
                ledger.get(HeatExchangerEnergyCategory.TRANSFER),
                ledger.get(HeatExchangerEnergyCategory.BAND_ROUNDING));
        writer.flush();
    }

    @Override
    public void close() {
        writer.close();
    }
}
