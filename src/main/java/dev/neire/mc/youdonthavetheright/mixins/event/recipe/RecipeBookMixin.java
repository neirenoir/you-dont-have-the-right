package dev.neire.mc.youdonthavetheright.mixins.event.recipe;

import dev.neire.mc.youdonthavetheright.event.recipe.RecipeEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.ServerRecipeBook;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.Collection;

@Mixin(ServerRecipeBook.class)
public abstract class RecipeBookMixin {
    @Inject(at = @At("HEAD"), method = "addRecipes", cancellable = true)
    private void beforeAddRecipes(
        Collection<Recipe<?>> recipes, ServerPlayer player, CallbackInfoReturnable<Integer> cir
    ) {
        boolean cancel = false;
        for (Recipe<?> recipe : recipes) {
            var recipeEvent = new RecipeEvents.RecipeLearned.Before(recipe.getId(), player);
            MinecraftForge.EVENT_BUS.post(recipeEvent);

            if (recipeEvent.isCanceled()) {
                cancel = true;
            }
        }

        if (cancel) {
            cir.setReturnValue(0);
            cir.cancel();
        }
    }

    @Inject(at = @At("TAIL"), method = "addRecipes")
    private void afterAddRecipes(
        Collection<Recipe<?>> recipes, ServerPlayer player, CallbackInfoReturnable<Integer> cir
    ) {
        for (Recipe<?> recipe : recipes) {
            var recipeEvent = new RecipeEvents.RecipeLearned.After(recipe.getId(), player);
            MinecraftForge.EVENT_BUS.post(recipeEvent);
        }
    }

    @Inject(at = @At("HEAD"), method = "removeRecipes", cancellable = true)
    private void beforeRemoveRecipes(
        Collection<Recipe<?>> recipes, ServerPlayer player, CallbackInfoReturnable<Integer> cir
    ) {
        boolean cancel = false;
        for (Recipe<?> recipe : recipes) {
            var recipeEvent = new RecipeEvents.RecipeForgotten.Before(recipe.getId(), player);
            MinecraftForge.EVENT_BUS.post(recipeEvent);

            if (recipeEvent.isCanceled()) {
                cancel = true;
            }
        }

        if (cancel) {
            cir.setReturnValue(0);
            cir.cancel();
        }
    }

    @Inject(at = @At("TAIL"), method = "removeRecipes")
    private void afterRemoveRecipes(
        Collection<Recipe<?>> recipes, ServerPlayer player, CallbackInfoReturnable<Integer> cir
    ) {
        for (Recipe<?> recipe : recipes) {
            var recipeEvent = new RecipeEvents.RecipeForgotten.After(recipe.getId(), player);
            MinecraftForge.EVENT_BUS.post(recipeEvent);
        }
    }
}
