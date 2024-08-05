package dev.neire.mc.youdonthavetheright.logic

import dev.neire.mc.youdonthavetheright.api.TimedCrafter
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.Container
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.crafting.AbstractCookingRecipe
import net.minecraft.world.item.crafting.Recipe
import net.minecraft.world.item.crafting.RecipeManager
import net.minecraft.world.item.crafting.RecipeType
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.AbstractFurnaceBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity
import net.minecraftforge.common.ForgeHooks

const val INPUT_SLOT = 0
const val FUEL_SLOT = 1
const val OUTPUT_SLOT = 2

fun tickLogic(
    level: Level,
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
            setLitBlockState(level, s, pos, false)
            furnace.updateState(level, pos, s)
        }
        return
    }

    val recipeResult = furnace.currentRecipe.getResultItem(level.registryAccess())

    // Is the output unobstructed?
    if (!shouldStep(furnace, recipeResult)) {
        return
    }

    // Check for refueling
    if (furnace.runway == 0) {
        val fuel = furnace.items[FUEL_SLOT]
        val duration = ForgeHooks.getBurnTime(fuel, furnace.getRecipeType())
        if (duration != 0) {
            furnace.runway = duration
            fuel.shrink(1)
            if (fuel.isEmpty && fuel.hasCraftingRemainingItem()) {
                furnace.items[FUEL_SLOT] = fuel.craftingRemainingItem
            }
        } else {
            setLitBlockState(level, s, pos, false)
            furnace.updateState(level, pos, s)
        }
    }

    ++furnace.progress
    if (furnace.progress == (furnace.currentRecipe as AbstractCookingRecipe).cookingTime) {
        burn(furnace, recipeResult)
        furnace.progress = 0
        furnace.recipeUsed = furnace.currentRecipe
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

private fun shouldStep(
    furnace: TimedCrafter<AbstractFurnaceBlockEntity>,
    recipeResult: ItemStack
): Boolean {
    return !recipeResult.isEmpty
            && (furnace.items[OUTPUT_SLOT].isEmpty
            || furnace.items[OUTPUT_SLOT].`is`(recipeResult.item))
            && furnace.items[OUTPUT_SLOT].count + recipeResult.count > recipeResult.maxStackSize
}

fun setLitBlockState(
    level: Level,
    state: BlockState,
    pos: BlockPos,
    lit: Boolean
) {
    state.setValue(AbstractFurnaceBlock.LIT, lit)
    level.setBlock(pos, state, 3)
}

@Suppress("UNCHECKED_CAST")
fun <C: Container> recalculateRecipe(
    level: Level,
    crafter: TimedCrafter<C>,
    player: ServerPlayer?
): Recipe<C>? {
    val recipes =
        level.recipeManager.getRecipesFor(
            crafter.recipeType,
            crafter as C,
            level
        )

    if (player != null) {
        return recipes.firstOrNull { r -> player.recipeBook.contains(r) }
    }

    return recipes.firstOrNull()
}