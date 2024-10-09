package dev.neire.mc.youdonthavetheright.mixins.compat.brb;

import dev.neire.mc.youdonthavetheright.logic.crafter.BrewingLogic;
import marsh.town.brb.brewingstand.BrewableResult;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static marsh.town.brb.brewingstand.PlatformPotionUtil.*;

@Mixin(BrewableResult.class)
public abstract class BrewableResultMixin {
    @Shadow
    public PotionBrewing.Mix<?> recipe;
    @Shadow
    public ResourceLocation input;

    @Inject(method = "id", at = @At("RETURN"), cancellable = true, remap = false)
    private void idOverride(CallbackInfoReturnable<ResourceLocation> cir) {
        cir.setReturnValue(BrewingLogic.INSTANCE.buildRecipeId(
            input,
            ForgeRegistries.POTIONS.getKey(getFrom(recipe)),
            ForgeRegistries.ITEMS.getKey(getIngredient(recipe).getItems()[0].getItem()),
            ForgeRegistries.POTIONS.getKey(getTo(recipe))
        ));
    }
}
