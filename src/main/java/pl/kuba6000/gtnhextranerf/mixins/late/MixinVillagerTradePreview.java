package pl.kuba6000.gtnhextranerf.mixins.late;

import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.kuba6000.mobsinfo.api.VillagerTrade;

import pl.kuba6000.gtnhextranerf.integration.railcraft.LocomotiveTrades;

/** Keeps fresh and cached Mobs Info offers consistent with the merchant's payment. */
@Mixin(value = VillagerTrade.class, remap = false)
public abstract class MixinVillagerTradePreview {

    @Shadow
    VillagerTrade.TradeItem firstInput;
    @Shadow
    VillagerTrade.TradeItem secondInput;
    @Shadow
    VillagerTrade.TradeItem output;

    @Inject(method = { "<init>", "reconstructStacks" }, at = @At("RETURN"))
    private void showSteelPayment(CallbackInfo ci) {
        if (firstInput == null || output == null) return;
        ItemStack payment = LocomotiveTrades.steelPayment(firstInput.stack, output.stack);
        if (payment != null) secondInput = VillagerTrade.createItem(payment);
    }
}
