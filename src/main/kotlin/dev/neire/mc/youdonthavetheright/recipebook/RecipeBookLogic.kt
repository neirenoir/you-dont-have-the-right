package dev.neire.mc.youdonthavetheright.recipebook

import dev.neire.mc.youdonthavetheright.YouDontHaveTheRight
import dev.neire.mc.youdonthavetheright.api.capability.RecipeBibleCapability
import dev.neire.mc.youdonthavetheright.api.capability.RecipeBookCapability
import dev.neire.mc.youdonthavetheright.api.capability.YdhtrCapabilities.RECIPE_BIBLE_CAPABILITY
import dev.neire.mc.youdonthavetheright.logic.crafter.BrewingLogic
import net.minecraft.core.Direction
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import net.minecraft.world.item.crafting.Recipe
import net.minecraft.world.item.crafting.RecipeType
import net.minecraft.world.level.Level
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.capabilities.ICapabilityProvider
import net.minecraftforge.common.util.LazyOptional
import net.minecraftforge.event.AttachCapabilitiesEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.registries.ForgeRegistries
import net.minecraftforge.registries.RegisterEvent
import net.minecraftforge.server.ServerLifecycleHooks


@Mod.EventBusSubscriber
object RecipeBookLogic {
    var BREWING_RECIPE_TYPE: RecipeType<Recipe<BrewingLogic.VirtualBrewingStandView>>? = null
    val BREWING_RECIPE_TYPE_KEY = "brewing"

    @SubscribeEvent
    fun attachRecipeBibleToPlayer(event: AttachCapabilitiesEvent<Entity>) {
        val target = event.getObject()
        if (target !is ServerPlayer) {
            return
        }

        val recipeBible =
            target.getCapability(RECIPE_BIBLE_CAPABILITY).orElse(genPlayerRecipeBible(target))
        if (!target.getCapability(RECIPE_BIBLE_CAPABILITY).isPresent) {
            event.addCapability(
                ResourceLocation(YouDontHaveTheRight.ID, "recipe_bible"),
                genRecipeBibleCapabilityProvider(recipeBible)
            )
        }
    }

    @SubscribeEvent
    fun attachRecipeBibleToOverworld(event: AttachCapabilitiesEvent<Level>) {
        if (event.`object` !is ServerLevel) {
            return
        }

        val target = event.`object` as ServerLevel

        if (target.dimension() != ServerLevel.OVERWORLD) {
            return
        }

        val recipeBible = WorldRecipeBook.capability(target).orElse(genLevelRecipeBible(target))
        if (!target.getCapability(RECIPE_BIBLE_CAPABILITY).isPresent) {
            event.addCapability(
                ResourceLocation(YouDontHaveTheRight.ID, "recipe_bible"),
                genRecipeBibleCapabilityProvider(recipeBible)
            )
        }
    }

    private fun genRecipeBibleCapabilityProvider(
        recipeBible: RecipeBibleCapability
    ): ICapabilityProvider {
        return object: ICapabilityProvider {
            val recipeBibleLazyOptional = LazyOptional.of { recipeBible }

            override fun <T : Any?> getCapability(
                cap: Capability<T>,
                dir: Direction?
            ): LazyOptional<T> {
                return if (cap == RECIPE_BIBLE_CAPABILITY) recipeBibleLazyOptional.cast()
                else LazyOptional.empty()
            }
        }
    }

    private fun genLevelRecipeBible(level: ServerLevel): RecipeBibleCapability {
        val bible = RecipeBibleCapability()
        bible.addRecipeBook(
            object: RecipeBookCapability {
                override fun hasRecipe(recipe: ResourceLocation?): Boolean {
                    if (recipe == null) {
                        return false
                    }
                    return WorldRecipeBook.getOrCreate(level).knows(recipe)
                }
            }
        )

        return bible
    }

    private fun genPlayerRecipeBible(player: ServerPlayer): RecipeBibleCapability {
        val bible = RecipeBibleCapability()

        bible.addRecipeBook(
            object: RecipeBookCapability {
                override fun hasRecipe(recipe: ResourceLocation?): Boolean {
                    if (recipe == null) {
                        return false
                    }
                    return player.recipeBook.contains(recipe)
                }
            }
        )

        return bible
    }

    /*
     * Pesky non-autowired events
     */
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
            ) as RecipeType<Recipe<BrewingLogic.VirtualBrewingStandView>>
    }
}