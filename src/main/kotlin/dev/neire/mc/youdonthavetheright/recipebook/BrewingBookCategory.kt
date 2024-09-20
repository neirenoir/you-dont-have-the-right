package dev.neire.mc.youdonthavetheright.recipebook

import net.minecraft.util.StringRepresentable
import net.minecraft.util.StringRepresentable.EnumCodec

enum class BrewingBookCategory(val category: String): StringRepresentable {
    POTION("potion"),
    SPLASHING("splashing"),
    LINGERING("lingering"),
    MISC("misc");

    override fun getSerializedName(): String {
        return this.category
    }

    companion object {
        val CODEC: EnumCodec<BrewingBookCategory> =
            StringRepresentable.fromEnum { BrewingBookCategory.values() }
    }
}