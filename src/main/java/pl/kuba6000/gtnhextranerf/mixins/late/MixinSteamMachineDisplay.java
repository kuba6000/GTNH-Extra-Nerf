package pl.kuba6000.gtnhextranerf.mixins.late;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import gregtech.api.metatileentity.implementations.MTEBasicMachine;
import pl.kuba6000.gtnhextranerf.mixins.support.SteamConsumption;

@Mixin(value = MTEBasicMachine.class, remap = false)
public abstract class MixinSteamMachineDisplay {

    @ModifyConstant(method = "getWailaBody", constant = @Constant(longValue = 40L), require = 1, allow = 1)
    private long gtnhextranerf$displaySteamUsage(long original) {
        MTEBasicMachine machine = (MTEBasicMachine) (Object) this;
        return machine.isSteampowered() ? original * SteamConsumption.MULTIPLIER : original;
    }
}
