package pl.kuba6000.gtnhextranerf.integration;

import java.util.UUID;

import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.common.util.ForgeDirection;

import com.mojang.authlib.GameProfile;

import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.VoidingMode;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatchInput;
import gregtech.common.tileentities.machines.multi.purification.MTEPurificationUnitOzonation;
import pl.kuba6000.gtnhextranerf.progression.ProgressionServerTasks;

/** Native recipe admission, fluid consumption and world explosions at the safety boundary. */
final class OzonationIntegrationTests {

    private OzonationIntegrationTests() {}

    static void verify(WorldServer world) {
        FakePlayer player = FakePlayerFactory.get(world, new GameProfile(UUID.randomUUID(), "OzoneNerfTest"));
        verifyBatch(world, player, 320, 1_000_000, false);
        verifyBatch(world, player, 384, 1_000_001, true);
        // A full, unchanged-capacity ZPM input hatch must also be unsafe.
        verifyBatch(world, player, 448, 1_024_000, true);
        verifyBatch(world, player, 512, 1_024_000, true, 2);
        new ProgressionServerTasks().onTick(new TickEvent.ServerTickEvent(TickEvent.Phase.END));
    }

    private static void verifyBatch(WorldServer world, FakePlayer player, int x, int ozoneAmount, boolean unsafe) {
        verifyBatch(world, player, x, ozoneAmount, unsafe, 1);
    }

    private static void verifyBatch(WorldServer world, FakePlayer player, int x, int ozoneAmount, boolean unsafe,
        int ozoneHatches) {
        BaseMetaTileEntity tile = place(world, player, ItemList.Machine_Multi_PurificationUnitOzonation.get(1), x);
        MTEPurificationUnitOzonation machine = (MTEPurificationUnitOzonation) tile.getMetaTileEntity();
        // This fixture exercises admission directly through actual native hatches. Structure is a separate boundary.
        machine.setVoidingMode(VoidingMode.VOID_ALL);
        BaseMetaTileEntity waterTile = place(world, player, ItemList.Hatch_Input_ZPM.get(1), x + 2);
        BaseMetaTileEntity ozoneTile = place(world, player, ItemList.Hatch_Input_ZPM.get(1), x + 4);
        check(machine.addInputHatchToMachineList(waterTile, 0), "Ozonator rejected its native water input hatch");
        check(machine.addInputHatchToMachineList(ozoneTile, 0), "Ozonator rejected its native ozone input hatch");
        MTEHatchInput water = (MTEHatchInput) waterTile.getMetaTileEntity();
        MTEHatchInput ozone = (MTEHatchInput) ozoneTile.getMetaTileEntity();
        check(
            water.fill(ForgeDirection.UNKNOWN, Materials.Grade1PurifiedWater.getFluid(1_000), true) == 1_000,
            "Native water input rejected its batch");
        check(
            ozone.fill(ForgeDirection.UNKNOWN, Materials.Ozone.getGas(ozoneAmount), true) == ozoneAmount,
            "ZPM hatch capacity changed or rejected ozone");
        if (ozoneHatches == 2) {
            BaseMetaTileEntity extra = place(world, player, ItemList.Hatch_Input_ZPM.get(1), x + 6);
            check(machine.addInputHatchToMachineList(extra, 0), "Ozonator rejected an additional input hatch");
            check(
                ((MTEHatchInput) extra.getMetaTileEntity())
                    .fill(ForgeDirection.UNKNOWN, Materials.Ozone.getGas(ozoneAmount), true) == ozoneAmount,
                "Additional native hatch rejected ozone");
        }
        check(
            machine.checkProcessing()
                .wasSuccessful(),
            "Ozonator did not admit the supplied water/ozone batch");
        if (unsafe) {
            check(world.getTileEntity(tile.xCoord, tile.yCoord, tile.zCoord) == null, "Excess ozone did not explode");
        } else {
            check(world.getTileEntity(tile.xCoord, tile.yCoord, tile.zCoord) == tile, "Safe ozone boundary exploded");
            check(machine.calculateBoostedSuccessChance() == 80.0f, "Safe boundary did not select the highest variant");
            machine.depleteRecipeInputs();
            check(water.getFluidAmount() == 0 && ozone.getFluidAmount() == 0, "Highest variant retained input fluids");
        }
        FixtureWorld.loadAround(world, x, 48);
        world.setBlockToAir(x, 210, 48);
        world.setBlockToAir(x + 2, 210, 48);
        world.setBlockToAir(x + 4, 210, 48);
        if (ozoneHatches == 2) world.setBlockToAir(x + 6, 210, 48);
    }

    private static BaseMetaTileEntity place(WorldServer world, FakePlayer player, ItemStack item, int x) {
        FixtureWorld.loadAround(world, x, 48);
        world.setBlockToAir(x, 210, 48);
        check(
            ((ItemBlock) item.getItem()).placeBlockAt(item, player, world, x, 210, 48, 1, 0.5f, 0.5f, 0.5f, 0),
            "Could not place native ozonator fixture block");
        return (BaseMetaTileEntity) world.getTileEntity(x, 210, 48);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
