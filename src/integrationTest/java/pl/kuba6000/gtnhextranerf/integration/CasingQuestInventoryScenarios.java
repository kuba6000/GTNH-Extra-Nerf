package pl.kuba6000.gtnhextranerf.integration;

import java.awt.Rectangle;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldSettings;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;
import com.gtnewhorizons.horizonqa.api.client.ClickTarget;
import com.gtnewhorizons.horizonqa.api.client.ClientTarget;
import com.gtnewhorizons.horizonqa.api.client.ClientTest;

import betterquesting.api.api.QuestingAPI;
import betterquesting.api.questing.IQuest;
import betterquesting.api2.cache.QuestCache;
import betterquesting.questing.QuestDatabase;
import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.enums.ItemList;
import team.chisel.client.gui.GuiChisel;
import team.chisel.init.ChiselItems;
import team.chisel.inventory.InventoryChiselSelection;

/** Follows the real hand-Chisel UI without pressing the quest's Detect button. */
@GameTestHolder(value = "gtnhextranerfquesttests", clientOnly = true)
public final class CasingQuestInventoryScenarios {

    private static final UUID MACHINE_HULL_QUEST = new UUID(0, 73);

    private CasingQuestInventoryScenarios() {}

    @GameTest(template = "gtnhextranerftests:native_fixture", timeoutTicks = 1800)
    public static void chiseledCasingIsDetectedAutomatically(GameTestHelper helper) {
        ClientTarget rawInput = chiselSlot(
            "raw casing in inventory",
            false,
            CasingQuestInventoryScenarios::isRawCasing);
        ClientTarget finishedOutput = chiselSlot("finished LV casing", true, ItemList.Casing_LV::isStackEqual);

        ClientTest.scenario(helper)
            .defaultTimeoutTicks(400)
            .server("prepare the actual quest and uncarved casing", () -> {
                EntityPlayerMP player = player();
                UUID user = QuestingAPI.getQuestingUUID(player);
                IQuest quest = quest();
                Map<UUID, NBTTagCompound> oldProgress = new HashMap<>();
                oldProgress.put(MACHINE_HULL_QUEST, quest.writeProgressToNBT(new NBTTagCompound(), null));
                for (UUID requirement : quest.getRequirements()) {
                    IQuest prerequisite = QuestDatabase.INSTANCE.get(requirement);
                    check(prerequisite != null, "Missing machine-hull quest prerequisite");
                    oldProgress.put(requirement, prerequisite.writeProgressToNBT(new NBTTagCompound(), null));
                    prerequisite.setComplete(user, System.currentTimeMillis());
                }
                ItemStack[] inventory = new ItemStack[player.inventory.getSizeInventory()];
                for (int index = 0; index < inventory.length; index++) {
                    ItemStack stack = player.inventory.getStackInSlot(index);
                    inventory[index] = stack == null ? null : stack.copy();
                    player.inventory.setInventorySlotContents(index, null);
                }
                ItemStack cursor = player.inventory.getItemStack();
                ItemStack oldCursor = cursor == null ? null : cursor.copy();
                player.inventory.setItemStack(null);
                int selectedSlot = player.inventory.currentItem;
                WorldSettings.GameType oldMode = player.theItemInWorldManager.getGameType();
                helper.afterTest(() -> {
                    player.closeScreen();
                    for (Map.Entry<UUID, NBTTagCompound> entry : oldProgress.entrySet()) {
                        IQuest original = QuestDatabase.INSTANCE.get(entry.getKey());
                        if (original != null) original.readProgressFromNBT(entry.getValue(), false);
                    }
                    for (int index = 0; index < inventory.length; index++) {
                        player.inventory.setInventorySlotContents(index, inventory[index]);
                    }
                    player.inventory.currentItem = selectedSlot;
                    player.inventory.setItemStack(oldCursor);
                    player.theItemInWorldManager.setGameType(oldMode);
                    player.sendContainerToPlayer(player.openContainer);
                });

                quest.resetUser(user, true);
                check(quest.isUnlocked(user), "Machine-hull quest is locked");
                player.theItemInWorldManager.setGameType(WorldSettings.GameType.SURVIVAL);
                player.inventory.currentItem = 0;
                player.inventory.setInventorySlotContents(0, new ItemStack(ChiselItems.chisel));
                player.inventory.setInventorySlotContents(1, rawCasing());
                player.inventory.setInventorySlotContents(2, ItemList.Hull_LV.get(1));
                player.inventory.setInventorySlotContents(3, ItemList.Casing_MV.get(1));
                QuestCache cache = (QuestCache) player.getExtendedProperties(QuestCache.LOC_QUEST_CACHE.toString());
                check(cache != null, "Missing BetterQuesting player cache");
                cache.updateCache(player);
                check(
                    cache.getActiveQuests()
                        .contains(MACHINE_HULL_QUEST),
                    "Machine-hull quest is not active");
                quest.detect(player);
                check(
                    !quest.getTasks()
                        .getValue(0)
                        .isComplete(user),
                    "Raw or wrong-tier casing satisfied the task");
                check(
                    !quest.getTasks()
                        .getValue(1)
                        .isComplete(user),
                    "Held hull satisfied a crafting task");
                player.sendContainerToPlayer(player.openContainer);
            })
            .awaitClient("player inventory has reached the client", client -> {
                Minecraft mc = Minecraft.getMinecraft();
                check(mc.currentScreen == null, "Another screen is open");
                ItemStack tool = mc.thePlayer.inventory.getStackInSlot(0);
                check(tool != null && tool.getItem() == ChiselItems.chisel, "Chisel is not synchronized");
                check(isRawCasing(mc.thePlayer.inventory.getStackInSlot(1)), "Raw casing is not synchronized");
            })
            .client("equip the Chisel and aim at the sky", client -> {
                Minecraft mc = Minecraft.getMinecraft();
                mc.thePlayer.inventory.currentItem = 0;
                mc.thePlayer.rotationPitch = -90;
            })
            .useHeldItem()
            .awaitScreen(GuiChisel.class)
            .shiftClick(rawInput)
            .shiftClick(finishedOutput)
            .escape()
            .awaitServer("finished casing automatically satisfies only the casing task", () -> {
                EntityPlayerMP player = player();
                UUID user = QuestingAPI.getQuestingUUID(player);
                IQuest quest = quest();
                check(
                    quest.getTasks()
                        .getValue(0)
                        .isComplete(user),
                    "Inventory change ignored the chiseled casing");
                check(
                    !quest.getTasks()
                        .getValue(1)
                        .isComplete(user),
                    "Held hull incorrectly satisfied crafting");
                check(count(player, ItemList.Casing_LV.get(1)) == 1, "Finished casing was consumed or duplicated");
                check(count(player, ItemList.Hull_LV.get(1)) == 1, "Held hull was consumed or duplicated");
            })
            .capture("automatic-casing-detection")
            .server("reload questbook and move the casing again", () -> {
                EntityPlayerMP player = player();
                UUID user = QuestingAPI.getQuestingUUID(player);
                MinecraftServer server = MinecraftServer.getServer();
                check(
                    server.getCommandManager()
                        .executeCommand(server, "bq_admin default load") > 0,
                    "Quest reload failed");
                IQuest reloaded = quest();
                check(
                    reloaded.getTasks()
                        .getValue(0)
                        .isComplete(user),
                    "Quest reload discarded completed progress");
                reloaded.resetUser(user, true);
                for (int index = 0; index < player.inventory.getSizeInventory(); index++) {
                    if (ItemList.Casing_LV.isStackEqual(player.inventory.getStackInSlot(index))) {
                        player.inventory.setInventorySlotContents(index, null);
                    }
                }
                player.inventory.setInventorySlotContents(4, ItemList.Casing_LV.get(1));
                QuestCache cache = (QuestCache) player.getExtendedProperties(QuestCache.LOC_QUEST_CACHE.toString());
                cache.updateCache(player);
                check(
                    !reloaded.getTasks()
                        .getValue(0)
                        .isComplete(user),
                    "Reset did not clear the casing task");
                player.sendContainerToPlayer(player.openContainer);
            })
            .awaitServer("automatic detection still works after reload", () -> {
                EntityPlayerMP player = player();
                UUID user = QuestingAPI.getQuestingUUID(player);
                check(
                    quest().getTasks()
                        .getValue(0)
                        .isComplete(user),
                    "Inventory detection stopped after reload");
                check(
                    !quest().getTasks()
                        .getValue(1)
                        .isComplete(user),
                    "Reload changed the hull task");
                check(count(player, ItemList.Casing_LV.get(1)) == 1, "Reloaded detection changed the casing count");
            })
            .awaitClient(
                "client remains responsive after questbook reload",
                client -> {
                    check(Minecraft.getMinecraft().theWorld != null, "Client world unavailable after reload");
                })
            .capture("automatic-detection-after-reload")
            .succeed();
    }

    private static ClientTarget chiselSlot(String name, boolean selection, Predicate<ItemStack> wanted) {
        return ClientTarget.of(name, window -> {
            GuiChisel gui = window.screen(GuiChisel.class);
            for (Object element : gui.inventorySlots.inventorySlots) {
                Slot slot = (Slot) element;
                boolean expectedInventory = selection ? slot.inventory instanceof InventoryChiselSelection
                    : slot.inventory == Minecraft.getMinecraft().thePlayer.inventory;
                if (!expectedInventory || !wanted.test(slot.getStack())) continue;
                // BogoSorter can defer ordinary-slot eligibility until it receives the shortcut input.
                return new ClickTarget(
                    slot,
                    new Rectangle(
                        (gui.width - GuiChisel.GUI_WIDTH) / 2 + slot.xDisplayPosition,
                        (gui.height - GuiChisel.GUI_HEIGHT) / 2 + slot.yDisplayPosition,
                        16,
                        16),
                    () -> slot.getHasStack() && (!selection || slot.canTakeStack(Minecraft.getMinecraft().thePlayer)));
            }
            return null;
        });
    }

    private static ItemStack rawCasing() {
        Block block = GameRegistry.findBlock("gtnhextranerf", "raw_machine_casing");
        check(block != null, "Raw machine casing is missing");
        return new ItemStack(block, 1, 0);
    }

    private static boolean isRawCasing(ItemStack stack) {
        Block block = GameRegistry.findBlock("gtnhextranerf", "raw_machine_casing");
        return stack != null && block != null
            && stack.getItem() == Item.getItemFromBlock(block)
            && stack.getItemDamage() == 0;
    }

    private static int count(EntityPlayerMP player, ItemStack wanted) {
        int result = 0;
        for (int index = 0; index < player.inventory.getSizeInventory(); index++) {
            ItemStack stack = player.inventory.getStackInSlot(index);
            if (stack != null && stack.isItemEqual(wanted)) result += stack.stackSize;
        }
        return result;
    }

    private static IQuest quest() {
        IQuest quest = QuestDatabase.INSTANCE.get(MACHINE_HULL_QUEST);
        check(quest != null, "Machine-hull quest missing from the loaded questbook");
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
