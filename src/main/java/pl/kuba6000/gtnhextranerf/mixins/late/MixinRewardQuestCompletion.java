package pl.kuba6000.gtnhextranerf.mixins.late;

import java.util.Map;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import betterquesting.api.api.ApiReference;
import betterquesting.api.api.QuestingAPI;
import betterquesting.api.questing.IQuest;
import betterquesting.api2.cache.QuestCache;
import betterquesting.api2.utils.ParticipantInfo;
import betterquesting.handlers.EventHandler;
import betterquesting.questing.QuestDatabase;
import bq_standard.rewards.RewardQuestCompletion;

/**
 * Hotfix until upstream BetterQuesting syncs RewardQuestCompletion across party members.
 * Injects at TAIL and skips already-completed members so it becomes a harmless no-op once upstream fixes it.
 */
@Mixin(value = RewardQuestCompletion.class, remap = false)
public abstract class MixinRewardQuestCompletion {

    @Shadow
    public UUID questNum;

    @Shadow
    public boolean isSilent;

    @Inject(method = "claimReward0", at = @At("TAIL"), require = 1, allow = 1)
    private void gtnhextranerf$completeForParty(EntityPlayer player, Map.Entry<UUID, IQuest> quest,
        CallbackInfo callback) {
        if (questNum == null) return;
        IQuest targetQuest = QuestingAPI.getAPI(ApiReference.QUEST_DB)
            .get(questNum);
        if (targetQuest == null) return;
        UUID questId = QuestDatabase.INSTANCE.lookupKey(targetQuest);
        if (questId == null) return;

        ParticipantInfo pInfo = new ParticipantInfo(player);
        long timestamp = System.currentTimeMillis();
        for (UUID uuid : pInfo.ALL_UUIDS) {
            if (targetQuest.isComplete(uuid)) continue;
            targetQuest.setComplete(uuid, timestamp);
            EntityPlayer member = uuid.equals(pInfo.UUID) ? player : QuestingAPI.getPlayer(uuid);
            if (member != null) {
                QuestCache qc = (QuestCache) member.getExtendedProperties(QuestCache.LOC_QUEST_CACHE.toString());
                if (qc != null) {
                    qc.markQuestDirty(questId);
                    qc.updateCache(member);
                }
                if (!isSilent) EventHandler.postPresetNotice(targetQuest, member, 2);
            }
        }
    }
}
