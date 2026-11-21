package com.windows2000.gtnuclear.modules;

import net.minecraft.util.ResourceLocation;

import com.windows2000.gtnuclear.api.modules.IModule;
import com.windows2000.gtnuclear.api.util.GTNLUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Set;

/**
 * Convenience base for non-core modules of this mod's own container: depends on {@link Modules#MODULE_CORE} by
 * default, so it always loads after the core module without every module having to repeat that declaration.
 */
public abstract class BaseModule implements IModule {

    @NotNull
    @Override
    public Set<ResourceLocation> getDependencyUids() {
        return Collections.singleton(GTNLUtils.gtnlID(Modules.MODULE_CORE));
    }
}
