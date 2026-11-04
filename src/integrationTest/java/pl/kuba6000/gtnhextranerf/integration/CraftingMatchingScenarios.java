package pl.kuba6000.gtnhextranerf.integration;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;
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
import bq_standard.tasks.TaskCrafting;

/** Exercises OreDict and NBT matching against tasks from the loaded questbook. */
@GameTestHolder(value = "gtnhextranerfquesttests", clientOnly = true)
public final class CraftingMatchingScenarios {

    private static final UUID MAGIC_ARMOR_QUEST = new UUID(0, 1839);

    private CraftingMatchingScenarios() {}

    @GameTest(template = "gtnhextranerftests:native_fixture", timeoutTicks = 1200)
    public static void alternateOreDictionaryItemIsDetected(GameTestHelper helper) {
        helper.startSequence()
            .thenExecute("detect an alternative item from a quest's OreDict group", () -> {
                UUID playerId = UUID.randomUUID();
                FakePlayer player = FakePlayerFactory
                    .get(helper.getWorld(), new GameProfile(playerId, "QuestOreProbe"));
                ParticipantInfo participant = new ParticipantInfo(player);
                TaskCrafting chosen = null;
                Map.Entry<UUID, IQuest> context = null;
                ItemStack alternative = null;
                search: for (Map.Entry<UUID, IQuest> quest : QuestDatabase.INSTANCE.entrySet()) {
                    if (quest.getValue() == null) continue;
                    for (DBEntry<ITask> entry : quest.getValue()
                        .getTasks()
                        .getEntries()) {
                        if (!(entry.getValue() instanceof TaskCrafting)) continue;
                        TaskCrafting task = (TaskCrafting) entry.getValue();
                        if (task.requiredItems.size() != 1) continue;
                        BigItemStack requirement = task.requiredItems.get(0);
                        if (!task.ignoreNBT && requirement.getBaseStack()
                            .hasTagCompound()) continue;
                        if (!requirement.hasOreDict() || requirement.stackSize <= 0) continue;
                        for (ItemStack candidate : OreDictionary.getOres(requirement.getOreDict())) {
                            if (candidate == null || candidate.getItem() == null) continue;
                            ItemStack stack = candidate.copy();
                            if (stack.getItemDamage() == OreDictionary.WILDCARD_VALUE) stack.setItemDamage(0);
                            if (requirement.getBaseStack()
                                .isItemEqual(stack) || requirement.stackSize > stack.getMaxStackSize()) continue;
                            stack.stackSize = requirement.stackSize;
                            chosen = task;
                            context = quest;
                            alternative = stack;
                            break search;
                        }
                    }
                }
                check(chosen != null, "No real crafting task has a distinct OreDict alternative");
                chosen.resetUser(playerId);
                try {
                    player.inventory.clearInventory(null, -1);
                    player.inventory.setInventorySlotContents(0, alternative.copy());
                    chosen.detect(participant, context);
                    check(chosen.isComplete(playerId), "Alternative OreDict item did not satisfy the task");
                    check(
                        ItemStack.areItemStacksEqual(alternative, player.inventory.getStackInSlot(0)),
                        "Detect consumed the item");
                } finally {
                    chosen.resetUser(playerId);
                    player.inventory.clearInventory(null, -1);
                }
            })
            .thenSucceed();
    }

    @GameTest(template = "gtnhextranerftests:native_fixture", timeoutTicks = 1200)
    public static void armorPiecesRequireMatchingEnchantments(GameTestHelper helper) {
        helper.startSequence()
            .thenExecute("detect the required enchanted armor pieces", () -> {
                UUID playerId = UUID.randomUUID();
                FakePlayer player = FakePlayerFactory
                    .get(helper.getWorld(), new GameProfile(playerId, "QuestNbtProbe"));
                ParticipantInfo participant = new ParticipantInfo(player);
                IQuest quest = QuestDatabase.INSTANCE.get(MAGIC_ARMOR_QUEST);
                check(quest != null, "Magic armor quest is missing");
                ITask rawTask = quest.getTasks()
                    .getValue(1);
                check(rawTask instanceof TaskCrafting, "Magic armor crafting task is missing");
                TaskCrafting task = (TaskCrafting) rawTask;
                Map.Entry<UUID, IQuest> context = new AbstractMap.SimpleImmutableEntry<>(MAGIC_ARMOR_QUEST, quest);
                List<BigItemStack> requirements = task.getItemInputs();
                check(requirements.size() >= 2, "Magic armor task needs at least two requirements");
                List<ItemStack> armor = new ArrayList<>();
                int enchantedSlot = -1;
                for (BigItemStack requirement : requirements) {
                    ItemStack base = requirement.getBaseStack();
                    if (enchantedSlot < 0 && base.hasTagCompound()
                        && base.getTagCompound()
                            .getTagList("ench", Constants.NBT.TAG_COMPOUND)
                            .tagCount() > 0) {
                        enchantedSlot = armor.size();
                    }
                    armor.addAll(requirement.getCombinedStacks());
                }
                check(enchantedSlot >= 0, "Magic armor task has no required enchantment");
                check(armor.size() <= player.inventory.mainInventory.length, "Armor does not fit in the inventory");
                task.resetUser(playerId);
                try {
                    player.inventory.clearInventory(null, -1);
                    for (int i = 0; i < armor.size(); i++) player.inventory.setInventorySlotContents(
                        i,
                        armor.get(i)
                            .copy());
                    ItemStack correct = armor.get(enchantedSlot)
                        .copy();
                    ItemStack unenchanted = correct.copy();
                    unenchanted.setTagCompound(null);
                    player.inventory.setInventorySlotContents(enchantedSlot, unenchanted.copy());
                    task.detect(participant, context);
                    check(!task.isComplete(playerId), "Unenchanted armor satisfied the task");
                    checkInventory(player, armor, enchantedSlot, unenchanted);

                    ItemStack wrongLevel = correct.copy();
                    NBTTagList enchantments = wrongLevel.getTagCompound()
                        .getTagList("ench", Constants.NBT.TAG_COMPOUND);
                    NBTTagCompound enchantment = enchantments.getCompoundTagAt(0);
                    enchantment.setShort("lvl", (short) (enchantment.getShort("lvl") + 1));
                    player.inventory.setInventorySlotContents(enchantedSlot, wrongLevel.copy());
                    task.detect(participant, context);
                    check(!task.isComplete(playerId), "Armor with the wrong enchantment level satisfied the task");
                    checkInventory(player, armor, enchantedSlot, wrongLevel);

                    ItemStack extraData = correct.copy();
                    extraData.getTagCompound()
                        .setString("hqa_extra_data", "retained");
                    player.inventory.setInventorySlotContents(enchantedSlot, extraData.copy());
                    task.detect(participant, context);
                    check(task.isComplete(playerId), "Correct enchantments with extra NBT did not satisfy the task");
                    checkInventory(player, armor, enchantedSlot, extraData);
                } finally {
                    task.resetUser(playerId);
                    player.inventory.clearInventory(null, -1);
                }
            })
            .thenSucceed();
    }

    private static void checkInventory(FakePlayer player, List<ItemStack> armor, int alteredSlot, ItemStack altered) {
        for (int i = 0; i < armor.size(); i++) {
            ItemStack expected = i == alteredSlot ? altered : armor.get(i);
            check(ItemStack.areItemStacksEqual(expected, player.inventory.getStackInSlot(i)), "Detect consumed armor");
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
