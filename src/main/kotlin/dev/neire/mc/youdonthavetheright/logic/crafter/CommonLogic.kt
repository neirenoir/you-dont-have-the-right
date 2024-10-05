package dev.neire.mc.youdonthavetheright.logic.crafter

import dev.neire.mc.youdonthavetheright.api.capability.YdhtrCapabilities.RECIPE_BIBLE_CAPABILITY
import dev.neire.mc.youdonthavetheright.api.crafter.TimedCrafter
import dev.neire.mc.youdonthavetheright.event.inventory.ContainerEvents
import dev.neire.mc.youdonthavetheright.recipebook.WorldRecipeBook
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.Container
import net.minecraft.world.item.crafting.Recipe
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity
import net.minecraftforge.common.capabilities.ICapabilityProvider
import net.minecraftforge.eventbus.api.SubscribeEvent

object CommonLogic {
    @Suppress("UNCHECKED_CAST")
    private fun <C : Container> recalculateRecipe(
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
            if (initiator != null)
                recipes.firstOrNull(
                    fun (r: Recipe<C>): Boolean {
                        var found = false
                        initiator.getCapability(RECIPE_BIBLE_CAPABILITY)
                            .ifPresent { b -> found = b.hasRecipe(r.id) }
                        return found
                    }
                )
            else
                recipes.firstOrNull(
                    fun (r: Recipe<C>): Boolean {
                        var found = false
                        WorldRecipeBook.capability(crafter.level as ServerLevel)
                            .ifPresent { b -> found = b.hasRecipe(r.id) }
                        return found
                    }
                )

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
    fun <T : Container> handleItemInserted(event: ContainerEvents.SlotChange.Moved.After) {
        val timedCrafter: TimedCrafter<T>? = when {
            event.source is TimedCrafter<*> -> event.source as TimedCrafter<T>
            event.target is TimedCrafter<*> -> event.target as TimedCrafter<T>
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
}