package pl.kuba6000.gtnhextranerf.mixins.late;

import java.util.function.LongSupplier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import gregtech.common.gui.modularui.singleblock.MTEBasicMachineBronzeGui;
import pl.kuba6000.gtnhextranerf.mixins.support.SteamConsumption;

@Mixin(value = MTEBasicMachineBronzeGui.class, remap = false)
public abstract class MixinSteamMachineGui {

    @ModifyArg(
        method = "build",
        at = @At(
            value = "INVOKE",
            target = "Lcom/cleanroommc/modularui/value/sync/LongSyncValue;<init>(Ljava/util/function/LongSupplier;)V"),
        index = 0,
        require = 2,
        allow = 2)
    private LongSupplier gtnhextranerf$displaySteamAmount(LongSupplier original) {
        // The original suppliers already apply the vanilla 2 L per EU conversion.
        return () -> original.getAsLong() * SteamConsumption.MULTIPLIER;
    }
}
