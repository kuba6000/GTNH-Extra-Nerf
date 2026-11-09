package pl.kuba6000.gtnhextranerf.mixins.late;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import pl.kuba6000.gtnhextranerf.progression.MachineProgression;

/** Pauses uncertified controllers after structure validation has populated their energy hatch lists. */
@Mixin(value = MTEMultiBlockBase.class, remap = false)
public abstract class MixinMultiblockCertification {

    @Inject(
        method = "onPostTick",
        at = @At(
            value = "INVOKE",
            target = "Lgregtech/api/metatileentity/implementations/MTEMultiBlockBase;runMachine(Lgregtech/api/interfaces/tileentity/IGregTechTileEntity;J)V"),
        cancellable = true,
        require = 1,
        allow = 1)
    private void gtnhextranerf$pauseAfterStructureCheck(IGregTechTileEntity tile, long tick, CallbackInfo callback) {
        // A loaded controller discovers its hatches during this tick, after the outer certification gate.
        if (MachineProgression.isLocked((MTEMultiBlockBase) (Object) this)) {
            tile.setActive(false);
            callback.cancel();
        }
    }
}
