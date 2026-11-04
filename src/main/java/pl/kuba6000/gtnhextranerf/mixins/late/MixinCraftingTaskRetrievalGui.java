package pl.kuba6000.gtnhextranerf.mixins.late;

import java.util.Map;
import java.util.UUID;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import betterquesting.api.questing.IQuest;
import betterquesting.api2.client.gui.misc.IGuiRect;
import betterquesting.api2.client.gui.panels.IGuiPanel;
import bq_standard.tasks.TaskCrafting;
import pl.kuba6000.gtnhextranerf.progression.quest.CraftingTaskRetrieval;

@Mixin(value = TaskCrafting.class, remap = false)
public abstract class MixinCraftingTaskRetrievalGui {

    @Inject(method = "getUnlocalisedName", at = @At("HEAD"), cancellable = true, require = 1)
    private void gtnhextranerf$retrievalName(CallbackInfoReturnable<String> cir) {
        cir.setReturnValue("gtnhextranerf.task.crafting_or_retrieval");
    }

    @Inject(method = "getTaskGui", at = @At("HEAD"), cancellable = true, require = 1)
    private void gtnhextranerf$retrievalGui(IGuiRect rect, Map.Entry<UUID, IQuest> quest,
        CallbackInfoReturnable<IGuiPanel> cir) {
        cir.setReturnValue(new CraftingTaskRetrieval((TaskCrafting) (Object) this, true).getTaskGui(rect, quest));
    }
}
