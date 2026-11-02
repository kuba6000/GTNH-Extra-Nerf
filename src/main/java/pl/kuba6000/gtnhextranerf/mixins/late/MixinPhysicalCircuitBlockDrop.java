package pl.kuba6000.gtnhextranerf.mixins.late;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.common.blocks.BlockMachines;
import pl.kuba6000.gtnhextranerf.mixins.support.PhysicalCircuitSlots;

@Mixin(BlockMachines.class)
public abstract class MixinPhysicalCircuitBlockDrop {

    @WrapOperation(
        method = "breakBlock",
        at = @At(
            value = "INVOKE",
            target = "Lgregtech/api/interfaces/tileentity/IGregTechTileEntity;isValidSlot(I)Z",
            remap = false),
        require = 1,
        allow = 1)
    private boolean gtnhextranerf$dropPhysicalCircuit(IGregTechTileEntity tile, int slot, Operation<Boolean> original) {
        return original.call(tile, slot) || PhysicalCircuitSlots.containsPhysicalCircuit(tile, slot);
    }
}
