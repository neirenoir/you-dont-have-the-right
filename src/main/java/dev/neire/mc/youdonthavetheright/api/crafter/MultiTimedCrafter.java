package dev.neire.mc.youdonthavetheright.api.crafter;

import net.minecraft.world.Container;
import net.minecraft.world.item.crafting.Recipe;

import java.util.List;

public interface MultiTimedCrafter<T extends Container> extends TimedCrafter<T> {
    List<Recipe<T>> getCurrentRecipes();
    default Recipe<T> getCurrentRecipe() {
        return getCurrentRecipes().get(0);
    }
    default void setCurrentRecipe(Recipe<T> recipe) {
        getCurrentRecipes().set(0, recipe);
    }
}
