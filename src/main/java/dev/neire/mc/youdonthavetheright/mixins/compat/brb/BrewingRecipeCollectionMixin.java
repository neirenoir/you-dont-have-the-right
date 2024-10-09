package dev.neire.mc.youdonthavetheright.mixins.compat.brb;

import marsh.town.brb.brewingstand.BrewableResult;
import marsh.town.brb.brewingstand.BrewingRecipeCollection;
import marsh.town.brb.generic.GenericRecipeBookCollection;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.inventory.BrewingStandMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

import static dev.neire.mc.youdonthavetheright.api.capability.YdhtrCapabilities.RECIPE_BIBLE_CAPABILITY;

@Mixin(BrewingRecipeCollection.class)
public abstract class BrewingRecipeCollectionMixin extends
    GenericRecipeBookCollection<BrewableResult, BrewingStandMenu>
{
    protected BrewingRecipeCollectionMixin(
        List<? extends BrewableResult> list, BrewingStandMenu menu, RegistryAccess registryAccess
    ) {
        super(list, menu, registryAccess);
    }

    @Inject(method = "getDisplayRecipes", at = @At("RETURN"), cancellable = true, remap = false)
    private void onDisplayRecipes(CallbackInfoReturnable<List<BrewableResult>> cir) {
        /*
        final var player = Minecraft.getInstance().player;
        final var finalRecipeList = new ArrayList<BrewableResult>();
        if (player == null) {
            return;
        }

        player.getCapability(RECIPE_BIBLE_CAPABILITY).ifPresent(
            (b) -> {
                var recipes = cir.getReturnValue();
                for (BrewableResult result : recipes) {
                    if (b.hasRecipe(result.id())) {
                        finalRecipeList.add(result);
                    }
                }
            }
        );

        cir.setReturnValue(finalRecipeList);
         */
    }
}
