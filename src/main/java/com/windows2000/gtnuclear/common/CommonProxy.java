package com.windows2000.gtnuclear.common;

import static com.windows2000.gtnuclear.common.blocks.GTNLMetaBlocks.*;

import com.windows2000.gtnuclear.api.util.GTNLLogger;

import gregtech.api.block.VariantItemBlock;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import com.windows2000.gtnuclear.api.GTNuclearValues;

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.registries.IForgeRegistry;

import java.util.function.Function;

@Mod.EventBusSubscriber(modid = GTNuclearValues.MODID)
public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {}

    public void init(FMLInitializationEvent event) {}

    @SubscribeEvent
    public static void registerBlock(RegistryEvent.Register<Block> blockRegister) {
        GTNLLogger.logger.info("Registering Blocks...");
        IForgeRegistry<Block> registry = blockRegister.getRegistry();

        registry.register(TEST_CASING);

    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> itemRegister) {
        GTNLLogger.logger.info("Registering Items...");
        IForgeRegistry<Item> registry = itemRegister.getRegistry();

        registry.register(createItemBlock(TEST_CASING, VariantItemBlock::new));
    }

    private static <T extends Block> ItemBlock createItemBlock(T block, Function<T, ItemBlock> producer) {
        ItemBlock itemBlock = producer.apply(block);
        ResourceLocation registryName = block.getRegistryName();
        if (registryName == null) {
            throw new IllegalArgumentException("Block " + block.getTranslationKey() + " has no registry name.");
        }
        itemBlock.setRegistryName(registryName);
        return itemBlock;
    }

}
