package dev.neire.mc.youdonthavetheright.config;

import dev.neire.mc.youdonthavetheright.YouDontHaveTheRight;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = YouDontHaveTheRight.ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class YdhtrConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue FORCE_PREVENT_IMPLICIT_LEARNING;

    static {
        var BUILDER = new ForgeConfigSpec.Builder();
        FORCE_PREVENT_IMPLICIT_LEARNING =
            BUILDER
                .comment("Prevents the learning of recipes through divine inspiration")
                .define("forcePreventImplicitLearning", true);

        SPEC = BUILDER.build();
    }
}
