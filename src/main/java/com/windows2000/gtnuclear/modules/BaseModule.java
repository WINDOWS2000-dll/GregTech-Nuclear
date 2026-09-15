package com.windows2000.gtnuclear.modules;

import java.util.Collections;
import java.util.Set;

import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.NotNull;

import com.windows2000.gtnuclear.api.modules.IModule;
import com.windows2000.gtnuclear.api.util.Utils;

/**
 * Convenience base for non-core modules of this mod's own container: depends on {@link Modules#MODULE_CORE} by
 * default, so it always loads after the core module without every module having to repeat that declaration.
 */
public abstract class BaseModule implements IModule {

    @NotNull
    @Override
    public Set<ResourceLocation> getDependencyUids() {
        return Collections.singleton(Utils.id(Modules.MODULE_CORE));
    }
}
