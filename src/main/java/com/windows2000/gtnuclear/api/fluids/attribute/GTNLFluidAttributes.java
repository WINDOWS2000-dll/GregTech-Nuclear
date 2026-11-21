package com.windows2000.gtnuclear.api.fluids.attribute;

import gregtech.api.fluids.attribute.FluidAttribute;

import net.minecraft.client.resources.I18n;

import com.windows2000.gtnuclear.api.util.GTNLUtils;

/**
 * Mirrors {@code gregtech.api.fluids.attribute.FluidAttributes} -- see {@link FluidAttribute}'s own JavaDoc
 * (referenced from {@code gregtech.api.capability.IPropertyFluidFilter}) for how this mechanism already gives us
 * "only specific pipes can carry this fluid" for free: a pipe's {@code FluidPipeProperties} only accepts fluids
 * whose attributes it explicitly declares support for via {@code setCanContain}, so any fluid carrying one of
 * these attributes is automatically rejected by every ordinary GT fluid pipe with no GTST changes needed.
 */
public final class GTNLFluidAttributes {

    /**
     * Marks a fluid as belonging to the PWR primary coolant loop. Real reactor-scale coolant flow would require
     * absurdly high-tier ordinary GT pipes just for throughput; a dedicated pipe (see
     * {@code com.windows2000.gtnuclear.api.unification.materials.reactor.ReactorFluidMaterials}) is built with much
     * higher throughput instead, and this attribute keeps the fluid from leaking into (or being cheaply
     * transported by) the ordinary pipe network.
     */
    public static final FluidAttribute REACTOR_COOLANT_WATER = new FluidAttribute(
            GTNLUtils.gtnlID("reactor_coolant_water"),
            list -> list.add(I18n.format("gtnuclear.fluid.type_reactor_coolant.tooltip")),
            list -> list.add(I18n.format("gtnuclear.fluid_pipe.reactor_coolant_proof")));

    /**
     * Marks a fluid as high-level radioactive primary coolant system letdown waste -- see this mod's design
     * memory for the boron letdown mechanism this exists for. Reuses the same dedicated-pipe pattern as
     * {@link #REACTOR_COOLANT_WATER} (declared containable on {@code ReactorCoolantPipeAlloy} alongside it)
     * specifically so this waste liquid cannot leak into or be moved by the ordinary GT pipe network.
     */
    public static final FluidAttribute RADIOACTIVE_WASTEWATER = new FluidAttribute(
            GTNLUtils.gtnlID("radioactive_wastewater"),
            list -> list.add(I18n.format("gtnuclear.fluid.type_radioactive_wastewater.tooltip")),
            list -> list.add(I18n.format("gtnuclear.fluid_pipe.radioactive_wastewater_proof")));

    private GTNLFluidAttributes() {}
}
