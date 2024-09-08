package dev.neire.mc.youdonthavetheright.logic.crafter

import dev.neire.mc.youdonthavetheright.api.TimedCrafter
import dev.neire.mc.youdonthavetheright.event.inventory.ContainerEvent
import net.minecraft.commands.arguments.coordinates.BlockPosArgument.getBlockPos
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.Container
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.crafting.AbstractCookingRecipe
import net.minecraft.world.item.crafting.Recipe
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.AbstractFurnaceBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity
import net.minecraftforge.common.ForgeHooks
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod.EventBusSubscriber

const val INPUT_SLOT = 0
const val FUEL_SLOT = 1
const val OUTPUT_SLOT = 2
object FurnaceLogic {
    fun tickLogic(
        pos: BlockPos,
        s: BlockState,
        furnace: TimedCrafter<AbstractFurnaceBlockEntity>
    ) {
        if (!furnace.isRunning) {
            // If a valid recipe was inserted, the runway should be at least 1
            return
        }

        furnace.runway = (--furnace.runway).coerceAtLeast(0)

        if (furnace.currentRecipe == null) {
            if (furnace.runway == 0) {
                setLitBlockState(furnace, s, pos, false)
            }
            furnace.progress = 0
            return
        }

        val recipeResult =
            furnace.currentRecipe.getResultItem(furnace.level.registryAccess())

        // Is the output unobstructed?
        if (!shouldStep(furnace, recipeResult)) {
            return
        }

        // Check for refueling
        if (furnace.runway == 0) {
            refuel(furnace)
            if (furnace.runway == 0) {
                // The furnace did not refuel. Shut it down
                setLitBlockState(furnace, s, pos, false)
                return;
            }
        }

        furnace.progress = Math.min(
            ++furnace.progress,
            (furnace.currentRecipe as AbstractCookingRecipe).cookingTime
        )
        if (furnace.progress == (furnace.currentRecipe as AbstractCookingRecipe).cookingTime
            && furnace.items[OUTPUT_SLOT].count + recipeResult.count <= recipeResult.maxStackSize) {
            burn(furnace, recipeResult)
            furnace.progress = 0
            furnace.recipeUsed = furnace.currentRecipe
        }
    }

    fun refuel(furnace: TimedCrafter<AbstractFurnaceBlockEntity>) {
        if (furnace.currentRecipe == null) {
            return;
        }

        val fuel = furnace.items[FUEL_SLOT]
        val duration = ForgeHooks.getBurnTime(fuel, furnace.getRecipeType())
        if (duration != 0) {
            furnace.runway = duration
            fuel.shrink(1)
            if (fuel.isEmpty && fuel.hasCraftingRemainingItem()) {
                furnace.items[FUEL_SLOT] = fuel.craftingRemainingItem
            }
        }
    }

    private fun burn(furnace: TimedCrafter<AbstractFurnaceBlockEntity>, recipeResult: ItemStack) {
        val output = furnace.items[OUTPUT_SLOT]
        if (output.isEmpty) {
            furnace.items[OUTPUT_SLOT] = recipeResult.copy()
        } else {
            // We already checked earlier in shouldStep
            output.grow(recipeResult.count)
            if (recipeResult.hasCraftingRemainingItem()) {
                furnace.items[INPUT_SLOT] = recipeResult.craftingRemainingItem
            }
        }

        // funni wet sponge interaction
        if (
            furnace.items[INPUT_SLOT].`is`(Blocks.WET_SPONGE.asItem())
            && !furnace.items[FUEL_SLOT].isEmpty
            && furnace.items[FUEL_SLOT].`is`(Items.BUCKET)
        ) {
            furnace.items[FUEL_SLOT] = ItemStack(Items.WATER_BUCKET)
        }

        furnace.items[INPUT_SLOT].shrink(1)
    }

    fun shouldStep(
        furnace: TimedCrafter<AbstractFurnaceBlockEntity>,
        recipeResult: ItemStack
    ): Boolean {
        return !recipeResult.isEmpty
            && furnace.items[INPUT_SLOT].count != 0
            && (
                furnace.items[OUTPUT_SLOT].isEmpty
                || (
                    furnace.items[OUTPUT_SLOT].`is`(recipeResult.item)
                    && furnace.items[OUTPUT_SLOT].count <= recipeResult.maxStackSize
                )
            )
    }

    fun setLitBlockState(
        furnace: TimedCrafter<AbstractFurnaceBlockEntity>,
        state: BlockState,
        pos: BlockPos,
        lit: Boolean
    ) {
        val newState = state.setValue(AbstractFurnaceBlock.LIT, lit)
        furnace.level.setBlock(pos, newState, 3)
        furnace.updateState(furnace.level, newState);
    }

    @Suppress("UNCHECKED_CAST")
    fun <C : Container> recalculateRecipe(
        crafter: TimedCrafter<C>,
        player: ServerPlayer?
    ): Recipe<C>? {
        val recipes =
            crafter.level.recipeManager.getRecipesFor(
                crafter.recipeType,
                crafter as C,
                crafter.level
            )
        val candidateRecipe =
            if (player != null)
                recipes.firstOrNull { r -> player.recipeBook.contains(r) }
            else
                recipes.firstOrNull()
        val registryAccess = crafter.level.registryAccess()
        val currentRecipeIngredients = crafter.currentRecipe?.ingredients?.first()?.items
        val candidateRecipeIngredients = candidateRecipe?.ingredients?.first()?.items

        // Check if inputs are still compatible
        if (
            currentRecipeIngredients != null && candidateRecipeIngredients != null
            && currentRecipeIngredients.contentEquals(candidateRecipeIngredients)
        ) {
            // If the inputs are the same, the current player is merely "refilling"
            // the crafter
            // We will only change the recipe if the output is different
            val currentResultItem = crafter.currentRecipe.getResultItem(registryAccess)
            val candidateResultItem = candidateRecipe.getResultItem(registryAccess)
            if (currentResultItem.`is`(candidateResultItem.item)) {
                // They are the same. Pick the larger output of the two
                return if (currentResultItem.count > candidateResultItem.count)
                    crafter.currentRecipe
                else
                    candidateRecipe
            }
        }

        return candidateRecipe
    }

    @Suppress("UNCHECKED_CAST")
    @SubscribeEvent
    fun <T : Container> handleItemInserted(event: ContainerEvent.SlotChange.Moved.After) {
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

        timedCrafter.setCurrentRecipe(
            recalculateRecipe(timedCrafter as TimedCrafter<T>, player as ServerPlayer)
        )
    }
}