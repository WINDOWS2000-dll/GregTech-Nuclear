package com.windows2000.gtnuclear.common.machines;

import gregtech.api.recipes.RecipeMap;
import gregtech.api.recipes.RecipeMapBuilder;
import gregtech.api.recipes.builders.SimpleRecipeBuilder;

/**
 * This addon's own {@link RecipeMap}s. See {@link gregtech.api.recipes.RecipeMaps} in GregTech itself for many
 * more examples of the builder options available (GUI texture slots, sounds, progress bar style, etc.) -- this
 * one is left at the bare minimum needed to compile and work.
 */
public class TemplateRecipeMaps {

    public static final RecipeMap<SimpleRecipeBuilder> SAMPLE_MACHINE_RECIPES = new RecipeMapBuilder<>(
            "sample_machine", new SimpleRecipeBuilder())
                    .itemInputs(1)
                    .itemOutputs(1)
                    .build();

    private TemplateRecipeMaps() {}
}
