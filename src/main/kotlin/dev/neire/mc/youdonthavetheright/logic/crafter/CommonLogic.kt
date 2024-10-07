package dev.neire.mc.youdonthavetheright.logic.crafter

import dev.neire.mc.youdonthavetheright.api.capability.YdhtrCapabilities.RECIPE_BIBLE_CAPABILITY
import dev.neire.mc.youdonthavetheright.api.crafter.TimedCrafter
import dev.neire.mc.youdonthavetheright.config.YdhtrConfig
import dev.neire.mc.youdonthavetheright.event.inventory.ContainerEvents
import dev.neire.mc.youdonthavetheright.recipebook.WorldRecipeBook
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.StringTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.Container
import net.minecraft.world.item.crafting.Recipe
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity
import net.minecraftforge.common.capabilities.ICapabilityProvider
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.server.ServerLifecycleHooks

object CommonLogic {
    @Suppress("UNCHECKED_CAST")
    private fun <C: Container> recalculateRecipe(
        crafter: TimedCrafter<C>,
        initiator: ICapabilityProvider?
    ): Recipe<C>? {
        val recipes =
            crafter.level.recipeManager.getRecipesFor(
                crafter.recipeType,
                crafter as C,
                crafter.level
            )
        val candidateRecipe =
            if (initiator != null) {
                recipes.firstOrNull(
                    fun(r: Recipe<C>): Boolean {
                        var found = false
                        initiator.getCapability(RECIPE_BIBLE_CAPABILITY)
                            .ifPresent { b -> found = b.hasRecipe(r.id) }
                        return found
                    }
                )
            }
            else {
                if (YdhtrConfig.ENABLE_WORLD_LEARNED_RECIPES.get()) {
                    recipes.firstOrNull(
                        fun(r: Recipe<C>): Boolean {
                            var found = false
                            WorldRecipeBook.capability(crafter.level as ServerLevel)
                                .ifPresent { b -> found = b.hasRecipe(r.id) }
                            return found
                        }
                    )
                } else {
                    recipes.firstOrNull()
                }
            }
        val registryAccess = crafter.level.registryAccess()
        val currentRecipeIngredients = crafter.getCurrentRecipe(0)?.ingredients?.first()?.items
        val candidateRecipeIngredients = candidateRecipe?.ingredients?.first()?.items

        // Check if inputs are still compatible
        if (
            currentRecipeIngredients != null && candidateRecipeIngredients != null
            && currentRecipeIngredients.contentEquals(candidateRecipeIngredients)
        ) {
            // If the inputs are the same, the current player is merely "refilling"
            // the crafter
            // We will only change the recipe if the output is different
            val currentResultItem = crafter.getCurrentRecipe(0).getResultItem(registryAccess)
            val candidateResultItem = candidateRecipe.getResultItem(registryAccess)
            if (currentResultItem.`is`(candidateResultItem.item)) {
                // They are the same item. Pick the larger output of the two
                return if (currentResultItem.count > candidateResultItem.count)
                    crafter.getCurrentRecipe(0)
                else
                    candidateRecipe
            } else {
                crafter.progress = 0
            }
        }

        return candidateRecipe
    }

    @Suppress("UNCHECKED_CAST")
    @SubscribeEvent
    fun <C: Container> handleItemInserted(event: ContainerEvents.SlotChange.Moved.After) {
        val timedCrafter: TimedCrafter<C>? = when {
            event.source is TimedCrafter<*> -> event.source as TimedCrafter<C>
            event.target is TimedCrafter<*> -> event.target as TimedCrafter<C>
            else -> null
        }

        if (timedCrafter == null) {
            return
        }

        val player = event.getInvolvedPlayer()

        // TODO: proximity!

        if (timedCrafter !is BrewingStandBlockEntity) {
            timedCrafter.setCurrentRecipe(
                0, recalculateRecipe(timedCrafter, player as ServerPlayer)
            )
        } else {
            val virtualCrafters = BrewingLogic.VirtualBrewingStandView.from(timedCrafter)
            for (virtualCrafter in virtualCrafters) {
                virtualCrafter.setCurrentRecipe(
                    0, recalculateRecipe(virtualCrafter, player as ServerPlayer)
                )
            }
        }

    }

    fun <C: Container> saveAdditionalData(tag: CompoundTag, crafter: TimedCrafter<C>) {
        val recipeListTag = ListTag()
        for (i in 0 until crafter.recipeSize) {
            recipeListTag.add(StringTag.valueOf(crafter.getCurrentRecipe(i)?.id.toString()))
        }
        tag.put("CurrentRecipes", recipeListTag)
    }

    @Suppress("UNCHECKED_CAST")
    fun <C: Container> loadAdditionalData(tag: CompoundTag, crafter: TimedCrafter<C>) {
        val recipeList = tag.get("CurrentRecipes") as ListTag
        val recipeManager =
            crafter.level?.recipeManager ?:
            ServerLifecycleHooks.getCurrentServer().overworld().recipeManager

        for (i in 0 until recipeList.size) {
            val recipeLocation = recipeList[i].asString

            if (recipeLocation == "null") {
                continue
            }

            val recipe =
                ResourceLocation.tryParse(recipeLocation)?.let {
                    recipeManager?.byKey(it)
                }

            if (recipe == null || recipe.isEmpty) {
                continue
            }

            crafter.setCurrentRecipe(i, recipe.get() as Recipe<C>)
        }
    }
}