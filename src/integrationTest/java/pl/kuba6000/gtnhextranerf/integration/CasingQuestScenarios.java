package pl.kuba6000.gtnhextranerf.integration;

import java.awt.Rectangle;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;
import com.gtnewhorizons.horizonqa.api.client.ClickTarget;
import com.gtnewhorizons.horizonqa.api.client.ClientTarget;
import com.gtnewhorizons.horizonqa.api.client.ClientTest;

import betterquesting.api.api.QuestingAPI;
import betterquesting.api.questing.IQuest;
import betterquesting.api2.client.gui.controls.PanelButton;
import betterquesting.api2.client.gui.misc.IGuiRect;
import betterquesting.api2.client.gui.panels.IGuiCanvas;
import betterquesting.api2.client.gui.panels.IGuiPanel;
import betterquesting.api2.utils.QuestTranslation;
import betterquesting.client.gui2.GuiQuest;
import betterquesting.questing.QuestDatabase;
import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.enums.ItemList;
import team.chisel.init.ChiselItems;
import team.chisel.inventory.ContainerChisel;
import team.chisel.inventory.InventoryChiselSelection;

/** Exercises the unchanged quest through BetterQuesting's player-facing Detect button. */
@GameTestHolder(value = "gtnhextranerfquesttests", clientOnly = true)
public final class CasingQuestScenarios {

    private static final UUID MACHINE_HULL_QUEST = new UUID(0, 73);

    private CasingQuestScenarios() {}

    @GameTest(template = "gtnhextranerftests:native_fixture", timeoutTicks = 1800)
    public static void chiseledCasingCanBeDetected(GameTestHelper helper) {
        ClientTarget detect = ClientTarget.of("quest Detect button", window -> {
            GuiQuest screen = window.screen(GuiQuest.class);
            PanelButton button = findButton(screen, QuestTranslation.translate("betterquesting.btn.detect_submit"));
            if (button == null) return null;
            IGuiRect bounds = button.getTransform();
            return new ClickTarget(
                button,
                new Rectangle(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight()),
                () -> button.isEnabled() && button.isActive());
        });

        ClientTest.scenario(helper)
            .defaultTimeoutTicks(300)
            .server("unlock the real machine-hull quest and prepare a chiseled casing", () -> {
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
                    ItemStack existing = player.inventory.getStackInSlot(index);
                    inventory[index] = existing == null ? null : existing.copy();
                    player.inventory.setInventorySlotContents(index, null);
                }
                int selectedSlot = player.inventory.currentItem;
                ItemStack cursor = player.inventory.getItemStack();
                ItemStack oldCursor = cursor == null ? null : cursor.copy();
                player.inventory.setItemStack(null);
                helper.afterTest(() -> {
                    for (Map.Entry<UUID, NBTTagCompound> entry : oldProgress.entrySet()) {
                        IQuest original = QuestDatabase.INSTANCE.get(entry.getKey());
                        if (original != null) original.readProgressFromNBT(entry.getValue(), false);
                    }
                    for (int index = 0; index < inventory.length; index++) {
                        player.inventory.setInventorySlotContents(index, inventory[index]);
                    }
                    player.inventory.currentItem = selectedSlot;
                    player.inventory.setItemStack(oldCursor);
                    player.sendContainerToPlayer(player.openContainer);
                });

                quest.resetUser(user, true);
                check(quest.isUnlocked(user), "Machine-hull quest is locked");
                checkCraftingTasks(quest);

                ItemStack tool = new ItemStack(ChiselItems.chisel);
                player.inventory.currentItem = 0;
                player.inventory.setInventorySlotContents(0, tool);
                ItemStack raw = new ItemStack(GameRegistry.findBlock("gtnhextranerf", "raw_machine_casing"), 1, 0);
                player.inventory.setInventorySlotContents(1, raw.copy());
                player.inventory.setInventorySlotContents(2, ItemList.Hull_LV.get(1));
                quest.detect(player);
                check(
                    !quest.getTasks()
                        .getValue(0)
                        .isComplete(user),
                    "Raw casing incorrectly satisfied the quest");
                check(
                    !quest.getTasks()
                        .getValue(1)
                        .isComplete(user),
                    "A held hull incorrectly satisfied crafting");

                InventoryChiselSelection selection = new InventoryChiselSelection(tool);
                ContainerChisel chisel = new ContainerChisel(player.inventory, selection);
                chisel.getSlot(InventoryChiselSelection.normalSlots)
                    .putStack(raw);
                check(
                    ItemList.Casing_LV.isStackEqual(
                        chisel.getSlot(1)
                            .getStack()),
                    "Finished LV casing unavailable");
                chisel.slotClick(1, 0, 0, player);
                ItemStack finished = player.inventory.getItemStack();
                check(finished != null && ItemList.Casing_LV.isStackEqual(finished), "Hand Chisel produced no casing");
                player.inventory.setItemStack(null);
                player.inventory.setInventorySlotContents(1, finished);
                chisel.onContainerClosed(player);
                // Run Detect before the next server tick, then reset so the GUI path is also exercised.
                quest.detect(player);
                check(
                    quest.getTasks()
                        .getValue(0)
                        .isComplete(user),
                    "Manual Detect ignored the finished LV casing");
                check(
                    !quest.getTasks()
                        .getValue(1)
                        .isComplete(user),
                    "Manual Detect also satisfied hull crafting");
                quest.getTasks()
                    .getValue(0)
                    .resetUser(user);
                player.sendContainerToPlayer(player.openContainer);
            })
            .client(
                "open the real quest",
                client -> Minecraft.getMinecraft()
                    .displayGuiScreen(new GuiQuest(null, MACHINE_HULL_QUEST)))
            .awaitScreen(GuiQuest.class)
            .click(detect)
            .awaitServer("finished casing satisfies its task without consuming items", () -> {
                EntityPlayerMP player = player();
                UUID user = QuestingAPI.getQuestingUUID(player);
                IQuest quest = quest();
                check(
                    quest.getTasks()
                        .getValue(0)
                        .isComplete(user),
                    "Detect ignored the finished LV casing");
                check(
                    !quest.getTasks()
                        .getValue(1)
                        .isComplete(user),
                    "The hull crafting task was also satisfied");
                check(
                    ItemList.Casing_LV.isStackEqual(player.inventory.getStackInSlot(1)),
                    "Detect consumed the casing");
                check(ItemList.Hull_LV.isStackEqual(player.inventory.getStackInSlot(2)), "Detect consumed the hull");
                checkCraftingTasks(quest);
            })
            .capture("casing-quest-detected")
            .client(
                "return to the game",
                client -> Minecraft.getMinecraft()
                    .displayGuiScreen(null))
            .capture("returned-to-game")
            .server("reload the native questbook", () -> {
                MinecraftServer server = MinecraftServer.getServer();
                check(
                    server.getCommandManager()
                        .executeCommand(server, "bq_admin default load") > 0,
                    "Quest reload failed");
                IQuest reloaded = quest();
                UUID user = QuestingAPI.getQuestingUUID(player());
                checkCraftingTasks(reloaded);
                check(
                    reloaded.getTasks()
                        .getValue(0)
                        .isComplete(user),
                    "Quest reload discarded casing progress");
                check(
                    !reloaded.getTasks()
                        .getValue(1)
                        .isComplete(user),
                    "Quest reload changed hull progress");
            })
            .awaitClient(
                "client remains responsive after reloading the questbook",
                client -> {
                    check(Minecraft.getMinecraft().theWorld != null, "Client world unavailable after reload");
                })
            .capture("questbook-reloaded")
            .succeed();
    }

    private static IQuest quest() {
        IQuest quest = QuestDatabase.INSTANCE.get(MACHINE_HULL_QUEST);
        check(quest != null, "Machine-hull quest missing from the loaded questbook");
        return quest;
    }

    private static void checkCraftingTasks(IQuest quest) {
        check(
            "bq_standard:crafting".equals(
                quest.getTasks()
                    .getValue(0)
                    .getFactoryID()
                    .toString()),
            "Casing task type changed");
        check(
            "bq_standard:crafting".equals(
                quest.getTasks()
                    .getValue(1)
                    .getFactoryID()
                    .toString()),
            "Hull task type changed");
    }

    private static PanelButton findButton(IGuiPanel panel, String text) {
        if (panel instanceof PanelButton button && text.equals(button.getText())) return button;
        if (panel instanceof IGuiCanvas canvas) {
            for (IGuiPanel child : canvas.getChildren()) {
                PanelButton result = findButton(child, text);
                if (result != null) return result;
            }
        }
        return null;
    }

    private static EntityPlayerMP player() {
        return (EntityPlayerMP) MinecraftServer.getServer()
            .getConfigurationManager().playerEntityList.get(0);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
