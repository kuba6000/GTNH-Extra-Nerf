package pl.kuba6000.gtnhextranerf.mixins.late;

import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import pl.kuba6000.gtnhextranerf.recipes.MetalDuplication;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.CrucibleRecipe;

/** Apply the same nerf to metal duplication regardless of the owning research. */
@Mixin(value = ThaumcraftApi.class, remap = false)
public abstract class MixinCrucibleMetalDuplication {

    @WrapMethod(method = "addCrucibleRecipe", require = 1)
    private static CrucibleRecipe gtnhextranerf$nerfMetalDuplication(String research, ItemStack output, Object catalyst,
        AspectList aspects, Operation<CrucibleRecipe> original) {
        ItemStack dust = MetalDuplication.replacement(research, output, catalyst, aspects);
        if (dust == null) return original.call(research, output, catalyst, aspects);
        CrucibleRecipe result = original.call(research, dust, catalyst, MetalDuplication.scaledAspects(aspects));
        if (result == null) throw new IllegalStateException("Failed to register metal duplication for " + research);
        MetalDuplication.registered();
        return result;
    }
}
