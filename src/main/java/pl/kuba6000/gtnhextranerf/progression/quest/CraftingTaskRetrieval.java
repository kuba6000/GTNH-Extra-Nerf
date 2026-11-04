package pl.kuba6000.gtnhextranerf.progression.quest;

import java.util.UUID;

import bq_standard.tasks.TaskCrafting;
import bq_standard.tasks.TaskRetrieval;

/** Keeps native retrieval progress separate from the crafting task's production counts. */
public final class CraftingTaskRetrieval extends TaskRetrieval {

    private final TaskCrafting original;
    private final boolean view;

    public CraftingTaskRetrieval(TaskCrafting original, boolean view) {
        this.original = original;
        this.view = view;
        // Keep the original stacks: BigItemStack.copy() does not preserve its OreDict ingredient.
        requiredItems.addAll(original.requiredItems);
        partialMatch = original.partialMatch;
        ignoreNBT = original.ignoreNBT;
    }

    @Override
    public boolean isComplete(UUID player) {
        return original.isComplete(player);
    }

    @Override
    public void setComplete(UUID player) {
        original.setComplete(player);
    }

    @Override
    public int[] getUsersProgress(UUID player) {
        if (view) return ((CraftingTaskProgress) (Object) original).gtnhextranerf$getCombinedProgress(player);
        return super.getUsersProgress(player);
    }
}
