package dev.neire.mc.youdonthavetheright.logic.crafter

import dev.neire.mc.youdonthavetheright.api.capability.RecipeBibleCapability
import dev.neire.mc.youdonthavetheright.api.crafter.TimedCrafter
import dev.neire.mc.youdonthavetheright.config.YdhtrConfig
import dev.neire.mc.youdonthavetheright.recipebook.WorldRecipeBook.Companion.capability
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.crafting.AbstractCookingRecipe
import net.minecraft.world.item.crafting.Recipe
import net.minecraft.world.item.crafting.RecipeManager
import net.minecraft.world.level.block.AbstractFurnaceBlock
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraftforge.common.ForgeHooks


object FurnaceLogic {
    const val INPUT_SLOT = 0
    const val FUEL_SLOT = 1
    const val OUTPUT_SLOT = 2
    fun tickLogic(
        pos: BlockPos,
        s: BlockState,
        furnace: TimedCrafter<AbstractFurnaceBlockEntity>
    ) {
        if (!furnace.isRunning) {
            return
        }

        furnace.runway = (--furnace.runway).coerceAtLeast(0)

        if (furnace.getCurrentRecipe(0) == null) {
            if (furnace.runway == 0) {
                setLitBlockState(furnace, s, pos, false)
            }
            furnace.progress = 0
            return
        }

        val recipeResult =
            furnace.getCurrentRecipe(0).getResultItem(furnace.level.registryAccess())

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
            (furnace.getCurrentRecipe(0) as AbstractCookingRecipe).cookingTime
        )
        if (furnace.progress == (furnace.getCurrentRecipe(0) as AbstractCookingRecipe).cookingTime
            && furnace.items[OUTPUT_SLOT].count + recipeResult.count <= recipeResult.maxStackSize) {
            burn(furnace, recipeResult)
            furnace.progress = 0
            furnace.recipeUsed = furnace.getCurrentRecipe(0)
        }
    }

    fun refuel(furnace: TimedCrafter<AbstractFurnaceBlockEntity>) {
        if (furnace.getCurrentRecipe(0) == null) {
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
        furnace.updateState(newState);
    }

    fun itemInserted(furnace: TimedCrafter<AbstractFurnaceBlockEntity>) {
        val level = furnace.level ?: return

        val recipeManager: RecipeManager = level.recipeManager

        val newRecipe =
            recipeManager
                .getRecipeFor<AbstractFurnaceBlockEntity?, Recipe<AbstractFurnaceBlockEntity>>(
                    furnace.recipeType,
                    furnace as AbstractFurnaceBlockEntity,
                    level
                )

        if (newRecipe.isEmpty) {
            furnace.setCurrentRecipe(0, null)
            return
        }

        if (level !is ServerLevel) {
            return
        }

        // This will get overridden by the After part of the SlotChange event
        // if it was initiated by a player
        val selectedRecipe =
            if (YdhtrConfig.ENABLE_WORLD_LEARNED_RECIPES.get()) {
                capability(level)
                    .filter { cap: RecipeBibleCapability -> cap.hasRecipe(newRecipe.get().id) }
                    .map { newRecipe.get() }
                    .orElse(null)
            } else {
                newRecipe.get()
            }
        furnace.setCurrentRecipe(0, selectedRecipe)

        furnace.jumpstart()
    }
}