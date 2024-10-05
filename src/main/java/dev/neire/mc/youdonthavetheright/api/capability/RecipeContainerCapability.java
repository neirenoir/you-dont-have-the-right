package dev.neire.mc.youdonthavetheright.api.capability;

import net.minecraft.world.Container;
import net.minecraft.world.item.crafting.Recipe;

public interface RecipeContainerCapability<R extends Container, T extends Recipe<R>> {
    void setRecipe(int slot, T recipe);
    T getRecipe(int slot);
    int getRecipeCapacity();
}
