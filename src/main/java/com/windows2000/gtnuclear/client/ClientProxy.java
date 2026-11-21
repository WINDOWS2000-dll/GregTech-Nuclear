package com.windows2000.gtnuclear.client;

import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

import com.cleanroommc.modularui.ModularUIConfig;
import com.windows2000.gtnuclear.api.GTNuclearValues;
import com.windows2000.gtnuclear.client.renderer.texture.GTNLTextures;
import com.windows2000.gtnuclear.common.CommonProxy;
import com.windows2000.gtnuclear.common.blocks.GTNLMetaBlocks;
import com.windows2000.gtnuclear.common.item.GTNLItems;
import com.windows2000.gtnuclear.common.item.ITintedItem;
import org.jetbrains.annotations.NotNull;

@Mod.EventBusSubscriber(Side.CLIENT)
public class ClientProxy extends CommonProxy {

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);

        GTNLTextures.preInit();

        // ModularUI's own default (smoothProgressBar=false) steps a ProgressWidget's fill by
        // getArea().width-sized texture-pixel increments (see ProgressWidget#getProgressUV) -- confirmed via a
        // real playtest (2026-10) that in this project's environment this collapses every ModularUI progress bar
        // (this mod's own turbine bars, and GT's own stock multiblocks alike) to only ever rendering fully empty
        // or fully full, never any intermediate fraction. Forced true here since this is a mod-wide ModularUI
        // config value, not something GTNuclear's own bars can opt into individually -- overrides whatever the
        // player's own modularui.cfg says, same tradeoff already accepted once this was confirmed to fix it.
        ModularUIConfig.smoothProgressBar = true;
    }

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        GTNLMetaBlocks.registerItemModel();

        ModelResourceLocation fuelRodModel = sharedModel("material_sets/fuel_rod/fuel_rod");
        for (Item fuelRod : GTNLItems.ALL_FUEL_RODS) {
            ModelLoader.setCustomModelResourceLocation(fuelRod, 0, fuelRodModel);
        }

        ModelResourceLocation controlRodModel = sharedModel("material_sets/control_rod/control_rod");
        for (Item controlRod : GTNLItems.ALL_CONTROL_RODS) {
            ModelLoader.setCustomModelResourceLocation(controlRod, 0, controlRodModel);
        }

        ModelLoader.setCustomModelResourceLocation(GTNLItems.NEUTRON_SOURCE_ROD, 0,
                sharedModel("material_sets/neutron_source_rod/neutron_source_rod"));

        ModelLoader.setCustomModelResourceLocation(GTNLItems.BROKEN_CONTROL_ROD, 0, sharedModel("broken_control_rod"));
    }

    @NotNull
    private static ModelResourceLocation sharedModel(String path) {
        return new ModelResourceLocation(new ResourceLocation(GTNuclearValues.MODID, path), "inventory");
    }

    /**
     * Single shared {@code ITintedItem} color handler for every fuel/control/neutron-source rod -- see that
     * interface's own JavaDoc for why one grayscale texture per shape plus this runtime tint replaces a bespoke
     * texture per enrichment/type variant.
     */
    @SubscribeEvent
    public static void registerItemColors(ColorHandlerEvent.Item event) {
        event.getItemColors().registerItemColorHandler(
                (stack, tintIndex) -> tintIndex == 0 ? ((ITintedItem) stack.getItem()).getTintColor() : 0xFFFFFF,
                GTNLItems.ALL_FUEL_RODS.toArray(new Item[0]));
        event.getItemColors().registerItemColorHandler(
                (stack, tintIndex) -> tintIndex == 0 ? ((ITintedItem) stack.getItem()).getTintColor() : 0xFFFFFF,
                GTNLItems.ALL_CONTROL_RODS.toArray(new Item[0]));
        event.getItemColors().registerItemColorHandler(
                (stack, tintIndex) -> tintIndex == 0 ? ((ITintedItem) stack.getItem()).getTintColor() : 0xFFFFFF,
                GTNLItems.NEUTRON_SOURCE_ROD);
    }
}
