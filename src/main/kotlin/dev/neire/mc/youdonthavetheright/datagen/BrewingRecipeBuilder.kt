package dev.neire.mc.youdonthavetheright.datagen

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import dev.neire.mc.youdonthavetheright.recipebook.BrewingBookCategory
import dev.neire.mc.youdonthavetheright.recipebook.RecipeBrewingRecipe
import net.minecraft.advancements.Advancement
import net.minecraft.advancements.AdvancementRewards
import net.minecraft.advancements.CriterionTriggerInstance
import net.minecraft.advancements.RequirementsStrategy
import net.minecraft.advancements.critereon.RecipeUnlockedTrigger
import net.minecraft.data.recipes.FinishedRecipe
import net.minecraft.data.recipes.RecipeBuilder
import net.minecraft.data.recipes.RecipeCategory
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraftforge.registries.ForgeRegistries
import java.util.function.Consumer

class BrewingRecipeBuilder private constructor(
    private val category: RecipeCategory,
    private val bookCategory: BrewingBookCategory,
    val result: ItemStack,
    private val ingredients: List<ItemStack>,
    private val serializer: RecipeSerializer<out RecipeBrewingRecipe?>
) : RecipeBuilder {
    private val advancement: Advancement.Builder = Advancement.Builder.recipeAdvancement()
    private var group: String? = null

    override fun unlockedBy(
        advancementName: String,
        criterionTrigger: CriterionTriggerInstance
    ): BrewingRecipeBuilder {
        advancement.addCriterion(advancementName, criterionTrigger)
        return this
    }

    override fun group(groupName: String?): BrewingRecipeBuilder {
        this.group = groupName
        return this
    }

    override fun getResult(): Item {
        return this.result.item
    }

    override fun save(recipeConsumer: Consumer<FinishedRecipe>, name: ResourceLocation) {
        advancement.parent(RecipeBuilder.ROOT_RECIPE_ADVANCEMENT)
            .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(name)).rewards(
                AdvancementRewards.Builder.recipe(name)
            ).requirements(RequirementsStrategy.OR)
        recipeConsumer.accept(
            Result(
                name,
                (if (this.group == null) "" else this.group)!!,
                this.bookCategory,
                this.ingredients,
                this.result,
                this.advancement, name.withPrefix("recipes/" + this.category.folderName + "/"),
                this.serializer
            )
        )
    }

    internal class Result(
        private val id: ResourceLocation,
        private val group: String,
        private val category: BrewingBookCategory,
        private val ingredients: List<ItemStack>,
        private val result: ItemStack,
        private val advancement: Advancement.Builder,
        private val advancementId: ResourceLocation,
        private val serializer: RecipeSerializer<out RecipeBrewingRecipe?>
    ) : FinishedRecipe {
        override fun serializeRecipeData(json: JsonObject) {
            val jsonIngredients = JsonArray()
            for (ingredient in this.ingredients) {
                val jsonIngredient = JsonObject()
                jsonIngredient.addProperty(
                    "item",
                    ForgeRegistries.ITEMS.getKey(ingredient.item).toString()
                )

                if (ingredient.hasTag()) {
                    val jsonPotion = JsonObject()
                    jsonPotion.addProperty(
                        "Potion", ingredient.tag?.get("Potion")?.asString
                    )
                    jsonIngredient.add(DATA_TAG, jsonPotion)
                }

                jsonIngredients.add(jsonIngredient)
            }

            if (group.isNotEmpty()) {
                json.addProperty("group", this.group.lowercase())
            }

            json.addProperty("category", this.category.serializedName)
            json.add("ingredients", jsonIngredients)

            val jsonResult = JsonObject()
            jsonResult.addProperty(
                "item",
                ForgeRegistries.ITEMS.getKey(result.item).toString()
            )

            if (result.hasTag()) {
                val jsonPotion = JsonObject()

                jsonPotion.addProperty(
                    "Potion", result.tag?.get("Potion")?.asString
                )
                jsonResult.add(DATA_TAG, jsonPotion)
            }

            json.add("result", jsonResult)
        }

        override fun getType(): RecipeSerializer<*> {
            return this.serializer
        }

        override fun getId(): ResourceLocation {
            return this.id
        }

        override fun serializeAdvancement(): JsonObject? {
            return advancement.serializeToJson()
        }

        override fun getAdvancementId(): ResourceLocation? {
            return this.advancementId
        }
    }

    companion object {
        val DATA_TAG: String = "data"
        fun brewing(
            ingredients: List<ItemStack>,
            result: ItemStack,
            category: BrewingBookCategory,
            serializer: RecipeSerializer<out RecipeBrewingRecipe?>
        ): BrewingRecipeBuilder {
            return BrewingRecipeBuilder(
                RecipeCategory.BREWING,
                category,
                result,
                ingredients,
                serializer
            )
        }


    }
}