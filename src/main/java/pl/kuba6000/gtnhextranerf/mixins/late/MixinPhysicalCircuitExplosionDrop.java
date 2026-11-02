package pl.kuba6000.gtnhextranerf.mixins.late;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import gregtech.api.metatileentity.BaseMetaTileEntity;
import pl.kuba6000.gtnhextranerf.mixins.support.PhysicalCircuitSlots;

@Mixin(value = BaseMetaTileEntity.class, remap = false)
public abstract class MixinPhysicalCircuitExplosionDrop {

    @WrapOperation(
        method = "doExplosion",
        at = @At(value = "INVOKE", target = "Lgregtech/api/metatileentity/BaseMetaTileEntity;isValidSlot(I)Z"),
        require = 1,
        allow = 1)
    private boolean gtnhextranerf$dropPhysicalCircuit(BaseMetaTileEntity tile, int slot, Operation<Boolean> original) {
        return original.call(tile, slot) || PhysicalCircuitSlots.containsPhysicalCircuit(tile, slot);
    }
}
