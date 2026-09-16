package com.windows2000.gtnuclear.common;

import com.windows2000.gtnuclear.api.GTNuclearValues;

import net.minecraftforge.common.config.Config;

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
}
