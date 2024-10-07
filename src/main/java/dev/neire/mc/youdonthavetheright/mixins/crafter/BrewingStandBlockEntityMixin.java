package dev.neire.mc.youdonthavetheright.mixins.crafter;

import dev.neire.mc.youdonthavetheright.api.crafter.PotionBits;
import dev.neire.mc.youdonthavetheright.api.crafter.TimedCrafter;
import dev.neire.mc.youdonthavetheright.logic.crafter.BrewingLogic;
import dev.neire.mc.youdonthavetheright.logic.crafter.CommonLogic;
import dev.neire.mc.youdonthavetheright.recipebook.RecipeBookLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;


@Mixin(BrewingStandBlockEntity.class)
public abstract class BrewingStandBlockEntityMixin
    extends BlockEntity
    implements TimedCrafter<BrewingLogic.VirtualBrewingStandView>, PotionBits
{
    @Shadow
    int brewTime;
    @Shadow
    int fuel;
    @Shadow
    private NonNullList<ItemStack> items;
    @Unique
    public byte lastPotionBits;

    @Unique
    protected List<Recipe<BrewingLogic.VirtualBrewingStandView>>
        you_dont_have_the_right$selectedRecipes = new ArrayList<>(3);

    private BrewingStandBlockEntityMixin(
        BlockEntityType<?> p_155228_, BlockPos p_155229_, BlockState p_155230_
    ) {
        super(p_155228_, p_155229_, p_155230_);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    public void constructorTail(CallbackInfo ci) {
        you_dont_have_the_right$selectedRecipes.add(null);
        you_dont_have_the_right$selectedRecipes.add(null);
        you_dont_have_the_right$selectedRecipes.add(null);
    }
    
    /**
     * @author Neirenoir
     * @reason fast as fucc boiiiiii
     */
    @SuppressWarnings("unchecked")
    @Overwrite
    public static void serverTick(
        Level level, BlockPos p, BlockState s, BrewingStandBlockEntity brewingStand
    ) {
        BrewingLogic.INSTANCE.tickLogic(
            p, s, (TimedCrafter<BrewingStandBlockEntity>) brewingStand
        );
    }

    @Inject(at = @At("TAIL"), method = "setItem")
    @SuppressWarnings("unchecked")
    private void itemInserted(int slot, ItemStack stack, CallbackInfo ci) {
        BrewingLogic.INSTANCE.itemInserted(
            getBlockPos(), getBlockState(),
            (TimedCrafter<BrewingStandBlockEntity>) (Object) this
        );
    }

    @Inject(at = @At("TAIL"), method = "load")
    @SuppressWarnings("unchecked")
    private void onLoad(CallbackInfo ci) {
        var stand = (BrewingStandBlockEntity) (Object) this;
        var views = BrewingLogic.VirtualBrewingStandView.Companion.from(stand);
        BrewingLogic.INSTANCE.reloadPotionBits(
            getBlockPos(), getBlockState(),
            (TimedCrafter<BrewingStandBlockEntity>) stand, views
        );
    }

    @Inject(at = @At("TAIL"), method = "clearContent")
    private void contentCleared(CallbackInfo ci) {
        setCurrentRecipe(0, null);
        setCurrentRecipe(1, null);
        setCurrentRecipe(2, null);
    }

    @Inject(at = @At("TAIL"), method = "saveAdditional")
    private void onSaveAdditional(CompoundTag tag, CallbackInfo ci) {
        CommonLogic.INSTANCE.saveAdditionalData(tag, this);
    }

    @Inject(at = @At("TAIL"), method = "load")
    private void onLoadAdditional(CompoundTag tag, CallbackInfo ci) {
        CommonLogic.INSTANCE.loadAdditionalData(tag, this);
    }

    @Override
    public boolean jumpstart() {
        return true;
    }

    @Override
    public boolean isRunning() {
        return brewTime > 0 && fuel > 0;
    }

    @Unique @Override
    public Level getLevel() {
        return level;
    }

    @Override
    public int getRunway() {
        return fuel;
    }

    @Override
    public void setRunway(int runway) {
        fuel = runway;
    }

    @Override
    public Recipe<BrewingLogic.VirtualBrewingStandView> getCurrentRecipe(int slot) {
        return you_dont_have_the_right$selectedRecipes.get(slot);
    }

    @Override
    public void setCurrentRecipe(int slot, Recipe<BrewingLogic.VirtualBrewingStandView> recipe) {
        you_dont_have_the_right$selectedRecipes.set(slot, recipe);
    }

    @Override
    public int getRecipeSize() {
        return 3;
    }

    @Override
    public int getProgress() {
        return brewTime;
    }

    @Override
    public void setProgress(int progress) {
        brewTime = progress;
    }

    @Unique @Override
    public NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    public void updateState(BlockState s) {
        setChanged(level, getBlockPos(), s);
    }

    @Override
    public RecipeType<Recipe<BrewingLogic.VirtualBrewingStandView>> getRecipeType() {
        return RecipeBookLogic.INSTANCE.getBREWING_RECIPE_TYPE();
    }

    @Override
    public byte getPotionBits() {
        return lastPotionBits;
    }

    @Override
    public void setPotionBits(byte bits) {
        lastPotionBits = bits;
    }

}
