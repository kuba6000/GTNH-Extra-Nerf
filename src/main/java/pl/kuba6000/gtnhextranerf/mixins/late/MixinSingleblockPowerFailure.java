package pl.kuba6000.gtnhextranerf.mixins.late;

import java.util.Arrays;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEBasicMachine;

@Mixin(value = MTEBasicMachine.class, remap = false)
public abstract class MixinSingleblockPowerFailure {

    @Unique
    private boolean gtnhextranerf$powerFailed;

    @WrapOperation(
        method = "onPostTick",
        at = @At(
            value = "INVOKE",
            target = "Lgregtech/api/metatileentity/implementations/MTEBasicMachine;drainEnergyForProcess(J)Z"),
        require = 1,
        allow = 1)
    private boolean gtnhextranerf$observePowerDrain(MTEBasicMachine machine, long energy, Operation<Boolean> original) {
        boolean supplied = original.call(machine, energy);
        gtnhextranerf$powerFailed = !supplied;
        return supplied;
    }

    @Inject(method = "onPostTick", at = @At("RETURN"), require = 1, allow = 1)
    private void gtnhextranerf$discardInterruptedRecipe(IGregTechTileEntity tile, long tick, CallbackInfo callback) {
        if (!gtnhextranerf$powerFailed) return;
        gtnhextranerf$powerFailed = false;

        // GT's failure branch changes progress after the drain, so reset the batch after its tick finishes.
        MTEBasicMachine machine = (MTEBasicMachine) (Object) this;
        Arrays.fill(machine.mOutputItems, null);
        machine.mOutputFluid = null;
        machine.mEUt = 0;
        machine.mProgresstime = 0;
        machine.mMaxProgresstime = 0;
        machine.mStuttering = false;
        tile.setActive(false);
        machine.abortProcess();
        machine.markDirty();
    }
}
