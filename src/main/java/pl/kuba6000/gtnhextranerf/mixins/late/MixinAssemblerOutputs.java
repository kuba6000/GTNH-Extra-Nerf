package pl.kuba6000.gtnhextranerf.mixins.late;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import pl.kuba6000.gtnhextranerf.audit.AuditManager;
import pl.kuba6000.gtnhextranerf.audit.AuditScope;

@Mixin(targets = "gregtech.common.tileentities.machines.basic.MTEBasicMachineWithRecipeBuilder$Builder", remap = false)
public abstract class MixinAssemblerOutputs {

    @Shadow
    private RecipeMap<?> recipes;

    @ModifyVariable(method = "setSlotsCount", at = @At("HEAD"), argsOnly = true, ordinal = 1, require = 1, allow = 1)
    private int gtnhextranerf$secondOutput(int original) {
        if (recipes != RecipeMaps.assemblerRecipes) return original;
        AuditManager.run(AuditScope.EXPAND_ASSEMBLER_OUTPUTS, audit -> {
            if (original != 1) throw new IllegalStateException("Unexpected assembler output slot count: " + original);
            audit.recipeModified();
        });
        return 2;
    }
}
