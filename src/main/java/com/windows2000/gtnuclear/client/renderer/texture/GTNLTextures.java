package com.windows2000.gtnuclear.client.renderer.texture;

import gregtech.client.renderer.texture.cube.OrientedOverlayRenderer;
import gregtech.client.renderer.texture.cube.SimpleOverlayRenderer;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.relauncher.Side;

import com.windows2000.gtnuclear.api.GTNuclearValues;

@Mod.EventBusSubscriber(modid = GTNuclearValues.MODID, value = Side.CLIENT)
public class GTNLTextures {

    public static final SimpleOverlayRenderer PWR_REACTOR_VESSEL_CASING = new SimpleOverlayRenderer(
            "casings/multiblockcasings/fisson/pwr/reactor_vessel");

    public static final OrientedOverlayRenderer PWR_FISSION_REACTOR_OVERLAY = new OrientedOverlayRenderer(
            "multiblock/fission_reactor/pwr");

    public static final OrientedOverlayRenderer HP_TURBINE_OVERLAY = new OrientedOverlayRenderer(
            "multiblock/turbine/hp");

    public static final OrientedOverlayRenderer LP_TURBINE_OVERLAY = new OrientedOverlayRenderer(
            "multiblock/turbine/lp");

    public static void preInit() {}
}
