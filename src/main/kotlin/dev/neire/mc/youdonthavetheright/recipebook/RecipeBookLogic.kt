package dev.neire.mc.youdonthavetheright.recipebook

import dev.neire.mc.youdonthavetheright.YouDontHaveTheRight
import dev.neire.mc.youdonthavetheright.api.capability.RecipeBibleCapability
import dev.neire.mc.youdonthavetheright.api.capability.YdhtrCapabilities.RECIPE_BIBLE_CAPABILITY
import net.minecraft.core.Direction
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.crafting.RecipeType
import net.minecraftforge.common.brewing.BrewingRecipe
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.capabilities.ICapabilityProvider
import net.minecraftforge.common.util.LazyOptional
import net.minecraftforge.event.AttachCapabilitiesEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.registries.ForgeRegistries
import net.minecraftforge.registries.RegisterEvent


object RecipeBookLogic {
    var BREWING_RECIPE_TYPE: RecipeType<RecipeBrewingRecipe>? = null
    val BREWING_RECIPE_TYPE_KEY = "brewing"

    @SubscribeEvent
    fun attachRecipeBibleToPlayer(event: AttachCapabilitiesEvent<Entity>) {
        val target = event.getObject()
        if (target !is Player) {
            return
        }

        val recipeBible =
            target.getCapability(RECIPE_BIBLE_CAPABILITY).orElse(RecipeBibleCapability())
        if (!target.getCapability(RECIPE_BIBLE_CAPABILITY).isPresent) {
            event.addCapability(
                ResourceLocation(YouDontHaveTheRight.ID, "recipe_bible"),
                object : ICapabilityProvider {
                    val recipeBibleLazyOptional = LazyOptional.of { recipeBible }

                    override fun <T : Any?> getCapability(
                        cap: Capability<T>,
                        dir: Direction?
                    ): LazyOptional<T> {
                        return if (cap == RECIPE_BIBLE_CAPABILITY) recipeBibleLazyOptional.cast()
                            else LazyOptional.empty()
                    }
                }
            )
        }
    }

    @SubscribeEvent
    fun registerBrewingRecipeType(event: RegisterEvent) {
        if (event.getForgeRegistry<RecipeType<*>>() != ForgeRegistries.RECIPE_TYPES) {
            return
        }

        val recipeType = object : RecipeType<RecipeBrewingRecipe> {
            override fun toString(): String {
                return BREWING_RECIPE_TYPE_KEY
            }
        }

        event.register(
            ForgeRegistries.RECIPE_TYPES.registryKey
        ) { helper -> helper.register(ResourceLocation(BREWING_RECIPE_TYPE_KEY), recipeType) }

        ForgeRegistries.RECIPE_SERIALIZERS.register(
            BREWING_RECIPE_TYPE_KEY, RecipeBrewingRecipe.Serializer
        )

        @Suppress("UNCHECKED_CAST")
        BREWING_RECIPE_TYPE =
            BuiltInRegistries.RECIPE_TYPE.get(
                ResourceLocation(BREWING_RECIPE_TYPE_KEY)
            ) as RecipeType<RecipeBrewingRecipe>
    }
}