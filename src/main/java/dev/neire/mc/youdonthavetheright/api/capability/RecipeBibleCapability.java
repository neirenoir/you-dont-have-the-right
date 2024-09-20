package dev.neire.mc.youdonthavetheright.api.capability;

import net.minecraft.resources.ResourceLocation;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class RecipeBibleCapability implements RecipeBookCapability {
    private final Set<RecipeBookCapability> recipeLibrary;

    public RecipeBibleCapability() {
        recipeLibrary = new HashSet<>();
    }

    public boolean hasRecipe(ResourceLocation recipe) {
        for (RecipeBookCapability recipeBookCapability : recipeLibrary) {
            if (recipeBookCapability.hasRecipe(recipe)) {
                return true;
            }
        }
        return false;
    }

    public <T extends RecipeBookCapability> Optional<T> getCapabilityType(Class<T> type) {
        for (RecipeBookCapability recipeBookCapability : recipeLibrary) {
            if (type.isInstance(recipeBookCapability)) {
                @SuppressWarnings("unchecked")
                var opt = Optional.of((T) recipeBookCapability);
                return opt;
            }
        }
        return Optional.empty();
    }

    public void addRecipeBook(RecipeBookCapability recipeBook) {
        recipeLibrary.add(recipeBook);
    }

    public void removeRecipeBook(RecipeBookCapability recipeBook) {
        recipeLibrary.remove(recipeBook);
    }

}
