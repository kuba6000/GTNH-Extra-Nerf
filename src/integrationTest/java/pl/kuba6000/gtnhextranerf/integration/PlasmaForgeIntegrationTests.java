package pl.kuba6000.gtnhextranerf.integration;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.common.util.ForgeDirection;

import com.mojang.authlib.GameProfile;

import gregtech.api.GregTechAPI;
import gregtech.api.enums.ItemList;
import gregtech.api.interfaces.ISecondaryDescribable;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.structure.error.StructureError;
import gregtech.api.structure.error.StructureErrorRegistry;
import gregtech.api.structure.error.StructureErrors;
import gregtech.common.tileentities.machines.multi.MTEPlasmaForge;
import io.netty.buffer.Unpooled;

/** Native structure validation and the same error serialization used by the controller GUI. */
final class PlasmaForgeIntegrationTests {

    private static final StructureError COIL_ERROR = StructureErrors.of("gtnhextranerf.structure_error.stinky_coils");

    private PlasmaForgeIntegrationTests() {}

    static void verify(WorldServer world) throws Exception {
        FakePlayer player = FakePlayerFactory.get(world, new GameProfile(UUID.randomUUID(), "PlasmaCoilTest"));
        BaseMetaTileEntity tile = place(world, player, ItemList.Machine_Multi_PlasmaForge.get(1), 320, 210, 80);
        tile.setFrontFacing(ForgeDirection.NORTH);
        MTEPlasmaForge machine = (MTEPlasmaForge) tile.getMetaTileEntity();
        machine.construct(ItemList.Machine_Multi_PlasmaForge.get(1), false);
        List<int[]> casings = positions(world, tile, GregTechAPI.sBlockCasings1, 13);
        List<int[]> coils = positions(world, tile, GregTechAPI.sBlockCasings5, -1);
        check(casings.size() > 5 && !coils.isEmpty(), "Native Plasma Forge construction is incomplete");
        ItemStack[] hatches = { ItemList.Hatch_Energy_UHV.get(1), ItemList.Hatch_Input_UV.get(1),
            ItemList.Hatch_Output_UV.get(1), ItemList.Hatch_Input_Bus_UV.get(1), ItemList.Hatch_Output_Bus_UV.get(1) };
        for (int i = 0; i < hatches.length; i++) {
            int[] pos = casings.get(i);
            place(world, player, hatches[i], pos[0], pos[1], pos[2]);
        }
        replaceCoils(world, coils, ItemList.Casing_Coil_Cupronickel.get(1));
        int[] missing = coils.get(0);
        world.setBlockToAir(missing[0], missing[1], missing[2]);
        check(!machine.checkStructure(true, tile), "Incomplete Plasma Forge formed");
        check(!errors(machine, tile).contains(COIL_ERROR), "Incomplete structure revealed the minimum coil tier");
        set(world, missing, ItemList.Casing_Coil_Cupronickel.get(1));
        check(!machine.checkStructure(true, tile), "Complete Plasma Forge accepted coils below Hypogen");
        List<StructureError> errors = errors(machine, tile);
        check(
            errors.size() == 1 && errors.contains(COIL_ERROR),
            "Complete low-coil structure lacks the GUI error: " + errors);
        PacketBuffer packet = new PacketBuffer(Unpooled.buffer());
        try {
            StructureErrorRegistry.serialize(packet, errors.get(0));
            check(
                COIL_ERROR.equals(StructureErrorRegistry.deserialize(packet)),
                "Controller error was lost across server/client serialization");
        } finally {
            packet.release();
        }
        int[] body = casings.get(5);
        world.setBlockToAir(body[0], body[1], body[2]);
        check(!machine.checkStructure(true, tile), "Damaged Plasma Forge formed");
        check(!errors(machine, tile).contains(COIL_ERROR), "Coil GUI error survived an incomplete structure");
        world.setBlock(body[0], body[1], body[2], GregTechAPI.sBlockCasings1, 13, 3);
        replaceCoils(world, coils, ItemList.Casing_Coil_Hypogen.get(1));
        boolean hypogenFormed = machine.checkStructure(true, tile);
        List<StructureError> hypogenErrors = errors(machine, tile);
        check(
            hypogenFormed && hypogenErrors.isEmpty(),
            "Hypogen did not restore Plasma Forge formation: formed=" + hypogenFormed
                + ", coil="
                + machine.getCoilLevel()
                + ", errors="
                + hypogenErrors);
        replaceCoils(world, coils, ItemList.Casing_Coil_Eternal.get(1));
        check(machine.checkStructure(true, tile), "A coil above Hypogen was rejected");
        String tooltip = String.join("\n", ((ISecondaryDescribable) machine).getPrimaryDescription());
        check(!tooltip.contains("Hypogen or better required"), "Controller tooltip reveals the hidden minimum");
        world.setBlockToAir(tile.xCoord, tile.yCoord, tile.zCoord);
    }

    private static List<StructureError> errors(MTEPlasmaForge machine, BaseMetaTileEntity tile) {
        machine.clearHatches();
        List<StructureError> errors = new ArrayList<>();
        machine.checkMachine(tile, null, errors);
        return errors;
    }

    private static List<int[]> positions(WorldServer world, BaseMetaTileEntity tile, Block block, int meta) {
        List<int[]> found = new ArrayList<>();
        for (int x = tile.xCoord - 16; x <= tile.xCoord + 16; x++)
            for (int y = tile.yCoord - 24; y <= tile.yCoord + 24; y++)
                for (int z = tile.zCoord - 16; z <= tile.zCoord + 16; z++)
                    if (world.getBlock(x, y, z) == block && (meta < 0 || world.getBlockMetadata(x, y, z) == meta))
                        found.add(new int[] { x, y, z });
        return found;
    }

    private static void replaceCoils(WorldServer world, List<int[]> positions, ItemStack coil) {
        for (int[] pos : positions) set(world, pos, coil);
    }

    private static void set(WorldServer world, int[] pos, ItemStack block) {
        world.setBlock(pos[0], pos[1], pos[2], Block.getBlockFromItem(block.getItem()), block.getItemDamage(), 3);
    }

    private static BaseMetaTileEntity place(WorldServer world, FakePlayer player, ItemStack item, int x, int y, int z) {
        FixtureWorld.loadAround(world, x, z);
        world.setBlockToAir(x, y, z);
        check(
            ((ItemBlock) item.getItem()).placeBlockAt(item, player, world, x, y, z, 1, 0.5f, 0.5f, 0.5f, 0),
            "Could not place native Plasma Forge fixture");
        return (BaseMetaTileEntity) world.getTileEntity(x, y, z);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
