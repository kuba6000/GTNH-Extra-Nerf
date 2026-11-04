package pl.kuba6000.gtnhextranerf.integration;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.WorldServer;

import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEBasicMachine;
import pl.kuba6000.gtnhextranerf.progression.MachineProgression;
import pl.kuba6000.gtnhextranerf.progression.ProgressionTier;

/** Exercises power loss on a placed GT machine through its normal server tick and inventory. */
final class SingleblockPowerFailureIntegrationTests {

    private SingleblockPowerFailureIntegrationTests() {}

    static void verifyElectrical(WorldServer world) {
        BaseMetaTileEntity tile = TransformerIntegrationTests.place(world, ItemList.Machine_LV_E_Furnace.get(1));
        MTEBasicMachine machine = (MTEBasicMachine) tile.getMetaTileEntity();
        try {
            machine.setInventorySlotContents(machine.getInputSlot(), new ItemStack(Blocks.cobblestone, 2));
            machine.setInventorySlotContents(machine.getOutputSlot(), new ItemStack(Blocks.stone, 2));
            tile.setStoredEU(tile.getEUCapacity());
            for (int i = 0; i < 100 && tile.getProgress() < 2; i++) tile.updateEntityProfiled();
            check(tile.getMaxProgress() > 0 && tile.getProgress() >= 2, "Electric furnace did not start its recipe");
            check(
                count(machine.getStackInSlot(machine.getInputSlot()), Blocks.cobblestone) == 1,
                "Recipe did not consume one input");

            tile.setStoredEU(0);
            tile.updateEntityProfiled();
            check(tile.getMaxProgress() == 0, "Power failure did not discard the in-flight recipe");
            check(tile.isAllowedToWork(), "Power failure disabled the machine");
            check(
                count(machine.getStackInSlot(machine.getInputSlot()), Blocks.cobblestone) == 1,
                "Power failure removed queued input");
            check(
                count(machine.getStackInSlot(machine.getOutputSlot()), Blocks.stone) == 2,
                "Power failure removed finished output");

            for (int i = 0; i < 1_200
                && count(machine.getStackInSlot(machine.getOutputSlot()), Blocks.stone) < 3; i++) {
                tile.setStoredEU(tile.getEUCapacity());
                tile.updateEntityProfiled();
            }
            check(
                count(machine.getStackInSlot(machine.getOutputSlot()), Blocks.stone) == 3,
                "Machine did not finish exactly the queued batch after power returned");
            check(machine.getStackInSlot(machine.getInputSlot()) == null, "Machine did not consume its queued input");
        } finally {
            world.setBlockToAir(tile.xCoord, tile.yCoord, tile.zCoord);
        }
    }

    static void verifySteam(WorldServer world) {
        BaseMetaTileEntity tile = TransformerIntegrationTests.place(world, ItemList.Machine_Bronze_Furnace.get(1));
        MTEBasicMachine machine = (MTEBasicMachine) tile.getMetaTileEntity();
        try {
            machine.setInventorySlotContents(machine.getInputSlot(), new ItemStack(Blocks.cobblestone));
            tile.setStoredSteam(tile.getSteamCapacity());
            for (int i = 0; i < 100 && tile.getProgress() < 2; i++) tile.updateEntityProfiled();
            check(tile.getMaxProgress() > 0 && tile.getProgress() >= 2, "Steam furnace did not start its recipe");
            check(machine.getStackInSlot(machine.getInputSlot()) == null, "Steam recipe did not consume its input");

            tile.setStoredSteam(0);
            tile.updateEntityProfiled();
            check(tile.getMaxProgress() == 0, "Steam shortage did not discard the in-flight recipe");
            check(tile.isAllowedToWork(), "Steam shortage disabled the machine");
            for (int i = 0; i < 200; i++) {
                tile.setStoredSteam(tile.getSteamCapacity());
                tile.updateEntityProfiled();
            }
            check(machine.getStackInSlot(machine.getOutputSlot()) == null, "Discarded steam recipe produced an item");
            machine.setInventorySlotContents(machine.getInputSlot(), new ItemStack(Blocks.cobblestone));
            for (int i = 0; i < 1_200 && machine.getStackInSlot(machine.getOutputSlot()) == null; i++) {
                tile.setStoredSteam(tile.getSteamCapacity());
                tile.updateEntityProfiled();
            }
            check(
                count(machine.getStackInSlot(machine.getOutputSlot()), Blocks.stone) == 1,
                "Steam furnace did not process a new batch after refilling");
        } finally {
            world.setBlockToAir(tile.xCoord, tile.yCoord, tile.zCoord);
        }
    }

    static void verifyCertification(WorldServer world) {
        BaseMetaTileEntity tile = TransformerIntegrationTests.place(world, ItemList.Machine_LV_E_Furnace.get(1));
        MTEBasicMachine machine = (MTEBasicMachine) tile.getMetaTileEntity();
        try {
            machine.setInventorySlotContents(machine.getInputSlot(), new ItemStack(Blocks.cobblestone));
            beginElectric(tile);
            int progress = tile.getProgress();
            MachineProgression.revoke(world, ProgressionTier.LV);
            tile.setStoredEU(0);
            for (int i = 0; i < 20; i++) tile.updateEntityProfiled();
            check(
                tile.getMaxProgress() > 0 && tile.getProgress() == progress,
                "Certification lock discarded or advanced the active recipe");
            tile.setStoredEU(tile.getEUCapacity());
            MachineProgression.certify(world, ProgressionTier.LV);
            for (int i = 0; i < 1_200 && machine.getStackInSlot(machine.getOutputSlot()) == null; i++) {
                tile.setStoredEU(tile.getEUCapacity());
                tile.updateEntityProfiled();
            }
            check(
                count(machine.getStackInSlot(machine.getOutputSlot()), Blocks.stone) == 1,
                "Recertified machine did not finish its preserved batch");

            machine.setInventorySlotContents(machine.getInputSlot(), new ItemStack(Blocks.cobblestone));
            beginElectric(tile);
            progress = tile.getProgress();
            MachineProgression.revoke(world, ProgressionTier.LV);
            tile.setStoredEU(0);
            for (int i = 0; i < 20; i++) tile.updateEntityProfiled();
            check(
                tile.getMaxProgress() > 0 && tile.getProgress() == progress,
                "Missing certificate caused a false power failure");
            MachineProgression.certify(world, ProgressionTier.LV);
            tile.updateEntityProfiled();
            check(tile.getMaxProgress() == 0, "Recertified machine did not detect a real power failure");
            for (int i = 0; i < 200; i++) {
                tile.setStoredEU(tile.getEUCapacity());
                tile.updateEntityProfiled();
            }
            check(
                count(machine.getStackInSlot(machine.getOutputSlot()), Blocks.stone) == 1,
                "Power-failed batch survived recertification");
        } finally {
            world.setBlockToAir(tile.xCoord, tile.yCoord, tile.zCoord);
        }
    }

    static void verifyFluidAndReload(WorldServer world) {
        BaseMetaTileEntity tile = TransformerIntegrationTests.place(world, ItemList.Machine_LV_FluidExtractor.get(1));
        MTEBasicMachine machine = (MTEBasicMachine) tile.getMetaTileEntity();
        try {
            machine.setInventorySlotContents(machine.getInputSlot(), new ItemStack(Items.fish, 1, 0));
            machine.setDrainableStack(Materials.FishOil.getFluid(10));
            beginElectric(tile);
            check(machine.getStackInSlot(machine.getInputSlot()) == null, "Fluid recipe did not consume its input");
            tile.setStoredEU(0);
            tile.updateEntityProfiled();
            check(tile.getMaxProgress() == 0, "Fluid recipe survived power failure");
            check(
                machine.getDrainableStack() != null && machine.getDrainableStack().amount == 10,
                "Power failure removed previously finished fluid");

            NBTTagCompound saved = new NBTTagCompound();
            tile.writeToNBT(saved);
            BaseMetaTileEntity restored = new BaseMetaTileEntity();
            restored.readFromNBT(saved);
            world.setTileEntity(tile.xCoord, tile.yCoord, tile.zCoord, restored);
            tile = restored;
            machine = (MTEBasicMachine) tile.getMetaTileEntity();
            for (int i = 0; i < 200; i++) {
                tile.setStoredEU(tile.getEUCapacity());
                tile.updateEntityProfiled();
            }
            check(tile.getMaxProgress() == 0, "Reload restored an interrupted fluid recipe");
            check(
                machine.getDrainableStack() != null && machine.getDrainableStack().amount == 10,
                "Discarded fluid appeared after power and reload");
        } finally {
            world.setBlockToAir(tile.xCoord, tile.yCoord, tile.zCoord);
        }
    }

    private static void beginElectric(BaseMetaTileEntity tile) {
        for (int i = 0; i < 100 && tile.getProgress() < 2; i++) {
            tile.setStoredEU(tile.getEUCapacity());
            tile.updateEntityProfiled();
        }
        check(tile.getMaxProgress() > 0 && tile.getProgress() >= 2, "Electric machine did not start its recipe");
    }

    private static int count(ItemStack stack, net.minecraft.block.Block block) {
        return stack != null && stack.getItem() == Item.getItemFromBlock(block) ? stack.stackSize : 0;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
