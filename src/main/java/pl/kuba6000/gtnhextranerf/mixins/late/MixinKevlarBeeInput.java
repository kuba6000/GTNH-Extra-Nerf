package pl.kuba6000.gtnhextranerf.mixins.late;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import forestry.api.apiculture.BeeManager;
import forestry.api.apiculture.IAlleleBeeSpecies;
import forestry.apiculture.genetics.BeeGenome;
import gregtech.api.objects.ItemData;
import gregtech.api.util.GTRecipe;

/** Matches required Kevlar drones by active species while preserving native counting and consumption. */
@Mixin(value = GTRecipe.RecipeItemInput.class, remap = false)
public abstract class MixinKevlarBeeInput {

    @Shadow
    @Final
    public ItemStack unifiedStack;

    @WrapOperation(
        method = "<init>(Lnet/minecraft/item/ItemStack;Z)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/item/ItemStack;setTagCompound(Lnet/minecraft/nbt/NBTTagCompound;)V",
            remap = true),
        require = 1)
    private void gtnhextranerf$preserveSpecies(ItemStack stack, NBTTagCompound tag, Operation<Void> original) {
        if (!gtnhextranerf$isKevlarDrone(stack)) original.call(stack, tag);
    }

    @Inject(method = "matchesType", at = @At("HEAD"), cancellable = true, require = 1)
    private void gtnhextranerf$matchDroneType(ItemStack other, CallbackInfoReturnable<Boolean> result) {
        if (gtnhextranerf$isKevlarDrone(unifiedStack)) result.setReturnValue(gtnhextranerf$isKevlarDrone(other));
    }

    @Inject(method = "matchesRecipe", at = @At("HEAD"), cancellable = true, require = 1)
    private void gtnhextranerf$matchDroneIngredient(ItemData data, ItemStack other,
        CallbackInfoReturnable<Boolean> result) {
        if (gtnhextranerf$isKevlarDrone(unifiedStack)) result.setReturnValue(gtnhextranerf$isKevlarDrone(other));
    }

    @Unique
    private static boolean gtnhextranerf$isKevlarDrone(ItemStack stack) {
        if (stack == null || BeeManager.beeRoot == null
            || !BeeManager.beeRoot.isDrone(stack)
            || stack.getTagCompound() == null) return false;
        IAlleleBeeSpecies species = BeeGenome.getSpecies(stack);
        return species != null && "gregtech.bee.speciesKevlar".equals(species.getUID());
    }
}
