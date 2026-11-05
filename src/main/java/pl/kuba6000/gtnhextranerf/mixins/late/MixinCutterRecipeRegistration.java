package pl.kuba6000.gtnhextranerf.mixins.late;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMapBackend;
import gregtech.api.util.GTModHandler;
import gregtech.api.util.GTRecipe;
import pl.kuba6000.gtnhextranerf.audit.AuditCounts;
import pl.kuba6000.gtnhextranerf.audit.AuditManager;
import pl.kuba6000.gtnhextranerf.audit.AuditScope;
import pl.kuba6000.gtnhextranerf.recipes.RecipeValidationException;

/** Filters ordinary and distilled water before cutter recipes are registered. */
@Mixin(value = RecipeMapBackend.class, remap = false)
public abstract class MixinCutterRecipeRegistration {

    @Shadow
    private RecipeMap<?> recipeMap;

    @WrapOperation(
        method = "doAdd",
        at = @At(value = "INVOKE", target = "Ljava/util/function/Function;apply(Ljava/lang/Object;)Ljava/lang/Object;"),
        require = 1)
    private Object gtnhextranerf$filterCutterVariants(Function<?, ?> emitter, Object builder,
        Operation<Object> original) {
        Object emitted = original.call(emitter, builder);
        if (!gtnhextranerf$isCutter()) return emitted;

        List<GTRecipe> kept = new ArrayList<>();
        int filtered = 0;
        for (Object value : (Iterable<?>) emitted) {
            GTRecipe recipe = (GTRecipe) value;
            if (gtnhextranerf$usesWater(recipe)) filtered++;
            else kept.add(recipe);
        }
        if (filtered > 0) {
            int count = filtered;
            AuditManager
                .run(AuditScope.FILTER_WATER_CUTTER_VARIANTS, audit -> audit.record(new AuditCounts(count, 0, 0)));
        }
        return kept;
    }

    @Unique
    private boolean gtnhextranerf$isCutter() {
        return recipeMap != null && "gt.recipe.cuttingsaw".equals(recipeMap.unlocalizedName);
    }

    @Unique
    private static boolean gtnhextranerf$usesWater(GTRecipe recipe) {
        boolean water = false, other = false;
        for (FluidStack input : recipe.mFluidInputs) {
            if (input == null) continue;
            if (gtnhextranerf$isWater(input)) water = true;
            else other = true;
        }
        if (recipe.mAltFluidInputs != null) for (FluidStack[] alternatives : recipe.mAltFluidInputs)
            if (alternatives != null) for (FluidStack input : alternatives) {
                if (input == null) continue;
                if (gtnhextranerf$isWater(input)) water = true;
                else other = true;
            }
        if (water && other)
            throw new RecipeValidationException("Cutter recipe mixes water with another fluid: " + recipe);
        return water;
    }

    @Unique
    private static boolean gtnhextranerf$isWater(FluidStack input) {
        return input.getFluid() == FluidRegistry.WATER || input.getFluid() == GTModHandler.getDistilledWater(1)
            .getFluid();
    }
}
