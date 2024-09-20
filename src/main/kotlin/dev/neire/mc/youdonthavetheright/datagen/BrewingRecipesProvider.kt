package dev.neire.mc.youdonthavetheright.datagen

import dev.neire.mc.youdonthavetheright.mixins.crafter.PotionBrewingAccessor
import dev.neire.mc.youdonthavetheright.recipebook.BrewingBookCategory
import dev.neire.mc.youdonthavetheright.recipebook.RecipeBrewingRecipe
import net.minecraft.data.PackOutput
import net.minecraft.data.recipes.FinishedRecipe
import net.minecraft.data.recipes.RecipeProvider
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.alchemy.Potion
import net.minecraft.world.item.alchemy.PotionUtils
import net.minecraft.world.item.crafting.Ingredient
import net.minecraftforge.data.event.GatherDataEvent
import net.minecraftforge.registries.ForgeRegistries
import java.util.function.Consumer
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

class BrewingRecipesProvider(packOutput: PackOutput) : RecipeProvider(packOutput) {

    // Initialize a logger for debugging purposes
    private val logger: Logger = LogManager.getLogger()

    override fun buildRecipes(consumer: Consumer<FinishedRecipe>) {
        val potionTypes = arrayOf(
            Pair(Items.POTION, BrewingBookCategory.POTION),
            Pair(Items.SPLASH_POTION, BrewingBookCategory.SPLASHING),
            Pair(Items.LINGERING_POTION, BrewingBookCategory.LINGERING)
        )

        // Iterate over each potion type and its category
        for ((potionItem, category) in potionTypes) {
            PotionBrewingAccessor.getPotionMixes().forEach { mix ->
                try {
                    val inputPotion: Potion = mix.from.get()
                    val outputPotion: Potion = mix.to.get()

                    logger.debug(
                        "Processing brewing mix: {} -> {} with ingredient: {}",
                        inputPotion,
                        outputPotion,
                        mix.ingredient
                    )

                    val ingredientRL: ResourceLocation? =
                        ForgeRegistries.ITEMS.getKey(mix.ingredient.items[0].item)
                    val inputRL: ResourceLocation? =
                        ForgeRegistries.POTIONS.getKey(inputPotion)
                    val outputRL: ResourceLocation? =
                        ForgeRegistries.POTIONS.getKey(outputPotion)
                    val potionItemRL: ResourceLocation? =
                        ForgeRegistries.ITEMS.getKey(potionItem)

                    if (ingredientRL == null) {
                        logger.warn("Ingredient item not registered: ${mix.ingredient.items[0]
                            .item}")
                        return@forEach
                    }

                    if (inputRL == null) {
                        logger.warn("Input potion not registered: $inputPotion")
                        return@forEach
                    }

                    if (outputRL == null) {
                        logger.warn("Output potion not registered: $outputPotion")
                        return@forEach
                    }

                    // Validate that the ingredient has at least one item
                    if (mix.ingredient.items.isEmpty()) {
                        logger.warn("Ingredient has no items: $mix.ingredient")
                        return@forEach
                    }

                    // Create ItemStacks for input and output potions
                    val basePotionStack =
                        PotionUtils.setPotion(ItemStack(potionItem), inputPotion)
                    val resultPotionStack =
                        PotionUtils.setPotion(ItemStack(potionItem), outputPotion)

                    val recipeId = ResourceLocation(
                        "${inputRL.namespace}:" +
                            "${potionItemRL?.path}_" +
                            "${inputRL.path}_${ingredientRL.path}_${outputRL.path}"
                    )

                    // Build and save the brewing recipe using the custom BrewingRecipeBuilder
                    BrewingRecipeBuilder
                        .brewing(
                            listOf(
                                mix.ingredient.items.first(),
                                basePotionStack
                            ),
                            resultPotionStack,
                            BrewingBookCategory.POTION,
                            RecipeBrewingRecipe.Serializer
                        )
                        .unlockedBy("has_potion", has(potionItem))
                        .group(category.toString())
                        .save(consumer, recipeId)
                } catch (e: Exception) {
                    // Catch and log any unexpected exceptions during recipe processing
                    logger.error("Error processing brewing mix: ${mix.from.get()} -> ${mix.to.get()} with ingredient: ${mix.ingredient}", e)
                }
            }
        }

        PotionBrewingAccessor.getContainerMixes().forEach { mix ->
            try {
                val input = ItemStack(mix.from.get(),1)
                val output = ItemStack(mix.to.get(), 1)
                val ingredient = ItemStack(mix.ingredient.items.first().item, 1)

                BrewingRecipeBuilder
                    .brewing(
                        listOf(ingredient, input),
                        output,
                        BrewingBookCategory.POTION,
                        RecipeBrewingRecipe.Serializer
                    )
                    .unlockedBy("has_potion", has(input.item))
                    .save(consumer)
            } catch (e: Exception) {
                // Catch and log any unexpected exceptions during recipe processing
                logger.error("Error processing brewing mix: ${mix.from.get()} -> ${mix.to.get()} with ingredient: ${mix.ingredient}", e)
            }
        }
    }
}

object BrewingRecipesEventListener {
    fun onGatherData(ev: GatherDataEvent) {
        ev.generator.addProvider(
            ev.includeServer(),
            BrewingRecipesProvider(ev.generator.packOutput)
        )
    }
}
