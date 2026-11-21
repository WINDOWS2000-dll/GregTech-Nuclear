package com.windows2000.gtnuclear.common.blocks.heatExchanger;

import gregtech.api.block.VariantBlock;
import gregtech.api.items.toolitem.ToolClasses;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLiving;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

import org.jetbrains.annotations.NotNull;

public class HeatExchangerCasing extends VariantBlock<HeatExchangerCasing.CasingType> {

    public HeatExchangerCasing() {
        super(Material.IRON);
        setTranslationKey("heat_exchanger_casing");
        setHardness(200.0F);
        setResistance(160.0F);
        setSoundType(SoundType.METAL);
        setHarvestLevel(ToolClasses.WRENCH, 3);
        setDefaultState(this.getState(CasingType.HEAT_EXCHANGER_SHELL_CASING));
    }

    @Override
    public boolean canCreatureSpawn(@NotNull IBlockState state, @NotNull IBlockAccess world, @NotNull BlockPos pos,
                                    @NotNull EntityLiving.SpawnPlacementType type) {
        return false;
    }

    public enum CasingType implements IStringSerializable {

        HEAT_EXCHANGER_SHELL_CASING("heat_exchanger_shell_casing"),
        HEAT_EXCHANGER_SHROUD_CASING("heat_exchanger_shroud_casing"),
        HEAT_EXCHANGER_SWIRL_SEPARATOR("heat_exchanger_swirl_separator"),
        HEAT_EXCHANGER_TUBE_CASING("heat_exchanger_tube_casing"),
        HEAT_EXCHANGER_TUBE_SHEET_CASING("heat_exchanger_tube_sheet_casing"),
        HEAT_EXCHANGER_BAFFLE_CASING("heat_exchanger_baffle_casing");

        private final String name;

        CasingType(String name) {
            this.name = name;
        }

        @Override
        public @NotNull String getName() {
            return this.name;
        }
    }
}
