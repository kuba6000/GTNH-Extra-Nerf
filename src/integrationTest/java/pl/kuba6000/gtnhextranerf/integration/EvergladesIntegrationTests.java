package pl.kuba6000.gtnhextranerf.integration;

import java.util.UUID;

import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;

import com.mojang.authlib.GameProfile;

import bartworks.common.loaders.ItemRegistry;
import cpw.mods.fml.common.gameevent.TickEvent;
import goodgenerator.util.ItemRefer;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.ItemList;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gtPlusPlus.xmod.gregtech.api.enums.GregtechItemList;
import pl.kuba6000.gtnhextranerf.progression.MachineProgression;
import pl.kuba6000.gtnhextranerf.progression.ProgressionServerTasks;
import pl.kuba6000.gtnhextranerf.progression.ProgressionTier;
import toxiceverglades.dimension.DimensionEverglades;
import toxiceverglades.world.WorldProviderMod;

/** Real server ticks in the registered dimension, including TecTech's native explosion immunity. */
final class EvergladesIntegrationTests {

    private EvergladesIntegrationTests() {}

    static void verify(WorldServer ordinary) {
        BaseMetaTileEntity safe = place(ordinary, ItemList.Machine_LV_E_Furnace.get(1), 736);
        safe.disableWorking();
        safe.updateEntityProfiled();
        check(ordinary.getTileEntity(736, 210, 160) == safe, "Everglades ban destroyed an Overworld machine");
        ordinary.setBlockToAir(736, 210, 160);

        DimensionManager.initDimension(DimensionEverglades.DIMID);
        WorldServer toxic = DimensionManager.getWorld(DimensionEverglades.DIMID);
        check(toxic != null && toxic.provider instanceof WorldProviderMod, "Native Everglades world is missing");
        MachineProgression.certify(ordinary, ProgressionTier.LV);
        check(
            MachineProgression.isCertified(ordinary, ProgressionTier.LV),
            "Fixture did not grant electrical certification");
        ItemStack[] machines = { ItemList.Machine_LV_E_Furnace.get(1), ItemList.Machine_Bronze_Furnace.get(1),
            ItemList.Generator_Diesel_LV.get(1), ItemList.Hatch_Energy_LV.get(1),
            GregtechItemList.Industrial_Mixer.get(1), ItemRegistry.vat.copy(), ItemRefer.Coolant_Tower.get(1) };
        for (int index = 0; index < machines.length; index++) {
            int x = 32 + index * 16;
            BaseMetaTileEntity tile = place(toxic, machines[index], x);
            tile.disableWorking();
            tile.setStoredEU(0);
            if (index == 3) {
                NBTTagCompound save = new NBTTagCompound();
                tile.writeToNBT(save);
                BaseMetaTileEntity reloaded = new BaseMetaTileEntity();
                reloaded.readFromNBT(save);
                toxic.setTileEntity(x, 210, 160, reloaded);
                tile = reloaded;
            }
            tile.updateEntityProfiled();
            check(toxic.isAirBlock(x, 210, 160), "GT machine survived its Everglades tick: " + machines[index]);
        }
        FixtureWorld.loadAround(toxic, 160, 160);
        toxic.setBlock(160, 210, 160, Blocks.furnace);
        toxic.getTileEntity(160, 210, 160)
            .updateEntity();
        check(toxic.getBlock(160, 210, 160) == Blocks.furnace, "Everglades ban affects non-GT machines");
        toxic.setBlockToAir(160, 210, 160);
        boolean terrainExplosions = GregTechAPI.sMachineExplosions;
        try {
            GregTechAPI.sMachineExplosions = false;
            BaseMetaTileEntity immune = place(toxic, ItemRefer.Coolant_Tower.get(1), 192);
            toxic.setBlock(193, 210, 160, Blocks.stone);
            immune.updateEntityProfiled();
            check(toxic.isAirBlock(192, 210, 160), "Disabling terrain damage bypasses the machine ban");
            check(toxic.getBlock(193, 210, 160) == Blocks.stone, "Fallback ignores disabled terrain damage");
            toxic.setBlockToAir(193, 210, 160);
        } finally {
            GregTechAPI.sMachineExplosions = terrainExplosions;
        }
        new ProgressionServerTasks().onTick(new TickEvent.ServerTickEvent(TickEvent.Phase.END));
        check(
            !MachineProgression.isCertified(ordinary, ProgressionTier.LV),
            "Forced explosion bypassed global accident revocation");
    }

    private static BaseMetaTileEntity place(WorldServer world, ItemStack machine, int x) {
        FakePlayer player = FakePlayerFactory.get(world, new GameProfile(UUID.randomUUID(), "EvergladesTest"));
        FixtureWorld.loadAround(world, x, 160);
        world.setBlockToAir(x, 210, 160);
        check(
            ((ItemBlock) machine.getItem()).placeBlockAt(machine, player, world, x, 210, 160, 1, .5f, .5f, .5f, 0),
            "Could not place native machine in dimension");
        BaseMetaTileEntity tile = (BaseMetaTileEntity) world.getTileEntity(x, 210, 160);
        check(tile != null && tile.getMetaTileEntity() != null, "Native machine tile did not initialize");
        return tile;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
