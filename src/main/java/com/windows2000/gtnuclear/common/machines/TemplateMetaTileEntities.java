package com.windows2000.gtnuclear.common.machines;

import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.NotNull;

import com.windows2000.gtnuclear.api.GTNuclearValues;

import gregtech.api.GTValues;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.RecipeWorkableSimpleMachineMetaTileEntity;
import gregtech.client.renderer.texture.Textures;

/**
 * This addon's own {@link MetaTileEntity} instances, registered by {@link #init()}. See GregTech's own {@code
 * MetaTileEntities}/{@code MetaTileEntityRegistration} split for how to organize this once you have more than a
 * handful of machines: one holder class for the fields + a shared {@code registerMetaTileEntity} helper, and one
 * (or several) registration classes with the actual construction calls, grouped by category with their own
 * reserved ID ranges.
 * <p>
 * The numeric ID passed to {@link #registerMetaTileEntity} only needs to be unique within this addon's own
 * namespace ({@link GTNuclearValues#MODID}) -- registration is bucketed per mod ID, so there is no need to
 * coordinate a global ID range with GregTech itself or with other addons.
 */
public final class TemplateMetaTileEntities {

    public static RecipeWorkableSimpleMachineMetaTileEntity SAMPLE_MACHINE;

    private TemplateMetaTileEntities() {}

    public static void init() {
        SAMPLE_MACHINE = registerMetaTileEntity(0,
                new RecipeWorkableSimpleMachineMetaTileEntity(id("sample_machine"),
                        TemplateRecipeMaps.SAMPLE_MACHINE_RECIPES, Textures.COMPRESSOR_OVERLAY, GTValues.LV, true));
    }

    /** Mirrors GregTech's own {@code MetaTileEntities#registerMetaTileEntity(int, MetaTileEntity)}. */
    static <T extends MetaTileEntity> @NotNull T registerMetaTileEntity(int id, @NotNull T mte) {
        mte.getRegistry().register(id, mte.metaTileEntityId, mte);
        return mte;
    }

    static ResourceLocation id(String path) {
        return new ResourceLocation(GTNuclearValues.MODID, path);
    }
}
