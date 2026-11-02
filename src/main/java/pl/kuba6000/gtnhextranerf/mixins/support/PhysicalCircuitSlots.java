package pl.kuba6000.gtnhextranerf.mixins.support;

import net.minecraft.item.ItemStack;

import gregtech.api.enums.VoltageIndex;
import gregtech.api.interfaces.IConfigurationCircuitSupport;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTETieredMachineBlock;
import gregtech.common.items.ItemIntegratedCircuit;

/** Shared eligibility for the physical GUI slot and its native inventory drops. */
public final class PhysicalCircuitSlots {

    private PhysicalCircuitSlots() {}

    public static boolean requiresPhysicalCircuit(IMetaTileEntity machine) {
        return machine instanceof MTETieredMachineBlock tiered && tiered.mTier < VoltageIndex.HV;
    }

    public static boolean containsPhysicalCircuit(IGregTechTileEntity tile, int slot) {
        IMetaTileEntity machine = tile.getMetaTileEntity();
        if (!(machine instanceof IConfigurationCircuitSupport support) || !requiresPhysicalCircuit(machine)
            || support.getCircuitSlot() != slot) return false;
        ItemStack stack = tile.getStackInSlot(slot);
        return stack != null && stack.stackSize > 0 && stack.getItem() instanceof ItemIntegratedCircuit;
    }
}
