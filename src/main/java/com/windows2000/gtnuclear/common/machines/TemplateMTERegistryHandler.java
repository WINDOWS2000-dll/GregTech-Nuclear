package com.windows2000.gtnuclear.common.machines;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import com.windows2000.gtnuclear.api.GTNuclearValues;

import gregtech.api.metatileentity.registry.MTEManager;

/**
 * Creates this addon's own {@link gregtech.api.metatileentity.registry.MTERegistry} the moment GregTech asks
 * for it. This mirrors GregTech's own addon hook exactly: this must run before any of this mod's
 * {@link gregtech.api.metatileentity.MetaTileEntity} instances are constructed, since their constructor looks
 * up its own namespace's registry immediately.
 */
@Mod.EventBusSubscriber(modid = GTNuclearValues.MODID)
public final class TemplateMTERegistryHandler {

    private TemplateMTERegistryHandler() {}

    @SubscribeEvent
    public static void onMTERegistryEvent(MTEManager.MTERegistryEvent event) {
        MTEManager.getInstance().createRegistry(GTNuclearValues.MODID);
    }
}
