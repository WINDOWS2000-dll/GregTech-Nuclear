package com.windows2000.gtnuclear.api.nuclear.operation;

/**
 * Why a SCRAM fired -- see this mod's design memory (2026-09-17): "温度/圧力の安全限界超過、一次冷却水量不足
 * (LOCA相当)、手動ボタン/レッドストーン信号、コンソール接続の喪失" (temperature/pressure limit exceeded,
 * coolant loss (LOCA), manual button/redstone signal, console link lost). Purely descriptive -- kept for the
 * console to display why the reactor tripped; carries no behavior of its own.
 * <p>
 * {@link #POWER_LIMIT} added 2026-09-24 (real-playtest-driven, see this mod's design memory): none of the
 * original five triggers watch relative power itself, so a slow reactivity drift (e.g. a control-loop bug, or
 * an operator simply leaving the reactor slightly supercritical) could climb to several hundred percent of rated
 * power without ever tripping, since temperature/pressure/coolant mass alone don't necessarily catch it in time.
 * A real PWR's reactor protection system always includes a high-power trip (commonly ~118% of rated) for exactly
 * this reason.
 * <p>
 * {@link #LOSS_OF_ELECTRICAL_POWER} added 2026-09-28 (user request): mirrors a real PWR's own control rod drive
 * mechanism (CRDM) fail-safe -- CRDMs are electrically powered, and losing that power drops the rods in by
 * gravity, an automatic trip, not an optional consequence. Fires once the reactor's own coolant pump draw (see
 * {@code MetaTileEntityPressurizedWaterReactor#updateReactorPowerConsumption}) finds its {@code INPUT_ENERGY}
 * hatch(es) completely unable to supply anything at all (not merely below full desired rate -- a real plant's
 * emergency diesel generators providing reduced but nonzero power still keep it running), debounced the same way
 * as {@link #CONSOLE_LINK_LOST} to ignore brief hiccups rather than genuine sustained loss.
 */
public enum ScramReason {
    MANUAL,
    REDSTONE_SIGNAL,
    TEMPERATURE_LIMIT,
    PRESSURE_LIMIT,
    COOLANT_LOSS,
    CONSOLE_LINK_LOST,
    POWER_LIMIT,
    LOSS_OF_ELECTRICAL_POWER
}
