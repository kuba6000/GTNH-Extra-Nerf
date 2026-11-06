package pl.kuba6000.gtnhextranerf.integration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;

import com.mojang.authlib.GameProfile;

import appeng.api.AEApi;
import cpw.mods.fml.common.Loader;
import eu.usrv.enhancedlootbags.EnhancedLootBags;
import eu.usrv.enhancedlootbags.core.serializer.LootGroups;
import eu.usrv.enhancedlootbags.core.serializer.LootGroups.LootGroup;
import eu.usrv.enhancedlootbags.core.serializer.LootGroups.LootGroup.Drop;
import pl.kuba6000.gtnhextranerf.GTNHExtraNerf;
import pl.kuba6000.gtnhextranerf.loader.LootLoader;
import pl.kuba6000.gtnhextranerf.loot.NativeLootTables;
import pl.kuba6000.gtnhextranerf.loot.modules.MeControllerLootModule;
import pl.kuba6000.gtnhextranerf.loot.runtime.ServerLootRuntime;

/** Runs the original transformed ELB right-click; only its RNG is deterministic in this fixture. */
public final class LootOpeningIntegrationTests {

    static void verify(net.minecraft.server.MinecraftServer server) {
        Random previous = EnhancedLootBags.Rnd;
        try {
            Path xml = Loader.instance()
                .getConfigDir()
                .toPath()
                .resolve("EnhancedLootBags/LootBags.xml");
            byte[] originalFile = Files.readAllBytes(xml);
            LootGroups published = EnhancedLootBags.LootGroupHandler.getLootGroups();
            LootLoader.verify(published);
            // Warm the inherited/Fortune caches before reload.
            EnhancedLootBags.Rnd = deterministic(0);
            LootGroup cached = EnhancedLootBags.LootGroupHandler.getMergedGroupFromID(38, 1);
            assertControllersAreOne(cached);
            if (!EnhancedLootBags.LootGroupHandler.reload()) throw new AssertionError("Audited reload failed");
            if (published == EnhancedLootBags.LootGroupHandler.getLootGroups())
                throw new AssertionError("Reload did not publish fresh native tables");
            LootGroup rebuilt = EnhancedLootBags.LootGroupHandler.getMergedGroupFromID(38, 1);
            if (cached == rebuilt) throw new AssertionError("Reload reused a stale merged cache");
            assertControllersAreOne(rebuilt);
            if (!java.util.Arrays.equals(originalFile, Files.readAllBytes(xml)))
                throw new AssertionError("Patch rewrote the user's XML");
            ItemStack controller = AEApi.instance()
                .definitions()
                .blocks()
                .controller()
                .maybeStack(1)
                .get();
            WorldServer world = DimensionManager.getWorld(0);
            open(world, 1, 0, false, 0, controller);
            // RNG deliberately chooses another item: the first eligible opening must still issue the controller.
            open(world, 38, 3, false, 1, controller);
            open(world, 37, 0, true, 0, controller);
            ServerLootRuntime.stop();
            ServerLootRuntime.start(server);
            if (!EnhancedLootBags.LootGroupHandler.reload()) throw new AssertionError("Second audited reload failed");
            open(world, 38, 1, true, 0, controller);
            open(world, 38, 1, false, 0, controller);
            for (int group : new int[] { 37, 38 }) for (int fortune = 0; fortune <= 3; fortune++)
                assertControllersAreOne(EnhancedLootBags.LootGroupHandler.getMergedGroupFromID(group, fortune));
        } catch (Exception | AssertionError failure) {
            throw new IllegalStateException("GTNH Extra Nerf integrity check failed: native loot integration", failure);
        } finally {
            EnhancedLootBags.Rnd = previous;
        }
        GTNHExtraNerf.LOG.info(
            "Extra Nerf native loot integration PASSED: audited reload, rebuilt caches, native right-click, global cap/reopen, unchanged XML and preview tables");
    }

    private static void assertControllersAreOne(LootGroup group) {
        int found = 0;
        for (Drop drop : group.getDrops()) if (NativeLootTables.item(drop)
            .equals(MeControllerLootModule.ITEM)) {
                if (drop.getAmount() != 1 || drop.getIsRandomAmount())
                    throw new AssertionError("Incorrect native preview reward");
                found++;
            }
        if (found == 0) throw new AssertionError("Controller disappeared from native preview");
    }

    private static Random deterministic(double ticket) {
        return new Random(0) {

            @Override
            public int nextInt(int bound) {
                return 0;
            } // no shuffle; one draw; minimum random quantity

            @Override
            public double nextDouble() {
                return ticket;
            }
        };
    }

    private static void open(WorldServer world, int group, int fortune, boolean selectController,
        int expectedControllers, ItemStack controller) {
        EnhancedLootBags.Rnd = deterministic(0);
        LootGroup table = EnhancedLootBags.LootGroupHandler.getMergedGroupFromID(group, fortune);
        long prefix = 0;
        Double ticket = null;
        for (Drop drop : table.getDrops()) {
            boolean isController = NativeLootTables.item(drop)
                .equals(MeControllerLootModule.ITEM);
            if (drop.getChance() > 0 && isController == selectController
                && drop.getItemDropGroup()
                    .isEmpty()
                && (selectController || drop.getLimitedDropCount() == 0)) {
                ticket = (prefix + drop.getChance() * 0.5) / table.getMaxWeight();
                break;
            }
            prefix += drop.getChance();
        }
        if (ticket == null) throw new AssertionError("Missing deterministic test reward");
        EnhancedLootBags.Rnd = deterministic(ticket);
        FakePlayer player = FakePlayerFactory.get(world, new GameProfile(UUID.randomUUID(), "LootTest"));
        player.setPosition(0, 250, 0);
        world.getChunkFromBlockCoords(0, 0);
        ItemStack bag = new ItemStack(EnhancedLootBags.LootGroupHandler.getLootBagItem(), 2, group);
        if (fortune > 0) bag.addEnchantment(Enchantment.fortune, fortune);
        player.inventory.setInventorySlotContents(player.inventory.currentItem, bag);
        List<Object> before = new ArrayList<>(world.loadedEntityList);
        // ELB uses World.rand for the opening budget, independently of its item-selection RNG.
        long budgetSeed = 0;
        while (new Random(budgetSeed).nextInt(table.getMaxItems()) != 0) budgetSeed++;
        world.rand.setSeed(budgetSeed);
        bag.getItem()
            .onItemRightClick(bag, world, player);
        int controllers = 0;
        int entities = 0;
        for (Object object : new ArrayList<>(world.loadedEntityList)) {
            if (!(object instanceof EntityItem) || before.contains(object)) continue;
            EntityItem entity = (EntityItem) object;
            entities++;
            if (entity.getEntityItem()
                .isItemEqual(controller)) controllers += entity.getEntityItem().stackSize;
            world.removeEntity(entity);
        }
        if (controllers != expectedControllers)
            throw new AssertionError("Incorrect native controller issuance: " + controllers);
        if (selectController && expectedControllers == 0) {
            if (entities != 0 || bag.stackSize != 2)
                throw new AssertionError("Exhausted native selection issued/consumed something");
        } else if (entities == 0 || bag.stackSize != 1)
            throw new AssertionError("Successful native opening did not consume one bag");
        player.inventory.clearInventory(null, -1);
    }
}
