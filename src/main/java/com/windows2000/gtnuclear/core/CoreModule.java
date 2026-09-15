package com.windows2000.gtnuclear.core;

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
    public void construction(FMLConstructionEvent event) {}

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit(event);

        logger.info("Hello World!");
    }
}
