package pl.kuba6000.gtnhextranerf.integration;

import net.minecraft.item.ItemStack;
import net.minecraft.world.WorldServer;

import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.common.tileentities.machines.basic.MTEIceCreamMachine;
import pl.kuba6000.gtnhextranerf.progression.MachineProgression;
import pl.kuba6000.gtnhextranerf.progression.ProgressionTier;

/** A real Ice Cream Machine must work with no certificates and no power. */
final class IceCreamCertificationIntegrationTests {

    private IceCreamCertificationIntegrationTests() {}

    static void verify(WorldServer world) {
        for (ProgressionTier tier : ProgressionTier.values()) MachineProgression.revoke(world, tier);
        BaseMetaTileEntity tile = TransformerIntegrationTests
            .placeAt(world, ItemList.Ice_Cream_Machine.get(1), 760, 210, 800);
        MTEIceCreamMachine machine = (MTEIceCreamMachine) tile.getMetaTileEntity();
        try {
            for (int i = 0; i < 20; i++) tile.updateEntityProfiled();
            // Trigger the native daily malfunction roll with no input, then repair via its public interaction.
            machine.checkRecipe();
            if (machine.isBrokenToday()) {
                ItemStack[] supplies = { ItemList.Duct_Tape.get(1),
                    GTOreDictUnificator.get(OrePrefixes.screw, Materials.Iron, 1),
                    GTOreDictUnificator.get(OrePrefixes.screw, Materials.Steel, 1),
                    GTOreDictUnificator.get(OrePrefixes.gearGtSmall, Materials.Iron, 1),
                    GTOreDictUnificator.get(OrePrefixes.wireFine, Materials.Copper, 1),
                    ItemList.Electric_Motor_LV.get(1) };
                for (ItemStack supply : supplies)
                    if (machine.isValidRepairItem(supply)) machine.tryRepairWithStack(supply);
                check(!machine.isBrokenToday(), "Could not prepare the Ice Cream Machine with its native repair items");
            }

            machine.setInventorySlotContents(machine.getInputSlot(), ItemList.Credit_Greg_Gold.get(64));
            for (int i = 0; i < 2_000 && machine.getStackInSlot(machine.getOutputSlot()) == null; i++)
                tile.updateEntityProfiled();
            check(
                machine.getStackInSlot(machine.getOutputSlot()) != null,
                "Ice Cream Machine did not produce ice cream without certification");
            check(tile.getStoredEU() == 0, "Ice Cream Machine unexpectedly required electrical power");
        } finally {
            world.setBlockToAir(tile.xCoord, tile.yCoord, tile.zCoord);
        }
    }

    private static void check(boolean success, String message) {
        if (!success) throw new AssertionError(message);
    }
}
