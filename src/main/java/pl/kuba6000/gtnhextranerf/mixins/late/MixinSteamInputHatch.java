package pl.kuba6000.gtnhextranerf.mixins.late;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import gtPlusPlus.xmod.gregtech.registration.gregtech.GregtechCustomHatches;

@Mixin(value = GregtechCustomHatches.class, remap = false)
public abstract class MixinSteamInputHatch {

    @ModifyConstant(method = "run1", constant = @Constant(intValue = 64000), require = 1, allow = 1)
    private static int gtnhextranerf$steamInputCapacity(int original) {
        return 640000;
    }
}
