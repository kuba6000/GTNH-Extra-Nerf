package pl.kuba6000.gtnhextranerf.mixins.late;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Slice;

import gregtech.api.recipe.RecipeMaps;
import pl.kuba6000.gtnhextranerf.audit.AuditManager;
import pl.kuba6000.gtnhextranerf.audit.AuditScope;

@Mixin(value = RecipeMaps.class, remap = false)
public abstract class MixinAssemblerRecipeMap {

    @ModifyArg(
        method = "<clinit>",
        slice = @Slice(
            from = @At(value = "CONSTANT", args = "stringValue=gt.recipe.assembler"),
            to = @At(
                value = "FIELD",
                target = "Lgregtech/api/recipe/RecipeMaps;assemblerRecipes:Lgregtech/api/recipe/RecipeMap;")),
        at = @At(
            value = "INVOKE",
            target = "Lgregtech/api/recipe/RecipeMapBuilder;maxIO(IIII)Lgregtech/api/recipe/RecipeMapBuilder;"),
        index = 1,
        require = 1,
        allow = 1)
    private static int gtnhextranerf$secondRecipeOutput(int original) {
        AuditManager.run(AuditScope.EXPAND_ASSEMBLER_OUTPUTS, audit -> {
            if (original != 1) throw new IllegalStateException("Unexpected assembler recipe output count: " + original);
            audit.recipeModified();
        });
        return 2;
    }
}
