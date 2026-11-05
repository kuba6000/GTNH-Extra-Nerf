package pl.kuba6000.gtnhextranerf.integration;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.ForgeDirection;

import gregtech.api.casing.Casings;
import gregtech.api.enums.Materials;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatchInputBus;
import gregtech.common.tileentities.machines.multi.steam.MTESteamMacerator;
import gtPlusPlus.xmod.gregtech.api.enums.GregtechItemList;
import gtPlusPlus.xmod.gregtech.api.metatileentity.implementations.base.MTEHatchCustomFluidBase;
import pl.kuba6000.gtnhextranerf.progression.MachineProgression;
import pl.kuba6000.gtnhextranerf.progression.ProgressionTier;

/** Exercises certificate changes through a formed Steam Grinder and its real steam hatch. */
final class SteamMultiCertificationIntegrationTests {

    private SteamMultiCertificationIntegrationTests() {}

    static void verify(WorldServer world) {
        ItemStack controllerItem = GregtechItemList.Controller_SteamMaceratorMulti.get(1);
        BaseMetaTileEntity controller = TransformerIntegrationTests.placeAt(world, controllerItem, 752, 210, 800);
        controller.setFrontFacing(ForgeDirection.NORTH);
        MTESteamMacerator grinder = (MTESteamMacerator) controller.getMetaTileEntity();
        grinder.construct(controllerItem, false);

        List<int[]> casing = new ArrayList<>();
        for (int x = 748; x <= 756; x++) for (int y = 205; y <= 215; y++) for (int z = 796; z <= 804; z++) {
            if (world.getBlock(x, y, z) == Casings.BronzePlatedBricks.getBlock()
                && world.getBlockMetadata(x, y, z) == Casings.BronzePlatedBricks.getBlockMeta())
                casing.add(new int[] { x, y, z });
        }
        check(casing.size() >= 17, "Steam Grinder builder did not supply enough casing positions");
        BaseMetaTileEntity steamTile = replace(world, GregtechItemList.Hatch_Input_Steam.get(1), casing.get(0));
        BaseMetaTileEntity inputTile = replace(world, GregtechItemList.Hatch_Input_Bus_Steam.get(1), casing.get(1));
        BaseMetaTileEntity outputTile = replace(world, GregtechItemList.Hatch_Output_Bus_Steam.get(1), casing.get(2));
        MTEHatchCustomFluidBase steam = (MTEHatchCustomFluidBase) steamTile.getMetaTileEntity();
        MTEHatchInputBus input = (MTEHatchInputBus) inputTile.getMetaTileEntity();
        BaseMetaTileEntity[] tiles = { steamTile, inputTile, outputTile, controller };

        try {
            check(grinder.checkStructure(true, controller), "Steam Grinder did not form with its native hatches");
            grinder.fixAllIssues();
            controller.enableWorking();
            check(
                steam.fill(ForgeDirection.UNKNOWN, Materials.Steam.getGas(100_000), true) == 100_000,
                "Certified steam hatch did not accept steam");
            input.setInventorySlotContents(0, new ItemStack(Blocks.iron_ore));
            for (int i = 0; i < 1_200 && controller.getMaxProgress() == 0; i++) tick(tiles);
            check(controller.getMaxProgress() > 0, "Certified Steam Grinder did not start its recipe");

            int before = steam.getFluidAmount();
            for (int i = 0; i < 10; i++) tick(tiles);
            check(steam.getFluidAmount() < before, "Certified Steam Grinder did not consume steam");

            MachineProgression.revoke(world, ProgressionTier.STEAM);
            int stored = steam.getFluidAmount();
            int progress = controller.getProgress();
            int duration = controller.getMaxProgress();
            for (int i = 0; i < 10; i++) tick(tiles);
            check(steam.getFluidAmount() == stored, "Uncertified Steam Grinder consumed stored steam");
            check(
                controller.getMaxProgress() == duration && controller.getProgress() == progress,
                "Uncertified Steam Grinder did not preserve its active recipe");

            MachineProgression.certify(world, ProgressionTier.STEAM);
            for (int i = 0; i < 10; i++) tick(tiles);
            check(
                steam.getFluidAmount() < stored && controller.getProgress() > progress,
                "Recertified Steam Grinder did not resume its preserved recipe");
        } finally {
            for (int x = 748; x <= 756; x++)
                for (int y = 205; y <= 215; y++) for (int z = 796; z <= 804; z++) world.setBlockToAir(x, y, z);
        }
    }

    private static BaseMetaTileEntity replace(WorldServer world, ItemStack stack, int[] position) {
        BaseMetaTileEntity tile = TransformerIntegrationTests
            .placeAt(world, stack, position[0], position[1], position[2]);
        tile.setFrontFacing(ForgeDirection.NORTH);
        return tile;
    }

    private static void tick(BaseMetaTileEntity[] tiles) {
        for (BaseMetaTileEntity tile : tiles) tile.updateEntityProfiled();
    }

    private static void check(boolean success, String message) {
        if (!success) throw new AssertionError(message);
    }
}
