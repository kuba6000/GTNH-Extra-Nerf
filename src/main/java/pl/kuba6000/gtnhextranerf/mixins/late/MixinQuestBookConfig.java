package pl.kuba6000.gtnhextranerf.mixins.late;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import drethic.questbook.config.QBConfig;

@Mixin(value = QBConfig.class, remap = false)
public abstract class MixinQuestBookConfig {

    @Shadow
    public static boolean syncQuestRewards;

    @Inject(method = "init", at = @At("RETURN"), require = 1, allow = 1)
    private static void gtnhextranerf$sharePartyRewardClaims(FMLPreInitializationEvent event, CallbackInfo callback) {
        syncQuestRewards = true;
    }
}
