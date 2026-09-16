package com.windows2000.gtnuclear.core;

import com.windows2000.gtnuclear.api.unification.materials.GTNLMaterials;
import com.windows2000.gtnuclear.common.blocks.GTNLMetaBlocks;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLConstructionEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import com.windows2000.gtnuclear.Tags;
import com.windows2000.gtnuclear.api.GTNuclearValues;
import com.windows2000.gtnuclear.api.modules.GTNuclearModule;
import com.windows2000.gtnuclear.api.modules.IModule;
import com.windows2000.gtnuclear.common.CommonProxy;
import com.windows2000.gtnuclear.modules.Modules;

@GTNuclearModule(
                 moduleID = Modules.MODULE_CORE,
                 containerID = GTNuclearValues.MODID,
                 name = "GTNuclear Core",
                 description = "Core of GTNuclear",
                 coreModule = true)
public class CoreModule implements IModule {

    public static final Logger logger = LogManager.getLogger(Tags.MODNAME + " Core");
    @SidedProxy(modId = GTNuclearValues.MODID,
                clientSide = "com.windows2000.gtnuclear.client.ClientProxy",
                serverSide = "com.windows2000.gtnuclear.common.CommonProxy")
    public static CommonProxy proxy;

    @Override
    public @NotNull Logger getLogger() {
        return logger;
    }

    @Override
    public void construction(FMLConstructionEvent event) {
        // Registered here (Construction phase, which precedes PreInit for every mod regardless of dependency
        // order) rather than via @Mod.EventBusSubscriber, so this mod's Forge event-bus wiring stays visible in
        // one place. GT's MaterialRegistryEvent/MaterialEvent fire during GT's own PreInit -- required-after:
        // gregtech only orders same-phase events, so this must already be subscribed before PreInit begins.
        MinecraftForge.EVENT_BUS.register(GTNLMaterials.class);
    }

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit(event);
        GTNLMetaBlocks.init();

        logger.info("Hello World!");
    }
}
