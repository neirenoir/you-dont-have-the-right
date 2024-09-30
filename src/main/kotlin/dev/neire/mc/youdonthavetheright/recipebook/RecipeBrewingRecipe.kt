package dev.neire.mc.youdonthavetheright.recipebook

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.mojang.brigadier.StringReader
import dev.neire.mc.youdonthavetheright.datagen.BrewingRecipeBuilder.Companion.DATA_TAG
import dev.neire.mc.youdonthavetheright.logic.crafter.BrewingLogic
import net.minecraft.core.NonNullList
import net.minecraft.core.RegistryAccess
import net.minecraft.nbt.TagParser
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.GsonHelper
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.*
import net.minecraft.world.level.Level
import net.minecraftforge.registries.ForgeRegistries

class RecipeBrewingRecipe(
    private val id: ResourceLocation,
    private val group: String,
    private val category: BrewingBookCategory,
    private val ingredients: MutableList<Ingredient>,
    private val result: ItemStack
) : Recipe<BrewingLogic.VirtualBrewingStandView> {
    init {
        ingredients.sortBy { i -> i.items[0].displayName.string }
    }

    override fun matches(container: BrewingLogic.VirtualBrewingStandView, level: Level): Boolean {
        val usedIngredients = container.getIngredients()
        if (usedIngredients.size != this.ingredients.size) {
            return false;
        }
        for (i in 0 until usedIngredients.size) {
            val copySingle = usedIngredients[i].copyWithCount(this.ingredients[i].items[0].count)
            if (!copySingle.equals(this.ingredients[i].items[0], true)) {
                return false
            }
        }
        return true
    }

    override fun assemble(container: BrewingLogic.VirtualBrewingStandView, registryAccess: RegistryAccess): ItemStack {
        return result.copy()
    }

    override fun canCraftInDimensions(a: Int, b: Int): Boolean {
        return true
    }

    override fun getIngredients(): NonNullList<Ingredient> {
        val nonNullList = NonNullList.createWithCapacity<Ingredient>(this.ingredients.size)
        this.ingredients.forEach(nonNullList::add)
        return nonNullList
    }

    override fun getResultItem(registryAccess: RegistryAccess): ItemStack {
        return this.result
    }

    override fun getGroup(): String {
        return this.group
    }

    override fun getId(): ResourceLocation {
        return this.id
    }

    override fun getSerializer(): RecipeSerializer<*> {
        return Serializer
    }

    override fun getType(): RecipeType<*> {
        // Nonnull assertion should be safe at this point in the lifecycle
        return RecipeBookLogic.BREWING_RECIPE_TYPE!!
    }

    fun category(): BrewingBookCategory {
        return this.category
    }

    object Serializer : RecipeSerializer<RecipeBrewingRecipe> {
        val NAME = ResourceLocation("minecraft", "crafting_brewing")

        override fun toString(): String {
            return RecipeBookLogic.BREWING_RECIPE_TYPE_KEY
        }

        override fun fromJson(id: ResourceLocation, jsonObject: JsonObject): RecipeBrewingRecipe {
            val group = GsonHelper.getAsString(jsonObject, "group", "")
            val category = BrewingBookCategory.CODEC.byName(
                GsonHelper.getAsString(
                    jsonObject,
                    "category",
                    null as String?
                ), BrewingBookCategory.MISC
            )
            val ingredients =
                itemsFromJson(GsonHelper.getAsJsonArray(jsonObject, "ingredients"))
            val itemStack =
                itemStackFromJson(GsonHelper.getAsJsonObject(jsonObject, "result"))
            return RecipeBrewingRecipe(id, group!!, category, ingredients, itemStack)
        }

        override fun fromNetwork(id: ResourceLocation, buf: FriendlyByteBuf): RecipeBrewingRecipe {
            val group = buf.readUtf()
            val category = buf.readEnum(BrewingBookCategory::class.java)

            val size = buf.readVarInt()
            val ingredients = NonNullList.withSize(size, Ingredient.EMPTY)

            for (j in ingredients.indices) {
                ingredients[j] = Ingredient.fromNetwork(buf)
            }

            val result = buf.readItem()

            return RecipeBrewingRecipe(id, group, category, ingredients, result)
        }

        override fun toNetwork(buf: FriendlyByteBuf, recipe: RecipeBrewingRecipe) {
            buf.writeUtf(recipe.group)
            buf.writeEnum(recipe.category)
            buf.writeVarInt(recipe.ingredients.size)
            val ingredients: Iterator<*> = recipe.ingredients.iterator()

            while (ingredients.hasNext()) {
                val ingredient = ingredients.next() as Ingredient
                ingredient.toNetwork(buf)
            }

            buf.writeItem(recipe.result)
        }


        private fun itemsFromJson(ingredients: JsonArray): NonNullList<Ingredient> {
            val parsedIngredients = NonNullList.create<Ingredient>()

            for (i in 0 until ingredients.size()) {
                parsedIngredients.add(Ingredient.of(itemStackFromJson(ingredients[i].asJsonObject)))
            }

            return parsedIngredients
        }

        private fun itemStackFromJson(jsonObject: JsonObject): ItemStack {
            val item =
                ForgeRegistries.ITEMS.getValue(
                    ResourceLocation((jsonObject).get("item").asString)
                )?.let {
                    ItemStack(it, 1)
                }

            if (item == null) {
                throw NullPointerException()
            }

            val potionData =
                if (jsonObject.has("data"))
                    jsonObject.get(DATA_TAG).toString()
                else null

            if (potionData != null) {
                item.tag = TagParser(StringReader(potionData)).readStruct()
            }

            return item
        }
    }
}