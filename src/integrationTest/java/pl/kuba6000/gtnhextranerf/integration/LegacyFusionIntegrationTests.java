package pl.kuba6000.gtnhextranerf.integration;

import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.common.util.ForgeDirection;

import com.mojang.authlib.GameProfile;

import goodgenerator.blocks.tileEntity.base.MTELargeFusionComputer;
import goodgenerator.loader.Loaders;
import goodgenerator.util.ItemRefer;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.ItemList;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.common.tileentities.machines.multi.MTEFusionComputer;
import gtPlusPlus.xmod.gregtech.api.enums.GregtechItemList;

/** Native creative construction and validation in a disposable server world. */
final class LegacyFusionIntegrationTests {

    private LegacyFusionIntegrationTests() {}

    static void verify(WorldServer world) {
        FakePlayer player = FakePlayerFactory.get(world, new GameProfile(UUID.randomUUID(), "FusionNerfTest"));
        ItemStack[] controllers = { ItemList.FusionComputer_ZPMV.get(1), ItemList.FusionComputer_UV.get(1),
            GregtechItemList.FusionComputer_UV2.get(1), GregtechItemList.FusionComputer_UV3.get(1) };
        ItemStack[] casings = { ItemList.Casing_Fusion.get(1), ItemList.Casing_Fusion2.get(1),
            GregtechItemList.Casing_Fusion_External.get(1), GregtechItemList.Casing_Fusion_External2.get(1) };
        ItemStack[] oldCoils = { ItemList.Casing_Fusion_Coil.get(1), ItemList.Casing_Fusion_Coil.get(1),
            GregtechItemList.Casing_Fusion_Internal.get(1), GregtechItemList.Casing_Fusion_Internal2.get(1) };
        ItemStack[] energy = { ItemList.Hatch_Energy_ZPM.get(1), ItemList.Hatch_Energy_UV.get(1),
            ItemList.Hatch_Energy_UHV.get(1), ItemList.Hatch_Energy_UEV.get(1) };
        for (int i = 0; i < controllers.length; i++) {
            BaseMetaTileEntity tile = place(world, player, controllers[i], 96 + 32 * i, 210, 32);
            tile.setFrontFacing(ForgeDirection.NORTH);
            MTEFusionComputer machine = (MTEFusionComputer) tile.getMetaTileEntity();
            check(
                machine.getFusionCoil() == GregTechAPI.sBlockCasingsBEC && machine.getFusionCoilMeta() == 1,
                "Legacy fusion must require the BEC isolation casing");
            String coilName = new ItemStack(GregTechAPI.sBlockCasingsBEC, 1, 1).getDisplayName();
            check(
                String.join("\n", machine.getSecondaryDescription())
                    .contains(coilName),
                "Legacy fusion structure tooltip does not identify its actual coil block");
            check(
                machine.getCasing() == Block.getBlockFromItem(casings[i].getItem())
                    && machine.getCasingMeta() == casings[i].getItemDamage(),
                "Coil replacement also changed a legacy fusion's ordinary casing");
            machine.construct(controllers[i], false);
            // Standard fusion hatch locations, expressed in the controller's public local coordinate system.
            hatch(world, player, machine, energy[i], -4, 0, -10);
            hatch(world, player, machine, ItemList.Hatch_Input_LuV.get(1), -1, -1, -11);
            hatch(world, player, machine, ItemList.Hatch_Output_LuV.get(1), -1, 0, 0);
            check(machine.checkStructure(true, tile), "BEC fusion structure did not form with native hatches");
            int[] coil = findCoil(world, tile);
            check(coil != null, "Native builder did not place the isolation coil element");
            world.setBlock(
                coil[0],
                coil[1],
                coil[2],
                Block.getBlockFromItem(oldCoils[i].getItem()),
                oldCoils[i].getItemDamage(),
                3);
            check(!machine.checkStructure(true, tile), "Legacy fusion still accepts its previous coil block");
            world.setBlock(coil[0], coil[1], coil[2], GregTechAPI.sBlockCasingsBEC, 1, 3);
            check(machine.checkStructure(true, tile), "Restoring an isolation coil did not restore formation");
            world.setBlockToAir(tile.xCoord, tile.yCoord, tile.zCoord);
        }
        MTEFusionComputer mk1 = (MTEFusionComputer) GregTechAPI.METATILEENTITIES[ItemList.FusionComputer_LuV.get(1)
            .getItemDamage()];
        check(
            mk1.getFusionCoil() == GregTechAPI.sBlockCasings1 && mk1.getFusionCoilMeta() == 15,
            "MK1 fusion's coil changed");
        ItemStack[] compact = { ItemRefer.Compact_Fusion_MK1.get(1), ItemRefer.Compact_Fusion_MK2.get(1),
            ItemRefer.Compact_Fusion_MK3.get(1), ItemRefer.Compact_Fusion_MK4.get(1),
            ItemRefer.Compact_Fusion_MK5.get(1) };
        for (int i = 0; i < compact.length; i++) {
            MTELargeFusionComputer machine = (MTELargeFusionComputer) GregTechAPI.METATILEENTITIES[compact[i]
                .getItemDamage()];
            check(
                machine.getCoilBlock() == Loaders.compactFusionCoil && machine.getCoilMeta() == i,
                "Compact fusion coil changed");
        }
    }

    private static int[] findCoil(WorldServer world, BaseMetaTileEntity tile) {
        for (int x = tile.xCoord - 16; x <= tile.xCoord + 16; x++)
            for (int y = tile.yCoord - 1; y <= tile.yCoord + 1; y++)
                for (int z = tile.zCoord - 16; z <= tile.zCoord + 16; z++) {
                    if (world.getBlock(x, y, z) == GregTechAPI.sBlockCasingsBEC && world.getBlockMetadata(x, y, z) == 1)
                        return new int[] { x, y, z };
                }
        return null;
    }

    private static void hatch(WorldServer world, FakePlayer player, MTEFusionComputer machine, ItemStack hatch, int a,
        int b, int c) {
        int[] offset = new int[3];
        machine.getExtendedFacing()
            .getWorldOffset(new int[] { a, b, c }, offset);
        BaseMetaTileEntity controller = (BaseMetaTileEntity) machine.getBaseMetaTileEntity();
        place(
            world,
            player,
            hatch,
            controller.xCoord + offset[0],
            controller.yCoord + offset[1],
            controller.zCoord + offset[2]);
    }

    private static BaseMetaTileEntity place(WorldServer world, FakePlayer player, ItemStack item, int x, int y, int z) {
        FixtureWorld.loadAround(world, x, z);
        world.setBlockToAir(x, y, z);
        check(
            ((ItemBlock) item.getItem()).placeBlockAt(item, player, world, x, y, z, 1, 0.5f, 0.5f, 0.5f, 0),
            "Could not place native fusion fixture block");
        return (BaseMetaTileEntity) world.getTileEntity(x, y, z);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
