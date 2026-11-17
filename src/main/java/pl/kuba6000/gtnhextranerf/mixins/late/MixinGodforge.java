package pl.kuba6000.gtnhextranerf.mixins.late;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import pl.kuba6000.gtnhextranerf.loader.recipes.ProgressionRecipeNerf;
import pl.kuba6000.gtnhextranerf.materials.TungstenSteelProduction;
import tectech.loader.recipe.Godforge;

@Mixin(value = Godforge.class, remap = false)
public abstract class MixinGodforge {

    @Inject(method = "initMoltenModuleRecipes", at = @At("RETURN"), require = 1, allow = 1)
    private static void finishMoltenRecipes(CallbackInfo callback) {
        ProgressionRecipeNerf.patchMeltingCoreAmericiumBoule();
        TungstenSteelProduction.auditMolten();
    }
}
