package com.windows2000.gtnuclear.api.util;

import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.NotNull;

import com.windows2000.gtnuclear.api.GTNuclearValues;

public class GTNLUtils {

    private GTNLUtils() {}

    public static @NotNull ResourceLocation gtnlID(String path) {
        return new ResourceLocation(GTNuclearValues.MODID, path);
    }
}
