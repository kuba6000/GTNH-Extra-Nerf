package pl.kuba6000.gtnhextranerf.mixins.late;

import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.objects.ItemData;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTUtility;
import pl.kuba6000.gtnhextranerf.audit.AuditManager;
import pl.kuba6000.gtnhextranerf.audit.AuditScope;
import pl.kuba6000.gtnhextranerf.recipes.RecipeValidationException;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.CrucibleRecipe;

@Pseudo
@Mixin(targets = "witchinggadgets.common.recipes.alchemic.WG_alchemic_clusters", remap = false)
public abstract class MixinMetalTransmutation {

    @WrapOperation(
        method = "loadGT5uClusters",
        at = @At(
            value = "INVOKE",
            target = "Lwitchinggadgets/common/recipes/WG_alchemic_recipes;registerAlchemyRecipe(Ljava/lang/String;Ljava/lang/String;Lnet/minecraft/item/ItemStack;Ljava/lang/Object;Lthaumcraft/api/aspects/AspectList;)Lthaumcraft/api/crafting/CrucibleRecipe;"),
        require = 1)
    private static CrucibleRecipe gtnhextranerf$metalRecipe(String research, String suffix, ItemStack output,
        Object catalyst, AspectList aspects, Operation<CrucibleRecipe> original) {
        if (!"METALLURGICPERFECTION_TRANSMUTATION".equals(research))
            return original.call(research, suffix, output, catalyst, aspects);

        return AuditManager.auditResult(AuditScope.MODIFY_METAL_TRANSMUTATION, audit -> {
            ItemData data = GTOreDictUnificator.getAssociation(output);
            if (data == null || data.mPrefix != OrePrefixes.nugget
                || data.mMaterial == null
                || data.mMaterial.mMaterial == null)
                throw new RecipeValidationException("Unknown GT transmutation output: " + output);
            Materials material = data.mMaterial.mMaterial;
            ItemStack dust = GTOreDictUnificator.get(OrePrefixes.dustTiny, material, 2);
            if (!GTUtility.isStackValid(dust))
                throw new RecipeValidationException("Missing tiny dust for transmutation: " + material);
            AspectList cost = new AspectList();
            for (Aspect aspect : aspects.getAspects())
                cost.add(aspect, Math.multiplyExact(aspects.getAmount(aspect), 4));
            CrucibleRecipe recipe = original.call(research, suffix, dust, catalyst, cost);
            if (recipe == null) throw new RecipeValidationException("Failed to register transmutation for " + material);
            audit.recipeModified();
            return recipe;
        });
    }
}
