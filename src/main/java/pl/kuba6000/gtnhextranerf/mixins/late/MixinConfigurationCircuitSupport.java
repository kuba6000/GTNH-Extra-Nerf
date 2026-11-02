package pl.kuba6000.gtnhextranerf.mixins.late;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;

import gregtech.api.interfaces.IConfigurationCircuitSupport;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.modularui2.common.CommonWidgets;
import pl.kuba6000.gtnhextranerf.mixins.support.PhysicalCircuitItemStackHandler;
import pl.kuba6000.gtnhextranerf.mixins.support.PhysicalCircuitSlots;

@Mixin(value = CommonWidgets.class, remap = false)
public abstract class MixinConfigurationCircuitSupport {

    @Inject(method = "createCircuitSlot", at = @At("HEAD"), cancellable = true, require = 1)
    private static void gtnhextranerf$usePhysicalCircuitBeforeHv(PanelSyncManager syncManager,
        IMetaTileEntity baseMachine, CallbackInfoReturnable<Widget<? extends Widget<?>>> callback) {
        if (!(baseMachine instanceof IConfigurationCircuitSupport circuitSupport)
            || !PhysicalCircuitSlots.requiresPhysicalCircuit(baseMachine)) return;

        gtnhextranerf$clearSavedPhantomCircuit(baseMachine, circuitSupport);
        callback.setReturnValue(
            new ItemSlot().slot(
                new ModularSlot(new PhysicalCircuitItemStackHandler(baseMachine, circuitSupport.getCircuitSlot()), 0)
                    .singletonSlotGroup()));
    }

    @Unique
    private static void gtnhextranerf$clearSavedPhantomCircuit(IMetaTileEntity baseMachine,
        IConfigurationCircuitSupport circuitSupport) {
        if (baseMachine.getBaseMetaTileEntity() == null || !baseMachine.getBaseMetaTileEntity()
            .isServerSide()) return;

        int circuitSlot = circuitSupport.getCircuitSlot();
        if (baseMachine.getInventoryHandler()
            .getStackInSlot(circuitSlot) != null
            && baseMachine.getInventoryHandler()
                .getStackInSlot(circuitSlot).stackSize <= 0) {
            baseMachine.getInventoryHandler()
                .setStackInSlot(circuitSlot, null);
        }
    }
}
