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

/**
 * Structural block forming a fuel channel below a fuel port -- see this mod's design memory: every block in the
 * column beneath a fuel port must be this block, verified by a runtime scan when the reactor forms (GT's own
 * multiblock pattern matcher cannot express "this whole column is uniform" as a single-cell predicate).
 */
public class FuelRodHousing extends VariantBlock<FuelRodHousing.CasingType> {

    public FuelRodHousing() {
        super(Material.IRON);
        setTranslationKey("fuel_rod_housing");
        setHardness(50.0F);
        setResistance(20.0F);
        setSoundType(SoundType.METAL);
        setHarvestLevel("pickaxe", 2);
        setDefaultState(this.getState(CasingType.PWR_FUEL_ROD_HOUSING));
    }

    @Override
    public boolean canCreatureSpawn(@NotNull IBlockState state, @NotNull IBlockAccess world, @NotNull BlockPos pos,
                                    @NotNull EntityLiving.SpawnPlacementType type) {
        return false;
    }

    public enum CasingType implements IStringSerializable {

        PWR_FUEL_ROD_HOUSING("pwr_fuel_rod_housing");

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
