package dev.neire.mc.youdonthavetheright.mixins.crafter;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(PotionBrewing.class)
public interface PotionBrewingAccessor {

    @Accessor("POTION_MIXES")
    static List<PotionBrewing.Mix<Potion>> getPotionMixes() {
        throw new AssertionError();
    }

    @Accessor("CONTAINER_MIXES")
    static List<PotionBrewing.Mix<Item>> getContainerMixes() {
        throw new AssertionError();
    }

    @Invoker("addMix")
    static void addMix(Potion input, Item ingredient, Potion output) {
        throw new AssertionError();
    }

    @Invoker("addContainer")
    static void addContainer(Item ingredient) {
        throw new AssertionError();
    }

    @Invoker("addContainerRecipe")
    static void addContainerRecipe(Item input, Item ingredient, Item output) {
        throw new AssertionError();
    }

}
