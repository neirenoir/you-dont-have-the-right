package dev.neire.mc.youdonthavetheright.api.capability;

import net.minecraft.resources.ResourceLocation;

public interface RecipeBookCapability {
    boolean hasRecipe(ResourceLocation recipe);
}
