package pl.kuba6000.gtnhextranerf.integration;

import java.util.UUID;

import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

import com.mojang.authlib.GameProfile;

import bartworks.system.material.WerkstoffLoader;
import goodgenerator.blocks.tileEntity.MTECoolantTower;
import goodgenerator.util.ItemRefer;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatchInput;
import gregtech.api.metatileentity.implementations.MTEHatchOutput;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.util.GTModHandler;

/** Real tower admission, powered output and retained passive condensation. */
final class CoolantTowerIntegrationTests {

    private CoolantTowerIntegrationTests() {}

    static void verify(WorldServer world) {
        FakePlayer player = FakePlayerFactory.get(world, new GameProfile(UUID.randomUUID(), "CoolantNerfTest"));
        verifySteam(world, player);
        BaseMetaTileEntity controller = tower(world, player, 640, 160);
        MTECoolantTower machine = (MTECoolantTower) controller.getMetaTileEntity();
        BaseMetaTileEntity inputTile = hatch(world, player, machine, ItemList.Hatch_Input_HV.get(1), -1, 0);
        BaseMetaTileEntity out1Tile = hatch(world, player, machine, ItemList.Hatch_Output_HV.get(1), 1, 0);
        BaseMetaTileEntity out2Tile = hatch(world, player, machine, ItemList.Hatch_Output_HV.get(1), 2, 0);
        BaseMetaTileEntity energy = hatch(world, player, machine, ItemList.Hatch_Energy_HV.get(1), -2, 0);
        check(machine.checkStructure(true, controller), "Cooling tower rejected the added energy hatch");
        MTEHatchInput input = (MTEHatchInput) inputTile.getMetaTileEntity();
        FluidStack hot = WerkstoffLoader.HotRutheniumTetroxideSollution.getFluidOrGas(9_000);
        check(input.fill(ForgeDirection.UNKNOWN, hot, true) == hot.amount, "Tower input rejected hot solution");
        check(
            !machine.checkProcessing()
                .wasSuccessful() && input.getFluidAmount() == hot.amount,
            "Missing output bus consumed hot solution");
        BaseMetaTileEntity bus = hatch(world, player, machine, ItemList.Hatch_Output_Bus_HV.get(1), -3, 1);
        world.setBlock(energy.xCoord, energy.yCoord, energy.zCoord, GregTechAPI.sBlockConcretes, 8, 3);
        check(machine.checkStructure(true, controller), "Optional powered hatches became mandatory for structure");
        check(
            !machine.checkProcessing()
                .wasSuccessful() && input.getFluidAmount() == hot.amount,
            "Missing power connection consumed hot solution");
        energy = hatch(world, player, machine, ItemList.Hatch_Energy_HV.get(1), -2, 0);
        check(machine.checkStructure(true, controller), "Powered tower did not form");
        IInventory items = (IInventory) bus.getMetaTileEntity();
        for (int slot = 0; slot < items.getSizeInventory(); slot++)
            items.setInventorySlotContents(slot, new ItemStack(Blocks.cobblestone, 64));
        check(
            !machine.checkProcessing()
                .wasSuccessful() && input.getFluidAmount() == hot.amount,
            "Blocked Salt output consumed hot solution");
        for (int slot = 0; slot < items.getSizeInventory(); slot++) items.setInventorySlotContents(slot, null);
        check(count(items, new ItemStack(Blocks.cobblestone)) == 0, "Fixture failed to clear output slots");
        check(
            machine.getOutputBusses()
                .size() == 1,
            "Output bus is missing from the formed tower");
        MTEHatchOutput out1 = (MTEHatchOutput) out1Tile.getMetaTileEntity();
        MTEHatchOutput out2 = (MTEHatchOutput) out2Tile.getMetaTileEntity();
        out1.fill(ForgeDirection.UNKNOWN, Materials.Water.getFluid(Integer.MAX_VALUE), true);
        out2.fill(ForgeDirection.UNKNOWN, WerkstoffLoader.RutheniumTetroxide.getFluidOrGas(Integer.MAX_VALUE), true);
        check(
            !machine.checkProcessing()
                .wasSuccessful() && input.getFluidAmount() == hot.amount,
            "Blocked fluid outputs consumed hot solution");
        out1.drain(ForgeDirection.UNKNOWN, Integer.MAX_VALUE, true);
        out2.drain(ForgeDirection.UNKNOWN, Integer.MAX_VALUE, true);
        energy.setStoredEU(energy.getEUCapacity());
        CheckRecipeResult admitted = machine.checkProcessing();
        check(
            admitted.wasSuccessful() && input.getFluidAmount() == 0,
            "Fully connected tower did not consume its hot batch: result=" + admitted.writeToNBT(new NBTTagCompound())
                + ", input="
                + input.getFluidAmount()
                + ", output1="
                + out1.getFluidAmount()
                + ", output2="
                + out2.getFluidAmount());
        BaseMetaTileEntity[] tiles = { controller, inputTile, out1Tile, out2Tile, energy, bus };
        long used = 0;
        int duration = machine.mMaxProgresstime;
        for (int tick = 0; tick < duration + 400 && count(items, Materials.Salt.getDust(1)) == 0; tick++) {
            energy.setStoredEU(energy.getEUCapacity());
            long before = energy.getStoredEU();
            for (BaseMetaTileEntity tile : tiles) tile.updateEntityProfiled();
            used += before - energy.getStoredEU();
        }
        check(used > 0, "Mapped cooling ran without consuming energy");
        check(count(items, Materials.Salt.getDust(1)) == 6, "Tower lost or duplicated Salt output");
        check(
            fluid(out1, Materials.Water.getFluid(1)) + fluid(out2, Materials.Water.getFluid(1)) == 1_800,
            "Tower lost or duplicated Water output");
        FluidStack ruthenium = WerkstoffLoader.RutheniumTetroxide.getFluidOrGas(1);
        check(fluid(out1, ruthenium) + fluid(out2, ruthenium) == 7_200, "Tower lost or duplicated Ruthenium output");
        world.setBlockToAir(controller.xCoord, controller.yCoord, controller.zCoord);
    }

    private static void verifySteam(WorldServer world, FakePlayer player) {
        BaseMetaTileEntity controller = tower(world, player, 608, 176);
        MTECoolantTower machine = (MTECoolantTower) controller.getMetaTileEntity();
        BaseMetaTileEntity inTile = hatch(world, player, machine, ItemList.Hatch_Input_HV.get(1), -1, 0);
        BaseMetaTileEntity outTile = hatch(world, player, machine, ItemList.Hatch_Output_HV.get(1), 1, 0);
        check(machine.checkStructure(true, controller), "Steam-only tower now requires a bus or power hatch");
        MTEHatchInput input = (MTEHatchInput) inTile.getMetaTileEntity();
        MTEHatchOutput output = (MTEHatchOutput) outTile.getMetaTileEntity();
        check(
            input.fill(ForgeDirection.UNKNOWN, Materials.Steam.getGas(1_600), true) == 1_600,
            "New recipe map blocked native Steam input");
        check(
            machine.checkProcessing()
                .wasSuccessful() && input.getFluidAmount() == 0,
            "Native passive condensation no longer works");
        check(
            fluid(output, GTModHandler.getDistilledWater(1)) == 10 && machine.mMaxProgresstime == 20,
            "Passive Steam conversion or cycle changed");
        check(machine.onRunningTick(null), "Passive cycle started requiring power");
        world.setBlockToAir(controller.xCoord, controller.yCoord, controller.zCoord);
    }

    private static int fluid(MTEHatchOutput hatch, FluidStack type) {
        FluidStack stored = hatch.getFluid();
        return stored != null && stored.isFluidEqual(type) ? stored.amount : 0;
    }

    private static int count(IInventory inventory, ItemStack item) {
        int amount = 0;
        for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack != null && stack.isItemEqual(item)) amount += stack.stackSize;
        }
        return amount;
    }

    private static BaseMetaTileEntity tower(WorldServer world, FakePlayer player, int x, int z) {
        BaseMetaTileEntity tile = place(world, player, ItemRefer.Coolant_Tower.get(1), x, 210, z);
        tile.setFrontFacing(ForgeDirection.NORTH);
        ((MTECoolantTower) tile.getMetaTileEntity()).construct(ItemRefer.Coolant_Tower.get(1), false);
        return tile;
    }

    private static BaseMetaTileEntity hatch(WorldServer world, FakePlayer player, MTECoolantTower machine,
        ItemStack item, int x, int z) {
        int[] offset = new int[3];
        machine.getExtendedFacing()
            .getWorldOffset(new int[] { x, 0, z }, offset);
        BaseMetaTileEntity controller = (BaseMetaTileEntity) machine.getBaseMetaTileEntity();
        return place(
            world,
            player,
            item,
            controller.xCoord + offset[0],
            controller.yCoord + offset[1],
            controller.zCoord + offset[2]);
    }

    private static BaseMetaTileEntity place(WorldServer world, FakePlayer player, ItemStack item, int x, int y, int z) {
        FixtureWorld.loadAround(world, x, z);
        world.setBlockToAir(x, y, z);
        check(
            ((ItemBlock) item.getItem()).placeBlockAt(item, player, world, x, y, z, 1, 0.5f, 0.5f, 0.5f, 0),
            "Could not place native Cooling Tower fixture");
        return (BaseMetaTileEntity) world.getTileEntity(x, y, z);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
