package pl.kuba6000.gtnhextranerf.integration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.oredict.OreDictionary;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;
import com.mojang.authlib.GameProfile;

import betterquesting.api.questing.IQuest;
import betterquesting.api.questing.tasks.ITask;
import betterquesting.api.utils.BigItemStack;
import betterquesting.api2.storage.DBEntry;
import betterquesting.api2.utils.ParticipantInfo;
import betterquesting.questing.QuestDatabase;
import bq_standard.tasks.ITaskItemInput;

/** Exercises every crafting task loaded from the real questbook through its public detection API. */
@GameTestHolder(value = "gtnhextranerfquesttests", clientOnly = true)
public final class CraftingCatalogRetrievalScenarios {

    private CraftingCatalogRetrievalScenarios() {}

    @GameTest(template = "gtnhextranerftests:native_fixture", timeoutTicks = 1200)
    public static void everyCraftingTaskAcceptsItsItems(GameTestHelper helper) {
        helper.startSequence()
            .thenExecute("detect the actual required items of each crafting task", () -> {
                UUID playerId = UUID.randomUUID();
                FakePlayer player = FakePlayerFactory.get(helper.getWorld(), new GameProfile(playerId, "QuestProbe"));
                ParticipantInfo participant = new ParticipantInfo(player);
                int tested = 0;
                for (Map.Entry<UUID, IQuest> quest : QuestDatabase.INSTANCE.entrySet()) {
                    if (quest.getValue() == null) continue;
                    for (DBEntry<ITask> entry : quest.getValue()
                        .getTasks()
                        .getEntries()) {
                        ITask task = entry.getValue();
                        if (!"bq_standard:crafting".equals(
                            task.getFactoryID()
                                .toString()))
                            continue;
                        String description = quest.getKey() + "/" + entry.getID();
                        check(task instanceof ITaskItemInput, "Missing item inputs for " + description);
                        task.resetUser(playerId);
                        player.inventory.clearInventory(null, -1);
                        try {
                            List<ItemStack> supplied = new ArrayList<>();
                            for (BigItemStack requirement : ((ITaskItemInput) task).getItemInputs()) {
                                check(requirement != null, "Missing required item in " + description);
                                for (ItemStack stack : requirement.getCombinedStacks()) {
                                    if (stack.getItemDamage() == OreDictionary.WILDCARD_VALUE) stack.setItemDamage(0);
                                    supplied.add(stack);
                                }
                            }
                            check(
                                supplied.size() <= player.inventory.mainInventory.length,
                                "Requirements exceed a player's inventory in " + description);
                            for (int i = 0; i < supplied.size(); i++) {
                                player.inventory.setInventorySlotContents(
                                    i,
                                    supplied.get(i)
                                        .copy());
                            }
                            task.detect(participant, quest);
                            check(task.isComplete(playerId), "Required items did not satisfy " + description);
                            for (int i = 0; i < supplied.size(); i++) {
                                check(
                                    ItemStack.areItemStacksEqual(supplied.get(i), player.inventory.getStackInSlot(i)),
                                    "Detection consumed or changed an item in " + description);
                            }
                            tested++;
                        } finally {
                            task.resetUser(playerId);
                            player.inventory.clearInventory(null, -1);
                        }
                    }
                }
                check(tested > 0, "No crafting tasks were found in the loaded questbook");
                System.out.println("Crafting retrieval: verified " + tested + " loaded tasks");
            })
            .thenSucceed();
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
