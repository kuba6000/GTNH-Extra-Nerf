package pl.kuba6000.gtnhextranerf.mixins.late;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import gregtech.api.enums.Materials;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.recipe.check.SingleRecipeCheck;
import gregtech.api.util.GTRecipe;

/** Uses the recipe's species matcher for locked Liquid Crystal Kevlar batches. */
@Mixin(value = SingleRecipeCheck.class, remap = false)
public abstract class MixinKevlarRecipeLock {

    @Shadow
    @Final
    private GTRecipe recipe;

    @Shadow
    @Final
    private RecipeMap<?> recipeMap;

    @Inject(method = "checkRecipeInputs", at = @At("HEAD"), cancellable = true, require = 1)
    private void gtnhextranerf$matchLockedKevlar(boolean consumeInputs, int maxParallel, ItemStack[] itemInputs,
        FluidStack[] fluidInputs, CallbackInfoReturnable<Integer> result) {
        if (recipeMap != RecipeMaps.multiblockChemicalReactorRecipes) return;
        for (FluidStack output : recipe.mFluidOutputs) {
            if (output == null || !output.isFluidEqual(Materials.LiquidCrystalKevlar.getFluid(1))) continue;
            int parallel = (int) recipe.maxParallelCalculatedByInputs(maxParallel, fluidInputs, itemInputs);
            if (consumeInputs && parallel > 0) recipe.consumeInput(parallel, fluidInputs, itemInputs);
            result.setReturnValue(parallel);
            return;
        }
    }
}
