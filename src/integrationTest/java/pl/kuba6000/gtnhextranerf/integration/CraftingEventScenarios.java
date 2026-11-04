package pl.kuba6000.gtnhextranerf.integration;

import java.awt.Rectangle;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiCrafting;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.ContainerWorkbench;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.stats.AchievementList;
import net.minecraft.stats.StatBase;
import net.minecraft.stats.StatList;
import net.minecraft.stats.StatisticsFile;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.world.WorldSettings;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.TestPos;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;
import com.gtnewhorizons.horizonqa.api.client.ClickTarget;
import com.gtnewhorizons.horizonqa.api.client.ClientTarget;
import com.gtnewhorizons.horizonqa.api.client.ClientTest;

import betterquesting.api.api.QuestingAPI;
import betterquesting.api.questing.IQuest;
import betterquesting.api2.cache.QuestCache;
import betterquesting.questing.QuestDatabase;

/** Tests crafting events and retrieval using the actual crafting table UI. */
@GameTestHolder(value = "gtnhextranerfquesttests", clientOnly = true)
public final class CraftingEventScenarios {

    private static final UUID CRAFTING_TABLE_QUEST = new UUID(0, 1204);

    private CraftingEventScenarios() {}

    @GameTest(template = "gtnhextranerftests:native_fixture", timeoutTicks = 1800)
    public static void craftingEventCompletesTask(GameTestHelper helper) {
        QuestReloadBarrier barrier = new QuestReloadBarrier();
        helper.afterTest(barrier::close);
        TestPos pos = helper.absolute("workbench");
        int chestX = pos.x() + 1, chestY = pos.y(), chestZ = pos.z();
        ItemStack table = new ItemStack(Blocks.crafting_table);
        ClientTarget result = craftingSlot("crafting result", 0, false, table);
        ClientTarget inventory = craftingSlot("empty player inventory slot", 10, true, table);

        ClientTest.scenario(helper)
            .defaultTimeoutTicks(400)
            .server("prepare the real crafting quest and workbench", () -> {
                EntityPlayerMP player = player();
                UUID user = QuestingAPI.getQuestingUUID(player);
                IQuest quest = quest();
                Map<UUID, NBTTagCompound> savedProgress = new HashMap<>();
                savedProgress.put(CRAFTING_TABLE_QUEST, quest.writeProgressToNBT(new NBTTagCompound(), null));
                for (UUID id : quest.getRequirements()) {
                    IQuest prerequisite = QuestDatabase.INSTANCE.get(id);
                    check(prerequisite != null, "Missing crafting-table quest prerequisite");
                    savedProgress.put(id, prerequisite.writeProgressToNBT(new NBTTagCompound(), null));
                }
                ItemStack[] savedInventory = new ItemStack[player.inventory.getSizeInventory()];
                for (int i = 0; i < savedInventory.length; i++) {
                    ItemStack old = player.inventory.getStackInSlot(i);
                    savedInventory[i] = old == null ? null : old.copy();
                }
                ItemStack cursor = player.inventory.getItemStack();
                ItemStack savedCursor = cursor == null ? null : cursor.copy();
                int savedSelected = player.inventory.currentItem;
                WorldSettings.GameType savedMode = player.theItemInWorldManager.getGameType();
                double x = player.posX, y = player.posY, z = player.posZ;
                Block savedBlock = helper.getWorld()
                    .getBlock(pos.x(), pos.y(), pos.z());
                int savedMetadata = helper.getWorld()
                    .getBlockMetadata(pos.x(), pos.y(), pos.z());
                Block savedChestBlock = helper.getWorld()
                    .getBlock(chestX, chestY, chestZ);
                int savedChestMetadata = helper.getWorld()
                    .getBlockMetadata(chestX, chestY, chestZ);
                check(savedChestBlock == Blocks.air, "Chest fixture location is occupied");
                StatisticsFile stats = player.func_147099_x();
                StatBase craftStat = craftStat();
                int savedCrafts = stats.writeStat(craftStat);
                int savedAchievement = stats.writeStat(AchievementList.buildWorkBench);

                helper.afterTest(() -> {
                    if (player.openContainer instanceof ContainerWorkbench) {
                        InventoryCrafting matrix = ((ContainerWorkbench) player.openContainer).craftMatrix;
                        for (int i = 0; i < matrix.getSizeInventory(); i++) matrix.setInventorySlotContents(i, null);
                    }
                    player.inventory.setItemStack(null);
                    player.closeScreen();
                    for (Map.Entry<UUID, NBTTagCompound> entry : savedProgress.entrySet()) {
                        IQuest original = QuestDatabase.INSTANCE.get(entry.getKey());
                        if (original != null) original.readProgressFromNBT(entry.getValue(), false);
                    }
                    for (int i = 0; i < savedInventory.length; i++) {
                        player.inventory.setInventorySlotContents(i, savedInventory[i]);
                    }
                    player.inventory.setItemStack(savedCursor);
                    player.inventory.currentItem = savedSelected;
                    player.theItemInWorldManager.setGameType(savedMode);
                    player.setPositionAndUpdate(x, y, z);
                    stats.func_150873_a(player, craftStat, savedCrafts);
                    stats.func_150873_a(player, AchievementList.buildWorkBench, savedAchievement);
                    helper.getWorld()
                        .setBlock(pos.x(), pos.y(), pos.z(), savedBlock, savedMetadata, 3);
                    if (helper.getWorld()
                        .getTileEntity(chestX, chestY, chestZ) instanceof TileEntityChest) {
                        TileEntityChest chest = (TileEntityChest) helper.getWorld()
                            .getTileEntity(chestX, chestY, chestZ);
                        for (int i = 0; i < chest.getSizeInventory(); i++) chest.setInventorySlotContents(i, null);
                    }
                    helper.getWorld()
                        .setBlock(chestX, chestY, chestZ, savedChestBlock, savedChestMetadata, 3);
                    QuestCache cache = cache(player);
                    cache.updateCache(player);
                    savedProgress.keySet()
                        .forEach(cache::markQuestDirty);
                    player.sendContainerToPlayer(player.openContainer);
                    player.updateHeldItem();
                });

                for (UUID id : quest.getRequirements()) {
                    QuestDatabase.INSTANCE.get(id)
                        .setComplete(user, System.currentTimeMillis());
                }
                for (int i = 0; i < savedInventory.length; i++) player.inventory.setInventorySlotContents(i, null);
                player.inventory.setItemStack(null);
                player.theItemInWorldManager.setGameType(WorldSettings.GameType.SURVIVAL);
                stats.func_150873_a(player, craftStat, 0);
                quest.resetUser(user, true);
                check(quest.isUnlocked(user), "Crafting-table quest is locked");
                QuestCache cache = cache(player);
                cache.updateCache(player);
                check(
                    cache.getActiveQuests()
                        .contains(CRAFTING_TABLE_QUEST),
                    "Crafting-table quest is not active");
                check(
                    !quest.getTasks()
                        .getValue(0)
                        .isComplete(user),
                    "Crafting task began completed");
                helper.getWorld()
                    .setBlock(pos.x(), pos.y(), pos.z(), Blocks.crafting_table, 0, 3);
                helper.getWorld()
                    .setBlock(chestX, chestY, chestZ, Blocks.chest, 0, 3);
                player.setPositionAndUpdate(pos.x() + 1.5, pos.y(), pos.z() + 1.5);
                player.sendContainerToPlayer(player.openContainer);
            })
            .awaitClient("workbench exists on the client", client -> {
                check(
                    Minecraft.getMinecraft().theWorld.getBlock(pos.x(), pos.y(), pos.z()) == Blocks.crafting_table,
                    "Crafting table is not synchronized");
            })
            .server("open the actual workbench and find its recipe", () -> {
                EntityPlayerMP player = player();
                check(
                    Blocks.crafting_table
                        .onBlockActivated(helper.getWorld(), pos.x(), pos.y(), pos.z(), player, 1, 0.5f, 0.5f, 0.5f),
                    "Crafting table did not open");
                check(player.openContainer instanceof ContainerWorkbench, "Workbench container is unavailable");
                InventoryCrafting matrix = ((ContainerWorkbench) player.openContainer).craftMatrix;
                int[] slots = { 0, 1, 3, 4 };
                boolean found = false;
                for (int mask = 0; mask < 16 && !found; mask++) {
                    if (Integer.bitCount(mask) != 2) continue;
                    for (int i = 0; i < matrix.getSizeInventory(); i++) matrix.setInventorySlotContents(i, null);
                    for (int i = 0; i < slots.length; i++) {
                        matrix.setInventorySlotContents(
                            slots[i],
                            (mask & (1 << i)) != 0 ? new ItemStack(Items.flint) : new ItemStack(Blocks.log));
                    }
                    ItemStack output = CraftingManager.getInstance()
                        .findMatchingRecipe(matrix, helper.getWorld());
                    found = output != null && output.isItemEqual(table);
                }
                check(found, "No registered flint-and-logs recipe produces the crafting table");
                player.sendContainerToPlayer(player.openContainer);
            })
            .awaitScreen(GuiCrafting.class)
            .awaitClient("crafting result reached the client", client -> {
                ItemStack output = Minecraft.getMinecraft().thePlayer.openContainer.getSlot(0)
                    .getStack();
                check(output != null && output.isItemEqual(table), "Crafting-table result is not synchronized");
                check(Minecraft.getMinecraft().thePlayer.inventory.getItemStack() == null, "Cursor is not empty");
            })
            .click(result)
            .awaitServer("crafting completes the task with the result only on the cursor", () -> {
                EntityPlayerMP player = player();
                ItemStack held = player.inventory.getItemStack();
                check(held != null && held.isItemEqual(table), "Crafted table did not reach the server cursor");
                check(count(player, table) == 0, "Crafted table unexpectedly reached inventory");
                check(
                    player.func_147099_x()
                        .writeStat(craftStat()) > 0,
                    "The crafting action did not happen");
                UUID user = QuestingAPI.getQuestingUUID(player);
                IQuest quest = quest();
                check(
                    quest.getTasks()
                        .getValue(0)
                        .isComplete(user),
                    "Crafting event did not complete the task");
                quest.detect(player);
                check(
                    quest.getTasks()
                        .getValue(0)
                        .isComplete(user),
                    "Detect discarded crafting completion");
            })
            .awaitClient("crafted table reached the cursor", client -> {
                ItemStack held = Minecraft.getMinecraft().thePlayer.inventory.getItemStack();
                check(held != null && held.isItemEqual(table), "Crafted table is not on the client cursor");
            })
            .click(inventory)
            .awaitServer("the crafted table is in the inventory", () -> {
                EntityPlayerMP player = player();
                check(player.inventory.getItemStack() == null, "Crafted table remains on the cursor");
                check(count(player, table) == 1, "Crafted table did not enter the inventory");
            })
            .escape()
            .awaitServer("crafted table remains owned", () -> {
                EntityPlayerMP player = player();
                UUID user = QuestingAPI.getQuestingUUID(player);
                check(
                    quest().getTasks()
                        .getValue(0)
                        .isComplete(user),
                    "Crafting task lost its completion");
                check(count(player, table) == 1, "Crafting table was consumed or duplicated");
            })
            .server("store the table and clear the crafting statistic", () -> {
                EntityPlayerMP player = player();
                TileEntityChest chest = chest(helper, chestX, chestY, chestZ);
                int slot = -1;
                for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
                    ItemStack stack = player.inventory.getStackInSlot(i);
                    if (stack != null && stack.isItemEqual(table)) {
                        check(slot < 0 && stack.stackSize == 1, "Unexpected extra crafting tables");
                        slot = i;
                    }
                }
                check(slot >= 0, "Crafted table is missing before storage");
                chest.setInventorySlotContents(
                    0,
                    player.inventory.getStackInSlot(slot)
                        .copy());
                player.inventory.setInventorySlotContents(slot, null);
                player.func_147099_x()
                    .func_150873_a(player, craftStat(), 0);
                check(count(player, table) == 0, "Crafting table remains in inventory");
                check(player.inventory.getItemStack() == null, "Crafting table remains on the cursor");
                check(
                    player.func_147099_x()
                        .writeStat(craftStat()) == 0,
                    "Crafting statistic was not cleared");
                check(
                    ItemStack.areItemStacksEqual(table, chest.getStackInSlot(0)),
                    "Crafting table was lost in storage");
                player.sendContainerToPlayer(player.openContainer);
            })
            .server("reload the real questbook", () -> {
                barrier.beforeReload();
                MinecraftServer server = MinecraftServer.getServer();
                check(
                    server.getCommandManager()
                        .executeCommand(server, "bq_admin default load") > 0,
                    "Questbook reload failed");
            })
            .client("request the questbook synchronization marker", client -> barrier.request())
            .awaitClient("questbook reload is synchronized", client -> barrier.await())
            .server("verify crafting completion after synchronization", () -> {
                UUID user = QuestingAPI.getQuestingUUID(player());
                check(
                    quest().getTasks()
                        .getValue(0)
                        .isComplete(user),
                    "Reload discarded crafting completion");
                EntityPlayerMP player = player();
                check(count(player, table) == 0, "Reload returned the crafting table to inventory");
                check(player.inventory.getItemStack() == null, "Crafting table returned to the cursor");
                check(
                    player.func_147099_x()
                        .writeStat(craftStat()) == 0,
                    "Reload changed the crafting statistic");
                check(
                    ItemStack.areItemStacksEqual(table, chest(helper, chestX, chestY, chestZ).getStackInSlot(0)),
                    "Reload consumed or changed the stored crafting table");
            })
            .awaitClient(
                "client remains in the world after the questbook reload",
                client -> {
                    check(Minecraft.getMinecraft().theWorld != null, "Client world unavailable after questbook reload");
                })
            .capture("crafting-task-completed")
            .succeed();
    }

    private static ClientTarget craftingSlot(String description, int index, boolean empty, ItemStack result) {
        return ClientTarget.of(description, window -> {
            GuiCrafting gui = window.screen(GuiCrafting.class);
            Slot slot = gui.inventorySlots.getSlot(index);
            return new ClickTarget(
                slot,
                new Rectangle(
                    (gui.width - 176) / 2 + slot.xDisplayPosition,
                    (gui.height - 166) / 2 + slot.yDisplayPosition,
                    16,
                    16),
                () -> {
                    ItemStack stack = slot.getStack();
                    if (!empty) return stack != null && stack.isItemEqual(result);
                    ItemStack cursor = Minecraft.getMinecraft().thePlayer.inventory.getItemStack();
                    return stack == null && cursor != null && cursor.isItemEqual(result);
                });
        });
    }

    private static int count(EntityPlayerMP player, ItemStack target) {
        int amount = 0;
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (stack != null && stack.isItemEqual(target)) amount += stack.stackSize;
        }
        return amount;
    }

    private static StatBase craftStat() {
        StatBase stat = StatList.objectCraftStats[Item.getIdFromItem(Item.getItemFromBlock(Blocks.crafting_table))];
        check(stat != null, "Crafting-table statistic is unavailable");
        return stat;
    }

    private static QuestCache cache(EntityPlayerMP player) {
        QuestCache cache = (QuestCache) player.getExtendedProperties(QuestCache.LOC_QUEST_CACHE.toString());
        check(cache != null, "BetterQuesting player cache is unavailable");
        return cache;
    }

    private static TileEntityChest chest(GameTestHelper helper, int x, int y, int z) {
        check(
            helper.getWorld()
                .getTileEntity(x, y, z) instanceof TileEntityChest,
            "Storage chest is unavailable");
        return (TileEntityChest) helper.getWorld()
            .getTileEntity(x, y, z);
    }

    private static IQuest quest() {
        IQuest quest = QuestDatabase.INSTANCE.get(CRAFTING_TABLE_QUEST);
        check(quest != null, "Crafting-table quest is unavailable");
        return quest;
    }

    private static EntityPlayerMP player() {
        return (EntityPlayerMP) MinecraftServer.getServer()
            .getConfigurationManager().playerEntityList.get(0);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
