package pl.kuba6000.gtnhextranerf.mixins.late;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TCAspects;
import gregtech.api.enums.TCAspects.TC_AspectStack;
import gregtech.api.interfaces.internal.IThaumcraftCompat;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTUtility;
import gregtech.loaders.postload.recipes.ThaumcraftRecipes;
import pl.kuba6000.gtnhextranerf.audit.AuditManager;
import pl.kuba6000.gtnhextranerf.audit.AuditScope;
import pl.kuba6000.gtnhextranerf.recipes.RecipeValidationException;

@Mixin(value = ThaumcraftRecipes.class, remap = false)
public abstract class MixinIronToSteelTransmutation {

    @WrapOperation(
        method = "run",
        at = @At(
            value = "INVOKE",
            target = "Lgregtech/api/interfaces/internal/IThaumcraftCompat;addCrucibleRecipe(Ljava/lang/String;Ljava/lang/Object;Lnet/minecraft/item/ItemStack;Ljava/util/List;)Ljava/lang/Object;"),
        require = 1)
    private Object gtnhextranerf$ironToSteel(IThaumcraftCompat compat, String research, Object catalyst,
        ItemStack output, List<TC_AspectStack> aspects, Operation<Object> original) {
        if (!IThaumcraftCompat.IRON_TO_STEEL.equals(research))
            return original.call(compat, research, catalyst, output, aspects);

        return AuditManager.auditResult(AuditScope.MODIFY_IRON_TO_STEEL_TRANSMUTATION, audit -> {
            ItemStack nugget = GTOreDictUnificator.get(OrePrefixes.nugget, Materials.Steel, 1);
            if (!String.valueOf(catalyst)
                .equals(String.valueOf(OrePrefixes.nugget.get(Materials.Iron)))
                || !GTUtility.areStacksEqual(output, nugget)
                || output.stackSize != 1
                || aspects.size() != 1
                || aspects.get(0).mAspect != TCAspects.ORDO
                || aspects.get(0).mAmount != 1)
                throw new RecipeValidationException("Unexpected original iron-to-steel transmutation");
            ItemStack dust = GTOreDictUnificator.get(OrePrefixes.dustTiny, Materials.Steel, 1);
            if (!GTUtility.isStackValid(dust)) throw new RecipeValidationException("Missing tiny steel dust");
            List<TC_AspectStack> cost = new ArrayList<>();
            for (TC_AspectStack aspect : aspects) cost.add(aspect.copy(Math.multiplyExact(aspect.mAmount, 4L)));
            Object recipe = original.call(compat, research, OrePrefixes.ingot.get(Materials.Iron), dust, cost);
            if (recipe == null) throw new RecipeValidationException("Failed to register iron-to-steel transmutation");
            audit.recipeModified();
            return recipe;
        });
    }
}
