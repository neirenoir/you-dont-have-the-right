package dev.neire.mc.youdonthavetheright.mixins;

import dev.neire.mc.youdonthavetheright.config.YdhtrConfig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.RecipeHolder;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(RecipeHolder.class)
public abstract class RecipeHolderMixin implements RecipeHolder {

    @Inject(
        method = "awardUsedRecipes",
        at = @At(
            value = "INVOKE",
            target =
                "Lnet/minecraft/world/entity/player/Player;awardRecipes(Ljava/util/Collection;)I"
        ),
        cancellable = true
    )
    public void handleAwardUsedRecipesBefore(
        Player p_281647_, List<ItemStack> p_282578_, CallbackInfo ci
    ) {
        if (YdhtrConfig.FORCE_PREVENT_IMPLICIT_LEARNING.get()) {
            this.setRecipeUsed(null);
            ci.cancel();
        }
    }
}
