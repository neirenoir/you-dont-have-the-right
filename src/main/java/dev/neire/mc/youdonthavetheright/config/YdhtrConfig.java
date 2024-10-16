package dev.neire.mc.youdonthavetheright.config;

import dev.neire.mc.youdonthavetheright.YouDontHaveTheRight;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = YouDontHaveTheRight.ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class YdhtrConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue FORCE_LIMITED_CRAFTING;
    public static final ForgeConfigSpec.BooleanValue FORCE_PREVENT_DIVINE_INSPIRATION;
    public static final ForgeConfigSpec.BooleanValue ENABLE_WORLD_LEARNED_RECIPES;

    static {
        var BUILDER = new ForgeConfigSpec.Builder();

        BUILDER.push("Crafting and recipes");
            FORCE_LIMITED_CRAFTING =
                BUILDER
                    .comment("Force the \"doLimitedCrafting\" game rule to always be true")
                    .define("forceLimitedCrafting", false);

            FORCE_PREVENT_DIVINE_INSPIRATION =
                BUILDER
                    .comment("Prevents the learning of recipes through non-explicit means")
                    .define("forcePreventDivineInspiration", true);

            ENABLE_WORLD_LEARNED_RECIPES =
                BUILDER
                    .comment("Automated appliances only know recipes known by players")
                    .define("enableWorldLearnedRecipes", true);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }
}
