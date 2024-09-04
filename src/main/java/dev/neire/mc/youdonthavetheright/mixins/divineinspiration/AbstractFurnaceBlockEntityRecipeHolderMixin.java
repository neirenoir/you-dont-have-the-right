package dev.neire.mc.youdonthavetheright.mixins.divineinspiration;

import dev.neire.mc.youdonthavetheright.config.YdhtrConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.RecipeHolder;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;


@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class AbstractFurnaceBlockEntityRecipeHolderMixin implements RecipeHolder {

    @Shadow @Final
    private  Object2IntOpenHashMap<ResourceLocation> recipesUsed;

    @Inject(
        method = "awardUsedRecipesAndPopExperience",
        at = @At(
            value = "INVOKE",
            target =
                "Lnet/minecraft/server/level/ServerPlayer;awardRecipes(Ljava/util/Collection;)I"
        ),
        cancellable = true
    )
    public void handleAwardUsedRecipesBefore(
        ServerPlayer p_281647_, CallbackInfo ci
    ) {
        if (YdhtrConfig.FORCE_PREVENT_DIVINE_INSPIRATION.get()) {
            this.recipesUsed.clear();
            ci.cancel();
        }
    }
}
