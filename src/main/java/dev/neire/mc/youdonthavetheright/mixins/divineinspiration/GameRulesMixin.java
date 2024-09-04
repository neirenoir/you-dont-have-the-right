package dev.neire.mc.youdonthavetheright.mixins.divineinspiration;

import dev.neire.mc.youdonthavetheright.config.YdhtrConfig;
import net.minecraft.world.level.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(GameRules.class)
public abstract class GameRulesMixin {

    @Inject(
        method = "getBoolean",
        at = @At(value = "RETURN"),
        cancellable = true
    )
    public void handleGetBooleanForLimitedCrafting(
        GameRules.Key<GameRules.BooleanValue> key, CallbackInfoReturnable<Boolean> cir
    ) {
        if (
            key.equals(GameRules.RULE_LIMITED_CRAFTING)
            && YdhtrConfig.FORCE_LIMITED_CRAFTING.get()
        ) {
            cir.setReturnValue(true);
        }
    }
}
