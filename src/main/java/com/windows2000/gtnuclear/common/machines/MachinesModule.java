package com.windows2000.gtnuclear.common.machines;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import com.windows2000.gtnuclear.api.GTNuclearValues;
import com.windows2000.gtnuclear.api.modules.GTNuclearModule;
import com.windows2000.gtnuclear.modules.BaseModule;
import com.windows2000.gtnuclear.modules.Modules;

/**
 * Registers this addon's sample machine and its one recipe. Delete this whole package once you've replaced the
 * sample with your own content, or keep the module and just swap out what it registers.
 */
@GTNuclearModule(
                 moduleID = Modules.MODULE_MACHINES,
                 containerID = GTNuclearValues.MODID,
                 name = "Sample Machines",
                 description = "This addon's sample machine.")
public class MachinesModule extends BaseModule {

    private static final Logger logger = LogManager.getLogger("GTNuclear Machines");

    @Override
    public @NotNull Logger getLogger() {
        return logger;
    }

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        // must run after GregTech's own preInit (guaranteed by this mod's "required-after:gregtech" dependency),
        // and after TemplateMTERegistryHandler has created this addon's MTE registry (guaranteed by that
        // handler responding to an event GregTech fires during ITS OWN preInit, earlier than this)
        TemplateMetaTileEntities.init();
    }

    @Override
    public void postInit(FMLPostInitializationEvent event) {
        // a trivial placeholder recipe so the sample machine actually does something in-game -- replace with
        // your own recipes. See gregtech.api.recipes.RecipeMaps in GregTech itself for many more examples of
        // recipeBuilder() options (fluids, chance outputs, cleanroom/dimension requirements, etc.)
        TemplateRecipeMaps.SAMPLE_MACHINE_RECIPES.recipeBuilder()
                .input(Blocks.COBBLESTONE)
                .output(Items.IRON_NUGGET)
                .duration(100).EUt(16)
                .buildAndRegister();
    }
}
