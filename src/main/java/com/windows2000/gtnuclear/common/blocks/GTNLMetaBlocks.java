package com.windows2000.gtnuclear.common.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import com.windows2000.gtnuclear.common.blocks.generator.TurbineCasing;
import com.windows2000.gtnuclear.common.blocks.heatExchanger.HeatExchangerCasing;
import com.windows2000.gtnuclear.common.blocks.reactor.ControlRodHousing;
import com.windows2000.gtnuclear.common.blocks.reactor.FuelRodHousing;
import com.windows2000.gtnuclear.common.blocks.reactor.ReactorVesselCasing;
import com.windows2000.gtnuclear.common.blocks.reactor.TestCasing;

import static gregtech.common.blocks.MetaBlocks.statePropertiesToString;

public class GTNLMetaBlocks {

    private GTNLMetaBlocks() {}

    public static TestCasing TEST_CASING;
    public static ReactorVesselCasing REACTOR_VESSEL_CASING;
    public static FuelRodHousing FUEL_ROD_HOUSING;
    public static ControlRodHousing CONTROL_ROD_HOUSING;
    public static HeatExchangerCasing HEAT_EXCHANGER_CASING;
    public static TurbineCasing TURBINE_CASING;

    public static void init() {
        TEST_CASING = new TestCasing();
        TEST_CASING.setRegistryName("gtnl_test_casing");
        REACTOR_VESSEL_CASING = new ReactorVesselCasing();
        REACTOR_VESSEL_CASING.setRegistryName("reactor_vessel_casing");
        FUEL_ROD_HOUSING = new FuelRodHousing();
        FUEL_ROD_HOUSING.setRegistryName("fuel_rod_housing");
        CONTROL_ROD_HOUSING = new ControlRodHousing();
        CONTROL_ROD_HOUSING.setRegistryName("control_rod_housing");
        HEAT_EXCHANGER_CASING = new HeatExchangerCasing();
        HEAT_EXCHANGER_CASING.setRegistryName("heat_exchanger_casing");
        TURBINE_CASING = new TurbineCasing();
        TURBINE_CASING.setRegistryName("turbine_casing");
    }

    @SideOnly(Side.CLIENT)
    public static void registerItemModel() {
        registerItemModel(TEST_CASING);
        registerItemModel(REACTOR_VESSEL_CASING);
        registerItemModel(FUEL_ROD_HOUSING);
        registerItemModel(CONTROL_ROD_HOUSING);
        registerItemModel(HEAT_EXCHANGER_CASING);
        registerItemModel(TURBINE_CASING);
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
