package pl.kuba6000.gtnhextranerf.mixins.late;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import gregtech.common.tileentities.machines.multi.steam.MTESteamWaterPump;
import gtPlusPlus.xmod.gregtech.api.metatileentity.implementations.base.MTESteamMultiBlockBase;
import pl.kuba6000.gtnhextranerf.mixins.support.SteamConsumption;

@Mixin(value = MTESteamMultiBlockBase.class, remap = false)
public abstract class MixinSteamMultiBlock {

    @WrapOperation(
        method = "onRunningTick",
        at = @At(
            value = "INVOKE",
            target = "LgtPlusPlus/xmod/gregtech/api/metatileentity/implementations/base/MTESteamMultiBlockBase;tryConsumeSteam(I)Z"),
        require = 1,
        allow = 1)
    private boolean gtnhextranerf$consumeMoreSteam(MTESteamMultiBlockBase<?> machine, int amount,
        Operation<Boolean> original) {
        if (machine instanceof MTESteamWaterPump) return original.call(machine, amount);
        if (amount < 0 || amount > Integer.MAX_VALUE / SteamConsumption.MULTIPLIER) return false;
        return original.call(machine, amount * SteamConsumption.MULTIPLIER);
    }

    @ModifyConstant(method = "getWailaBody", constant = @Constant(longValue = 20L), require = 1, allow = 1)
    private long gtnhextranerf$displaySteamUsage(long original) {
        return (Object) this instanceof MTESteamWaterPump ? original : original * SteamConsumption.MULTIPLIER;
    }
}
