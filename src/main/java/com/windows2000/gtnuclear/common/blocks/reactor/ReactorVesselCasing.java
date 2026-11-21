package com.windows2000.gtnuclear.common.blocks.reactor;

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

/** Casing block for the pressurized water reactor multiblock's cylindrical vessel wall/caps. */
public class ReactorVesselCasing extends VariantBlock<ReactorVesselCasing.CasingType> {

    public ReactorVesselCasing() {
        super(Material.IRON);
        setTranslationKey("reactor_vessel_casing");
        setHardness(200.0F);
        setResistance(320.0F);
        setSoundType(SoundType.METAL);
        setHarvestLevel(ToolClasses.WRENCH, 3);
        setDefaultState(this.getState(CasingType.PWR_REACTOR_VESSEL));
    }

    @Override
    public boolean canCreatureSpawn(@NotNull IBlockState state, @NotNull IBlockAccess world, @NotNull BlockPos pos,
                                    @NotNull EntityLiving.SpawnPlacementType type) {
        return false;
    }

    public enum CasingType implements IStringSerializable {

        PWR_REACTOR_VESSEL("pwr_reactor_vessel");

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
