package com.windows2000.gtnuclear.common.metatileentities;

import com.windows2000.gtnuclear.api.GTNuclearValues;

import gregtech.api.GregTechAPI;
import gregtech.api.metatileentity.registry.MTEManager;

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Creates this mod's own MTE registry namespace, mirroring {@link com.windows2000.gtnuclear.api.unification.materials.GTNLMaterials}'s
 * handling of {@code MaterialRegistryEvent}/{@code MaterialEvent}. Not {@code @Mod.EventBusSubscriber}-annotated
 * for the same reason -- see that class's own JavaDoc; {@code CoreModule#construction} registers this class to the
 * Forge event bus explicitly instead.
 * <p>
 * {@link MTEManager.MTERegistryEvent} fires during GT's own preInit, right before {@code MetaBlocks.init()} (which
 * automatically creates a {@code BlockMachine} for every registry that exists by that point) -- so this registry
 * must already exist by then, same timing constraint as the material registry.
 */
public class GTNLMTERegistryHandler {

    private GTNLMTERegistryHandler() {}

    @SubscribeEvent
    public static void onMTERegistryEvent(MTEManager.MTERegistryEvent event) {
        GregTechAPI.mteManager.createRegistry(GTNuclearValues.MODID);
    }
}
