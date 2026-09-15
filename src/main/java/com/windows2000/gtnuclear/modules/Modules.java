package com.windows2000.gtnuclear.modules;

import com.windows2000.gtnuclear.api.GTNuclearValues;
import com.windows2000.gtnuclear.api.modules.IModuleContainer;

/**
 * This mod's single {@link IModuleContainer}, registered manually by {@code GTNuclear} (see its
 * {@code onConstruction}). Add a constant here for each new module ID you introduce.
 */
public class Modules implements IModuleContainer {

    public static final String MODULE_CORE = "core";
    public static final String MODULE_MACHINES = "machines";

    @Override
    public String getID() {
        return GTNuclearValues.MODID;
    }
}
