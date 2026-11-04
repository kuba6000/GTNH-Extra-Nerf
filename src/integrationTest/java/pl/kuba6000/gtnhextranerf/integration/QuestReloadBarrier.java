package pl.kuba6000.gtnhextranerf.integration;

import java.util.Collections;
import java.util.UUID;

import net.minecraftforge.common.MinecraftForge;

import betterquesting.api.events.DatabaseEvent;
import betterquesting.api.questing.IQuest;
import betterquesting.api.questing.tasks.ITask;
import betterquesting.network.handlers.NetQuestSync;
import betterquesting.questing.QuestDatabase;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;

/** Waits for both reload syncs by requesting a separate, unmodified quest as a network fence. */
public final class QuestReloadBarrier {

    private static final UUID SENTINEL = new UUID(0, 0);

    private volatile int expectedQuests;
    private boolean registered;
    private boolean acknowledged;
    private IQuest observedQuest;
    private ITask observedTask;

    /** Called on the server immediately before the reload command. */
    public void beforeReload() {
        expectedQuests = QuestDatabase.INSTANCE.size();
        check(expectedQuests > 0, "Questbook is empty before reload");
    }

    /** Called on the client after the server has enqueued its full config and progress syncs. */
    public void request() {
        check(expectedQuests > 0, "Questbook reload barrier was not prepared");
        observedQuest = QuestDatabase.INSTANCE.get(SENTINEL);
        check(observedQuest != null, "Questbook synchronization marker is missing");
        observedTask = observedQuest.getTasks()
            .getValue(0);
        check(observedTask != null, "Questbook synchronization marker has no task");
        acknowledged = false;
        MinecraftForge.EVENT_BUS.register(this);
        registered = true;
        NetQuestSync.requestSync(Collections.singleton(SENTINEL), true, true);
    }

    @SubscribeEvent
    public void onQuestUpdate(DatabaseEvent.Update event) {
        if (FMLCommonHandler.instance()
            .getEffectiveSide() != Side.CLIENT || event.getType() != DatabaseEvent.DBType.QUEST) return;
        IQuest current = QuestDatabase.INSTANCE.get(SENTINEL);
        if (current == null) return;
        ITask currentTask = current.getTasks()
            .getValue(0);
        if (currentTask == null) return;
        if (current != observedQuest) {
            // A full sync replaces quest instances; it is not the targeted response.
            observedQuest = current;
            observedTask = currentTask;
        } else if (currentTask != observedTask && QuestDatabase.INSTANCE.size() == expectedQuests) {
            // The requested config replaces this one task on the existing quest; progress is applied before the event.
            acknowledged = true;
            observedTask = currentTask;
        }
    }

    /** Called repeatedly on the client until the targeted response and full database are present. */
    public void await() {
        check(acknowledged, "Waiting for the questbook synchronization marker");
        check(QuestDatabase.INSTANCE.size() == expectedQuests, "Questbook is incomplete after synchronization");
        close();
    }

    public void close() {
        if (registered) {
            MinecraftForge.EVENT_BUS.unregister(this);
            registered = false;
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
