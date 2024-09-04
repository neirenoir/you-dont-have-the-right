package dev.neire.mc.youdonthavetheright.mixins.divineinspiration;

import dev.neire.mc.youdonthavetheright.config.YdhtrConfig;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.commands.CommandFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AdvancementRewards.class)
public abstract class AdvancementRewardsMixin {

    @Shadow @Final
    private int experience;
    @Shadow @Final
    private ResourceLocation[] loot;
    @Shadow @Final
    private ResourceLocation[] recipes;
    @Shadow @Final
    private CommandFunction.CacheableFunction function;

    @Redirect(
        method = "grant",
        at = @At(
            value = "INVOKE",
            target =
                "Lnet/minecraft/server/level/ServerPlayer;awardRecipesByKey(" +
                    "[Lnet/minecraft/resources/ResourceLocation;" +
                ")V"
        )
    )
    public void handleGrantBefore(ServerPlayer player, ResourceLocation[] resourceLocations) {
        // This prevents those pesky non-Advancements you get from picking up items in
        // the vanilla datapack. Proper Advancements that grant Experience will still work
        // If only I still had the context of the Advancement here...
        if (
            YdhtrConfig.FORCE_PREVENT_DIVINE_INSPIRATION.get()
            && (
                experience == 0
                && loot.length == 0
                && (function == CommandFunction.CacheableFunction.NONE  || function == null)
            )
        ) {
            return;
        }

        // Passthrough to original function
        player.awardRecipesByKey(this.recipes);
    }

}
