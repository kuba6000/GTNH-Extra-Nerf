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
import gregtech.api.enums.ItemList;

/** Tests actual crafting quests as non-consuming inventory retrieval tasks. */
@GameTestHolder(value = "gtnhextranerfquesttests", clientOnly = true)
public final class CraftingRetrievalScenarios {

    private static final UUID MACHINE_HULL_QUEST = new UUID(0, 73);

    private CraftingRetrievalScenarios() {}

    @GameTest(template = "gtnhextranerftests:native_fixture", timeoutTicks = 1200)
    public static void ownedHullIsDetected(GameTestHelper helper) {
        helper.startSequence()
            .thenExecute("detect an already owned hull in the real questbook", () -> {
                EntityPlayerMP player = player();
                UUID user = QuestingAPI.getQuestingUUID(player);
                IQuest quest = QuestDatabase.INSTANCE.get(MACHINE_HULL_QUEST);
                check(quest != null, "Machine-hull quest missing");
                Map<UUID, NBTTagCompound> saved = new HashMap<>();
                saved.put(MACHINE_HULL_QUEST, quest.writeProgressToNBT(new NBTTagCompound(), null));
                for (UUID id : quest.getRequirements()) {
                    IQuest prerequisite = QuestDatabase.INSTANCE.get(id);
                    check(prerequisite != null, "Missing quest prerequisite");
                    saved.put(id, prerequisite.writeProgressToNBT(new NBTTagCompound(), null));
                    prerequisite.setComplete(user, System.currentTimeMillis());
                }
                ItemStack[] inventory = new ItemStack[player.inventory.getSizeInventory()];
                for (int i = 0; i < inventory.length; i++) {
                    ItemStack old = player.inventory.getStackInSlot(i);
                    inventory[i] = old == null ? null : old.copy();
                    player.inventory.setInventorySlotContents(i, null);
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
                quest.resetUser(user, true);
                check(quest.isUnlocked(user), "Machine-hull quest is locked");
                player.inventory.setInventorySlotContents(0, ItemList.Hull_LV.get(1));
                quest.detect(player);
                check(
                    quest.getTasks()
                        .getValue(1)
                        .isComplete(user),
                    "Owned hull did not satisfy its crafting task");
                check(
                    !quest.getTasks()
                        .getValue(0)
                        .isComplete(user),
                    "Missing casing incorrectly satisfied its task");
                check(ItemList.Hull_LV.isStackEqual(player.inventory.getStackInSlot(0)), "Detection consumed the hull");
            })
            .thenSucceed();
    }

    private static EntityPlayerMP player() {
        return (EntityPlayerMP) MinecraftServer.getServer()
            .getConfigurationManager().playerEntityList.get(0);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
