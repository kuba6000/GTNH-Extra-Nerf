package pl.kuba6000.gtnhextranerf.mixins.early;

import net.minecraft.item.ItemStack;
import net.minecraft.village.MerchantRecipe;
import net.minecraft.village.MerchantRecipeList;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import pl.kuba6000.gtnhextranerf.integration.railcraft.LocomotiveTrades;

/** Distinguishes steel plates from other GregTech materials sharing their item ID. */
@Mixin(MerchantRecipeList.class)
public abstract class MixinMerchantRecipeList {

    @Inject(method = "canRecipeBeUsed", at = @At("RETURN"), cancellable = true)
    private void checkSteelMaterial(ItemStack first, ItemStack second, int selected,
        CallbackInfoReturnable<MerchantRecipe> cir) {
        MerchantRecipe recipe = cir.getReturnValue();
        if (recipe == null) return;
        ItemStack payment = LocomotiveTrades.steelPayment(recipe.getItemToBuy(), recipe.getItemToSell());
        if (payment != null && (second == null || !payment.isItemEqual(second))) cir.setReturnValue(null);
    }
}
