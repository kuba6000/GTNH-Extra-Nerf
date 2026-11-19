package pl.kuba6000.gtnhextranerf.mixins.late;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import gregtech.api.interfaces.tileentity.ICoverable;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.common.covers.CoverEnergyWireless;
import pl.kuba6000.gtnhextranerf.progression.EnergyHatchCertification;

/** Blocks wireless cover withdrawals while their energy hatch is uncertified. */
@Mixin(value = CoverEnergyWireless.class, remap = false)
public abstract class MixinWirelessCoverCertification implements EnergyHatchCertification.WirelessInputHook {

    @Inject(method = "tryFetchingEnergy", at = @At("HEAD"), cancellable = true, require = 1, allow = 1)
    private void gtnhextranerf$noUnlicensedWithdrawal(ICoverable target, CallbackInfo callback) {
        if (target instanceof BaseMetaTileEntity tile && EnergyHatchCertification.isLocked(tile.getMetaTileEntity()))
            callback.cancel();
    }
}
