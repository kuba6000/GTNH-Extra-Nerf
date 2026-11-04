package pl.kuba6000.gtnhextranerf.mixins.late;

import java.util.Map;
import java.util.UUID;

import net.minecraft.nbt.NBTTagList;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import betterquesting.api.questing.IQuest;
import betterquesting.api.questing.tasks.ITask;
import betterquesting.api2.storage.IDatabaseNBT;
import betterquesting.api2.utils.ParticipantInfo;
import bq_standard.handlers.EventHandler;
import bq_standard.tasks.TaskCrafting;
import pl.kuba6000.gtnhextranerf.progression.quest.CasingQuest;

@Mixin(value = EventHandler.class, remap = false)
public abstract class MixinCasingQuestInventory {

    @WrapOperation(
        method = "onServerTick",
        at = @At(
            value = "INVOKE",
            target = "Lbetterquesting/api/questing/IQuest;getTasks()Lbetterquesting/api2/storage/IDatabaseNBT;"),
        require = 1,
        allow = 1)
    private IDatabaseNBT<ITask, NBTTagList, NBTTagList> gtnhextranerf$checkCasing(IQuest quest,
        Operation<IDatabaseNBT<ITask, NBTTagList, NBTTagList>> original, @Local Map.Entry<UUID, IQuest> entry,
        @Local ParticipantInfo participants) {
        IDatabaseNBT<ITask, NBTTagList, NBTTagList> tasks = original.call(quest);
        if (CasingQuest.isMachineHullQuest(entry.getKey())) {
            ITask casingTask = tasks.getValue(0);
            if (casingTask instanceof TaskCrafting) {
                CasingQuest.acceptHeldCasing((TaskCrafting) casingTask, participants, entry);
            }
        }
        return tasks;
    }
}
