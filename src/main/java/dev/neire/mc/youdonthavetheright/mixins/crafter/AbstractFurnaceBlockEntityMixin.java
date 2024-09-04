package dev.neire.mc.youdonthavetheright.mixins.crafter;

import dev.neire.mc.youdonthavetheright.api.TimedCrafter;
import dev.neire.mc.youdonthavetheright.logic.crafter.FurnaceLogicKt;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import org.apache.commons.lang3.tuple.MutablePair;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SuppressWarnings("unused")
@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class
    AbstractFurnaceBlockEntityMixin
    extends BlockEntity
    implements TimedCrafter<AbstractFurnaceBlockEntity>
{
    private AbstractFurnaceBlockEntityMixin(
        BlockEntityType<?> p_155228_, BlockPos p_155229_, BlockState p_155230_
    ) {
        super(p_155228_, p_155229_, p_155230_);
    }

    @Shadow
    protected abstract boolean isLit();
    @Shadow int litTime;
    @Shadow int cookingProgress;
    @Shadow protected NonNullList<ItemStack> items;
    @Shadow @Final private RecipeType<Recipe<AbstractFurnaceBlockEntity>> recipeType;

    @Shadow public abstract ItemStack getItem(int p_58328_);

    @Unique
    protected final MutablePair<Recipe<AbstractFurnaceBlockEntity>, ICapabilityProvider>
        you_dont_have_the_right$selectedRecipe = new MutablePair<>(null, this);

    /**
     * @author Neirenoir
     * @reason fast as fucc boiiiiii
     */
    @SuppressWarnings("unchecked")
    @Overwrite
    public static void serverTick(
        Level level, BlockPos p, BlockState s, AbstractFurnaceBlockEntity furnace
    ) {
        FurnaceLogicKt.tickLogic(level, p, s,
            (TimedCrafter<AbstractFurnaceBlockEntity>) furnace);
    }

    @Inject(at = @At("TAIL"), method = "setItem")
    private void itemInserted(int slot, ItemStack stack, CallbackInfo ci) {
        boolean setByPlayer = (you_dont_have_the_right$selectedRecipe.getLeft() instanceof Player);

        boolean sameRecipe = (
            getCurrentRecipe() != null
             && !getCurrentRecipe().matches((AbstractFurnaceBlockEntity) (Object) this, level)
        );

        if (!setByPlayer && !sameRecipe) {
            // Current recipe (if any) was not set by a player; likely inserted by hopper
            setCurrentRecipe(
                FurnaceLogicKt.recalculateRecipe(level, this, null), this
            );
        }
    }

    @Inject(at = @At("TAIL"), method = "clearContent")
    private void contentCleared(CallbackInfo ci) {
        setCurrentRecipe(null, this);
    }

    @Inject(at = @At("TAIL"), method = "removeItem")
    private void removedItem(int i, int j, CallbackInfoReturnable<ItemStack> ci) {
        // FIXME: if, for some unholy reason, the selected recipe consumes more than one
        //        ingredient, this may not invalidate the recipe correctly
        if (getItem(0).isEmpty()) {
            setCurrentRecipe(null, this);
        }
    }

    @Unique
    public boolean isRunning() {
        return isLit();
    }

    @Unique
    public int getRunway() {
        return litTime;
    }

    @Unique
    public void setRunway(int runway) {
        litTime = runway;
    }

    @Unique
    public int getProgress() {
        return cookingProgress;
    }

    @Unique
    public void setProgress(int progress) {
        cookingProgress = progress;
    }

    @Unique
    public Recipe<AbstractFurnaceBlockEntity> getCurrentRecipe() {
        return you_dont_have_the_right$selectedRecipe.getLeft();
    }

    @Unique
    public void setCurrentRecipe(Recipe<AbstractFurnaceBlockEntity> recipe, ICapabilityProvider owner) {
        you_dont_have_the_right$selectedRecipe.setLeft(recipe);
        you_dont_have_the_right$selectedRecipe.setRight(owner);
    }

    @Unique
    public Recipe<AbstractFurnaceBlockEntity> calculateRecipe() {
        return null;
    }

    @Unique
    public NonNullList<ItemStack> getItems() {
        return items;
    }

    @Unique
    public void updateState(Level level, BlockPos pos, BlockState s) {
        setChanged(level, pos, s);
    }

    @Unique
    public RecipeType<Recipe<AbstractFurnaceBlockEntity>> getRecipeType() {
        return recipeType;
    }
}
