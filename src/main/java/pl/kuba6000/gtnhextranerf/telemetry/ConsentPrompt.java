package pl.kuba6000.gtnhextranerf.telemetry;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentTranslation;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import pl.kuba6000.gtnhextranerf.Config;
import pl.kuba6000.gtnhextranerf.GTNHExtraNerf;
import pl.kuba6000.gtnhextranerf.teams.TeamTracking;

/**
 * Asks a world that has not answered yet whether it may send statistics: operators (and the owner of a singleplayer
 * world) are shown a window shortly after they join, and a dedicated server's console is told how to answer. No
 * statistics or team reports are sent, and nothing waits, until someone answers.
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
    private int consoleTicks;

    private ConsentPrompt(MinecraftServer server) {
        this.server = server;
    }

    /** Starts watching a world that has just been loaded; does nothing when it has already been asked. */
    public static void start(MinecraftServer server, boolean development) {
        stop();
        if (!Config.worldAvailable() || Config.telemetryAsked || (development && !Boolean.getBoolean(PREVIEW_PROPERTY)))
            return;
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
        if (mayAnswer(player)) pending.put(id, OPEN_DELAY_TICKS);
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
        if (choice == TEAM) {
            if (!TeamTracking.enable(server, player)) {
                player.addChatMessage(new ChatComponentTranslation("gtnhextranerf.team.needs_player"));
                return;
            }
            player.addChatMessage(TeamTracking.status());
        } else {
            Config.setTelemetryEnabled(choice == MINIMAL);
            TeamTracking.stop();
            ServerTelemetry.start(server);
            player.addChatMessage(ServerTelemetry.status());
        }
        GTNHExtraNerf.LOG.info(
            "{} answered the telemetry question: {}",
            player.getCommandSenderName(),
            choice == DECLINE ? "disable reporting" : choice == MINIMAL ? "basic statistics" : "team tracking");
        stop();
    }

    private static void logQuestion() {
        GTNHExtraNerf.LOG.info("GTNH Extra Nerf asks how this world may contribute to the Extra Nerf website.");
        GTNHExtraNerf.LOG.info(
            "Basic statistics contribute to anonymous, aggregated statistics on the Extra Nerf website. Reports include a random ID for this server run, the number of players online, the highest progression tier, whether this is singleplayer and an optional randomized build proof. "
                + "They never include player names, UUIDs, chat or coordinates. Privacy policy: "
                + ServerTelemetry.PRIVACY_URL);
        GTNHExtraNerf.LOG.info(
            "No statistics or team reports are sent until you answer. Type one of these here, or run it as an operator in game:");
        GTNHExtraNerf.LOG.info("  extranerftelemetry on    send basic statistics");
        GTNHExtraNerf.LOG.info("  extranerftelemetry off   disable telemetry");
        GTNHExtraNerf.LOG.info(
            "An operator who joins can instead select team tracking, which lets the team appear publicly on the website. "
                + "It sends a team ID, the leader's and online players' names and UUIDs, plus basic statistics. "
                + "Team and basic reports are mutually exclusive.");
    }

    private static void logReminder() {
        GTNHExtraNerf.LOG.info(
            "No answer about telemetry yet. The server carries on without statistics or team reports; you will be asked again at the next start. "
                + "Answer any time with extranerftelemetry on or extranerftelemetry off.");
    }
}
