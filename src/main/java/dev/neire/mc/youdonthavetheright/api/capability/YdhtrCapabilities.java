package dev.neire.mc.youdonthavetheright.api.capability;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

public class YdhtrCapabilities {
    public static final Capability<RecipeBookCapability> RECIPE_BOOK_CAPABILITY =
        CapabilityManager.get(new CapabilityToken<>() {});
    public static final Capability<RecipeBibleCapability> RECIPE_BIBLE_CAPABILITY =
        CapabilityManager.get(new CapabilityToken<>() {});

}
