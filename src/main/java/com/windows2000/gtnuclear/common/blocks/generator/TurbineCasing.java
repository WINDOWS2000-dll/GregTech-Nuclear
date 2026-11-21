package com.windows2000.gtnuclear.common.blocks.generator;

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

public class TurbineCasing extends VariantBlock<TurbineCasing.CasingType> {

    public TurbineCasing() {
        super(Material.IRON);
        setTranslationKey("turbine_casing");
        setHardness(10.0F);
        setResistance(20.0F);
        setSoundType(SoundType.METAL);
        setHarvestLevel(ToolClasses.WRENCH, 3);
        setDefaultState(this.getState(CasingType.TURBINE_HP_CASING));
    }

    @Override
    public boolean canCreatureSpawn(@NotNull IBlockState state, @NotNull IBlockAccess world, @NotNull BlockPos pos,
                                    @NotNull EntityLiving.SpawnPlacementType type) {
        return false;
    }

    public enum CasingType implements IStringSerializable {

        TURBINE_HP_PIPE_CASING("turbine_hp_pipe_casing"),
        TURBINE_LP_PIPE_CASING("turbine_lp_pipe_casing"),
        TURBINE_HP_GEARBOX_CASING("turbine_hp_gearbox_casing"),
        TURBINE_LP_GEARBOX_CASING("turbine_lp_gearbox_casing"),
        TURBINE_HP_CASING("turbine_hp_casing"),
        TURBINE_LP_CASING("turbine_lp_casing");

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
