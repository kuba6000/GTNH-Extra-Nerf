package pl.kuba6000.gtnhextranerf.integration;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiCrafting;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerWorkbench;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.stats.StatBase;
import net.minecraft.stats.StatList;
import net.minecraft.stats.StatisticsFile;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.TestPos;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;
import com.gtnewhorizons.horizonqa.api.client.ClickTarget;
import com.gtnewhorizons.horizonqa.api.client.ClientScenario;
import com.gtnewhorizons.horizonqa.api.client.ClientTarget;
import com.gtnewhorizons.horizonqa.api.client.ClientTest;

import betterquesting.api.api.QuestingAPI;
import betterquesting.api.questing.IQuest;
import betterquesting.api.questing.tasks.ITask;
import betterquesting.api.utils.BigItemStack;
import betterquesting.api2.cache.QuestCache;
import betterquesting.questing.QuestDatabase;
import bq_standard.tasks.ITaskItemInput;

/** Exercises combined crafting and inventory progress through the actual Berry Medley quest and GUI. */
@GameTestHolder(value = "gtnhextranerfquesttests", clientOnly = true)
public final class CraftingMixedProgressScenarios {

    private static final UUID BERRY_MEDLEY_QUEST = new UUID(0, 470);
    private static final int CRAFTING_TASK = 2;

    private CraftingMixedProgressScenarios() {}

    @GameTest(template = "gtnhextranerftests:native_fixture", timeoutTicks = 1800)
    public static void craftedAndOwnedRequirementsCombineWithoutDoubleCounting(GameTestHelper helper) {
        QuestReloadBarrier barrier = new QuestReloadBarrier();
        helper.afterTest(barrier::close);
        ITask task = quest().getTasks()
            .getValue(CRAFTING_TASK);
        check(task instanceof ITaskItemInput, "Berry Medley crafting inputs are unavailable");
        List<BigItemStack> requirements = ((ITaskItemInput) task).getItemInputs();
        ItemStack bowl = new ItemStack(Items.bowl);
        BigItemStack bowlRequirement = null;
        BigItemStack knifeRequirement = null;
        List<ItemStack> owned = new ArrayList<>();
        for (BigItemStack requirement : requirements) {
            check(requirement != null && requirement.stackSize > 0, "Invalid Berry Medley requirement");
            if (requirement.getBaseStack()
                .isItemEqual(bowl)) {
                check(bowlRequirement == null, "Multiple bowl requirements need a different test setup");
                bowlRequirement = requirement;
            } else {
                owned.addAll(requirement.getCombinedStacks());
                if ("craftingToolKnife".equals(requirement.getOreDict())) knifeRequirement = requirement;
            }
        }
        check(bowlRequirement != null && knifeRequirement != null, "Berry Medley needs both a bowl and a knife");
        int required = bowlRequirement.stackSize;
        check(required >= 2 && required <= bowl.getMaxStackSize(), "Bowl requirement does not fit this scenario");
        ItemStack knife = knifeRequirement.getBaseStack()
            .copy();
        Recipe recipe = findBowlRecipe(helper.getWorld(), knife, required);
        check(recipe.output > 0 && recipe.output < required, "Bowl recipe must require multiple crafts");
        int crafts = (required + recipe.output - 1) / recipe.output;
        int totalBowls = crafts * recipe.output;
        List<Integer> knifeSlots = new ArrayList<>();
        for (int i = 0; i < owned.size(); i++) {
            if (owned.get(i)
                .isItemEqual(knife)) knifeSlots.add(i);
        }
        check(!knifeSlots.isEmpty(), "Owned knife is missing from the prepared requirements");
        TestPos pos = helper.absolute("workbench");
        int chestX = pos.x() + 1, chestY = pos.y(), chestZ = pos.z();
        ClientTarget result = craftingOutput(bowl);
        ClientTarget inventory = playerInventory(bowl);

        ClientScenario scenario = ClientTest.scenario(helper)
            .defaultTimeoutTicks(400)
            .server("prepare the real Berry Medley quest", () -> {
                EntityPlayerMP player = player();
                UUID user = QuestingAPI.getQuestingUUID(player);
                IQuest quest = quest();
                Map<UUID, NBTTagCompound> savedProgress = new HashMap<>();
                savedProgress.put(BERRY_MEDLEY_QUEST, quest.writeProgressToNBT(new NBTTagCompound(), null));
                for (UUID id : quest.getRequirements()) {
                    IQuest prerequisite = QuestDatabase.INSTANCE.get(id);
                    check(prerequisite != null, "Missing Berry Medley prerequisite");
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
                StatBase stat = StatList.objectCraftStats[Item.getIdFromItem(Items.bowl)];
                int savedCrafts = stat == null ? 0 : stats.writeStat(stat);
                check(owned.size() < player.inventory.mainInventory.length, "Required items leave no inventory space");

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
                    if (stat != null) stats.func_150873_a(player, stat, savedCrafts);
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
                if (stat != null) stats.func_150873_a(player, stat, 0);
                quest.resetUser(user, true);
                check(quest.isUnlocked(user), "Berry Medley quest is locked");
                for (int i = 0; i < owned.size(); i++) player.inventory.setInventorySlotContents(
                    i,
                    owned.get(i)
                        .copy());
                QuestCache cache = cache(player);
                cache.updateCache(player);
                check(
                    cache.getActiveQuests()
                        .contains(BERRY_MEDLEY_QUEST),
                    "Berry Medley quest is not active");
                quest.detect(player);
                check(
                    !quest.getTasks()
                        .getValue(CRAFTING_TASK)
                        .isComplete(user),
                    "Task completed without bowls");
                checkOwned(player, owned);
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
            .server("open workbench with a real bowl recipe", () -> {
                EntityPlayerMP player = player();
                check(
                    Blocks.crafting_table
                        .onBlockActivated(helper.getWorld(), pos.x(), pos.y(), pos.z(), player, 1, 0.5f, 0.5f, 0.5f),
                    "Crafting table did not open");
                check(player.openContainer instanceof ContainerWorkbench, "Workbench container is unavailable");
                InventoryCrafting matrix = ((ContainerWorkbench) player.openContainer).craftMatrix;
                matrix.setInventorySlotContents(recipe.knifeSlot, knife.copy());
                matrix.setInventorySlotContents(recipe.plankSlot, new ItemStack(Blocks.planks, crafts));
                ItemStack output = CraftingManager.getInstance()
                    .findMatchingRecipe(matrix, helper.getWorld());
                check(
                    output != null && output.isItemEqual(bowl) && output.stackSize == recipe.output,
                    "Bowl recipe changed while opening the workbench");
                player.sendContainerToPlayer(player.openContainer);
            })
            .awaitScreen(GuiCrafting.class)
            .awaitClient("bowl recipe reached the client", client -> {
                ItemStack output = Minecraft.getMinecraft().thePlayer.openContainer.getSlot(0)
                    .getStack();
                check(output != null && output.isItemEqual(bowl), "Bowl result is not synchronized");
                check(Minecraft.getMinecraft().thePlayer.inventory.getItemStack() == null, "Cursor is not empty");
            });

        for (int i = 0; i < crafts - 1; i++) {
            final int before = i * recipe.output;
            final int after = (i + 1) * recipe.output;
            scenario.click(result)
                .awaitServer("a crafted bowl batch reaches the cursor", () -> {
                    EntityPlayerMP player = player();
                    ItemStack held = player.inventory.getItemStack();
                    check(
                        held != null && held.isItemEqual(bowl) && held.stackSize == recipe.output,
                        "Crafted bowls did not reach the cursor");
                    check(count(player, bowl) == before, "Unexpected bowls in inventory before placing the batch");
                })
                .click(inventory)
                .awaitServer("the same bowls cannot be counted twice", () -> {
                    EntityPlayerMP player = player();
                    check(player.inventory.getItemStack() == null, "Bowls remain on the cursor");
                    check(count(player, bowl) == after, "Crafted bowls did not reach the inventory");
                    UUID user = QuestingAPI.getQuestingUUID(player);
                    IQuest quest = quest();
                    quest.detect(player);
                    check(
                        !quest.getTasks()
                            .getValue(CRAFTING_TASK)
                            .isComplete(user),
                        "The same crafted and owned bowls were counted twice");
                    checkOwned(player, owned);
                });
        }

        scenario.server("store the detected knife and reload the questbook", () -> {
            EntityPlayerMP player = player();
            TileEntityChest chest = chest(helper.getWorld(), chestX, chestY, chestZ);
            check(knifeSlots.size() <= chest.getSizeInventory(), "Detected knives do not fit in the chest");
            for (int i = 0; i < knifeSlots.size(); i++) {
                int slot = knifeSlots.get(i);
                ItemStack stack = player.inventory.getStackInSlot(slot);
                check(ItemStack.areItemStacksEqual(owned.get(slot), stack), "Owned knife changed before storage");
                chest.setInventorySlotContents(i, stack.copy());
                player.inventory.setInventorySlotContents(slot, null);
            }
            check(count(player, knife) == 0, "A detected knife remains in the inventory");
            checkStashedOwned(player, chest, owned, knifeSlots);
            check(count(player, bowl) == (crafts - 1) * recipe.output, "Partial bowl inventory changed");
            UUID user = QuestingAPI.getQuestingUUID(player);
            IQuest before = quest();
            before.detect(player);
            check(
                !before.getTasks()
                    .getValue(CRAFTING_TASK)
                    .isComplete(user),
                "Task completed before reload");

            barrier.beforeReload();
            MinecraftServer server = MinecraftServer.getServer();
            check(
                server.getCommandManager()
                    .executeCommand(server, "bq_admin default load") > 0,
                "Questbook reload failed");
        })
            .client("request the questbook synchronization marker", client -> barrier.request())
            .awaitClient("questbook reload is synchronized", client -> barrier.await())
            .server("verify partial progress after synchronization", () -> {
                EntityPlayerMP player = player();
                UUID user = QuestingAPI.getQuestingUUID(player);
                TileEntityChest chest = chest(helper.getWorld(), chestX, chestY, chestZ);
                IQuest reloaded = quest();
                check(
                    !reloaded.getTasks()
                        .getValue(CRAFTING_TASK)
                        .isComplete(user),
                    "Reload completed the still-partial task");
                QuestCache cache = cache(player);
                cache.updateCache(player);
                check(
                    cache.getActiveQuests()
                        .contains(BERRY_MEDLEY_QUEST),
                    "Berry Medley was not active after reload");
                check(count(player, knife) == 0, "Reload returned the knife to inventory");
                checkStashedOwned(player, chest, owned, knifeSlots);
                player.sendContainerToPlayer(player.openContainer);
            })
            .awaitScreen(GuiCrafting.class)
            .awaitClient("bowl recipe is still available after reload", client -> {
                ItemStack output = Minecraft.getMinecraft().thePlayer.openContainer.getSlot(0)
                    .getStack();
                check(output != null && output.isItemEqual(bowl), "Bowl result is unavailable after reload");
                check(Minecraft.getMinecraft().thePlayer.inventory.getItemStack() == null, "Cursor is not empty");
            })
            .click(result)
            .awaitServer("crafting and owned ingredients complete different requirements", () -> {
                EntityPlayerMP player = player();
                ItemStack held = player.inventory.getItemStack();
                check(
                    held != null && held.isItemEqual(bowl) && held.stackSize == recipe.output,
                    "Final bowl batch is not on the cursor");
                check(count(player, bowl) == (crafts - 1) * recipe.output, "Final bowls entered inventory too soon");
                UUID user = QuestingAPI.getQuestingUUID(player);
                check(
                    quest().getTasks()
                        .getValue(CRAFTING_TASK)
                        .isComplete(user),
                    "Crafted bowls and the previously detected knife did not survive reload");
                check(count(player, knife) == 0, "Knife returned to inventory before completion");
                checkStashedOwned(player, chest(helper.getWorld(), chestX, chestY, chestZ), owned, knifeSlots);
            })
            .click(inventory)
            .escape()
            .awaitServer("all crafted bowls and owned ingredients remain", () -> {
                EntityPlayerMP player = player();
                check(count(player, bowl) == totalBowls, "Crafted bowls were consumed or duplicated");
                checkStashedOwned(player, chest(helper.getWorld(), chestX, chestY, chestZ), owned, knifeSlots, true);
                check(count(player, knife) == 1, "The separate crafting knife was not returned to the inventory");
                UUID user = QuestingAPI.getQuestingUUID(player);
                check(
                    quest().getTasks()
                        .getValue(CRAFTING_TASK)
                        .isComplete(user),
                    "Task lost completion");
            })
            .capture("combined-crafting-and-inventory")
            .succeed();
    }

    private static Recipe findBowlRecipe(World world, ItemStack knife, int required) {
        Container dummy = new Container() {

            @Override
            public boolean canInteractWith(EntityPlayer player) {
                return true;
            }
        };
        InventoryCrafting matrix = new InventoryCrafting(dummy, 3, 3);
        ItemStack bowl = new ItemStack(Items.bowl);
        for (int knifeSlot = 0; knifeSlot < 9; knifeSlot++) {
            for (int plankSlot = 0; plankSlot < 9; plankSlot++) {
                if (knifeSlot == plankSlot) continue;
                for (int i = 0; i < matrix.getSizeInventory(); i++) matrix.setInventorySlotContents(i, null);
                matrix.setInventorySlotContents(knifeSlot, knife.copy());
                matrix.setInventorySlotContents(plankSlot, new ItemStack(Blocks.planks, required));
                ItemStack result = CraftingManager.getInstance()
                    .findMatchingRecipe(matrix, world);
                if (result != null && result.isItemEqual(bowl))
                    return new Recipe(knifeSlot, plankSlot, result.stackSize);
            }
        }
        throw new AssertionError("No registered knife-and-plank recipe produces a bowl");
    }

    private static ClientTarget craftingOutput(ItemStack bowl) {
        return ClientTarget.of("bowl result", window -> {
            GuiCrafting gui = window.screen(GuiCrafting.class);
            Slot slot = gui.inventorySlots.getSlot(0);
            return new ClickTarget(slot, slotBounds(gui, slot), () -> {
                ItemStack stack = slot.getStack();
                return stack != null && stack.isItemEqual(bowl);
            });
        });
    }

    private static ClientTarget playerInventory(ItemStack bowl) {
        return ClientTarget.of("player inventory for bowls", window -> {
            GuiCrafting gui = window.screen(GuiCrafting.class);
            ItemStack cursor = Minecraft.getMinecraft().thePlayer.inventory.getItemStack();
            if (cursor == null || !cursor.isItemEqual(bowl)) return null;
            for (int i = 10; i < 46; i++) {
                Slot slot = gui.inventorySlots.getSlot(i);
                ItemStack stack = slot.getStack();
                if (stack != null
                    && (!stack.isItemEqual(bowl) || stack.stackSize + cursor.stackSize > stack.getMaxStackSize()))
                    continue;
                return new ClickTarget(slot, slotBounds(gui, slot), () -> {
                    ItemStack held = Minecraft.getMinecraft().thePlayer.inventory.getItemStack();
                    ItemStack target = slot.getStack();
                    return held != null && held.isItemEqual(bowl)
                        && (target == null || target.isItemEqual(bowl)
                            && target.stackSize + held.stackSize <= target.getMaxStackSize());
                });
            }
            return null;
        });
    }

    private static Rectangle slotBounds(GuiCrafting gui, Slot slot) {
        return new Rectangle(
            (gui.width - 176) / 2 + slot.xDisplayPosition,
            (gui.height - 166) / 2 + slot.yDisplayPosition,
            16,
            16);
    }

    private static void checkOwned(EntityPlayerMP player, List<ItemStack> owned) {
        for (int i = 0; i < owned.size(); i++) {
            check(
                ItemStack.areItemStacksEqual(owned.get(i), player.inventory.getStackInSlot(i)),
                "Owned quest ingredient was consumed or changed");
        }
    }

    private static void checkStashedOwned(EntityPlayerMP player, TileEntityChest chest, List<ItemStack> owned,
        List<Integer> knifeSlots) {
        checkStashedOwned(player, chest, owned, knifeSlots, false);
    }

    private static void checkStashedOwned(EntityPlayerMP player, TileEntityChest chest, List<ItemStack> owned,
        List<Integer> knifeSlots, boolean allowCraftingKnife) {
        for (int i = 0; i < owned.size(); i++) {
            int chestSlot = knifeSlots.indexOf(i);
            if (chestSlot < 0) {
                check(
                    ItemStack.areItemStacksEqual(owned.get(i), player.inventory.getStackInSlot(i)),
                    "Owned quest ingredient was consumed or changed");
            } else {
                if (!allowCraftingKnife)
                    check(player.inventory.getStackInSlot(i) == null, "Stored knife returned to inventory");
                check(
                    ItemStack.areItemStacksEqual(owned.get(i), chest.getStackInSlot(chestSlot)),
                    "Detected knife was consumed or changed");
            }
        }
    }

    private static TileEntityChest chest(World world, int x, int y, int z) {
        check(world.getTileEntity(x, y, z) instanceof TileEntityChest, "Storage chest is unavailable");
        return (TileEntityChest) world.getTileEntity(x, y, z);
    }

    private static int count(EntityPlayerMP player, ItemStack target) {
        int amount = 0;
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (stack != null && stack.isItemEqual(target)) amount += stack.stackSize;
        }
        return amount;
    }

    private static QuestCache cache(EntityPlayerMP player) {
        QuestCache cache = (QuestCache) player.getExtendedProperties(QuestCache.LOC_QUEST_CACHE.toString());
        check(cache != null, "BetterQuesting player cache is unavailable");
        return cache;
    }

    private static IQuest quest() {
        IQuest quest = QuestDatabase.INSTANCE.get(BERRY_MEDLEY_QUEST);
        check(quest != null, "Berry Medley quest is unavailable");
        return quest;
    }

    private static EntityPlayerMP player() {
        return (EntityPlayerMP) MinecraftServer.getServer()
            .getConfigurationManager().playerEntityList.get(0);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final class Recipe {

        final int knifeSlot;
        final int plankSlot;
        final int output;

        Recipe(int knifeSlot, int plankSlot, int output) {
            this.knifeSlot = knifeSlot;
            this.plankSlot = plankSlot;
            this.output = output;
        }
    }
}
