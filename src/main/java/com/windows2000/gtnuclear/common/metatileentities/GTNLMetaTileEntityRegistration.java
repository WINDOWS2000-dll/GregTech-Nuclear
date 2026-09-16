package com.windows2000.gtnuclear.common.metatileentities;

import com.windows2000.gtnuclear.api.GTNuclearValues;
import com.windows2000.gtnuclear.common.metatileentities.storage.MetaTileEntityIsotopeStorage;

import gregtech.api.GregTechAPI;

import net.minecraft.util.ResourceLocation;

import static com.windows2000.gtnuclear.api.util.GTNLUtils.gtnlID;
import static com.windows2000.gtnuclear.common.metatileentities.GTNLMetaTileEntities.*;

/**
 * Actually constructs and registers this mod's MetaTileEntities into its own MTE registry (created earlier by
 * {@link GTNLMTERegistryHandler}). Called from {@code CoreModule#preInit}, mirroring how
 * {@code GTNLMetaBlocks.init()} is already called there -- unlike material/MTE-registry creation, individual MTE
 * registration has no dedicated addon-hook event and has no ordering constraint against GT's own preInit finishing
 * first.
 */
public class GTNLMetaTileEntityRegistration {

    private GTNLMetaTileEntityRegistration() {}

    public static void register() {
        ResourceLocation isotopeStorageId = gtnlID("isotope_storage");
        ISOTOPE_STORAGE = new MetaTileEntityIsotopeStorage(isotopeStorageId);
        GregTechAPI.mteManager.getRegistry(GTNuclearValues.MODID).register(0, isotopeStorageId, ISOTOPE_STORAGE);
    }
}
