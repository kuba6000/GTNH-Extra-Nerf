package pl.kuba6000.gtnhextranerf.mixins.late;

import java.util.Map;
import java.util.UUID;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import betterquesting.api.questing.IQuest;
import betterquesting.api2.utils.ParticipantInfo;
import bq_standard.tasks.TaskCrafting;
import pl.kuba6000.gtnhextranerf.progression.quest.CasingQuest;

@Mixin(value = TaskCrafting.class, remap = false)
public abstract class MixinCasingQuestCrafting {

    @Inject(method = "detect", at = @At("HEAD"), require = 1)
    private void gtnhextranerf$acceptCasing(ParticipantInfo participants, Map.Entry<UUID, IQuest> quest,
        CallbackInfo ci) {
        CasingQuest.acceptHeldCasing((TaskCrafting) (Object) this, participants, quest);
    }
}
