package com.windows2000.gtnuclear.api.unification.materials.reactor;

import gregtech.api.fluids.FluidBuilder;
import gregtech.api.unification.material.Material;
import gregtech.api.unification.material.properties.FluidPipeProperties;
import gregtech.api.unification.material.properties.PropertyKey;

import com.windows2000.gtnuclear.api.fluids.attribute.GTNLFluidAttributes;
import com.windows2000.gtnuclear.api.nuclear.thermal.CoolingWaterTemperatureBands;
import com.windows2000.gtnuclear.api.nuclear.thermal.ReactorCoolantTemperatureBands;

import static com.windows2000.gtnuclear.api.unification.materials.GTNLMaterials.*;
import static com.windows2000.gtnuclear.api.util.GTNLUtils.gtnlID;
import static gregtech.api.unification.material.info.MaterialIconSet.METALLIC;

/**
 * The PWR primary coolant loop's fluid and dedicated pipe material -- see
 * {@link com.windows2000.gtnuclear.api.fluids.attribute.GTNLFluidAttributes#REACTOR_COOLANT_WATER}'s own JavaDoc
 * for why a dedicated pipe (rather than ordinary GT fluid pipes) is needed. Future dedicated coolants (e.g. liquid
 * metal for a fast reactor) should get their own {@link gregtech.api.fluids.attribute.FluidAttribute} here,
 * declared as contained on whichever pipe(s) are meant to carry them.
 */
public class ReactorFluidMaterials {

    public static void register() {
        ReactorCoolantWater = Material.builder(54, gtnlID("reactor_coolant_water"))
                .liquid(new FluidBuilder()
                        .temperature(300)
                        .attribute(GTNLFluidAttributes.REACTOR_COOLANT_WATER))
                .color(0x3C78C8)
                .iconSet(METALLIC)
                .build();

        // Not a nuclide, so the Material.Builder conventions in feedback_material_builder_conventions.md (flags,
        // dust+ingot, etc.) don't apply here -- this is a plain structural pipe material. Throughput (10000) is
        // deliberately far above what an ordinary same-tier GT pipe offers, since it exists specifically to avoid
        // needing end-game pipe materials just to move reactor-scale coolant volume.
        ReactorCoolantPipeAlloy = Material.builder(55, gtnlID("reactor_coolant_pipe_alloy"))
                .ingot()
                .liquid(new FluidBuilder().temperature(1700))
                .color(0x8C96A0)
                .iconSet(METALLIC)
                .fluidPipeProperties(1600, 400000, true)
                .build();

        BoricAcidSolution = Material.builder(56, gtnlID("boric_acid_solution"))
                .liquid(new FluidBuilder().temperature(300))
                .color(0xE6DCC8)
                .iconSet(METALLIC)
                .build();

        RadioactiveWastewater = Material.builder(57, gtnlID("radioactive_wastewater"))
                .liquid(new FluidBuilder()
                        .temperature(300)
                        .attribute(GTNLFluidAttributes.RADIOACTIVE_WASTEWATER))
                .color(0x647828)
                .iconSet(METALLIC)
                .build();

        // Secondary loop steam output, ID 62 -- a new fluid (not GTST's own Materials.Steam), per this mod's
        // design memory (2026-09-22): the secondary loop is a from-scratch model, not a GT steam boiler reskin.
        // 560K is representative of real PWR secondary-side steam at its own operating pressure (~7 MPa,
        // noticeably lower than the primary loop's ~15.5 MPa), not a precise correlation.
        HighPressureSteam = Material.builder(62, gtnlID("high_pressure_steam"))
                .gas(new FluidBuilder()
                        .temperature(560))
                .color(0xD8D8E8)
                .iconSet(METALLIC)
                .build();

        // High-pressure turbine exhaust / low-pressure turbine inlet, ID 143 (the coolant bands above occupy
        // 63-142) -- see this mod's design memory (2026-10) for the HP/LP two-stage turbine design this anchors:
        // 480K is chosen so the temperature drop each stage is responsible for (HighPressureSteam 560K -> this
        // 480K -> AMBIENT_TEMPERATURE_KELVIN 300K) splits overall turbine thermal efficiency roughly 30.8%/69.2%
        // HP/LP, matching real nuclear turbine-generator sets (commonly ~25-35% HP / 65-75% LP combined).
        LowPressureSteam = Material.builder(143, gtnlID("low_pressure_steam"))
                .gas(new FluidBuilder()
                        .temperature(480))
                .color(0xC0C8D8)
                .iconSet(METALLIC)
                .build();

        // Discrete primary coolant temperature bands, IDs 63-142 (81 bands total, 5K spacing -- see
        // ReactorCoolantTemperatureBands's own JavaDoc for why the primary loop's fluid transport is quantized
        // into separate registered fluids rather than a single fluid carrying a continuous NBT-tagged temperature,
        // and for why 5K spacing specifically). Band 0 (300K) reuses the already-registered ReactorCoolantWater
        // above rather than a new material. Colors interpolate from ReactorCoolantWater's own blue (0x3C78C8) at
        // 300K toward red (0xE8482C) at 700K, purely cosmetic -- computed rather than hand-listed since narrowing
        // the spacing (originally 50K, 8 hand-picked colors) would otherwise need one color literal per band.
        Material[] bands = new Material[ReactorCoolantTemperatureBands.BAND_TEMPERATURES_KELVIN.length];
        bands[0] = ReactorCoolantWater;
        int coldestColor = 0x3C78C8;
        int hottestColor = 0xE8482C;
        for (int i = 1; i < bands.length; i++) {
            int temperatureKelvin = (int) ReactorCoolantTemperatureBands.BAND_TEMPERATURES_KELVIN[i];
            double t = (double) i / (bands.length - 1);
            // "..._350_kelvin", not "..._350kelvin" -- GT's Material naming validator round-trips the name
            // through its own camelCase conversion and rejects a digit run directly followed by a letter with no
            // underscore between them (confirmed via a real launch crash, 2026-09-22:
            // IllegalArgumentException: Cannot add materials with names like 'materialnumber'!).
            bands[i] = Material.builder(62 + i, gtnlID("reactor_coolant_water_" + temperatureKelvin + "_kelvin"))
                    .liquid(new FluidBuilder()
                            .temperature(temperatureKelvin)
                            .attribute(GTNLFluidAttributes.REACTOR_COOLANT_WATER))
                    .color(interpolateColor(coldestColor, hottestColor, t))
                    .iconSet(METALLIC)
                    .build();
        }
        ReactorCoolantWaterBands = bands;

        // Non-radioactive cooling water bands, IDs 144-158 (2026-11-24, stage 1-4a: the cooling side of the
        // reactor's built-in RHR heat exchanger, and later component cooling water / cooling towers) -- see
        // CoolingWaterTemperatureBands. Plain fluids without the reactor-coolant attribute, so ordinary GT pipes
        // carry them. Every band, including 300 K, is its own material (a new system, unlike the primary coolant
        // bands' reuse of an older fluid). Pale blue at 300 K toward warm orange at 370 K, purely cosmetic.
        Material[] coolingBands = new Material[CoolingWaterTemperatureBands.BAND_TEMPERATURES_KELVIN.length];
        int coolColor = 0x7AB8E8;
        int warmColor = 0xE8A060;
        for (int i = 0; i < coolingBands.length; i++) {
            int temperatureKelvin = (int) CoolingWaterTemperatureBands.BAND_TEMPERATURES_KELVIN[i];
            double t = coolingBands.length > 1 ? (double) i / (coolingBands.length - 1) : 0.0;
            coolingBands[i] = Material.builder(144 + i, gtnlID("cooling_water_" + temperatureKelvin + "_kelvin"))
                    .liquid(new FluidBuilder().temperature(temperatureKelvin))
                    .color(interpolateColor(coolColor, warmColor, t))
                    .iconSet(METALLIC)
                    .build();
        }
        CoolingWaterBands = coolingBands;

        FluidPipeProperties pipeProperties = ReactorCoolantPipeAlloy.getProperty(PropertyKey.FLUID_PIPE);
        pipeProperties.setCanContain(GTNLFluidAttributes.REACTOR_COOLANT_WATER, true);
        pipeProperties.setCanContain(GTNLFluidAttributes.RADIOACTIVE_WASTEWATER, true);
    }

    /** Linear per-channel RGB interpolation between two {@code 0xRRGGBB} colors, {@code t} in [0, 1]. */
    private static int interpolateColor(int colorA, int colorB, double t) {
        int r = lerpChannel((colorA >> 16) & 0xFF, (colorB >> 16) & 0xFF, t);
        int g = lerpChannel((colorA >> 8) & 0xFF, (colorB >> 8) & 0xFF, t);
        int b = lerpChannel(colorA & 0xFF, colorB & 0xFF, t);
        return (r << 16) | (g << 8) | b;
    }

    private static int lerpChannel(int a, int b, double t) {
        return (int) Math.round(a + (b - a) * t);
    }

    private ReactorFluidMaterials() {}
}
