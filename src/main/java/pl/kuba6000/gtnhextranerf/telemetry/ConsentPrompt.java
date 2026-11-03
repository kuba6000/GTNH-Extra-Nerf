package pl.kuba6000.gtnhextranerf.telemetry;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import pl.kuba6000.gtnhextranerf.Config;
import pl.kuba6000.gtnhextranerf.GTNHExtraNerf;
import pl.kuba6000.gtnhextranerf.teams.TeamTracking;

/**
 * Asks a world that has not answered yet whether it may send statistics: operators (and the owner of a singleplayer
 * world) are shown a window shortly after they join, and a dedicated server's console is told how to answer. Nothing
 * is sent, and nothing waits, until someone answers.
 */
public final class ConsentPrompt {

    public static final int DECLINE = 0;
    public static final int MINIMAL = 1;
    public static final int TEAM = 2;
    /** Property that shows the question in a development environment, where nothing is ever sent. */
    public static final String PREVIEW_PROPERTY = "gtnhextranerf.consentPreview";
    /** The client needs a moment after joining before it can open a window. */
    static final int OPEN_DELAY_TICKS = 60;
    /**
     * The console is told how to answer a while after the server has started: other mods keep logging for a few
     * seconds, and the question must not scroll away.
     */
    static final int CONSOLE_QUESTION_TICKS = 15 * 20;
    /** The console is reminded, once, that the server carries on without an answer. */
    static final int CONSOLE_REMINDER_TICKS = CONSOLE_QUESTION_TICKS + 2 * 60 * 20;

    private static ConsentPrompt active;

    private final MinecraftServer server;
    private final Map<UUID, Integer> pending = new LinkedHashMap<>();
    private final Set<UUID> shown = new HashSet<>();
    private int consoleTicks;

    private ConsentPrompt(MinecraftServer server) {
        this.server = server;
    }

    /** Starts watching a world that has just been loaded; does nothing when it has already been asked. */
    public static void start(MinecraftServer server, boolean development) {
        stop();
        if (Config.telemetryAsked || (development && !Boolean.getBoolean(PREVIEW_PROPERTY))) return;
        ConsentPrompt prompt = new ConsentPrompt(server);
        active = prompt;
        FMLCommonHandler.instance()
            .bus()
            .register(prompt);
    }

    public static void stop() {
        ConsentPrompt previous = active;
        active = null;
        if (previous != null) FMLCommonHandler.instance()
            .bus()
            .unregister(previous);
    }

    /** The same rule as for /extranerftelemetry: operators, and the owner of a singleplayer world. */
    static boolean mayAnswer(EntityPlayerMP player) {
        return new TelemetryCommand().canCommandSenderUseCommand(player);
    }

    @SubscribeEvent
    public void playerJoined(PlayerEvent.PlayerLoggedInEvent event) {
        if (active != this || Config.telemetryAsked || !(event.player instanceof EntityPlayerMP)) return;
        EntityPlayerMP player = (EntityPlayerMP) event.player;
        UUID id = player.getUniqueID();
        if (!shown.contains(id) && mayAnswer(player)) pending.put(id, OPEN_DELAY_TICKS);
    }

    @SubscribeEvent
    public void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || active != this) return;
        if (Config.telemetryAsked) {
            stop();
            return;
        }
        for (Iterator<Map.Entry<UUID, Integer>> entries = pending.entrySet()
            .iterator(); entries.hasNext();) {
            Map.Entry<UUID, Integer> entry = entries.next();
            int remaining = entry.getValue() - 1;
            if (remaining > 0) {
                entry.setValue(remaining);
                continue;
            }
            entries.remove();
            EntityPlayerMP player = online(entry.getKey());
            // A player who is busy with another window is not interrupted; they are asked after the next login.
            if (player == null || player.openContainer != player.inventoryContainer) continue;
            shown.add(entry.getKey());
            ConsentGui.factory()
                .open(player);
        }
        if (server.isDedicatedServer()) {
            consoleTicks++;
            if (consoleTicks == CONSOLE_QUESTION_TICKS) logQuestion();
            else if (consoleTicks == CONSOLE_REMINDER_TICKS) logReminder();
        }
    }

    private EntityPlayerMP online(UUID id) {
        for (Object entry : server.getConfigurationManager().playerEntityList) {
            EntityPlayerMP player = (EntityPlayerMP) entry;
            if (id.equals(player.getUniqueID())) return player;
        }
        return null;
    }

    /** Applies the choice made in the window. Runs on the server thread; a late or unauthorized click does nothing. */
    public static void answer(MinecraftServer server, EntityPlayerMP player, int choice) {
        if (Config.telemetryAsked || choice < DECLINE || choice > TEAM || !mayAnswer(player)) return;
        Config.setTelemetryEnabled(choice != DECLINE);
        ServerTelemetry.start(server);
        player.addChatMessage(ServerTelemetry.status());
        if (choice == TEAM) {
            if (TeamTracking.enable(server, player)) player.addChatMessage(TeamTracking.status());
        }
        GTNHExtraNerf.LOG.info(
            "{} answered the telemetry question: {}",
            player.getCommandSenderName(),
            choice == DECLINE ? "disable telemetry" : choice == MINIMAL ? "minimal statistics" : "full team tracking");
        stop();
    }

    private static void logQuestion() {
        GTNHExtraNerf.LOG.info("GTNH Extra Nerf asks whether this world may send anonymous statistics.");
        GTNHExtraNerf.LOG.info(
            "Minimal statistics are a random ID for this server run, the number of players online, the highest progression tier and whether this is singleplayer. "
                + "They never include player names, UUIDs, chat or coordinates, and are used only to show community statistics on the Extra Nerf website. Privacy policy: "
                + ServerTelemetry.PRIVACY_URL);
        GTNHExtraNerf.LOG
            .info("Nothing is sent until you answer. Type one of these here, or run it as an operator in game:");
        GTNHExtraNerf.LOG.info("  extranerftelemetry on    send minimal statistics");
        GTNHExtraNerf.LOG.info("  extranerftelemetry off   disable telemetry");
        GTNHExtraNerf.LOG.info(
            "An operator who joins is also shown a window with the same choices and full team tracking, "
                + "which lists the team and its players publicly.");
    }

    private static void logReminder() {
        GTNHExtraNerf.LOG.info(
            "No answer about telemetry yet. The server carries on and sends nothing; you will be asked again at the next start. "
                + "Answer any time with extranerftelemetry on or extranerftelemetry off.");
    }
}
