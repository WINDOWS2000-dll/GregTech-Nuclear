package com.windows2000.gtnuclear.common.metatileentities;

import com.windows2000.gtnuclear.common.metatileentities.storage.MetaTileEntityIsotopeStorage;

/**
 * MetaTileEntity field declarations for this mod, mirroring GT's own {@code common.metatileentities.MetaTileEntities}:
 * fields live here, actual construction/registration happens in {@link GTNLMetaTileEntityRegistration}.
 */
public class GTNLMetaTileEntities {

    private GTNLMetaTileEntities() {}

    public static MetaTileEntityIsotopeStorage ISOTOPE_STORAGE;
}
