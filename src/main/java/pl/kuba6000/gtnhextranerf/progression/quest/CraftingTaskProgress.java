package pl.kuba6000.gtnhextranerf.progression.quest;

import java.util.UUID;

/** Exposes the effective progress of a crafting task to its retrieval view. */
public interface CraftingTaskProgress {

    int[] gtnhextranerf$getCombinedProgress(UUID player);
}
