package pl.kuba6000.gtnhextranerf.mixins.early;

import net.minecraft.item.ItemStack;
import net.minecraft.village.MerchantRecipe;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import pl.kuba6000.gtnhextranerf.integration.railcraft.LocomotiveTrades;

/** Applies the steel contribution to newly generated and saved locomotive offers. */
@Mixin(MerchantRecipe.class)
public abstract class MixinMerchantRecipe {

    @Shadow
    private ItemStack itemToBuy;
    @Shadow
    private ItemStack secondItemToBuy;
    @Shadow
    private ItemStack itemToSell;

    @Inject(
        method = {
            "<init>(Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemStack;)V",
            "readFromTags" },
        at = @At("RETURN"))
    private void requireSuppliedSteel(CallbackInfo ci) {
        ItemStack payment = LocomotiveTrades.steelPayment(itemToBuy, itemToSell);
        if (payment != null) secondItemToBuy = payment;
    }
}
