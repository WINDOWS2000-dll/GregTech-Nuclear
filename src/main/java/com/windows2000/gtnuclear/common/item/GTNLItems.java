package com.windows2000.gtnuclear.common.item;

import com.windows2000.gtnuclear.api.GTNuclearValues;
import com.windows2000.gtnuclear.api.nuclear.fuel.GTNLFuelCompositions;
import com.windows2000.gtnuclear.common.ConfigHolder;

import java.util.Arrays;
import java.util.List;

/** Plain (non-GT-MetaItem) item field declarations for this mod, mirroring {@code GTNLMetaBlocks}. */
public class GTNLItems {

    private GTNLItems() {}

    /**
     * One entry per {@link GTNLFuelCompositions} enrichment tier. {@code MetaTileEntityFuelPort#isItemAllowed}
     * accepts any item in this list, so adding a fuel type later is just adding another entry here.
     */
    public static ItemFuelRod FUEL_ROD_NATURAL_URANIUM;
    public static ItemFuelRod FUEL_ROD_LEU;
    public static ItemFuelRod FUEL_ROD_HALEU;
    public static ItemFuelRod FUEL_ROD_HEU;
    public static List<ItemFuelRod> ALL_FUEL_RODS;

    public static ItemControlRod CONTROL_ROD_STANDARD;
    public static ItemControlRod CONTROL_ROD_HIGH_VALUE;
    public static List<ItemControlRod> ALL_CONTROL_RODS;

    /** See {@link ItemBrokenControlRod}'s own JavaDoc -- the single shared "worn-out rod" item. */
    public static ItemBrokenControlRod BROKEN_CONTROL_ROD;

    /** Held by the reactor's startup neutron source hatch -- see {@code MetaTileEntityNeutronSourceHatch}. */
    public static ItemNeutronSourceRod NEUTRON_SOURCE_ROD;

    /**
     * Tint colors ({@code ITintedItem}): enrichment tiers run pale green-grey (natural, unenriched metal) through
     * yellow-green (LEU, the real UO2 pellet hue) to orange/red (HALEU/HEU) -- a color ramp intuitively readable
     * as "rising enrichment/danger" without needing a tooltip to explain it.
     */
    private static final int TINT_NATURAL_URANIUM = 0xB8C9A8;
    private static final int TINT_LEU = 0xC9D94A;
    private static final int TINT_HALEU = 0xE08A2E;
    private static final int TINT_HEU = 0xD33B2C;
    /** Dark grey, the real color of boron carbide (the standard neutron-absorbing control rod material). */
    private static final int TINT_CONTROL_ROD_STANDARD = 0x3A3A3E;
    /** Deep blue, distinguishing the premium tier from the standard rod's realistic dark grey. */
    private static final int TINT_CONTROL_ROD_HIGH_VALUE = 0x2E4E8A;
    /** Magenta/purple, distinct from every fuel/control rod tint, evoking an exotic radioactive source. */
    private static final int TINT_NEUTRON_SOURCE_ROD = 0x8A2E8A;

    public static void init() {
        FUEL_ROD_NATURAL_URANIUM = new ItemFuelRod(GTNLFuelCompositions.NATURAL_URANIUM, "natural_uranium",
                TINT_NATURAL_URANIUM);
        FUEL_ROD_NATURAL_URANIUM.setRegistryName(GTNuclearValues.MODID, "fuel_rod_natural_uranium");

        FUEL_ROD_LEU = new ItemFuelRod(GTNLFuelCompositions.LEU, "leu", TINT_LEU);
        FUEL_ROD_LEU.setRegistryName(GTNuclearValues.MODID, "fuel_rod_leu");

        FUEL_ROD_HALEU = new ItemFuelRod(GTNLFuelCompositions.HALEU, "haleu", TINT_HALEU);
        FUEL_ROD_HALEU.setRegistryName(GTNuclearValues.MODID, "fuel_rod_haleu");

        FUEL_ROD_HEU = new ItemFuelRod(GTNLFuelCompositions.HEU, "heu", TINT_HEU);
        FUEL_ROD_HEU.setRegistryName(GTNuclearValues.MODID, "fuel_rod_heu");

        ALL_FUEL_RODS = Arrays.asList(FUEL_ROD_NATURAL_URANIUM, FUEL_ROD_LEU, FUEL_ROD_HALEU, FUEL_ROD_HEU);

        CONTROL_ROD_STANDARD = new ItemControlRod(ConfigHolder.reactor.standardControlRodMaxWorth, "standard",
                TINT_CONTROL_ROD_STANDARD);
        CONTROL_ROD_STANDARD.setRegistryName(GTNuclearValues.MODID, "control_rod_standard");

        CONTROL_ROD_HIGH_VALUE = new ItemControlRod(ConfigHolder.reactor.highValueControlRodMaxWorth, "high_value",
                TINT_CONTROL_ROD_HIGH_VALUE);
        CONTROL_ROD_HIGH_VALUE.setRegistryName(GTNuclearValues.MODID, "control_rod_high_value");

        ALL_CONTROL_RODS = Arrays.asList(CONTROL_ROD_STANDARD, CONTROL_ROD_HIGH_VALUE);

        BROKEN_CONTROL_ROD = new ItemBrokenControlRod();
        BROKEN_CONTROL_ROD.setRegistryName(GTNuclearValues.MODID, "broken_control_rod");

        NEUTRON_SOURCE_ROD = new ItemNeutronSourceRod("californium_252", TINT_NEUTRON_SOURCE_ROD);
        NEUTRON_SOURCE_ROD.setRegistryName(GTNuclearValues.MODID, "neutron_source_rod_californium_252");
    }
}
