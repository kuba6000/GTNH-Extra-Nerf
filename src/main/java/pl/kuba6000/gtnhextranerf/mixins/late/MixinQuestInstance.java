package pl.kuba6000.gtnhextranerf.mixins.late;

import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import betterquesting.api.questing.IQuest;
import betterquesting.api.utils.NBTConverter;
import betterquesting.questing.QuestDatabase;
import betterquesting.questing.QuestInstance;
import cpw.mods.fml.common.FMLCommonHandler;
import pl.kuba6000.gtnhextranerf.progression.quest.SmelteryQuestRewards;

@Mixin(value = QuestInstance.class, remap = false)
public abstract class MixinQuestInstance implements IQuest {

    @Inject(method = "readFromNBT", at = @At("RETURN"), require = 1, allow = 1)
    private void gtnhextranerf$removeSmelteryRewards(NBTTagCompound definition, CallbackInfo callback) {
        if (FMLCommonHandler.instance()
            .getEffectiveSide()
            .isClient()) return;

        UUID id = QuestDatabase.INSTANCE.lookupKey(this);
        // Split defaults read the definition before registering the quest
        if (id == null) id = NBTConverter.UuidValueType.QUEST.tryReadId(definition)
            .orElse(null);
        if (SmelteryQuestRewards.QUEST.equals(id)) getRewards().reset();
    }
}
