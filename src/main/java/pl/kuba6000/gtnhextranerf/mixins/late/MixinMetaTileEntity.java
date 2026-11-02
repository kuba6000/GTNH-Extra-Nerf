package pl.kuba6000.gtnhextranerf.mixins.late;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import gregtech.api.metatileentity.MetaTileEntity;
import pl.kuba6000.gtnhextranerf.mixins.support.SteamConsumption;
import pl.kuba6000.gtnhextranerf.progression.EnergyHatchCertification;

@Mixin(value = MetaTileEntity.class, remap = false)
public abstract class MixinMetaTileEntity implements EnergyHatchCertification.BufferHook {

    @Inject(method = "getEUVar", at = @At("HEAD"), cancellable = true, require = 1, allow = 1)
    private void gtnhextranerf$hideLockedBuffer(CallbackInfoReturnable<Long> callback) {
        if (EnergyHatchCertification.isLocked((MetaTileEntity) (Object) this)) callback.setReturnValue(0L);
    }

    @Inject(method = "setEUVar", at = @At("HEAD"), cancellable = true, require = 1, allow = 1)
    private void gtnhextranerf$preserveLockedBuffer(long energy, CallbackInfo callback) {
        if (EnergyHatchCertification.isLocked((MetaTileEntity) (Object) this)) callback.cancel();
    }

    @ModifyConstant(
        method = "fill(Lnet/minecraftforge/common/util/ForgeDirection;Lnet/minecraftforge/fluids/FluidStack;Z)I",
        constant = @Constant(intValue = 2),
        require = 2,
        allow = 2)
    private int modifySteamRatio(int original) {
        return SteamConsumption.SINGLE_STEAM_PER_EU;
    }
}
