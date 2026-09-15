package com.windows2000.gtnuclear.common.blocks;

import com.windows2000.gtnuclear.common.blocks.reactor.TestCasing;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import static gregtech.common.blocks.MetaBlocks.statePropertiesToString;

public class GTNLMetaBlocks {

    private GTNLMetaBlocks() {}

    public static TestCasing TEST_CASING;

    public static void init() {
        TEST_CASING = new TestCasing();
        TEST_CASING.setRegistryName("gtnl_test_casing");
    }

    @SideOnly(Side.CLIENT)
    public static void registerItemModel() {
        registerItemModel(TEST_CASING);
    }

    @SideOnly(Side.CLIENT)
    private static void registerItemModel(Block block) {
        for (IBlockState state : block.getBlockState().getValidStates()) {
            // noinspection ConstantConditions
            ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(block),
                    block.getMetaFromState(state),
                    new ModelResourceLocation(block.getRegistryName(),
                            statePropertiesToString(state.getProperties())));
        }
    }

}
