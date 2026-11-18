package pl.kuba6000.gtnhextranerf.progression.quest;

import java.util.UUID;

import betterquesting.api.questing.IQuest;
import betterquesting.questing.QuestDatabase;
import pl.kuba6000.gtnhextranerf.GTNHExtraNerf;

/**
 * The Smeltery quest ("You Are Not Prepared!!!") grants no rewards. {@code MixinQuestInstance} clears them while
 * BetterQuesting reads the definition; {@link #verify} checks the loaded questbook once the server has started, so a
 * renamed or missing quest stops the server instead of silently keeping its rewards. Loaded only with BetterQuesting.
 */
public final class SmelteryQuestRewards {

    public static final UUID QUEST = new UUID(0, 3031);

    private SmelteryQuestRewards() {}

    /** BetterQuesting loads its database while the server starts, so this runs after every start handler. */
    public static void verify() {
        String failure = failure(QuestDatabase.INSTANCE.get(QUEST));
        if (failure == null) return;
        String message = "GTNH Extra Nerf integrity check failed: Smeltery quest rewards\n- " + failure;
        if (!GTNHExtraNerf.isDevelopmentEnvironment()) throw new IllegalStateException(message);
        GTNHExtraNerf.LOG.warn("Development questbook check failed; continuing startup\n{}", message);
    }

    static String failure(IQuest quest) {
        if (quest == null) return "quest " + QUEST + " is missing from the questbook";
        int rewards = quest.getRewards()
            .size();
        return rewards == 0 ? null : "quest " + QUEST + " still has " + rewards + " reward(s)";
    }
}
