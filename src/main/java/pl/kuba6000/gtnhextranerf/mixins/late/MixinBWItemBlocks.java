package pl.kuba6000.gtnhextranerf.mixins.late;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import bartworks.common.items.BWItemBlocks;

@Mixin(BWItemBlocks.class)
public abstract class MixinBWItemBlocks {

    @Inject(
        method = "getUnlocalizedName(Lnet/minecraft/item/ItemStack;)Ljava/lang/String;",
        at = @At("RETURN"),
        cancellable = true,
        require = 1)
    private void gtnhextranerf$reinforcedGlassName(CallbackInfoReturnable<String> callback) {
        if ("BW_TieredGlass.2".equals(callback.getReturnValue())) {
            callback.setReturnValue("tile.gtnhextranerf.tungtungtungstensteel_reinforced_glass");
        }
    }
}
