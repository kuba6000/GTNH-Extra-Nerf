package pl.kuba6000.gtnhextranerf.mixins.late;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import gregtech.api.metatileentity.implementations.MTEBasicMachineBronze;
import pl.kuba6000.gtnhextranerf.mixins.support.SteamConsumption;

@Mixin(value = MTEBasicMachineBronze.class, remap = false)
public abstract class MixinSteamMachineTank {

    @ModifyConstant(method = "getTankInfo", constant = @Constant(intValue = 2), require = 2, allow = 2)
    private int gtnhextranerf$displaySteamTankRatio(int original) {
        return SteamConsumption.SINGLE_STEAM_PER_EU;
    }
}
