package dev.neire.mc.youdonthavetheright.api;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.RecipeHolder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

public interface TimedCrafter<T extends Container> extends RecipeHolder, Container {
    boolean isRunning();
    int getRunway();
    void setRunway(int runway);
    Recipe<T> getCurrentRecipe();
    void setCurrentRecipe(Recipe<T> recipe, ICapabilityProvider owner);
    int getProgress();
    void setProgress(int progress);
    Recipe<T> calculateRecipe();
    NonNullList<ItemStack> getItems();
    void updateState(Level level, BlockPos pos, BlockState state);
    RecipeType<Recipe<T>> getRecipeType();
}
