package dev.neire.mc.youdonthavetheright.mixins.divineinspiration;

import dev.neire.mc.youdonthavetheright.config.YdhtrConfig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.RecipeHolder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class ResultContainerRecipeHolderMixin implements RecipeHolder {

    @Inject(
        method = "awardUsedRecipes",
        at = @At(value = "HEAD"),
        cancellable = true
    )
    public void handleAwardUsedRecipesBefore(
        Player p_58396_, List<ItemStack> p_282202_, CallbackInfo ci
    ) {
        if (YdhtrConfig.FORCE_PREVENT_DIVINE_INSPIRATION.get()) {
            this.setRecipeUsed(null);
            ci.cancel();
        }
    }
}
