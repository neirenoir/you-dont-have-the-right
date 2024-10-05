package dev.neire.mc.youdonthavetheright.recipebook

import dev.neire.mc.youdonthavetheright.api.capability.RecipeBibleCapability
import dev.neire.mc.youdonthavetheright.api.capability.YdhtrCapabilities.RECIPE_BIBLE_CAPABILITY
import dev.neire.mc.youdonthavetheright.event.recipe.RecipeEvents
import dev.neire.mc.youdonthavetheright.recipebook.WorldRecipeBook.Companion.getOrCreate
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.StringTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.saveddata.SavedData
import net.minecraftforge.common.util.LazyOptional
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.server.ServerLifecycleHooks
import java.util.*

class WorldRecipeBook: SavedData() {
    private val worldRecipes: MutableMap<ResourceLocation, MutableSet<UUID>> = mutableMapOf()

    override fun save(compound: CompoundTag): CompoundTag {
        val tag = CompoundTag()
        for (recipe in worldRecipes) {
            val teachersTag = ListTag()
            for (teacher in recipe.value) {
                teachersTag.add(StringTag.valueOf(teacher.toString()))
            }
            tag.put(recipe.key.toString(), teachersTag)
        }
        val rootTag = CompoundTag()
        rootTag.put("recipes", tag)
        return rootTag
    }

    fun knows(recipe: ResourceLocation): Boolean {
        return worldRecipes.contains(recipe)
    }

    fun teach(recipe: ResourceLocation, teacher: UUID): Boolean {
        val teachers = worldRecipes.getOrElse(recipe) { mutableSetOf() }
        teachers.add(teacher)
        worldRecipes.put(recipe, teachers)
        this.setDirty()
        return teachers.size == 1
    }

    fun forget(recipe: ResourceLocation, teacher: UUID): Boolean {
        val teachers = worldRecipes.getOrElse(recipe) { mutableSetOf() }
        teachers.remove(teacher)
        if (teachers.size == 0) {
            worldRecipes.remove(recipe)
        } else {
            worldRecipes[recipe] = teachers
        }
        this.setDirty()
        return teachers.size == 0
    }

    companion object {
        const val DATA_TAG = "world_recipe_data"

        fun load(compound: CompoundTag): WorldRecipeBook {
            val data = WorldRecipeBook()
            val recipes = compound.get("recipes") as CompoundTag
            for (recipe in recipes.allKeys) {
                val teachers: MutableSet<UUID> = mutableSetOf()
                for (teacher in recipes.get(recipe) as ListTag) {
                    teachers.add(UUID.fromString(teacher.asString))
                }
                data.worldRecipes[ResourceLocation(recipe)] = teachers
            }
            return data
        }

        fun getOrCreate(level: ServerLevel): WorldRecipeBook {
            val storage = level.dataStorage
            return storage.computeIfAbsent(::load, ::WorldRecipeBook, DATA_TAG)
        }

        fun capability(level: ServerLevel): LazyOptional<RecipeBibleCapability> {
            return level.getCapability(RECIPE_BIBLE_CAPABILITY)
        }

        @SubscribeEvent
        fun recipeLearned(event: RecipeEvents.RecipeLearned.After) {
            // TODO: check for "advanced" recipes somehow
            getOrCreate(ServerLifecycleHooks.getCurrentServer().overworld())
                .teach(event.recipe, event.target.uuid)
        }

        @SubscribeEvent
        fun recipeForgotten(event: RecipeEvents.RecipeForgotten.After) {
            getOrCreate(ServerLifecycleHooks.getCurrentServer().overworld())
                .forget(event.recipe, event.target.uuid)
        }
    }
}