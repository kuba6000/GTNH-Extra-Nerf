package pl.kuba6000.gtnhextranerf.mixins.late;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import betterquesting.api.questing.IQuest;
import betterquesting.api.questing.tasks.IItemTask;
import betterquesting.api.utils.BigItemStack;
import betterquesting.api2.utils.ParticipantInfo;
import bq_standard.tasks.ITaskInventory;
import bq_standard.tasks.TaskCrafting;
import bq_standard.tasks.base.TaskProgressableBase;
import pl.kuba6000.gtnhextranerf.progression.quest.CraftingTaskProgress;
import pl.kuba6000.gtnhextranerf.progression.quest.CraftingTaskRetrieval;

@Mixin(value = TaskCrafting.class, remap = false)
public abstract class MixinCraftingTaskRetrieval extends TaskProgressableBase<int[]>
    implements ITaskInventory, IItemTask, CraftingTaskProgress {

    @Unique
    private static final String GTNH_EXTRA_NERF_RETRIEVAL = "gtnhextranerf:retrieval";

    @Unique
    private CraftingTaskRetrieval gtnhextranerf$retrieval;

    @Unique
    private synchronized CraftingTaskRetrieval gtnhextranerf$retrieval() {
        if (gtnhextranerf$retrieval == null) {
            gtnhextranerf$retrieval = new CraftingTaskRetrieval((TaskCrafting) (Object) this, false);
        }
        return gtnhextranerf$retrieval;
    }

    @Override
    public synchronized int[] gtnhextranerf$getCombinedProgress(UUID player) {
        int[] crafted = getUsersProgress(player);
        int[] found = gtnhextranerf$retrieval().getUsersProgress(player);
        int[] combined = crafted.clone();
        for (int i = 0; i < combined.length; i++) combined[i] = Math.max(combined[i], found[i]);
        return combined;
    }

    @Unique
    private synchronized void gtnhextranerf$completeCombined(ParticipantInfo participants,
        Map.Entry<UUID, IQuest> quest) {
        List<BigItemStack> required = ((TaskCrafting) (Object) this).requiredItems;
        for (UUID player : participants.ALL_UUIDS) {
            int[] progress = gtnhextranerf$getCombinedProgress(player);
            boolean complete = true;
            for (int i = 0; i < required.size(); i++) {
                if (progress[i] < required.get(i).stackSize) {
                    complete = false;
                    break;
                }
            }
            if (complete) {
                participants.ALL_UUIDS.forEach(this::setComplete);
                participants.markDirtyParty(quest.getKey());
                return;
            }
        }
    }

    @Inject(method = "detect", at = @At("RETURN"), require = 1)
    private void gtnhextranerf$detectInventory(ParticipantInfo participants, Map.Entry<UUID, IQuest> quest,
        CallbackInfo ci) {
        if (participants.PLAYER.worldObj.isRemote) return;
        synchronized (this) {
            gtnhextranerf$retrieval().detect(participants, quest);
            gtnhextranerf$completeCombined(participants, quest);
        }
    }

    @Override
    public void onInventoryChange(Map.Entry<UUID, IQuest> quest, ParticipantInfo participants) {
        if (!participants.PLAYER.worldObj.isRemote) ((TaskCrafting) (Object) this).detect(participants, quest);
    }

    @Override
    public synchronized boolean canAcceptItem(UUID owner, Map.Entry<UUID, IQuest> quest, ItemStack stack) {
        return gtnhextranerf$retrieval().canAcceptItem(owner, quest, stack);
    }

    @Override
    public synchronized ItemStack submitItem(UUID owner, Map.Entry<UUID, IQuest> quest, ItemStack stack) {
        return gtnhextranerf$retrieval().submitItem(owner, quest, stack);
    }

    @Override
    public synchronized void retrieveItems(ParticipantInfo participants, Map.Entry<UUID, IQuest> quest,
        ItemStack[] stacks) {
        if (participants.PLAYER.worldObj.isRemote) return;
        gtnhextranerf$retrieval().retrieveItems(participants, quest, stacks);
        gtnhextranerf$completeCombined(participants, quest);
    }

    @Override
    public synchronized void resetUser(@Nullable UUID player) {
        super.resetUser(player);
        gtnhextranerf$retrieval().resetUser(player);
    }

    @Override
    public synchronized NBTTagCompound writeProgressToNBT(NBTTagCompound nbt, @Nullable List<UUID> players) {
        super.writeProgressToNBT(nbt, players);
        nbt.setTag(
            GTNH_EXTRA_NERF_RETRIEVAL,
            gtnhextranerf$retrieval().writeProgressToNBT(new NBTTagCompound(), players));
        return nbt;
    }

    @Override
    public synchronized void readProgressFromNBT(NBTTagCompound nbt, boolean merge) {
        super.readProgressFromNBT(nbt, merge);
        gtnhextranerf$retrieval().readProgressFromNBT(nbt.getCompoundTag(GTNH_EXTRA_NERF_RETRIEVAL), merge);
    }

    @Inject(method = "readFromNBT", at = @At("RETURN"), require = 1)
    private synchronized void gtnhextranerf$refreshRequirements(NBTTagCompound nbt, CallbackInfo ci) {
        if (gtnhextranerf$retrieval == null) return;
        NBTTagCompound progress = gtnhextranerf$retrieval.writeProgressToNBT(new NBTTagCompound(), null);
        CraftingTaskRetrieval updated = new CraftingTaskRetrieval((TaskCrafting) (Object) this, false);
        updated.readProgressFromNBT(progress, false);
        gtnhextranerf$retrieval = updated;
    }
}
