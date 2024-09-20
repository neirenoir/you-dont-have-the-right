package dev.neire.mc.youdonthavetheright.mixins.crafter;

import dev.neire.mc.youdonthavetheright.api.crafter.TimedCrafter;
import dev.neire.mc.youdonthavetheright.logic.crafter.FurnaceLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

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
    protected Recipe<AbstractFurnaceBlockEntity> you_dont_have_the_right$selectedRecipe = null;

    /**
     * @author Neirenoir
     * @reason fast as fucc boiiiiii
     */
    @SuppressWarnings("unchecked")
    @Overwrite
    public static void serverTick(
        Level level, BlockPos p, BlockState s, AbstractFurnaceBlockEntity furnace
    ) {
        FurnaceLogic.INSTANCE.tickLogic(
            p, s, (TimedCrafter<AbstractFurnaceBlockEntity>) furnace
        );
    }

    @Inject(at = @At("TAIL"), method = "setItem")
    private void itemInserted(int slot, ItemStack stack, CallbackInfo ci) {
        if (level == null) {
            // I am not sure this could even happen
            return;
        }

        RecipeManager recipeManager = level.getRecipeManager();
        RegistryAccess registryAccess = level.registryAccess();

        Optional<Recipe<AbstractFurnaceBlockEntity>> newRecipe =
            recipeManager.getRecipeFor(
                recipeType,
                (AbstractFurnaceBlockEntity) (Object) this,
                level
            );

        if (newRecipe.isEmpty()) {
            setCurrentRecipe(null);
            return;
        }

        // This will get overridden by the After part of the SlotChange event
        // if it was initiated by a player
        setCurrentRecipe(newRecipe.get());

        jumpstart();
    }

    @Inject(at = @At("TAIL"), method = "clearContent")
    private void contentCleared(CallbackInfo ci) {
        setCurrentRecipe(null);
    }

    @Unique @Override
    public boolean jumpstart() {
        if (level == null) {
            return false;
        }

        RegistryAccess registryAccess = level.registryAccess();
        var recipe = getCurrentRecipe();
        if (
            FurnaceLogic.INSTANCE.shouldStep(
            this, recipe != null ? recipe.getResultItem(registryAccess) : ItemStack.EMPTY
            ) && !isRunning() && recipe != null
        ) {
            // FIXME: this will consume fuel even if the recipe is later
            //        overridden by the player's recipes later
            FurnaceLogic.INSTANCE.refuel(this);
            FurnaceLogic.INSTANCE.setLitBlockState(
                this, getBlockState(), getBlockPos(), true
            );
            return true;
        }

        return false;
    }

    @Unique
    public boolean isRunning() {
        return isLit();
    }

    @Unique @Override
    public Level getLevel() {
        return level;
    }

    @Unique @Override
    public int getRunway() {
        return litTime;
    }

    @Unique @Override
    public void setRunway(int runway) {
        litTime = runway;
    }

    @Unique @Override
    public int getProgress() {
        return cookingProgress;
    }

    @Unique @Override
    public void setProgress(int progress) {
        cookingProgress = progress;
    }

    @Unique @Override
    public Recipe<AbstractFurnaceBlockEntity> getCurrentRecipe() {
        return you_dont_have_the_right$selectedRecipe;
    }

    @Unique @Override
    public void setCurrentRecipe(Recipe<AbstractFurnaceBlockEntity> recipe) {
        you_dont_have_the_right$selectedRecipe = recipe;
    }

    @Unique @Override
    public Recipe<AbstractFurnaceBlockEntity> calculateRecipe() {
        return null;
    }

    @Unique @Override
    public NonNullList<ItemStack> getItems() {
        return items;
    }

    @Unique @Override
    public void updateState(Level level, BlockState s) {
        setChanged(level, getBlockPos(), s);
    }

    @Unique @Override
    public RecipeType<Recipe<AbstractFurnaceBlockEntity>> getRecipeType() {
        return recipeType;
    }
}
