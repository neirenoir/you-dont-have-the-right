package dev.neire.mc.youdonthavetheright

import dev.neire.mc.youdonthavetheright.config.YdhtrConfig
import dev.neire.mc.youdonthavetheright.datagen.BrewingRecipesEventListener
import dev.neire.mc.youdonthavetheright.logic.crafter.CommonLogic
import dev.neire.mc.youdonthavetheright.recipebook.RecipeBookLogic
import dev.neire.mc.youdonthavetheright.recipebook.WorldRecipeBook
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.ModLoadingContext
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.config.ModConfig.Type
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent
import net.minecraftforge.fml.event.lifecycle.FMLDedicatedServerSetupEvent
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import thedarkcolour.kotlinforforge.forge.FORGE_BUS
import thedarkcolour.kotlinforforge.forge.MOD_BUS
import thedarkcolour.kotlinforforge.forge.runForDist

/**
 * Main mod class. Should be an `object` declaration annotated with `@Mod`.
 * The modid should be declared in this object and should match the modId entry
 * in mods.toml.
 *
 * An example for blocks is in the `blocks` package of this mod.
 */
@Mod(YouDontHaveTheRight.ID)
object YouDontHaveTheRight {
    const val ID = "youdonthavetheright"

    // the logger for our mod
    val LOGGER: Logger = LogManager.getLogger(ID)

    init {
        LOGGER.log(Level.INFO, "Initializing You Don't Have The Right")

        // Register the KDeferredRegister to the mod-specific event bus
        //ModBlocks.REGISTRY.register(MOD_BUS)

        val obj = runForDist(
            clientTarget = {
                MOD_BUS.addListener(YouDontHaveTheRight::onClientSetup)
                Minecraft.getInstance()
            },
            serverTarget = {
                MOD_BUS.addListener(YouDontHaveTheRight::onServerSetup)
                "test"
            })

        // RecipeBook events
        MOD_BUS.addListener(RecipeBookLogic::registerBrewingRecipeType)
        MOD_BUS.addListener(BrewingRecipesEventListener::onGatherData)
        FORGE_BUS.register(WorldRecipeBook.Companion)

        // Crafter events
        FORGE_BUS.register(CommonLogic)

        ModLoadingContext.get().registerConfig(Type.SERVER, YdhtrConfig.SPEC)
    }

    /**
     * This is used for initializing client specific
     * things such as renderers and keymaps
     * Fired on the mod specific event bus.
     */
    private fun onClientSetup(event: FMLClientSetupEvent) {
        LOGGER.log(Level.INFO, "Initializing client...")
    }

    /**
     * Fired on the global Forge bus.
     */
    private fun onServerSetup(event: FMLDedicatedServerSetupEvent) {
        LOGGER.log(Level.INFO, "Server starting...")
    }
}