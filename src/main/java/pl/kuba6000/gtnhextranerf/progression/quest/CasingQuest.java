package pl.kuba6000.gtnhextranerf.progression.quest;

import java.util.Map;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import betterquesting.api.questing.IQuest;
import betterquesting.api.utils.BigItemStack;
import betterquesting.api2.utils.ParticipantInfo;
import bq_standard.tasks.TaskCrafting;
import gregtech.api.enums.ItemList;

/** Preserve the native crafting task while accepting its chiseled finished casing. */
public final class CasingQuest {

    private static final UUID MACHINE_HULL_QUEST = new UUID(0, 73);

    private CasingQuest() {}

    public static boolean isMachineHullQuest(UUID questId) {
        return MACHINE_HULL_QUEST.equals(questId);
    }

    public static boolean matches(TaskCrafting task, Map.Entry<UUID, IQuest> quest) {
        if (!isMachineHullQuest(quest.getKey()) || quest.getValue() == null
            || quest.getValue()
                .getTasks()
                .getValue(0) != task
            || task.requiredItems.size() != 1) return false;
        BigItemStack required = task.requiredItems.get(0);
        return required != null && required.stackSize == 1
            && !required.hasOreDict()
            && ItemList.Casing_LV.get(1)
                .isItemEqual(required.getBaseStack());
    }

    public static void acceptHeldCasing(TaskCrafting task, ParticipantInfo participants,
        Map.Entry<UUID, IQuest> quest) {
        if (participants.PLAYER.worldObj.isRemote || !matches(task, quest)) return;
        boolean missing = false;
        for (UUID user : participants.ALL_UUIDS) {
            if (!task.isComplete(user) && task.getUsersProgress(user)[0] < 1) {
                missing = true;
                break;
            }
        }
        // onItemCraft invokes detect again; completed progress prevents a recursive second submission.
        if (!missing) return;

        ItemStack casing = ItemList.Casing_LV.get(1);
        for (EntityPlayer player : participants.ACTIVE_PLAYERS) {
            for (int index = 0; index < player.inventory.getSizeInventory(); index++) {
                ItemStack stack = player.inventory.getStackInSlot(index);
                if (stack != null && stack.stackSize > 0 && casing.isItemEqual(stack)) {
                    task.onItemCraft(participants, quest, stack, null);
                    return;
                }
            }
        }
    }
}
