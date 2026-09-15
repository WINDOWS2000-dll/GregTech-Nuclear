package com.windows2000.gtnuclear.common.blocks.reactor;

import gregtech.api.block.VariantBlock;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLiving;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;

public class TestCasing extends VariantBlock<TestCasing.CasingType> {

    public TestCasing() {
        super(Material.IRON);
        setTranslationKey("gtnl_test_casing");
        setHardness(114.514F);
        setResistance(19.19F);
        setSoundType(SoundType.METAL);
        setHarvestLevel("pickaxe", 0);
        setDefaultState(this.getState(CasingType.TEST_CASING));
    }

    @Override
    public boolean canCreatureSpawn(@Nonnull IBlockState state, @Nonnull IBlockAccess world, @Nonnull BlockPos pos,
                                    @Nonnull EntityLiving.SpawnPlacementType type) {
        return false;
    }

    public enum CasingType implements IStringSerializable {

        TEST_CASING("test_casing");

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
