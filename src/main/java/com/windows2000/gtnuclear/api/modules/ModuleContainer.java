package com.windows2000.gtnuclear.api.modules;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotate your {@link IModuleContainer} implementation with this for it to be automatically discovered and
 * registered. Only needed if you're adding a container beyond the one this template already registers manually
 * in its {@code @Mod} class (e.g. an addon-of-an-addon contributing its own container).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ModuleContainer {}
