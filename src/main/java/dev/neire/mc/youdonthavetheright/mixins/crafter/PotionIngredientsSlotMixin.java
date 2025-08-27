package dev.neire.mc.youdonthavetheright.mixins.crafter;

import com.mojang.authlib.minecraft.client.MinecraftClient;
import dev.neire.mc.youdonthavetheright.logic.crafter.BrewingLogic;
import net.minecraft.client.Minecraft;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net.minecraft.world.inventory.BrewingStandMenu$IngredientsSlot")
public abstract class PotionIngredientsSlotMixin extends Slot {
    private PotionIngredientsSlotMixin(BrewingStandBlockEntity p_40223_, int p_40224_, int p_40225_, int p_40226_) {
        super(p_40223_, p_40224_, p_40225_, p_40226_);
    }

    @Redirect(
            method = "mayPlace(Lnet/minecraft/world/item/ItemStack;)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraftforge/common/brewing/BrewingRecipeRegistry;isValidIngredient(Lnet/minecraft/world/item/ItemStack;)Z"
            )
    )
    private boolean redirectIsValidIngredient(ItemStack ingredient) {
        Level level = null;
        if (this.container instanceof BrewingStandBlockEntity) {
            // This is serverside code
            level = ((BrewingStandBlockEntity) this.container).getLevel();
        } else {
            // This is clientside code
            level = Minecraft.getInstance().level;
        }

        return BrewingLogic.INSTANCE.isValidIngredient(level, ingredient);
    }
}
