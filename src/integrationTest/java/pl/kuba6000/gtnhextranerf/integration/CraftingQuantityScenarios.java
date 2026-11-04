package pl.kuba6000.gtnhextranerf.integration;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import betterquesting.api.api.QuestingAPI;
import betterquesting.api.questing.IQuest;
import betterquesting.questing.QuestDatabase;
import bq_standard.tasks.TaskCrafting;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.util.GTOreDictUnificator;

/** Tests inventory quantities against the real Book Parts quest. */
@GameTestHolder(value = "gtnhextranerfquesttests", clientOnly = true)
public final class CraftingQuantityScenarios {

    private static final UUID BOOK_PARTS_QUEST = new UUID(0, 32);

    private CraftingQuantityScenarios() {}

    @GameTest(template = "gtnhextranerftests:native_fixture", timeoutTicks = 1200)
    public static void detectionRecountsItemsAcrossStacks(GameTestHelper helper) {
        helper.startSequence()
            .thenExecute("detect the quantity in the real Book Parts quest", () -> {
                EntityPlayerMP player = (EntityPlayerMP) MinecraftServer.getServer()
                    .getConfigurationManager().playerEntityList.get(0);
                UUID user = QuestingAPI.getQuestingUUID(player);
                IQuest quest = QuestDatabase.INSTANCE.get(BOOK_PARTS_QUEST);
                check(quest != null, "Book Parts quest missing");

                Map<UUID, NBTTagCompound> saved = new HashMap<>();
                saved.put(BOOK_PARTS_QUEST, quest.writeProgressToNBT(new NBTTagCompound(), null));
                for (UUID id : quest.getRequirements()) {
                    IQuest prerequisite = QuestDatabase.INSTANCE.get(id);
                    check(prerequisite != null, "Missing Book Parts prerequisite");
                    saved.put(id, prerequisite.writeProgressToNBT(new NBTTagCompound(), null));
                }
                ItemStack[] inventory = new ItemStack[player.inventory.getSizeInventory()];
                for (int i = 0; i < inventory.length; i++) {
                    ItemStack old = player.inventory.getStackInSlot(i);
                    inventory[i] = old == null ? null : old.copy();
                }
                helper.afterTest(() -> {
                    for (Map.Entry<UUID, NBTTagCompound> entry : saved.entrySet()) {
                        IQuest original = QuestDatabase.INSTANCE.get(entry.getKey());
                        if (original != null) original.readProgressFromNBT(entry.getValue(), false);
                    }
                    for (int i = 0; i < inventory.length; i++)
                        player.inventory.setInventorySlotContents(i, inventory[i]);
                    player.sendContainerToPlayer(player.openContainer);
                });

                for (UUID id : quest.getRequirements()) {
                    QuestDatabase.INSTANCE.get(id)
                        .setComplete(user, System.currentTimeMillis());
                }
                for (int i = 0; i < inventory.length; i++) player.inventory.setInventorySlotContents(i, null);
                quest.resetUser(user, true);
                check(quest.isUnlocked(user), "Book Parts quest is locked");
                check(
                    quest.getTasks()
                        .getValue(0) instanceof TaskCrafting,
                    "Wood-pulp task is missing");
                TaskCrafting wood = (TaskCrafting) quest.getTasks()
                    .getValue(0);

                ItemStack pulp = GTOreDictUnificator.get(OrePrefixes.dust, Materials.Wood, 1);
                check(pulp != null, "Wood pulp is unavailable");
                check(
                    wood.getItemInputs()
                        .size() == 1,
                    "Wood-pulp task no longer has one requirement");
                check(
                    "dustWood".equals(
                        wood.getItemInputs()
                            .get(0)
                            .getOreDict()),
                    "Wood-pulp requirement changed");
                int required = wood.getItemInputs()
                    .get(0).stackSize;
                check(
                    required >= 3 && required <= 2 * pulp.getMaxStackSize(),
                    "Requirement cannot be split into two stacks");
                int firstCount = required / 2;
                int secondCount = required - 1 - firstCount;
                ItemStack first = pulp.copy();
                first.stackSize = firstCount;
                ItemStack second = pulp.copy();
                second.stackSize = secondCount;
                player.inventory.setInventorySlotContents(0, first.copy());
                player.inventory.setInventorySlotContents(1, second.copy());

                for (int attempt = 0; attempt < 3; attempt++) {
                    quest.detect(player);
                    check(!wood.isComplete(user), "Repeated Detect counted the same wood pulp twice");
                    check(
                        ItemStack.areItemStacksEqual(first, player.inventory.getStackInSlot(0)),
                        "Detect changed wood pulp");
                    check(
                        ItemStack.areItemStacksEqual(second, player.inventory.getStackInSlot(1)),
                        "Detect changed wood pulp");
                }

                player.inventory.setInventorySlotContents(1, null);
                quest.detect(player);
                check(!wood.isComplete(user), "Removing a stack unexpectedly completed the task");

                player.inventory.setInventorySlotContents(1, second.copy());
                quest.detect(player);
                check(!wood.isComplete(user), "Wood pulp incorrectly satisfied the task before the threshold");

                second.stackSize++;
                player.inventory.setInventorySlotContents(1, second.copy());
                quest.detect(player);
                check(wood.isComplete(user), "Required wood pulp across two stacks did not satisfy the task");
                check(
                    !quest.getTasks()
                        .getValue(1)
                        .isComplete(user),
                    "Missing paper incorrectly satisfied its task");
                check(!quest.isComplete(user), "Quest completed without paper");
                check(
                    ItemStack.areItemStacksEqual(first, player.inventory.getStackInSlot(0)),
                    "Detect consumed wood pulp");
                check(
                    ItemStack.areItemStacksEqual(second, player.inventory.getStackInSlot(1)),
                    "Detect consumed wood pulp");
            })
            .thenSucceed();
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
