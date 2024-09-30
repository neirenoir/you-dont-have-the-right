package dev.neire.mc.youdonthavetheright.logic.crafter

import dev.neire.mc.youdonthavetheright.api.crafter.MultiTimedCrafter
import dev.neire.mc.youdonthavetheright.api.crafter.PotionBits
import dev.neire.mc.youdonthavetheright.api.crafter.TimedCrafter
import dev.neire.mc.youdonthavetheright.logic.crafter.BrewingLogic.VirtualBrewingStandView.Companion.from
import dev.neire.mc.youdonthavetheright.recipebook.RecipeBookLogic
import dev.neire.mc.youdonthavetheright.recipebook.RecipeBrewingRecipe
import net.minecraft.core.BlockPos
import net.minecraft.core.NonNullList
import net.minecraft.world.Containers
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.crafting.Recipe
import net.minecraft.world.item.crafting.RecipeType
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BrewingStandBlock
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraftforge.event.ForgeEventFactory
import java.util.function.Consumer
import kotlin.experimental.and
import kotlin.properties.Delegates

object BrewingLogic {
    const val INGREDIENT_SLOT = 3
    const val FUEL_SLOT = 4
    private val virtualBrewingStands = listOf(
        VirtualBrewingStandView(),
        VirtualBrewingStandView(),
        VirtualBrewingStandView()
    )

    fun tickLogic(
        pos: BlockPos, state: BlockState, brewingStand: TimedCrafter<BrewingStandBlockEntity>
    ) {
        if (brewingStand.runway <= 0) {
            val fuelStack = brewingStand.items.get(FUEL_SLOT)
            if (fuelStack.`is`(Items.BLAZE_POWDER)) {
                brewingStand.runway = BrewingStandBlockEntity.FUEL_USES
                fuelStack.shrink(1)
                brewingStand.updateState(state)
            } else {
                // There is no need to calculate anything else if there is no
                // fuel, Mojang!
                return
            }
        }

        for (i in 0..2) {
            virtualBrewingStands[i].reset(brewingStand as BrewingStandBlockEntity, i)
        }

        val anyBrewable = virtualBrewingStands
            .map { stand -> if (stand.currentRecipe != null) 1 else 0 }
            .reduce { acc, stand -> (acc shl 1) or stand } > 0;
        if (brewingStand.progress > 0) {
            brewingStand.progress--
            if (anyBrewable && brewingStand.progress == 0) {
                doBrew(pos, brewingStand, virtualBrewingStands)
                brewingStand.updateState(state)
            } else if (!anyBrewable) {
                brewingStand.progress = 0
                brewingStand.updateState(state)
            }
        } else if (anyBrewable) {
            --brewingStand.runway
            brewingStand.progress = 400
            // The double updateState is intentional. Don't ask.
            brewingStand.updateState(state)
            brewingStand.updateState(state)
        }
    }

    fun doBrew(
        pos: BlockPos,
        brewingStand: TimedCrafter<BrewingStandBlockEntity>,
        view: List<VirtualBrewingStandView>
    ) {
        if (ForgeEventFactory.onPotionAttemptBrew(brewingStand.items)) {
            return
        }

        val registry = brewingStand.level.registryAccess()
        for (brewingView in view) {
            val currentRecipe = brewingView.currentRecipe ?: continue

            if ((currentRecipe as RecipeBrewingRecipe).matches(brewingView, brewingStand.level)) {
                brewingView.setResult(
                    brewingView.currentRecipe!!.getResultItem(registry).copy()
                )
            }
        }

        ForgeEventFactory.onPotionBrewed(brewingStand.items)

        var ingredient = brewingStand.getItem(INGREDIENT_SLOT)
        if (ingredient.hasCraftingRemainingItem()) {
            val remains = ingredient.craftingRemainingItem
            ingredient.shrink(1)
            if (ingredient.isEmpty) {
                ingredient = remains
            } else {
                Containers.dropItemStack(
                    brewingStand.level,
                    pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(),
                    remains
                )
            }
        } else {
            ingredient.shrink(1)
        }

        brewingStand.items[3] = ingredient
        brewingStand.level.levelEvent(1035, pos, 0)
    }

    fun itemInserted(
        pos: BlockPos, state: BlockState,
        brewingStand: TimedCrafter<BrewingStandBlockEntity>
    ) {
        val views = from(brewingStand as BrewingStandBlockEntity)

        views.forEach { view -> view.recalcRecipe() }

        reloadPotionBits(pos, state, brewingStand, views)
    }

    fun reloadPotionBits(
        pos: BlockPos, state: BlockState,
        brewingStand: TimedCrafter<BrewingStandBlockEntity>, views: List<VirtualBrewingStandView>
    ) {
        val potionBits = views
            .map{ stand -> if (!brewingStand.items[stand.slot].isEmpty) 1 else 0 }
            .reduce { acc, stand -> (acc shl 1) or stand }.toByte()
        val brewingStandPotionBits = brewingStand as PotionBits
        if (potionBits != brewingStandPotionBits.potionBits) {
            brewingStandPotionBits.potionBits = potionBits
            if (state.block !is BrewingStandBlock){
                return;
            }

            var currState = state
            for (i in 0 until BrewingStandBlock.HAS_BOTTLE.size) {
                val bit = potionBits and ((1 shl i).toByte()) != 0.toByte()
                currState = currState.setValue(BrewingStandBlock.HAS_BOTTLE[i], bit)
            }

            brewingStand.level?.setBlock(pos, currState, 2)
        }
    }

    class VirtualBrewingStandView: TimedCrafter<BrewingStandBlockEntity> {
        lateinit var parentBrewingStand: BrewingStandBlockEntity
        var slot by Delegates.notNull<Int>()

        companion object {
            fun from(source: BrewingStandBlockEntity): List<VirtualBrewingStandView> {
                val list = listOf(
                    VirtualBrewingStandView().reset(source, 0),
                    VirtualBrewingStandView().reset(source, 1),
                    VirtualBrewingStandView().reset(source, 2)
                )

                return list
            }
        }

        fun reset(
            parentBrewingStand: BrewingStandBlockEntity,
            slot: Int
        ): VirtualBrewingStandView {
            this.parentBrewingStand = parentBrewingStand
            this.slot = slot

            return this
        }

        fun recalcRecipe() {
            val level = parentBrewingStand.level
            val recipeManager = level?.recipeManager ?: return

            val newRecipe =
                recipeManager.getRecipeFor(
                    RecipeBookLogic.BREWING_RECIPE_TYPE!!,
                    this,
                    level
                )

            if (newRecipe.isEmpty) {
                currentRecipe = null
                return
            }


            // This will get overridden by the After part of the SlotChange event
            // if it was initiated by a player
            currentRecipe = newRecipe.get() as Recipe<BrewingStandBlockEntity>
        }

        fun setResult(result: ItemStack) {
            parentBrewingStand.setItem(slot, result)
        }

        override fun jumpstart(): Boolean {
            return true
        }

        override fun isRunning(): Boolean {
            return getMultiTimedCrafter().currentRecipes[slot] != null
        }

        override fun getLevel(): Level {
            return getMultiTimedCrafter().level
        }

        override fun getRunway(): Int {
            return getMultiTimedCrafter().runway
        }

        override fun setRunway(runway: Int) {
            getMultiTimedCrafter().runway = runway
        }

        override fun getCurrentRecipe(): Recipe<BrewingStandBlockEntity>? {
            return getMultiTimedCrafter().currentRecipes[slot]
        }

        override fun getProgress(): Int {
            return getMultiTimedCrafter().progress
        }

        override fun setProgress(progress: Int) {
            getMultiTimedCrafter().progress = progress
        }

        override fun getItems(): NonNullList<ItemStack> {
            val b = getMultiTimedCrafter()
            return NonNullList.of(b.items[slot], b.items[INGREDIENT_SLOT], b.items[FUEL_SLOT])
        }

        fun getIngredients(): List<ItemStack> {
            val ingredients: MutableList<ItemStack> = mutableListOf(
                parentBrewingStand.getItem(INGREDIENT_SLOT),
                parentBrewingStand.getItem(slot)
            )
            // Yes, this is terrible. No, I do not care. It is only called on brew
            ingredients.sortBy { i -> i.displayName.string }
            return ingredients
        }

        override fun updateState(state: BlockState?) {
            getMultiTimedCrafter().updateState(state)
        }

        override fun getRecipeType(): RecipeType<Recipe<BrewingStandBlockEntity>> {
            return getMultiTimedCrafter().recipeType
        }

        override fun setCurrentRecipe(recipe: Recipe<BrewingStandBlockEntity>?) {
            getMultiTimedCrafter().currentRecipes[slot] = recipe
        }

        // region RecipeContainer
        override fun setRecipeUsed(recipe: Recipe<*>?) {
            @Suppress("UNCHECKED_CAST")
            getMultiTimedCrafter().currentRecipes[slot] =
                recipe as Recipe<BrewingStandBlockEntity>?
        }

        override fun getRecipeUsed(): Recipe<*>? {
            return getMultiTimedCrafter().currentRecipes[slot]
        }
        // endregion

        // region Container
        override fun clearContent() {
            parentBrewingStand.removeItemNoUpdate(slot)
        }

        override fun getContainerSize(): Int {
            // Fuel, reagent and result
            return 3;
        }

        override fun isEmpty(): Boolean {
            return parentBrewingStand.getItem(FUEL_SLOT).isEmpty
                && parentBrewingStand.getItem(INGREDIENT_SLOT).isEmpty
                && parentBrewingStand.getItem(slot).isEmpty
        }

        override fun getItem(slotNum: Int): ItemStack {
            checkBounds(slotNum)
            return parentBrewingStand.getItem(slotNum)
        }

        override fun removeItem(slotNum: Int, amount: Int): ItemStack {
            checkBounds(slotNum)
            return parentBrewingStand.removeItem(slotNum, amount)
        }

        override fun removeItemNoUpdate(slotNum: Int): ItemStack {
            checkBounds(slotNum)
            return parentBrewingStand.removeItemNoUpdate(slotNum)
        }

        override fun setItem(slotNum: Int, newStack: ItemStack) {
            checkBounds(slotNum)
            parentBrewingStand.setItem(slotNum, newStack)
        }

        override fun setChanged() {
            parentBrewingStand.setChanged()
        }

        override fun stillValid(player: Player): Boolean {
            return parentBrewingStand.stillValid(player)
        }
        // endregion

        private fun checkBounds(slotNum: Int) {
            if (slotNum != slot && slotNum != FUEL_SLOT && slotNum != INGREDIENT_SLOT) {
                throw IndexOutOfBoundsException()
            }
        }

        private fun getMultiTimedCrafter(): MultiTimedCrafter<BrewingStandBlockEntity> {
            @Suppress("UNCHECKED_CAST")
            return parentBrewingStand as MultiTimedCrafter<BrewingStandBlockEntity>
        }

    }
}