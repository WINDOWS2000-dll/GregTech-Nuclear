package com.windows2000.gtnuclear.common;

import net.minecraftforge.common.config.Config;

import com.windows2000.gtnuclear.api.GTNuclearValues;

@Config(modid = GTNuclearValues.MODID, name = GTNuclearValues.MODID + '/' + GTNuclearValues.MODID)
public class ConfigHolder {

    @Config.Comment("Config options for the radioactive decay simulation")
    @Config.Name("Decay Options")
    public static DecayOptions decay = new DecayOptions();

    public static class DecayOptions {

        @Config.Comment({
                "How many real-world days of continuous server uptime the slowest-decaying registered isotope " +
                        "(by real-world half-life) should take for ONE in-game half-life to pass (i.e. half of it " +
                        "decayed) -- not full decay.",
                "Used together with compressionExponent to derive the power-law time compression " +
                        "(compressedHalfLife = k * realHalfLife^compressionExponent) applied to every isotope's " +
                        "half-life -- see com.windows2000.gtnuclear.api.nuclear.decay.TimeCompression.",
                "Default: 5.0" })
        @Config.RangeDouble(min = 0.01, max = 3650.0)
        public double daysForLongestIsotopeHalfLife = 5.0;

        @Config.Comment({
                "Exponent of the power-law time compression (compressedHalfLife = k * realHalfLife^exponent).",
                "Real-world half-lives span ~21 orders of magnitude (microseconds to billions of years); this " +
                        "exponent controls how much that range is compressed. Lower values compress more " +
                        "aggressively, making very long-lived and very short-lived isotopes decay at more " +
                        "similar in-game speeds; higher values preserve more of the real relative spread. Must be " +
                        "strictly between 0 and 1.",
                "Default: 0.3" })
        @Config.RangeDouble(min = 0.01, max = 0.99)
        public double compressionExponent = 0.3;
    }

    @Config.Comment("Config options for the reactor neutron physics simulation")
    @Config.Name("Reactor Options")
    public static ReactorOptions reactor = new ReactorOptions();

    public static class ReactorOptions {

        @Config.Comment({
                "How many real-world seconds the 'xenon dead time' (the post-shutdown window during which xenon " +
                        "poisoning can prevent restart, i.e. the 'iodine pit') should take under reference " +
                        "conditions (instantaneous shutdown from full-power equilibrium xenon).",
                "Dead time here is defined as the time for xenon reactivity worth to decay back down to its " +
                        "pre-shutdown equilibrium value. The actual in-game dead time varies with reactor state " +
                        "(power history, available reactivity margin) -- this value only calibrates the linear " +
                        "time-scaling factor applied to the real I-135/Xe-135 decay constants, see " +
                        "com.windows2000.gtnuclear.api.nuclear.neutronics.XenonDeadTimeCalibration.",
                "Default: 1800.0 (30 in-game minutes)" })
        @Config.RangeDouble(min = 1.0, max = 1.0E9)
        public double xenonDeadTimeSeconds = 1800.0;

        @Config.Comment({
                "Magnitude of the xenon-135 reactivity worth (|delta-k/k|) at full-power equilibrium.",
                "Default: 0.028 (2800 pcm), a typical real-world PWR value." })
        @Config.RangeDouble(min = 0.0, max = 1.0)
        public double equilibriumXenonWorth = 0.028;

        @Config.Comment({
                "Magnitude of the boron reactivity worth (|delta-k/k| per ppm of boron in the primary coolant).",
                "Corrected 2026-09-28 (real-playtest-driven, see this mod's design memory): the previous default, " +
                        "1.0E-5, was documented as \"10 pcm/ppm\" but BoronReactivity.reactivity()'s own linear " +
                        "formula (-worthPerPpm * concentrationPpm) actually yields exactly 1 pcm/ppm from that " +
                        "value (1.0E-5 in Δk/k-per-ppm units equals 1.0E-5 * 1.0E5 = 1.0 in pcm-per-ppm units, not " +
                        "10) -- confirmed by isolating boron's own contribution from two logged rows 10 seconds " +
                        "apart in a real playtest (all other reactivity terms held fixed: rods still 100% inserted, " +
                        "xenon/void still zero), subtracting off that same window's own logged moderator-" +
                        "temperature/Doppler deltas, and dividing the remainder by the logged boron ppm delta -- " +
                        "the result matched 1.0 pcm/ppm almost exactly. At that (unintentionally 10x weak) " +
                        "strength, a real playtest reached a state fully diluting boron from " +
                        "autoControlInitialBoronChargePpm's own 800 ppm down to 0 could only ever recover 800 pcm, " +
                        "hundreds of pcm short of what was needed to reach criticality (moderator-temperature/" +
                        "Doppler feedback alone was already -7755 pcm at that heatup temperature) -- boron could " +
                        "never have been enough on its own regardless of how the automatic STARTUP boron-assist " +
                        "safety valve orchestrates it.",
                "Default: 1.0E-4 (10 pcm/ppm, matching this field's own original intent and a typical real-world " +
                        "PWR value)." })
        @Config.RangeDouble(min = 0.0, max = 1.0)
        public double boronWorthPerPpm = 1.0E-4;

        @Config.Comment({
                "Baseline reactor power consumption (EU/t) per fuel channel -- instrumentation/control load, " +
                        "drawn whenever the console is connected regardless of whether the reactor is running or " +
                        "stopped. See com.windows2000.gtnuclear.api.nuclear.power.ReactorPowerConsumption.",
                "Default: 2.0 (a placeholder pending calibration once reactor thermal output's own unit/scale " +
                        "is finalized)." })
        @Config.RangeDouble(min = 0.0, max = 1.0E6)
        public double baseConsumptionPerChannel = 2.0;

        @Config.Comment({
                "Primary coolant pump power consumption (EU/t) per interior cell at 100% rated thermal output " +
                        "-- scales linearly down toward zero as current thermal output drops toward zero while " +
                        "OPERATING/STARTUP, mirroring how real pump power scales with coolant flow rate (itself " +
                        "proportional to heat being removed); forced to full rate instead while SHUTDOWN and " +
                        "still below heatupTargetTemperatureKelvin, matching a real RCP running near rated flow " +
                        "well before criticality specifically to heat up via friction (see that field's own " +
                        "JavaDoc). The actually-drawn amount (not just this nominal rate) converts into real " +
                        "coolant heat via euPerTickToWattsConversion -- see " +
                        "MetaTileEntityPressurizedWaterReactor#updateReactorPowerConsumption's own JavaDoc.",
                "Revised 2026-09-28 twice (see this mod's design memory): first from a flat 512.0 placeholder to " +
                        "a per-interior-cell value derived from real RCP motor ratings (a typical 4-loop PWR's " +
                        "own RCPs draw roughly 18-30 MW combined against a ~3400 MWth core), giving ~8823.53 -- " +
                        "then overridden by an explicit user-specified gameplay-progression target (this mod's " +
                        "own reactor is meant to sit in the LuV-to-mid-UHV EU/t consumption range, around " +
                        "~12000 EU/t total for the reference reactor at rated output) rather than the real-data " +
                        "figure, since GT's own tier economy has no real-world analog to calibrate consumption " +
                        "against in the first place. New value derived from that target: " +
                        "(12000 - baseConsumptionPerChannel*1010) / 1010 interior cells =~ 9.88.",
                "Default: 9.88, pending playtesting." })
        @Config.RangeDouble(min = 0.0, max = 1.0E9)
        public double pumpConsumptionPerInteriorCellAtRatedOutput = 9.88;

        @Config.Comment({
                "Real-world Watts represented by drawing 1 EU/t, steadily -- there is no canonical official " +
                        "GregTech lore value for this (community estimates for a real-world equivalent vary " +
                        "roughly 3-8 W per EU/t with no consensus), so this is a deliberate, documented gameplay-" +
                        "balance convention for this mod specifically, not a physical constant. Used to convert " +
                        "the reactor's own actually-drawn coolant pump EU/t into real thermal Watts entering the " +
                        "coolant as pump friction heat.",
                "Revised 2026-09-28 a second time, sharply upward, alongside " +
                        "pumpConsumptionPerInteriorCellAtRatedOutput's own drop to a GT-progression-appropriate " +
                        "EU/t figure (~12000 total instead of ~8.9 million): at the original 4.0 W/(EU/t), the " +
                        "reference reactor's own ~445,588 kg coolant pool would take ~4355 real-world HOURS to " +
                        "heat from 300K to heatupTargetTemperatureKelvin on pump friction heat alone, since the " +
                        "much smaller EU/t draw carries proportionally less real heat at that conversion rate. " +
                        "This value is picked instead to target a ~15-minute heatup for that same reference " +
                        "reactor -- a real physical constant cannot serve both 'EU/t sits in a sane GT tier " +
                        "range' and 'heatup completes in a playable time' simultaneously, so (per this field's " +
                        "own JavaDoc on there being no real EU-to-Watts lore anyway) the conversion itself " +
                        "absorbs the difference rather than either of those two other values.",
                "Default: 69675.88, pending playtesting." })
        @Config.RangeDouble(min = 0.0, max = 1.0E9)
        public double euPerTickToWattsConversion = 69675.88;

        @Config.Comment({
                "Added 2026-09-28 (real-PWR-data-driven, see this mod's design memory): primary coolant " +
                        "temperature (K) the automatic startup sequence heats the coolant to (via reactor coolant " +
                        "pump friction heat, see pumpConsumptionPerInteriorCellAtRatedOutput) before it will " +
                        "call requestStartup() -- mirroring a real PWR's own 'heatup' phase, where RCPs bring the " +
                        "RCS to its own 'no-load operating temperature' using pump friction heat ALONE, well " +
                        "before the reactor is ever made critical, so the reactor's own nuclear heating only has " +
                        "to cover the much smaller remaining gap up to full rated temperature. A real PWR's own " +
                        "technical specifications additionally forbid criticality below a hard minimum " +
                        "temperature entirely (this mod enforces the analogous check in isStartupReady()).",
                "Default: 565.0, matching a real PWR's typical ~557 degF (~565 K) no-load operating temperature " +
                        "(comfortably above the analogous real Tech Spec criticality minimum of ~551 degF / " +
                        "~561 K), pending playtesting." })
        @Config.RangeDouble(min = 0.0, max = 5000.0)
        public double heatupTargetTemperatureKelvin = 565.0;

        @Config.Comment({
                "Fraction of heatupTargetTemperatureKelvin the primary coolant must reach before the console's " +
                        "'startup' command is accepted -- mirrors startupCoolantReadyFraction/" +
                        "startupPressureReadyFraction's own established pattern (the plant must actually be " +
                        "physically prepared -- filled, pressurized, AND heated up -- before STARTUP begins).",
                "Default: 0.95." })
        @Config.RangeDouble(min = 0.0, max = 1.0)
        public double startupHeatupReadyFraction = 0.95;

        @Config.Comment({
                "Added 2026-09-28 (user-specified target): flat, always-on EU/t the control console draws (see " +
                        "MetaTileEntityControlConsole#updatePowerConsumption) from whatever INPUT_ENERGY hatch(es) " +
                        "are piped into its own optional energy hatch slot -- representing the console's own " +
                        "display/control electronics load, distinct from (and much smaller than) the linked " +
                        "reactor's own instrumentation and coolant pump draw.",
                "Default: 1024.0." })
        @Config.RangeDouble(min = 0.0, max = 1.0E9)
        public double controlConsoleEuPerTickConsumption = 1024.0;

        @Config.Comment({
                "Core melt fraction (0.0-1.0) at or above which a meltdown escalates to a full explosion even " +
                        "without direct molten-fuel/coolant contact. A gameplay-balance value, not a physically " +
                        "derived one -- real severe-accident explosion energetics (steam/hydrogen explosions) " +
                        "are far too complex to reduce to a single threshold. See " +
                        "com.windows2000.gtnuclear.api.nuclear.accident.SevereAccidentEvaluator.",
                "Default: 0.5 (placeholder pending playtesting)." })
        @Config.RangeDouble(min = 0.0, max = 1.0)
        public double explosionMeltFractionThreshold = 0.5;

        @Config.Comment({
                "Scales explosion magnitude, which is linear in melted fuel mass and core melt fraction -- see " +
                        "com.windows2000.gtnuclear.api.nuclear.accident.SevereAccidentState#explosionMagnitude.",
                "Default: 1.0 (placeholder pending calibration once magnitude is tied to an actual in-world " +
                        "explosion effect)." })
        @Config.RangeDouble(min = 0.0, max = 1.0E9)
        public double explosionMagnitudeCoefficient = 1.0;

        @Config.Comment({
                "Reactivity magnitude (|delta-k/k|) of a single STANDARD control rod when fully inserted, " +
                        "following the standard cosine-shaped integral rod worth curve (see " +
                        "com.windows2000.gtnuclear.api.nuclear.neutronics.ControlRodWorth). The reactor's total " +
                        "control rod reactivity is the sum of every placed control rod controller's own " +
                        "contribution at its current insertion depth. Read once at item registration time (see " +
                        "com.windows2000.gtnuclear.common.item.GTNLItems), not per-tick.",
                "Default: 0.003 (300 pcm), a representative single-rod worth for a PWR-scale control assembly." })
        @Config.RangeDouble(min = 0.0, max = 1.0)
        public double standardControlRodMaxWorth = 0.003;

        @Config.Comment({
                "Same as standardControlRodMaxWorth, but for the HIGH-VALUE control rod type (2026-09-20 " +
                        "decision: two control rod types, standard and high-value).",
                "Default: 0.006 (600 pcm), twice the standard rod's worth -- faster shutdown margin at the cost " +
                        "of faster wear (see this mod's design memory), pending playtesting calibration." })
        @Config.RangeDouble(min = 0.0, max = 1.0)
        public double highValueControlRodMaxWorth = 0.006;

        @Config.Comment({
                "Representative primary coolant mass (kg) held by one interior cell of the reactor's cylindrical " +
                        "cross-section when full -- see " +
                        "com.windows2000.gtnuclear.api.nuclear.thermal.RatedCoolantMass. The reactor's rated " +
                        "('full tank') coolant mass scales with its own size (interior cell count) rather than " +
                        "being a fixed value.",
                "Revised 2026-09-24 (real-playtest-driven, see this mod's design memory, same session as " +
                        "heatExchangerUAWattsPerKelvin/primaryCirculationRateKgPerSecond's own revisions): the old " +
                        "10.0 default gave this mod's own diameter-13/height-10 reference reactor (1010 interior " +
                        "cells) only ~10,100 kg of primary inventory against the newly-calibrated 27,222.4 kg/s " +
                        "circulation rate -- less than one second to fully turn over, versus a real PWR's own " +
                        "~18-20 second turnover (see primaryCirculationRateKgPerSecond's own comment for its " +
                        "reference plant: ~300,000 kg RCS inventory against ~3400 MWth and ~17,000 kg/s flow). " +
                        "New default derived from that same reference plant's inventory/power ratio: " +
                        "mass = ratedThermalPowerPerInteriorCellWatts * (300000 kg / 3.4E9 W) = " +
                        "5.0E6 * 8.82353E-5 =~ 441.1765.",
                "Default: 441.1765 (the reference reactor's rated coolant mass becomes ~445,592 kg, restoring a " +
                        "~16.4 second turnover against the new circulation rate)." })
        @Config.RangeDouble(min = 0.0, max = 1.0E6)
        public double ratedCoolantMassPerInteriorCellKg = 441.1765;

        @Config.Comment({
                "Fraction (0-1) of the reactor's rated coolant mass below which a loss-of-coolant accident (LOCA) " +
                        "is declared -- see com.windows2000.gtnuclear.api.nuclear.accident.LocaDetector.",
                "Default: 0.7 (placeholder pending playtesting)." })
        @Config.RangeDouble(min = 0.0, max = 1.0)
        public double locaCoolantFraction = 0.35;

        @Config.Comment({
                "Number of consecutive ticks the reactor-console link must stay lost before it triggers a SCRAM, " +
                        "debouncing brief network hiccups (e.g. a chunk reload) instead of treating them as a " +
                        "genuine loss of console connection.",
                "Default: 20 (1 in-game second)." })
        @Config.RangeInt(min = 0, max = 72000)
        public int linkLossScramDebounceTicks = 20;

        @Config.Comment({
                "Added 2026-09-28 (user request): number of consecutive ticks the reactor's own coolant pump " +
                        "must be completely unable to draw any EU/t at all (INPUT_ENERGY hatch(es) empty or " +
                        "disconnected) before it triggers a SCRAM -- see ScramReason.LOSS_OF_ELECTRICAL_POWER's " +
                        "own JavaDoc. Debounces brief hiccups the same way as linkLossScramDebounceTicks.",
                "Default: 20 (1 in-game second)." })
        @Config.RangeInt(min = 0, max = 72000)
        public int powerLossScramDebounceTicks = 20;

        @Config.Comment({
                "Primary coolant temperature (Kelvin) at or above which a SCRAM fires automatically. A trip " +
                        "setpoint (gameplay safety margin), distinct from the water saturation/boiling physics in " +
                        "com.windows2000.gtnuclear.api.nuclear.thermal.WaterSaturationCurve.",
                "Revised back 2026-09-26 (user decision) from the 2026-09-24 598.0 value to 620.0: this mod's " +
                        "actual operating target is a primary coolant temperature around 600K (needed to clear " +
                        "the secondary loop's own ~558-560K saturation point at secondaryPressureMPa=7.0 by a " +
                        "wide enough margin for the discrete coolant temperature bands, see " +
                        "com.windows2000.gtnuclear.api.nuclear.thermal.ReactorCoolantTemperatureBands, to reliably " +
                        "drive net heat transfer once the exported band snaps to 600K rather than 550K) -- 598.0 " +
                        "made that target itself unreachable, tripping before the reactor could ever get there. " +
                        "620.0 restores a real ~20K margin above the 600K operating target instead, matching a " +
                        "real PWR's own typical subcooling margin above its normal hot-leg temperature (600.0 " +
                        "operating vs. this mod's own saturation temperature at targetOperatingPressureMPa's 15.5 " +
                        "MPa, ~618K, being close enough that 620.0 is also near that saturation point -- accepted " +
                        "as the tradeoff for reaching the 600K target at all)." })
        @Config.RangeDouble(min = 0.0, max = 5000.0)
        public double maxSafeTemperatureKelvin = 620.0;

        @Config.Comment({
                "Primary coolant pressure (MPa) at or above which a SCRAM fires automatically. Pressure is a " +
                        "plain operator-managed status value (raised/lowered by startup/shutdown sequence " +
                        "operations), not simulated as an ODE -- see this mod's design memory.",
                "Default: 17.0 (above a typical real PWR's ~15.5 MPa operating pressure)." })
        @Config.RangeDouble(min = 0.0, max = 100.0)
        public double maxSafePressureMPa = 17.0;

        @Config.Comment({
                "Added 2026-11-21 (stage 1-1 of this mod's reactor physics rework, see this mod's design memory): " +
                        "time (s) for a fully withdrawn control rod to reach full insertion after a reactor trip -- " +
                        "every rod is driven toward full insertion at a constant speed for as long as the reactor " +
                        "stays TRIPPED, modeling rod cluster control assemblies dropping under gravity once their " +
                        "drive mechanisms release them. 0 makes insertion instantaneous.",
                "Default: 2.2, within a real PWR's technical-specification rod drop time limit (roughly " +
                        "2.2-2.7 s)." })
        @Config.RangeDouble(min = 0.0, max = 60.0)
        public double scramRodDropTimeSeconds = 2.2;

        @Config.Comment({
                "Added 2026-09-24 (real-playtest-driven, see this mod's design memory): relative neutron " +
                        "population (1.0 = rated) at or above which a SCRAM fires automatically -- none of this " +
                        "mod's original SCRAM triggers (temperature/pressure/coolant loss) watch power itself, so " +
                        "a slow reactivity drift (control-loop bug, or an operator simply leaving the reactor " +
                        "slightly supercritical) could climb to several hundred percent of rated power without " +
                        "ever tripping (confirmed via a real playtest: 785% of rated, still climbing, with none " +
                        "of the other SCRAM conditions violated). A real PWR's reactor protection system always " +
                        "includes a high-power trip for exactly this reason.",
                "Default: 1.1 (110% of rated power), a typical real PWR high-power trip margin (commonly cited " +
                        "around 118%; 110% chosen slightly more conservative here pending playtesting)." })
        @Config.RangeDouble(min = 0.0, max = 1.0E6)
        public double maxSafeRelativePower = 1.1;

        @Config.Comment({
                "Initial uranium (heavy metal) mass (kg) per channel layer of a fuel assembly -- an assembly " +
                        "fabricated with length L holds L times this (2026-11-21, stage 1-3 of this mod's reactor " +
                        "physics rework, see this mod's design memory: replaces the former flat per-rod " +
                        "fuelRodUraniumMassKg, which gave a 9-layer and a 36-layer channel the same amount of " +
                        "fuel). Also sets fuel (UO2) heat capacity and melt mass via the UO2/U mass ratio.",
                "Default: 27.0, a real Westinghouse 17x17 assembly (~460 kg U over 3.66 m) per 21.5 cm layer." })
        @Config.RangeDouble(min = 0.0, max = 1.0E6)
        public double fuelUraniumPerLayerKg = 27.0;

        @Config.Comment({
                "Gameplay-balance scaling constant for com.windows2000.gtnuclear.api.nuclear.fuel." +
                        "FuelBurnupEngine's per-tick reaction rate -- multiplies relative power, an isotope's own " +
                        "amount, and its flux-weighted microscopic cross-section (raw barns, not converted to a " +
                        "real macroscopic rate) every tick, so this single coefficient absorbs all of those unit " +
                        "mismatches at once; it is not a physical constant.",
                "Default: 3.0E-10, chosen so a fresh LEU assembly's U-235 depletes over roughly an hour of " +
                        "continuous rated-power operation -- a rough gameplay-pacing target, not derived from a " +
                        "real burnup rate, pending playtesting. The rate is linear in each nuclide's amount, so " +
                        "the stage 1-3 switch from kg to per-initial-heavy-metal-atom units (n_k) leaves the " +
                        "relative depletion speed unchanged." })
        @Config.RangeDouble(min = 0.0, max = 1.0)
        public double fuelBurnupRateCoefficient = 3.0E-10;

        @Config.Comment({
                "Compressed half-life (in-game days) used for the startup neutron source hatch's Californium-252 " +
                        "decay -- a dedicated scale, not the shared com.windows2000.gtnuclear.api.nuclear.decay." +
                        "TimeCompression (calibrated around Thorium-232's real half-life, which would compress " +
                        "Cf-252's real 2.645-year half-life down to only ~9 real-world minutes, far too fast for " +
                        "a component meant to last a meaningful chunk of a play session).",
                "Default: 3.0 (a fresh source is noticeably weaker after a few real-world days of continuous " +
                        "placement, encouraging periodic replacement), pending playtesting." })
        @Config.RangeDouble(min = 0.001, max = 36500.0)
        public double neutronSourceCompressedHalfLifeDays = 3.0;

        @Config.Comment({
                "Cf-252 in a freshly made startup neutron source rod, in MILLIGRAMS (the rod itself stores grams). " +
                        "Since stage 2-1 (2026-11-27) the source strength is physical: 1 mg of Cf-252 emits " +
                        "~2.3E9 neutrons/s (real half-life 2.645 y, 3.09% spontaneous fission, 3.77 neutrons " +
                        "each). Renamed from neutronSourceInitialCf252Grams so old config files do not carry " +
                        "the previous 1 g default over.",
                "Default: 1.0 (1 mg, close to a real PWR primary startup source of ~1E8-1E9 n/s; 1 g would be " +
                        "~1000x stronger and drown out the fuel's own spontaneous fission source)." })
        @Config.RangeDouble(min = 0.0, max = 1.0E6)
        public double neutronSourceInitialCf252Milligrams = 1.0;

        @Config.Comment({
                "DEBUG/VERIFICATION multiplier on the external neutron source term (startup source + fuel " +
                        "spontaneous fission) of point kinetics (stage 2-1). The strength itself is physical " +
                        "(see com.windows2000.gtnuclear.api.nuclear.neutronics.NeutronSourceStrength); 1.0 keeps " +
                        "it so. Replaces neutronSourceStrengthCoefficient, which converted the old one-off seed.",
                "Default: 1.0" })
        @Config.RangeDouble(min = 0.0, max = 1.0E12)
        public double neutronSourceStrengthMultiplier = 1.0;

        @Config.Comment({
                "Relative neutron population below which a planned shutdown (SHUTTING_DOWN stage) is considered " +
                        "complete, moving the reactor to SHUTDOWN -- the other condition (every control rod fully " +
                        "inserted) is checked directly, not configurable.",
                "Default: 1.0E-3 (0.1% of rated power)." })
        @Config.RangeDouble(min = 0.0, max = 1.0)
        public double shutdownCompletePowerThreshold = 1.0E-3;

        @Config.Comment({
                "Fraction (0-1) of the reactor's rated coolant mass (see " +
                        "com.windows2000.gtnuclear.api.nuclear.thermal.RatedCoolantMass) at or below which the " +
                        "console's 'begin coolant injection' command is accepted -- above this fraction, the " +
                        "command is a no-op (matches the LOCA trip threshold's own default, since by that point " +
                        "the reactor would already have SCRAMmed anyway, but is a separate, independently " +
                        "tunable value -- see locaCoolantFraction).",
                "Default: 0.7." })
        @Config.RangeDouble(min = 0.0, max = 1.0)
        public double coolantInjectionTriggerFraction = 0.7;

        @Config.Comment({
                "Primary coolant injection rate (kg/s) once the console's 'begin coolant injection' command is " +
                        "active -- draws from whatever is available in the reactor coolant import/emergency " +
                        "import hatches' tanks (1000 mB = 1 kg), capped at this rate even if more is available.",
                "Revised 2026-09-24 alongside ratedCoolantMassPerInteriorCellKg's own real-PWR-driven increase " +
                        "(10.0 -> 441.1765, a 44.11765x jump) -- scaled up by the same factor so a startup fill " +
                        "still takes about the same number of in-game seconds as before, rather than the fill " +
                        "time growing 44x alongside the now much larger rated coolant mass.",
                "Default: 4411.765 (100.0 * 44.11765) -- e.g. the reference reactor's ~445,592 kg rated mass " +
                        "still fills in ~100 seconds, same pacing as the old 100kg/s-against-10,100kg case." })
        @Config.RangeDouble(min = 0.0, max = 1.0E9)
        public double coolantInjectionRateKgPerSecond = 4411.765;

        @Config.Comment({
                "Primary coolant pressure (MPa) the console's 'begin pressurization' command ramps toward -- a " +
                        "provisional (2026-09-21) placeholder value pending this mod's secondary coolant loop / " +
                        "heat exchanger design, at which point pressure and temperature's real implementation " +
                        "(currently both still plain operator-managed status values, not physically simulated) " +
                        "will very likely need revisiting together, per this mod's design memory.",
                "Default: 15.5 (a typical real PWR operating pressure, comfortably below maxSafePressureMPa's " +
                        "own default of 17.0)." })
        @Config.RangeDouble(min = 0.0, max = 100.0)
        public double targetOperatingPressureMPa = 15.5;

        @Config.Comment({
                "Rate (MPa/s) the console's 'begin pressurization'/'begin depressurization' commands ramp " +
                        "pressureMPa at, toward targetOperatingPressureMPa or 0 respectively -- see that field's " +
                        "own comment for why this whole mechanism is a provisional placeholder.",
                "Default: 0.15 (0 to targetOperatingPressureMPa's default 15.5 in just over 100 seconds), " +
                        "pending playtesting." })
        @Config.RangeDouble(min = 0.0, max = 1.0E6)
        public double pressureAdjustmentRateMPaPerSecond = 0.15;

        @Config.Comment({
                "Boron concentration (ppm) of the full-strength boric acid solution injected by the console's " +
                        "'begin boron concentration' command (see com.windows2000.gtnuclear.api.unification." +
                        "materials.reactor.ReactorFluidMaterials.BoricAcidSolution) -- a concentrated stock " +
                        "solution mixed into the primary coolant, not the coolant's own operating range (see " +
                        "BoronReactivity/equilibriumXenonWorth for realistic in-coolant ppm scales).",
                "Default: 10000.0 (an order of magnitude above typical in-coolant operating levels, so a modest " +
                        "injection meaningfully moves boronPpm), pending playtesting." })
        @Config.RangeDouble(min = 0.0, max = 1.0E6)
        public double boricAcidSolutionConcentrationPpm = 10000.0;

        @Config.Comment({
                "Revised 2026-09-26 (user request) from a flat mb/s rate to this time-based knob: the in-game " +
                        "minutes for one full coolant-pool turnover (injecting a volume of pure water/boric acid " +
                        "solution equal to the reactor's own computeRatedCoolantMassKg()) of boron dilution/" +
                        "concentration input. The actual mb/s rate is derived fresh every tick as " +
                        "computeRatedCoolantMassKg() / (boronAdjustmentDurationMinutes * 60), the same " +
                        "'specify the target pacing, derive the throughput per-reactor-size' pattern as " +
                        "computeRatedThermalPowerWatts()/computeRatedCoolantMassKg() themselves -- unlike the flat " +
                        "mb/s rate this replaces, this stays meaningful regardless of reactor size instead of only " +
                        "being calibrated for one specific reference reactor.",
                "This also happens to be the boron mixing tank's own exponential time constant tau = V/Q (see " +
                        "this mod's design memory, 2026-09-17, mass-balance mixing formula): boronPpm relaxes " +
                        "toward the input concentration as ~(1 - e^(-t/tau)), so this is directly the 'how long " +
                        "until boron adjustment meaningfully takes effect' duration a player would actually feel, " +
                        "not just an abstract flow-rate number.",
                "Superseded 2026-09-26 (real-PWR-data-driven) determination that boron adjustment needed to be " +
                        "compressed by roughly XenonDeadTimeCalibration.scalingFactorFor(1800.0) = 52.2336x to " +
                        "keep pace with this mod's own already-compressed xenon/iodine dynamics -- see this mod's " +
                        "design memory for the full derivation (real ~14-20 hour normal PWR boration time vs. " +
                        "this mod's own ~26-hour-compressed-to-1800s xenon poison-out dead time).",
                "Briefly widened 10x, 2026-09-28 (precautionary, alongside boronWorthPerPpm's own correction from " +
                        "an unintentional 1 pcm/ppm up to its originally-intended 10 pcm/ppm), then reverted back " +
                        "the same day once a real playtest showed the widened duration was actually a mistake: the " +
                        "52.2336x xenon-matching derivation above was itself already worked out assuming boron's " +
                        "own correctly-intended 10 pcm/ppm worth, so once boronWorthPerPpm was corrected to match " +
                        "that intent, this duration needed no change at all to reach its own originally-derived " +
                        "pcm/s pace -- widening it alongside the worth fix merely reintroduced the same 10x-too-" +
                        "slow pcm/s response the worth bug had been causing all along, just via a different knob. " +
                        "Confirmed via a real playtest, 2026-09-28: after a long-delayed criticality recovery (see " +
                        "this mod's design memory on point kinetics population decaying to ~1e-97 during an " +
                        "extended subcritical period) finally let power climb to ~4.16%, a xenon transient grew " +
                        "at roughly +1.0 pcm/s while bank 0 was already fully withdrawn (0% insertion, no rod " +
                        "margin left) -- at the widened 250.0-minute duration, boron could only claw back about " +
                        "+0.128 pcm/s, an order of magnitude too slow to keep pace, and power was declining. " +
                        "Reverting to 25.0 minutes (at the now-correct 10 pcm/ppm worth) yields roughly +1.28 " +
                        "pcm/s instead -- enough to outpace that same transient.",
                "Default: 25.0, pending further playtesting now that both boronWorthPerPpm and this duration are " +
                        "each individually correct again." })
        @Config.RangeDouble(min = 0.001, max = 1.0E6)
        public double boronAdjustmentDurationMinutes = 25.0;

        @Config.Comment({
                "Fraction (0-1) of the reactor's rated coolant mass that must be filled before the console's " +
                        "'startup' command is accepted -- stage 6 of this mod's startup/shutdown sequence work " +
                        "(2026-09-21): the plant must actually be physically prepared (coolant filled, pressurized) " +
                        "before the STARTUP stage begins; boron dilution and control rod withdrawal happen " +
                        "afterward, within that stage, not as a precondition to entering it.",
                "Default: 0.95." })
        @Config.RangeDouble(min = 0.0, max = 1.0)
        public double startupCoolantReadyFraction = 0.95;

        @Config.Comment({
                "Fraction (0-1) of targetOperatingPressureMPa that pressureMPa must reach before the console's " +
                        "'startup' command is accepted -- see startupCoolantReadyFraction's own comment.",
                "Default: 0.95." })
        @Config.RangeDouble(min = 0.0, max = 1.0)
        public double startupPressureReadyFraction = 0.95;

        @Config.Comment({
                "Reactor thermal power output (W) at relative power 1.0 (rated), per interior cell of the " +
                        "reactor's own core geometry (see com.windows2000.gtnuclear.api.nuclear.thermal." +
                        "RatedCoolantMass's own per-cell scaling pattern, applied identically here) -- scales the " +
                        "core's total rated thermal power with reactor size, same spirit as " +
                        "ratedCoolantMassPerInteriorCellKg.",
                "Default: 5.0E6 (5 MW per interior cell), pending playtesting -- not derived from a real power " +
                        "density calculation." })
        @Config.RangeDouble(min = 0.0, max = 1.0E12)
        public double ratedThermalPowerPerInteriorCellWatts = 5.0E6;

        @Config.Comment({
                "Fuel-to-primary-coolant heat transfer coefficient (W/K) per interior cell -- scales with " +
                        "reactor size the same way as ratedThermalPowerPerInteriorCellWatts. Governs how quickly " +
                        "heat generated in the fuel node reaches the coolant node; a lower value lets fuel " +
                        "temperature run further ahead of coolant temperature during a transient (the mechanism " +
                        "that makes a meltdown possible even while coolant itself stays comparatively cool).",
                "Revised 2026-09-28 (real-PWR-data-driven, see this mod's design memory) from an earlier 5.0E5: " +
                        "that value implied a mere 10K fuel-coolant temperature difference at rated power " +
                        "(ratedThermalPowerPerInteriorCellWatts / 5.0E5 = 10), drastically underestimating real " +
                        "UO2 fuel temperature -- real PWR fuel runs hundreds to over a thousand K above coolant " +
                        "(peak centerline ~1540 degC/~1813K against coolant ~280-300 degC/~553-573K is one cited " +
                        "example; even a more modest 'average fuel pin' figure sits well above coolant). Since " +
                        "this class tracks a single lumped fuel node rather than a radial profile, matching the " +
                        "extreme centerline figure would be inappropriate; 150K is instead used as a deliberately " +
                        "moderate representative average-fuel-temperature elevation, still a large qualitative " +
                        "correction over the previous 10K. New value: ratedThermalPowerPerInteriorCellWatts / " +
                        "150K = 5.0E6 / 150 =~ 33333.33.",
                "Default: 33333.33, pending playtesting -- the real average-vs-centerline distinction makes this " +
                        "still an approximation, not a precise derivation." })
        @Config.RangeDouble(min = 0.0, max = 1.0E12)
        public double fuelCoolantHeatTransferPerInteriorCellWattsPerKelvin = 33333.33;

        @Config.Comment({
                "Reactivity (pcm) contributed by the primary coolant reaching 100% void fraction (fully flashed " +
                        "to steam, see primaryVoidFraction) -- negative, modeling a PWR's inherent void feedback " +
                        "(loss of moderation as coolant voids). Scaled linearly by the current void fraction and " +
                        "added to total reactivity every tick.",
                "Default: -15000.0 (-150 pcm per % void), the order of magnitude of a real PWR void coefficient." })
        @Config.RangeDouble(min = -1.0E7, max = 0.0)
        public double voidReactivityCoefficientPcm = -15000.0;

        @Config.Comment({
                "Added 2026-09-24 (real-playtest-driven, see this mod's design memory): moderator temperature " +
                        "coefficient of reactivity (pcm/K) -- negative, modeling a real PWR's continuous " +
                        "self-limiting feedback as primary coolant (the moderator) warms and its density drops, " +
                        "reducing moderation. Unlike voidReactivityCoefficientPcm (which only engages once bulk " +
                        "boiling actually starts), this is this reactor's first line of defense, acting " +
                        "continuously across the entire normal operating temperature range, not just once things " +
                        "are already going wrong. Its absence was confirmed via real playtest logs to let a merely " +
                        "slightly-supercritical reactor climb to 785% of rated power over ~2.5 hours without any " +
                        "inherent physical brake, since this mod's CMFD shape reactivity was previously " +
                        "temperature-independent entirely.",
                "Reinterpreted 2026-09-28 (real-playtest-driven, see this mod's design memory and " +
                        "ModeratorTemperatureReactivity's own JavaDoc): originally applied as a flat constant " +
                        "(this value * (temperatureKelvin - 300.0)) across the entire heatup range, which over-" +
                        "integrates a real MTC's own behavior (near zero/cold-boron-diluted conditions, growing " +
                        "steadily more negative only as temperature actually approaches hot/operating conditions) " +
                        "-- a real playtest found the flat model alone produced a -7050 pcm burden at just 535K, " +
                        "leaving criticality unreachable even with boron fully diluted to 0 ppm, against a real " +
                        "PWR's own typical total cold-to-hot temperature defect of only about -1000 to -2500 pcm. " +
                        "This value is now instead the INSTANTANEOUS slope at " +
                        "ConfigHolder.reactor.heatupTargetTemperatureKelvin specifically (not a flat constant), " +
                        "with ModeratorTemperatureReactivity integrating a linearly-growing-magnitude slope from " +
                        "300K up to that reference temperature into a quadratic reactivity curve -- exactly half " +
                        "of the old flat model's value at the reference temperature itself, and proportionally " +
                        "much less below it.",
                "Default: -30.0, within the real PWR range (roughly -10 to -50 pcm/K depending on core life and " +
                        "boron concentration) as an instantaneous near-hot-temperature slope, pending further " +
                        "playtesting now that the curve shape itself has changed." })
        @Config.RangeDouble(min = -1.0E7, max = 1.0E7)
        public double moderatorTemperatureCoefficientPcmPerKelvin = -30.0;

        @Config.Comment({
                "Added 2026-09-26 (real-PWR-data-driven, see this mod's design memory): Doppler (prompt fuel " +
                        "temperature) coefficient of reactivity (pcm/K) -- negative, modeling fuel resonance " +
                        "capture broadening as fuel temperature itself rises. Applied every tick as " +
                        "dopplerCoefficientPcmPerKelvin * (fuelTemperatureKelvin - 300.0), the same reference-" +
                        "temperature convention as moderatorTemperatureCoefficientPcmPerKelvin. Unlike that " +
                        "coefficient (which responds to coolant temperature, itself lagging fuel temperature " +
                        "through fuelCoolantHeatTransferPerInteriorCellWattsPerKelvin's own heat-transfer delay), " +
                        "this responds to fuel temperature directly and thus within the same tick fission power " +
                        "itself changes -- a real PWR's actual fastest-acting negative feedback, present from the " +
                        "instant fuel starts heating rather than only once that heat has propagated to the " +
                        "coolant. Its prior absence meant this reactor's only self-limiting feedback during a " +
                        "fast transient was the slower, delayed moderator term.",
                "Default: -3.0, within the real PWR Doppler coefficient range (roughly -2 to -5 pcm/degC), pending " +
                        "playtesting." })
        @Config.RangeDouble(min = -1.0E7, max = 1.0E7)
        public double dopplerCoefficientPcmPerKelvin = -3.0;

        @Config.Comment({
                "Rate (fraction of 1.0 per second) primaryVoidFraction decays back toward 0 once primary coolant " +
                        "temperature drops back below its own saturation temperature (condensing back to liquid).",
                "Default: 0.05 (a full void collapses back to 0 in about 20 seconds once subcooled), pending " +
                        "playtesting." })
        @Config.RangeDouble(min = 0.0, max = 1.0E6)
        public double voidCondenseRatePerSecond = 0.05;

        @Config.Comment({
                "Primary coolant circulation rate (kg/s) pumped between the reactor and a piped heat exchanger " +
                        "-- see this mod's design memory (2026-09-22): primary coolant physically circulates via " +
                        "real fluid pipes (REACTOR_COOLANT_IMPORT/EXPORT hatches on both multiblocks) rather than " +
                        "a logical link, carrying its own temperature as FluidStack NBT.",
                "Revised 2026-09-24 (real-playtest-driven, see this mod's design memory): raising " +
                        "heatExchangerUAWattsPerKelvin alone (same playtest) only marginally delayed voiding -- a " +
                        "second real-playtest log showed the reactor still reaching saturation at ~26% relative " +
                        "power, because this rate, not heat exchanger UA, was the actual bottleneck: at the old " +
                        "500.0 default, even an infinitely good heat exchanger can move at most " +
                        "500 * 5300 J/kg*K * 60K (the largest plausible primary-to-secondary delta near " +
                        "saturation) =~ 159 MW -- only ~3% of this mod's own diameter-13/height-10 reference " +
                        "reactor's 5.05 GW rated thermal power. New default derived the same way as " +
                        "heatExchangerUAWattsPerKelvin, from a real PWR's own primary flow/power ratio (a 4-loop " +
                        "plant's ~17,000 kg/s total flow against ~3400 MWth and a ~35K core delta-T works out to " +
                        "~200 kJ removed per kg circulated): " +
                        "flow = (reference reactor's rated thermal power) / (specific heat * 35K) = " +
                        "5.05E9 / (5300 * 35) =~ 27222.4.",
                "Default: 27222.4. Same scale-up story as heatExchangerUAWattsPerKelvin: a bigger player-built " +
                        "reactor needs proportionally more total flow, provided by piping multiple heat exchanger " +
                        "loops in parallel rather than scaling this single flat value." })
        @Config.RangeDouble(min = 0.0, max = 1.0E9)
        public double primaryCirculationRateKgPerSecond = 27222.4;

        @Config.Comment({
                "Heat exchanger overall heat transfer coefficient (W/K, i.e. UA in Q=UA*deltaT) for ONE heat " +
                        "exchanger multiblock -- flat, not scaled by structure size, since the heat exchanger's " +
                        "own multiblock shape is still a placeholder (see MetaTileEntityHeatExchanger's own " +
                        "JavaDoc) and is deliberately reactor-independent (no reference to which reactor it's " +
                        "piped to, matching how a real steam generator is its own separately-sized component, " +
                        "not derived from the reactor core's own dimensions).",
                "Revised 2026-09-24 (real-playtest-driven, see this mod's design memory): the previous default " +
                        "(1.0E6) was confirmed via an actual playtest log to be roughly 100-200x too small for " +
                        "even this mod's own diameter-13/height-10 all-LEU calibration reactor (5.05 GW rated " +
                        "thermal) -- primary coolant reached saturation and voided at only ~23% relative power " +
                        "despite active circulation, because even a fully-engaged heat exchanger could remove at " +
                        "most ~1.0E6 * (a few hundred K) ~ a few hundred MW, nowhere near enough. New default " +
                        "derived from a real PWR steam generator design target: size ONE heat exchanger's UA so " +
                        "that, piped to that same reference reactor at its full rated power, primary temperature " +
                        "settles about 30K below saturation (a real PWR's own typical full-power subcooling " +
                        "margin, matching maxSafeTemperatureKelvin's own revised reasoning) -- " +
                        "UA = (reference reactor's rated thermal power) / 30K = " +
                        "(1010 interior cells * 5.0E6 W/cell) / 30 =~ 1.6833E8.",
                "Default: 1.6833E8. A larger player-built reactor (more interior cells, proportionally more " +
                        "rated thermal power) needs correspondingly more aggregate UA than one heat exchanger " +
                        "provides -- piping multiple heat exchangers in parallel off the same reactor (their " +
                        "REACTOR_COOLANT_IMPORT/EXPORT hatches all drawing from/returning to the same primary " +
                        "loop) is the intended way to scale up, exactly mirroring how a real large PWR uses " +
                        "multiple steam generators (typically 2-4) rather than one oversized unit." })
        @Config.RangeDouble(min = 0.0, max = 1.0E12)
        public double heatExchangerUAWattsPerKelvin = 1.6833E8;

        @Config.Comment({
                "Heat exchanger's own primary-side coolant buffer capacity (kg) -- flat, not size-scaled, same " +
                        "reasoning as heatExchangerUAWattsPerKelvin. Caps how much MetaTileEntityHeatExchanger#" +
                        "receivePrimaryCoolant() will drain from its own REACTOR_COOLANT_IMPORT hatch per tick, " +
                        "same purpose as the reactor's own computeRatedCoolantMassKg() cap on its own " +
                        "updatePrimaryCirculation() -- without a cap, continuously pouring coolant into this " +
                        "hatch by hand grows the buffer without bound (confirmed via a real playtest report, " +
                        "2026-09-22, of the reactor's own analogous unbounded growth).",
                "Revised 2026-09-24 alongside primaryCirculationRateKgPerSecond's own real-PWR-driven increase " +
                        "(500.0 -> 27222.4, a 54.4448x jump) -- scaled up by the same factor so this buffer's own " +
                        "turnover time (buffer / flow rate) stays the same ~10 seconds as before, rather than " +
                        "shrinking to an unrealistic fraction of a second at the new flow rate.",
                "Default: 272224.0 (5000.0 * 54.4448), pending playtesting." })
        @Config.RangeDouble(min = 0.0, max = 1.0E9)
        public double heatExchangerPrimarySideRatedMassKg = 272224.0;

        @Config.Comment({
                "Secondary loop operating pressure (MPa) -- a fixed design assumption (the heat exchanger has no " +
                        "console/operator control over it, unlike the primary loop's pressureMPa), noticeably " +
                        "lower than a real PWR's primary pressure, matching real secondary-side steam generator " +
                        "practice.",
                "Default: 7.0 (real PWR secondary side is typically ~6-7 MPa); GTNLMaterials.HighPressureSteam's " +
                        "own registered temperature (560K) is chosen to roughly match this pressure's saturation " +
                        "temperature." })
        @Config.RangeDouble(min = 0.0, max = 100.0)
        public double secondaryPressureMPa = 7.0;

        @Config.Comment({
                "Secondary loop working water inventory (kg) the heat exchanger tops itself up to automatically " +
                        "from its feedwater import hatch -- a level-control abstraction (real steam generators " +
                        "maintain a roughly constant water level), not itself size-scaled since the heat " +
                        "exchanger's own shape is still a placeholder (see heatExchangerUAWattsPerKelvin's own " +
                        "comment).",
                "Default: 2000.0, pending playtesting." })
        @Config.RangeDouble(min = 0.0, max = 1.0E9)
        public double secondaryWaterRatedMassKg = 2000.0;

        @Config.Comment({
                "Minimum feedwater intake rate (kg/s) the heat exchanger draws even while secondaryWaterMassKg is " +
                        "already at secondaryWaterRatedMassKg -- mixed in and the resulting excess mass immediately " +
                        "discarded (a blowdown abstraction) so the secondary side keeps slowly cycling toward the " +
                        "cool feedwater's own 300K rather than permanently freezing at whatever temperature it last " +
                        "reached. Without this, a real playtest (2026-09-26) showed a genuine dead-lock: once " +
                        "secondaryWaterMassKg reaches its rated cap, replenishFeedwater() used to refuse all further " +
                        "intake, so a secondary side that had ever gotten hotter than the primary loop's current " +
                        "temperature could never cool back down (no heat was ever flowing to boil it off, and no " +
                        "fresh water could displace it either) -- qRemovedWatts stayed exactly 0.0 for the entire " +
                        "2494-second log.",
                "Default: 20.0 (1% of the current 2000.0 kg secondaryWaterRatedMassKg per second, i.e. a full " +
                        "turnover in ~100 seconds), pending playtesting; expected to be revisited alongside " +
                        "secondaryWaterRatedMassKg's own eventual real-PWR-based recalibration." })
        @Config.RangeDouble(min = 0.0, max = 1.0E6)
        public double feedwaterTrickleRateKgPerSecond = 20.0;

        @Config.Comment({
                "Maximum rate (K/s) MetaTileEntityHeatExchanger#coolPrimaryAndPumpBack() lets " +
                        "secondaryWaterTemperatureKelvin rise while it is still below its own saturation point -- " +
                        "caps how much heat that method actually removes from the primary side during this " +
                        "sub-cooled warming phase, regardless of heatExchangerUAWattsPerKelvin.",
                "Added 2026-09-28 (real-playtest-driven, see this mod's design memory): heatExchangerUAWattsPerKelvin " +
                        "is calibrated for the reference reactor's full rated fission power (5.05GW at 30K design " +
                        "margin), so during pre-criticality heatup (reactor coolant pump friction heat alone, a small " +
                        "fraction of rated power) even a handful of Kelvin of primary-secondary difference already " +
                        "matches the pump's entire heat input, pinning primary coolant near ambient indefinitely -- a " +
                        "real playtest observed exactly this, stalled at 305K, matching this mechanism almost exactly " +
                        "by hand calculation. Mirrors a real PWR's own administrative heatup rate limit (imposed on " +
                        "both loops specifically to avoid thermal shock, commonly on the order of ~50 degF/hr =~ " +
                        "0.0077 K/s in reality). This cap only ever applies while secondaryWaterTemperatureKelvin is " +
                        "sub-cooled; once it reaches saturation, coolPrimaryAndPumpBack() removes the cap entirely so " +
                        "full-power steam production is never throttled by it -- a real plant's secondary side " +
                        "reaches saturation by the time primary heatup completes (the whole point of \"no-load " +
                        "operating temperature\"), so this cap should no longer bind by the time real fission power " +
                        "ever arrives, provided it is tuned to keep pace with primaryCirculationRateKgPerSecond/" +
                        "euPerTickToWattsConversion's own heatup pacing.",
                "Also degrades gracefully to zero while secondaryWaterMassKg is itself zero (no feedwater ever " +
                        "supplied) -- since the cap scales with that mass, an empty secondary side now correctly " +
                        "removes no heat at all, rather than acting as an infinite sink pinned at its own stale " +
                        "initial temperature.",
                "Raised from an initial 0.05, 2026-09-29 (real-playtest-driven, see this mod's design memory): the " +
                        "0.05 default accidentally landed almost exactly at feedwaterTrickleRateKgPerSecond's own " +
                        "opposing cooling pull, permanently deadlocking secondaryWaterTemperatureKelvin at a fixed " +
                        "equilibrium point well below saturation -- this cap's own warming (a FLAT rate, " +
                        "independent of temperature) versus the trickle's own relaxation-toward-300K pull (which " +
                        "GROWS with how far above 300K secondary already is) settle at " +
                        "T_eq = 300 + capRateKelvinPerSecond * secondaryWaterRatedMassKg / " +
                        "feedwaterTrickleRateKgPerSecond -- at the old 0.05 default this evaluates to exactly " +
                        "305K, matching a real playtest that found secondaryWaterTemperatureKelvin (and " +
                        "qRemovedWatts, pinned at exactly 530000.0 W) frozen there for 1135+ real seconds even " +
                        "with the reactor already at real fission power (16%+ and climbing), never once reaching " +
                        "saturation -- meaning the whole 'this cap lifts once saturated' design this field's own " +
                        "earlier comment describes could structurally never trigger. Solved by aiming T_eq " +
                        "comfortably above saturation (558K at secondaryPressureMPa's own 7.0 default) instead: " +
                        "3.0 K/s implies T_eq = 300 + 3.0*2000/20 = 600K, well above 558K, while its own " +
                        "capWatts (secondaryWaterRatedMassKg * REPRESENTATIVE_SPECIFIC_HEAT_CAPACITY * 3.0 =~ " +
                        "31.8 MW) remains comfortably below reactor coolant pump heat during pre-criticality " +
                        "heatup (~695 MW in that same real playtest reference reactor), so this cap's original " +
                        "purpose (protecting a weak RCP-only heat source from an oversized full UA) is preserved.",
                "Default: 3.0, pending further playtesting -- like heatExchangerUAWattsPerKelvin, not size-scaled; " +
                        "a larger player-built reactor is expected to pipe proportionally more heat exchangers in " +
                        "parallel, each contributing its own capped secondary-side heatup capacity alongside its " +
                        "own share of UA." })
        @Config.RangeDouble(min = 0.0, max = 1.0E6)
        public double heatExchangerMaxSecondaryHeatupRateKelvinPerSecond = 3.0;

        @Config.Comment({
                "Primary coolant void fraction (0-1, see primaryVoidFraction) above which " +
                        "SevereAccidentEvaluator's abnormalCoolingOrControl condition is considered true (alongside " +
                        "LOCA and structural housing mismatch) -- even modest unexpected voiding is a real safety " +
                        "margin precursor condition, not something to wait until it gets severe.",
                "Default: 0.1 (10% void), pending playtesting." })
        @Config.RangeDouble(min = 0.0, max = 1.0)
        public double severeAccidentVoidFractionThreshold = 0.1;

        @Config.Comment({
                "Scales SevereAccidentState#explosionMagnitude (itself explosionMagnitudeCoefficient * melted " +
                        "fuel mass kg * core melt fraction, an already gameplay-balance formula -- see its own " +
                        "JavaDoc) down into a Minecraft explosion strength (vanilla TNT = 4, a Creeper = 3) -- an " +
                        "additional gameplay-balance scale, not a real energetics conversion.",
                "Default: 0.05, pending playtesting." })
        @Config.RangeDouble(min = 0.0, max = 1.0E6)
        public double explosionStrengthScale = 0.05;

        @Config.Comment({
                "Hard cap on the explosion strength computed via explosionStrengthScale, regardless of how much " +
                        "fuel melted -- a safety valve against an unbounded-strength explosion at extreme melt " +
                        "masses.",
                "Default: 100.0 (already far beyond vanilla TNT's 4), pending playtesting." })
        @Config.RangeDouble(min = 0.0, max = 1.0E6)
        public double maxExplosionStrength = 100.0;

        @Config.Comment({
                "Whether the severe accident EXPLOSION consequence destroys terrain (World#newExplosion's own " +
                        "isSmoking parameter) -- set false for a purely cosmetic/damage-only explosion if terrain " +
                        "destruction is undesirable.",
                "Default: true." })
        public boolean explosionDestroysBlocks = true;

        @Config.Comment({
                "Replaced 2026-09-29 (real-playtest-driven, see this mod's design memory) from a flat pcm target " +
                        "(autoControlAscensionReactivityTargetPcm, formerly 300.0) to this fraction: " +
                        "ReactorAutoControlSequencer#computeAutoRodControlTargetPcm derives the actual " +
                        "target as currentReactivityPcm + this fraction * (bank 0's own remaining withdrawal " +
                        "ceiling -- the reactivity full withdrawal from the current insertion depth would still " +
                        "release, computed from whatever rod worth this specific reactor actually has built, not " +
                        "a flat number). The old flat target was tuned against one reference reactor and predated " +
                        "the moderator-temperature-coefficient's own quadratic recalibration entirely, so it could " +
                        "demand more reactivity than was physically reachable even at 0% insertion -- a real " +
                        "playtest confirmed the loop then spins a permanently unreachable 'withdraw' command " +
                        "forever once pinned at that floor. This fraction-of-remaining-ceiling design is " +
                        "self-limiting instead: as insertion approaches 0% the remaining ceiling itself shrinks " +
                        "toward zero, so the target converges to current reactivity right along with it, and the " +
                        "loop settles into 'hold' rather than continuing to chase the unreachable.",
                "Default: 0.5 (aim to use up half of whatever withdrawal margin currently remains) -- deliberately " +
                        "aggressive relative to autoControlHoldMarginFraction's own default, to still climb " +
                        "decisively out of source range, pending playtesting." })
        @Config.RangeDouble(min = 0.0, max = 1.0)
        public double autoControlAscensionMarginFraction = 0.5;

        @Config.Comment({
                "Experimental (2026-09-24): relative power (0-1) at or above which the automatic rod control loop " +
                        "switches from autoControlAscensionMarginFraction to autoControlHoldMarginFraction instead " +
                        "-- i.e. from 'climb toward rated power' mode to 'track xenon at rated power' mode.",
                "Default: 0.9." })
        @Config.RangeDouble(min = 0.0, max = 1.0)
        public double autoControlHoldModePowerThreshold = 0.9;

        @Config.Comment({
                "Replaced 2026-09-29 (real-playtest-driven, see this mod's design memory) from a flat pcm target " +
                        "(autoControlHoldReactivityTargetPcm, formerly 100.0) to a fraction of bank 0's own " +
                        "remaining withdrawal ceiling -- see autoControlAscensionMarginFraction's own JavaDoc for " +
                        "the shared formula and the real-playtest failure mode (a permanently unreachable flat " +
                        "target) this replaces.",
                "Default: 0.1, deliberately gentler than autoControlAscensionMarginFraction's own 0.5 -- once " +
                        "already near rated power, a small cushion is enough to absorb a still-climbing xenon " +
                        "transient without chasing an aggressive target that risks overshoot, pending " +
                        "playtesting." })
        @Config.RangeDouble(min = 0.0, max = 1.0)
        public double autoControlHoldMarginFraction = 0.1;

        @Config.Comment({
                "Ceiling (in dollars, rho/beta) on the auto control's rod reactivity target (2026-11-28, replacing " +
                        "autoControlMaxTargetReactivityPcm = 300 pcm, which was 0.22 $ with the old mole-fraction " +
                        "beta but 0.46 $ -- a ~7 s period -- once stage 2-2 corrected beta to ~0.0065). Expressed " +
                        "in dollars so it keeps its meaning whatever the fuel's beta is.",
                "Default: 0.15 (~100 pcm for fresh LEU)" })
        @Config.RangeDouble(min = 0.0, max = 1.0)
        public double autoControlMaxTargetReactivityDollars = 0.15;

        @Config.Comment({
                "Startup-rate limiter (2026-11-28): while the measured reactor period is positive and shorter than " +
                        "this (s), the auto control withdraws no rods and stops boron dilution -- an operator " +
                        "keeping the startup rate within limits on the period meter.",
                "Default: 60.0" })
        @Config.RangeDouble(min = 0.0, max = 1.0E6)
        public double autoControlMinWithdrawalPeriodSeconds = 60.0;

        @Config.Comment({
                "Startup-rate limiter (2026-11-28): while the measured reactor period is positive and shorter than " +
                        "this (s), the auto control inserts rods regardless of its reactivity target.",
                "Default: 20.0" })
        @Config.RangeDouble(min = 0.0, max = 1.0E6)
        public double autoControlInsertPeriodSeconds = 20.0;

        @Config.Comment({
                "Added 2026-09-27 (real-playtest-driven, see this mod's design memory): relative power (0-1, " +
                        "1.0 = 100% rated) above which the automatic rod control loop's target reactivity gets an " +
                        "increasingly large negative override (see autoControlPowerCeilingPenaltyPcmPerUnit), " +
                        "regardless of whether it is currently in ascension or hold mode. A real playtest found " +
                        "power creeping past 100% (up to ~106%) purely because reactivity stayed positive (from " +
                        "either mode's own nominal target) with nothing telling the loop to actively push back " +
                        "once already at/above rated power -- the resulting brief overshoot's own fuel " +
                        "temperature spike (445K to 473K in under 90 seconds in that log) then overwhelmed the " +
                        "rod loop's remaining withdrawal margin faster than boron could compensate, the " +
                        "proximate trigger of that run's rod-exhaustion collapse.",
                "Default: 1.0 (100% rated power)." })
        @Config.RangeDouble(min = 0.0, max = 10.0)
        public double autoControlPowerCeilingThreshold = 1.0;

        @Config.Comment({
                "Slope (pcm per unit of relative power above autoControlPowerCeilingThreshold) of the target " +
                        "reactivity override described in that field's own JavaDoc -- e.g. at the default 6000.0, " +
                        "power sitting just 1% over the ceiling (0.01) already overrides the target by -60 pcm, " +
                        "and 5% over (0.05) by -300 pcm, comfortably overwhelming autoControlHoldMarginFraction's " +
                        "own typically-modest positive cushion and driving the target solidly negative so the loop " +
                        "actively inserts rather than merely easing off.",
                "Default: 6000.0, pending playtesting." })
        @Config.RangeDouble(min = 0.0, max = 1.0E7)
        public double autoControlPowerCeilingPenaltyPcmPerUnit = 6000.0;

        @Config.Comment({
                "Experimental (2026-09-24): the automatic rod control loop takes no action while reactivity is " +
                        "within this many pcm of its own computed target (see autoControlAscensionMarginFraction) " +
                        "during the ascension phase (relative power still below autoControlHoldModePowerThreshold) " +
                        "-- a wide margin here is fine (even desirable, for a quick climb); see " +
                        "autoControlHoldDeadbandPcm for why hold mode needs a much tighter value instead.",
                "Default: 50.0." })
        @Config.RangeDouble(min = 0.0, max = 1.0E6)
        public double autoControlDeadbandPcm = 50.0;

        @Config.Comment({
                "Added 2026-09-24 (real-playtest-driven, see this mod's design memory): a separate, much tighter " +
                        "deadband used once in hold mode (relative power at/above autoControlHoldModePowerThreshold, " +
                        "target reactivity 0) -- sharing autoControlDeadbandPcm's wide 50pcm margin here let a real " +
                        "playtest's reactor sit at a barely-inside-the-deadband +27 to +49 pcm for over 10 minutes " +
                        "straight (never triggering a correction), compounding to a ~539% of rated power overshoot " +
                        "before reactivity finally drifted negative on its own.",
                "Revised 2026-09-24 a second time (same session, later real-playtest log): an initial 10.0 default " +
                        "was itself still too loose once paired with MetaTileEntityPressurizedWaterReactor#" +
                        "updateAutoRodControl's own 0.1% hold-mode correction step -- a single 0.1% click at " +
                        "typical hold-mode rod positions swings reactivity by ~23 pcm, overshooting straight " +
                        "through a 10 pcm deadband and out the other side every time, producing a positive-biased " +
                        "limit cycle that still let power climb unbounded (189% at 5010s, 785% by 9109s in that " +
                        "log). Hold mode now uses a much finer 0.01% correction step instead (see that method's " +
                        "own JavaDoc), so this deadband can shrink to match its smaller ~2-3 pcm swing per click.",
                "Default: 2.0." })
        @Config.RangeDouble(min = 0.0, max = 1.0E6)
        public double autoControlHoldDeadbandPcm = 2.0;

        @Config.Comment({
                "Added 2026-09-25 (real-playtest-driven, see this mod's design memory): hold mode's own medium " +
                        "correction-step threshold -- above this (but below autoControlLargeStepThresholdPcm), " +
                        "uses a 0.1% step instead of the fine 0.01% one. Without this middle tier, a real playtest " +
                        "showed the ascension-to-hold transition leaving a large leftover reactivity residual " +
                        "(~250 pcm, from autoControlDeadbandPcm's wide ascension-mode margin) that the fine 0.01% " +
                        "step alone corrected too slowly, letting relative power climb unchecked from ~90% past " +
                        "400% before the power-limit SCRAM finally caught it. The 0.1% step clears large residuals " +
                        "quickly; the fine 0.01% step still handles final convergence into autoControlHoldDeadbandPcm.",
                "Default: 50.0." })
        @Config.RangeDouble(min = 0.0, max = 1.0E6)
        public double autoControlHoldMediumStepThresholdPcm = 50.0;

        @Config.Comment({
                "Experimental (2026-09-24): if the automatic rod control loop's reactivity error exceeds this " +
                        "many pcm, it nudges by 1% instead of the normal 0.1% correction step, so large initial " +
                        "excursions (e.g. right after startup, fully inserted) are corrected in a reasonable time " +
                        "instead of taking hundreds of 0.1% steps.",
                "Default: 500.0." })
        @Config.RangeDouble(min = 0.0, max = 1.0E6)
        public double autoControlLargeStepThresholdPcm = 500.0;

        @Config.Comment({
                "Experimental (2026-09-24): how often (in ticks) the automatic rod control loop re-evaluates and " +
                        "potentially nudges control rods -- also the cadence of MetaTileEntityPressurizedWaterReactor" +
                        "'s telemetry CSV logging, which is tied 1:1 to the same auto-control toggle.",
                "Default: 20 (once per second at normal tick speed)." })
        @Config.RangeInt(min = 1, max = 72000)
        public int autoControlIntervalTicks = 20;

        @Config.Comment({
                "Redesigned 2026-09-26 (real-PWR-data-driven, see this mod's design memory) from a fixed-" +
                        "threshold hysteresis to a proportional target: the automatic boron controller pushes " +
                        "bank 0's own average rod insertion (%) toward this value, harder the further away it " +
                        "drifts, rather than only reacting once insertion crosses a threshold near 0%/100%. 50.0 " +
                        "is deliberately the exact midpoint of ControlRodWorth's own cosine curve -- " +
                        "d(rho)/d(insertion) = -(W/2)*pi*sin(pi*d) is maximized at d=0.5 and falls to zero at both " +
                        "extremes, so a real playtest's own back-derived total bank worth (~15,500 pcm, more than " +
                        "a real PWR's typical 6,000-9,000 pcm) still let rods run out of *usable* withdrawal " +
                        "margin near 0% insertion, since that end of the curve yields almost no reactivity per " +
                        "further step. Keeping insertion centered here keeps rods on the curve's own steep, " +
                        "responsive middle at all times, mirroring a real PWR's own control-bank insertion-limits " +
                        "practice.",
                "Default: 50.0." })
        @Config.RangeDouble(min = 0.0, max = 100.0)
        public double autoControlRodInsertionTargetPercent = 50.0;

        @Config.Comment({
                "No boron adjustment runs while bank 0's average rod insertion is within this many percentage " +
                        "points of autoControlRodInsertionTargetPercent -- avoids constant micro-adjustment for " +
                        "insertion noise too small to matter.",
                "Default: 10.0 (i.e. idle between 40% and 60% insertion at the default 50% target)." })
        @Config.RangeDouble(min = 0.0, max = 100.0)
        public double autoControlRodInsertionDeadbandPercent = 10.0;

        @Config.Comment({
                "The insertion error (percentage points away from autoControlRodInsertionTargetPercent) at which " +
                        "boron adjustment runs at its full configured rate (see boronAdjustmentDurationMinutes). " +
                        "Between autoControlRodInsertionDeadbandPercent and this, the rate scales linearly from " +
                        "0 to full -- so correction already ramps up well before insertion could ever reach the " +
                        "cosine worth curve's dead tails near 0%/100%, rather than waiting for a fixed threshold " +
                        "right at the danger zone the way the hysteresis version this replaced did.",
                "Narrowed 2026-09-27 (real-playtest-driven, see this mod's design memory) from an initial 40.0: " +
                        "even though dilution correctly started early (insertion 40%, 402 seconds before rods " +
                        "actually ran out), the linear ramp meant only a small fraction of full rate was applied " +
                        "through most of that window -- boron moved only ~108 ppm of a theoretical ~265 ppm " +
                        "possible at full rate over the same span, because full rate was not reached until " +
                        "insertion fell all the way to 10% (40.0's own implied full-rate point), by which time " +
                        "rods were already deep in the danger zone and collapsed shortly after anyway.",
                "Default: 25.0 (full rate reached by 25% insertion instead of 10%, at the default 50% target and " +
                        "10.0 deadband), pending further playtesting." })
        @Config.RangeDouble(min = 0.0, max = 100.0)
        public double autoControlRodInsertionProportionalBandPercent = 25.0;

        @Config.Comment({
                "Experimental (2026-09-26): boron concentration (ppm) the automatic control loop's startup " +
                        "sequence charges the coolant up to (via forced boron concentration) before it will call " +
                        "requestStartup(), mirroring a real PWR being loaded with a substantial boron inventory " +
                        "before ever going critical rather than starting boron-free. Without this, a real " +
                        "playtest (2026-09-26) confirmed the purely reactive rod-position-threshold hysteresis " +
                        "this mod used at the time accumulated almost no boron during the fast initial rod " +
                        "withdrawal out of source range (rods crossed from 100% to 49% in under a minute, " +
                        "stopping concentration again long before any meaningful reserve built up -- only 1.4 " +
                        "ppm, worth -14 pcm against the thousands of pcm of xenon/moderator-temperature burden " +
                        "still to come), so rods still ran out of withdrawal margin and power collapsed later in " +
                        "the same run. If no boric acid solution is supplied to the import hatch, the loop waits " +
                        "here indefinitely rather than starting boron-free (2026-09-26 design decision -- a real " +
                        "plant cannot go critical without its planned boron load either).",
                "Default: 800.0, within the real PWR beginning-of-cycle range (roughly 800-1500 ppm depending on " +
                        "fuel cycle design), pending playtesting." })
        @Config.RangeDouble(min = 0.0, max = 1.0E6)
        public double autoControlInitialBoronChargePpm = 800.0;

        @Config.Comment({
                "Added 2026-09-28 (real-playtest-driven, see this mod's design memory): bank 0's own average rod " +
                        "insertion (%) below which, while still STARTUP and still subcritical, the automatic " +
                        "control loop dilutes boron to help reach criticality -- a narrow safety valve, distinct " +
                        "from updateAutoBoronControl's own full proportional controller (which stays OPERATING-" +
                        "only, see that method's own JavaDoc for why). A real playtest reached exactly 0% " +
                        "insertion (rods fully withdrawn, no further lever available) while still ~1600 pcm " +
                        "subcritical purely from moderator-temperature/Doppler feedback at an elevated heatup " +
                        "temperature, and sat there frozen indefinitely -- boron was never touched during STARTUP " +
                        "at all, so nothing existed to close that last gap, exactly the role boron dilution plays " +
                        "in a real PWR's own approach-to-criticality procedure alongside rod withdrawal. Kept low " +
                        "(rods must already be nearly exhausted) specifically so this cannot fire right at the " +
                        "STARTUP handoff moment (rods still near their pre-ascension ~100% insertion) -- the exact " +
                        "scenario that made the original full controller fight the handoff (see " +
                        "updateAutoBoronControl's own JavaDoc).",
                "Widened 2026-09-30 (real-playtest-driven, see this mod's design memory) from an initial 5.0: " +
                        "shapeReactivityPcm's own 2026-09-29 recalibration (6963 -> 11866 pcm) shifted where bank " +
                        "0's ascension naturally settles -- with more shape reactivity available, the reactor " +
                        "crosses into (and the ascension loop's own deadband settles around) criticality at a " +
                        "noticeably shallower withdrawal than before (the shape-and-rod-worth-only crossing point " +
                        "alone moved from roughly 46.6% insertion to roughly 67.6%). A real playtest's own " +
                        "ascension settled in a stable 'hold' at exactly 5.10% insertion -- just 0.10 points above " +
                        "the old 5.0 threshold -- with reactivity sitting just inside updateAutoRodControlAscension" +
                        "OrHold's own deadband on the other side too, so neither safety valve engaged and the " +
                        "reactor sat there indefinitely (safely, but making no further progress). Widening gives " +
                        "real headroom above wherever this reactor's own ascension happens to settle, rather than " +
                        "relying on staying just below a threshold tuned for the old, lower shape reactivity.",
                "Default: 15.0 (only engages once rods are nearly withdrawn, but with real margin above the " +
                        "5.10% near-miss above), pending further playtesting." })
        @Config.RangeDouble(min = 0.0, max = 100.0)
        public double startupBoronAssistRodThresholdPercent = 15.0;

        @Config.Comment({
                "Added 2026-10 (see this mod's design memory for the full HP/LP turbine design): combined thermal " +
                        "efficiency of the HP+LP turbine pair, as a fraction of the latent heat the reference " +
                        "reactor's own full steam output carries -- the rest is rejected at the condenser, " +
                        "exactly like a real Rankine cycle. See SteamTurbineOutput#stageEfficiencyFraction for " +
                        "how this splits between the HP and LP stages individually (by the temperature drop each " +
                        "is responsible for, not a fixed ratio).",
                "Default: 0.33, within the real PWR secondary-cycle range (roughly 33-37%)." })
        @Config.RangeDouble(min = 0.0, max = 1.0)
        public double turbineThermalEfficiency = 0.33;

        @Config.Comment({
                "Real-world Watts represented by drawing 1 EU/t of turbine output, steadily -- a separate " +
                        "gameplay-balance convention from euPerTickToWattsConversion (consumption side), not the " +
                        "same exchange rate, exactly like that field's own JavaDoc explains there is no canonical " +
                        "GT lore value for this conversion either way.",
                "Derived from a user-specified target (2026-10, see this mod's design memory): the reference " +
                        "reactor (1010 interior cells, 5050 MWth) should generate approximately 125,000 EU/t " +
                        "(near ZPM) when its full steam output is consumed by enough turbines at a nominal " +
                        "(100%) rotor. At turbineThermalEfficiency=0.33, that reactor's own electrical output is " +
                        "5.05E9 * 0.33 =~ 1.6665E9 W, so 1.6665E9 / 125000 =~ 13332.",
                "Default: 13332.0, pending playtesting." })
        @Config.RangeDouble(min = 0.0, max = 1.0E9)
        public double euPerTickToWattsConversionGeneration = 13332.0;

        @Config.Comment({
                "One HP turbine's own steam throughput ceiling (mB/t) at a nominal (100%) rotor power -- see " +
                        "SteamTurbineOutput#maxThroughputMbPerTick. Derived (2026-10, see this mod's design " +
                        "memory) from the reference reactor's own full-power HighPressureSteam output " +
                        "(168333.33 mB/t, from ratedThermalPowerPerInteriorCellWatts/SteamProperties' own latent " +
                        "heat) divided across a user-specified 4 HP turbines.",
                "Default: 42083.33, pending playtesting." })
        @Config.RangeDouble(min = 0.0, max = 1.0E9)
        public double highPressureTurbineBaseThroughputMbPerTick = 42083.33;

        @Config.Comment({
                "As highPressureTurbineBaseThroughputMbPerTick, for the LP stage -- divided across 12 LP " +
                        "turbines (2026-10 decision: a real nuclear turbine-generator set's own typical 1 HP : " +
                        "2-3 LP ratio, applied to this mod's 4-HP-turbine reference split, rounds to 3 LP per HP " +
                        "=~ 12 total), processing the same total mass flow the HP stage already consumed " +
                        "(mass-conserved through the turbine, only pressure/temperature changes).",
                "Default: 14027.78 (168333.33 / 12), pending playtesting." })
        @Config.RangeDouble(min = 0.0, max = 1.0E9)
        public double lowPressureTurbineBaseThroughputMbPerTick = 14027.78;

        @Config.Comment({
                "Ambient/condenser temperature (K) the LP turbine stage's own temperature-drop share is measured " +
                        "down to -- reuses the same representative value as the reactor/heat exchanger's own " +
                        "AMBIENT_TEMPERATURE_KELVIN constant, now exposed here since SteamTurbineOutput itself " +
                        "has no Minecraft/GT dependency and cannot reference that constant directly.",
                "Default: 300.0." })
        @Config.RangeDouble(min = 0.0, max = 1000.0)
        public double turbineCondenserTemperatureKelvin = 300.0;

        @Config.Comment({
                "Gameplay-balance scaling constant for a loaded control rod's own vanilla-damage wear, applied " +
                        "every tick as controlRodWearRateCoefficient * insertionDepth * localThermalFlux * " +
                        "relativePower * dtSeconds (2026-10 design: approved approach is \"local neutron capture " +
                        "~= CMFD thermal flux x power\", scaled by how much of the rod is actually inserted into " +
                        "the flux region) -- same unit-absorbing role as fuelBurnupRateCoefficient's own comment " +
                        "explains, not a physical constant. localThermalFlux reuses MetaTileEntityPressurizedWater" +
                        "Reactor#sumControlRodReactivity's own per-rod CMFD sample (core average = 1.0).",
                "Default: 2.3148E-3, chosen (2026-10, user-specified target) so a rod sitting at a representative " +
                        "0.3 insertion depth/1.0 local flux/rated power (a bank doing load-following duty while " +
                        "others sit withdrawn) depletes its full 1000 damage over about 400 hours of continuous " +
                        "operation -- a rod withdrawn near 0 barely wears at all, one left fully inserted the " +
                        "whole time wears out in about 120 hours. Pending playtesting." })
        @Config.RangeDouble(min = 0.0, max = 1.0)
        public double controlRodWearRateCoefficient = 2.3148E-3;

        // ---------------------------------------------------------------- residual heat removal (stage 1-4a)

        @Config.Comment({
                "Thermal power (W) of the real plant the rhr* flow/UA/pump values below describe -- each reactor " +
                        "scales them by its own rated thermal power / this value (2026-11-24, stage 1-4a of this " +
                        "mod's reactor physics rework, see this mod's design memory).",
                "Default: 3.411E9, a real Westinghouse 4-loop PWR." })
        @Config.RangeDouble(min = 1.0, max = 1.0E12)
        public double rhrReferenceThermalPowerWatts = 3.411E9;

        @Config.Comment({
                "Primary-side flow (kg/s) through the RHR heat exchangers at the reference plant -- two trains of " +
                        "about 240 kg/s (~3800 gpm) each.",
                "Default: 480.0" })
        @Config.RangeDouble(min = 0.0, max = 1.0E9)
        public double rhrPrimaryFlowKgPerSecond = 480.0;

        @Config.Comment({
                "Design cooling water flow (kg/s) through the RHR heat exchangers' cooling side at the reference " +
                        "plant (1 kg = 1000 mB). The flow actually used each tick is limited by what the cooling " +
                        "water import hatch holds and what the export hatch can accept.",
                "Default: 600.0 (two trains of ~300 kg/s)" })
        @Config.RangeDouble(min = 0.0, max = 1.0E9)
        public double rhrCoolingWaterFlowKgPerSecond = 600.0;

        @Config.Comment({
                "Combined UA (W/K) of the RHR heat exchangers at the reference plant, used with the " +
                        "effectiveness-NTU method (counterflow).",
                "Default: 6.2E5 -- about 37 MW per train with the primary at 450 K and 300 K cooling water, a " +
                        "typical real RHR heat exchanger duty at the start of RHR cooldown." })
        @Config.RangeDouble(min = 0.0, max = 1.0E12)
        public double rhrHeatExchangerUaWattsPerKelvin = 6.2E5;

        @Config.Comment({
                "RHR pump heat (W) deposited in the primary coolant at the reference plant while RHR is running.",
                "Default: 6.0E5 (two ~300 kW pumps)" })
        @Config.RangeDouble(min = 0.0, max = 1.0E12)
        public double rhrPumpHeatWatts = 6.0E5;

        @Config.Comment({
                "RHR pump electrical draw (EU/t) at the reference plant, scaled like the values above. Without " +
                        "power the RHR pumps stop and no heat is removed (it stays in service).",
                "Default: 300.0 -- the same EU-per-real-MW ratio this mod uses for the reactor coolant pumps " +
                        "(~20 MW real <-> ~10,000 EU/t), pending playtesting." })
        @Config.RangeDouble(min = 0.0, max = 1.0E9)
        public double rhrPumpEuPerTick = 300.0;

        @Config.Comment({
                "RHR can only be placed in service at or below this primary coolant temperature (K).",
                "Default: 450.0 (~350 F, the real PWR RHR entry condition)" })
        @Config.RangeDouble(min = 0.0, max = 1.0E4)
        public double rhrMaxStartTemperatureKelvin = 450.0;

        @Config.Comment({
                "RHR can only be placed in service at or below this primary pressure (MPa, absolute).",
                "Default: 3.1 (~425 psig, the real suction valve open-permissive)" })
        @Config.RangeDouble(min = 0.0, max = 1.0E3)
        public double rhrMaxStartPressureMPa = 3.1;

        @Config.Comment({
                "An in-service RHR isolates itself automatically above this primary pressure (MPa, absolute), " +
                        "protecting its low-pressure piping.",
                "Default: 4.2 (~600 psig, the real suction valve auto-closure)" })
        @Config.RangeDouble(min = 0.0, max = 1.0E3)
        public double rhrIsolationPressureMPa = 4.2;

        @Config.Comment({
                "DEBUG/VERIFICATION ONLY: multiplies the heat RHR removes from the primary coolant, to shorten " +
                        "cooldown while testing. 1.0 is the physical value; anything else breaks the cooling " +
                        "water's own energy balance (its outlet temperature still reflects the unmultiplied value).",
                "Default: 1.0" })
        @Config.RangeDouble(min = 0.0, max = 1.0E6)
        public double rhrCooldownAssistFactor = 1.0;

        // ---------------------------------------------------------------- heat exchanger cooldown (stage 1-4b)

        @Config.Comment({
                "Fastest the heat exchanger's effective secondary pressure setpoint may be lowered, expressed as " +
                        "the fall of its saturation temperature (K per hour) -- the operator sets a target and " +
                        "the setpoint ramps toward it (2026-11-26, stage 1-4b, decision R1). Raise it only to " +
                        "shorten cooldown while testing.",
                "Default: 55.6 (100 F/h, the real PWR reactor coolant cooldown rate limit)" })
        @Config.RangeDouble(min = 0.0, max = 1.0E6)
        public double secondaryCooldownRateKelvinPerHour = 55.6;

        @Config.Comment({
                "Steam dump (to condenser) capacity per heat exchanger (kg/s). Steam that the turbines cannot take " +
                        "-- all of it in cooldown mode -- is dumped up to this rate and leaves through the steam " +
                        "dump condensate export hatch as water.",
                "Default: 1346.7, 40% (the real design value) of the reference reactor's full-power steam flow " +
                        "(168,333 mB/t)." })
        @Config.RangeDouble(min = 0.0, max = 1.0E9)
        public double steamDumpCapacityKgPerSecond = 1346.7;

        @Config.Comment({
                "Main steam safety valve setpoint (MPa, absolute): above this saturation pressure the secondary " +
                        "side vents steam to atmosphere instead of rising further.",
                "Default: 8.3 (the real first-stage setpoint)" })
        @Config.RangeDouble(min = 0.0, max = 1.0E3)
        public double mainSteamSafetyValvePressureMPa = 8.3;

        // ---------------------------------------------------------------- refueling, Mode 6 (stage 1-4c)

        @Config.Comment({
                "Refueling start condition (4) and hold limit: minimum primary boron concentration (ppm) " +
                        "(2026-11-27, stage 1-4c). Until stage 2 it also stands in for condition (5), k_eff <= " +
                        "0.95 with all rods withdrawn.",
                "Default: 2000.0 (typical real PWR Mode 6 requirement)" })
        @Config.RangeDouble(min = 0.0, max = 1.0E6)
        public double refuelingMinBoronPpm = 2000.0;

        @Config.Comment({
                "Refueling start condition (6) and hold limit: maximum primary coolant temperature (K).",
                "Default: 366.0 (200 F, the real Mode 5/6 boundary)" })
        @Config.RangeDouble(min = 0.0, max = 1.0E4)
        public double refuelingMaxTemperatureKelvin = 366.0;

        @Config.Comment({
                "Refueling start condition (8): minutes (world time) the core must have stayed below the shutdown " +
                        "level since it last operated -- a decay heat stand-in until stage 2 replaces it with " +
                        "decay heat <= ~0.3%. A core that has never operated skips it. Lower it to shorten tests.",
                "Default: 115.0 (the real ~100 hours divided by this mod's 52.2x time compression)" })
        @Config.RangeDouble(min = 0.0, max = 1.0E6)
        public double refuelingMinSubcriticalMinutes = 115.0;

        @Config.Comment({
                "DEBUG: log every refused fuel assembly port slot operation (side, slot, item, lock and channel " +
                        "context) to the game log with the [FAP-DEBUG] prefix. Off by default -- it logs on " +
                        "every locked interaction, including automation.",
                "Default: false" })
        public boolean debugFuelAssemblyPortLogging = false;
    }
}
