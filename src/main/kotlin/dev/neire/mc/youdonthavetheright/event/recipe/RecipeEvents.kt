package dev.neire.mc.youdonthavetheright.event.recipe

import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity
import net.minecraftforge.eventbus.api.Cancelable
import net.minecraftforge.eventbus.api.Event

open class RecipeEvents(): Event() {
    open class RecipeLearned(
        open val recipe: ResourceLocation,
        open val target: Entity
    ): RecipeEvents() {
        @Cancelable
        class Before(
            override val recipe: ResourceLocation,
            override val target: Entity
        ): RecipeLearned(recipe, target) {}

        class After(
            override val recipe: ResourceLocation,
            override val target: Entity
        ): RecipeLearned(recipe, target) {}
    }

    open class RecipeForgotten(
        open val recipe: ResourceLocation,
        open val target: Entity
    ): RecipeEvents() {
        @Cancelable
        class Before(
            override val recipe: ResourceLocation,
            override val target: Entity
        ): RecipeForgotten(recipe, target) {}

        class After(
            override val recipe: ResourceLocation,
            override val target: Entity
        ): RecipeForgotten(recipe, target) {}
    }
}