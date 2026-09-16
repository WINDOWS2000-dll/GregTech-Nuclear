package com.windows2000.gtnuclear.api.unification.materials;

import com.windows2000.gtnuclear.api.GTNuclearValues;
import com.windows2000.gtnuclear.api.unification.materials.series.ActiniumSeriesMaterials;
import com.windows2000.gtnuclear.api.unification.materials.series.NeptuniumSeriesMaterials;
import com.windows2000.gtnuclear.api.unification.materials.series.ThoriumSeriesMaterials;
import com.windows2000.gtnuclear.api.unification.materials.series.UraniumSeriesMaterials;

import gregtech.api.GregTechAPI;
import gregtech.api.unification.material.Material;
import gregtech.api.unification.material.event.MaterialEvent;
import gregtech.api.unification.material.event.MaterialRegistryEvent;

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Material field declarations for this mod, mirroring {@code gregtech.api.unification.material.Materials}: fields
 * live here, actual construction is delegated to one {@code register()} call per decay series/category (see
 * {@code api.unification.materials.series}), also mirroring {@code ElementMaterials}/{@code FirstDegreeMaterials}
 * etc.
 * <p>
 * Registration is driven by GT's own {@link MaterialRegistryEvent}/{@link MaterialEvent}, <b>not</b> this mod's own
 * {@link com.windows2000.gtnuclear.api.modules.IModule} lifecycle -- both events fire during GT's own preInit
 * (before this mod's preInit even starts, since {@code required-after:gregtech} only orders same-phase lifecycle
 * events against each other), and GT's material registries are frozen before that finishes. This class is
 * <b>not</b> {@code @Mod.EventBusSubscriber}-annotated; {@code CoreModule#construction} registers it to the Forge
 * event bus explicitly instead (during the even-earlier Construction phase, which precedes preInit for every mod
 * regardless of dependency order), so all of this mod's event-bus wiring stays visible in one place rather than
 * scattered across annotations.
 */
public class GTNLMaterials {

    private GTNLMaterials() {}

    /*
     * Uranium series (U-238 decay chain), IDs 1-14. See UraniumSeriesMaterials for the actual registration and
     * nuclide data.
     */
    public static Material Thorium234;
    public static Material Protactinium234;
    public static Material Uranium234;
    public static Material Thorium230;
    public static Material Radium226;
    public static Material Radon222;
    public static Material Polonium218;
    public static Material Lead214;
    public static Material Bismuth214;
    public static Material Polonium214;
    public static Material Lead210;
    public static Material Bismuth210;
    public static Material Polonium210;
    public static Material Lead206;

    /*
     * Actinium series (U-235 decay chain), IDs 15-27. See ActiniumSeriesMaterials. Includes two branches at/above
     * the 0.1% inclusion threshold: Actinium-227's 1.38% alpha branch (-> Francium-223) and Bismuth-211's 0.276%
     * beta-minus branch (-> Polonium-211); both reconverge on the main chain (Ra-223 / Pb-207 respectively).
     */
    public static Material Thorium231;
    public static Material Protactinium231;
    public static Material Actinium227;
    public static Material Francium223;
    public static Material Thorium227;
    public static Material Radium223;
    public static Material Radon219;
    public static Material Polonium215;
    public static Material Lead211;
    public static Material Bismuth211;
    public static Material Polonium211;
    public static Material Thallium207;
    public static Material Lead207;

    /*
     * Thorium series (Th-232 decay chain), IDs 28-39. See ThoriumSeriesMaterials. Unlike the uranium/actinium
     * series, GTST has no existing isotope-flagged "Th-232" Element to build on (only the generic, non-isotope
     * "Thorium" used by GTST's own Materials.Thorium) -- Thorium232 here is a brand-new isotope Element/Material,
     * coexisting with GTST's plain Thorium exactly the way GTST's own generic Uranium/Plutonium coexist with their
     * specific Uranium238/Plutonium239 isotopes. No GTST changes needed for this series.
     */
    public static Material Thorium232;
    public static Material Radium228;
    public static Material Actinium228;
    public static Material Thorium228;
    public static Material Radium224;
    public static Material Radon220;
    public static Material Polonium216;
    public static Material Lead212;
    public static Material Bismuth212;
    public static Material Polonium212;
    public static Material Thallium208;
    public static Material Lead208;

    /*
     * Neptunium series (4n+1 chain, reached from Pu-241 -> Am-241 -> Np-237; already defined natively in GTST, see
     * Elements.Pu241's decayModes), IDs 40-53. See NeptuniumSeriesMaterials. Includes one branch at/above the 0.1%
     * inclusion threshold (Bismuth-213: 97.80% beta-minus, 2.20% alpha), reconverging on Pb-209 one step later.
     * Bismuth-209 (the chain's endpoint) is technically alpha-radioactive in reality (half-life ~2.01e19 years,
     * over a billion times the age of the universe) but is modeled as stable here -- see NeptuniumSeriesMaterials.
     */
    public static Material Americium241;
    public static Material Neptunium237;
    public static Material Protactinium233;
    public static Material Uranium233;
    public static Material Thorium229;
    public static Material Radium225;
    public static Material Actinium225;
    public static Material Francium221;
    public static Material Astatine217;
    public static Material Bismuth213;
    public static Material Polonium213;
    public static Material Thallium209;
    public static Material Lead209;
    public static Material Bismuth209;

    @SubscribeEvent
    public static void onMaterialRegistryEvent(MaterialRegistryEvent event) {
        GregTechAPI.materialManager.createRegistry(GTNuclearValues.MODID);
    }

    @SubscribeEvent
    public static void onMaterialEvent(MaterialEvent event) {
        UraniumSeriesMaterials.register();
        ActiniumSeriesMaterials.register();
        ThoriumSeriesMaterials.register();
        NeptuniumSeriesMaterials.register();
    }
}
