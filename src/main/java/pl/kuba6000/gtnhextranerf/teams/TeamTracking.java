package pl.kuba6000.gtnhextranerf.teams;

import java.io.File;
import java.net.URL;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import pl.kuba6000.gtnhextranerf.Config;
import pl.kuba6000.gtnhextranerf.GTNHExtraNerf;
import pl.kuba6000.gtnhextranerf.progression.ProgressionData;
import pl.kuba6000.gtnhextranerf.telemetry.ServerTelemetry;
import pl.kuba6000.gtnhextranerf.telemetry.TelemetryClient;

/**
 * Opt-in team tracking, independent of telemetry. Every 15 minutes the server reports its permanent team ID, the team
 * leader and the players who are online, so the team can be listed as "<leader>'s team".
 */
public final class TeamTracking implements AutoCloseable {

    public static final String ENDPOINT = "https://extranerf.kuba6000.pl/teamtracking";
    static final String SITE = "https://extranerf.kuba6000.pl";
    public static final String POLICY_URL = SITE + "/team-tracking";
    static final String CONTACT = "extranerf@kuba6000.pl";
    private static final long INTERVAL = 15 * 60 * 1000L;
    private static final long FIRST_REPORT_DELAY = 10 * 60 * 1000L;
    private static volatile TeamTracking active;
    private static File modSource;
    private static boolean development;
    private static String inactiveReason = "not started";
    private final Thread owner = Thread.currentThread();
    private final Sender sender;
    private final ExecutorService executor;
    private final long intervalMillis;
    private final AtomicBoolean inFlight = new AtomicBoolean();
    private volatile boolean closed;
    private volatile String lastStatus = "waiting";
    private volatile TelemetryClient.Response lastResponse;
    private TelemetryClient.Response observedResponse;
    private MinecraftServer server;
    private boolean registered;
    private long blockedUntil = Long.MIN_VALUE; // nanoTime's origin is arbitrary and may be negative
    private long nextDue;

    @FunctionalInterface
    public interface Sender {

        TelemetryClient.Response send(Snapshot snapshot, java.util.function.BooleanSupplier cancelled) throws Exception;
    }

    public TeamTracking(MinecraftServer server, TeamClient client) {
        this(client::send, Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "ExtraNerf-Teams");
            thread.setDaemon(true);
            return thread;
        }), INTERVAL, nowMillis() + FIRST_REPORT_DELAY);
        this.server = Objects.requireNonNull(server);
    }

    public TeamTracking(Sender sender, ExecutorService executor, long intervalMillis, long firstDue) {
        if (intervalMillis < 1) throw new IllegalArgumentException("Team tracking interval must be positive");
        this.sender = Objects.requireNonNull(sender);
        this.executor = Objects.requireNonNull(executor);
        this.intervalMillis = intervalMillis;
        nextDue = firstDue;
    }

    public static void initialize(File source, boolean developmentEnvironment) {
        modSource = source;
        development = developmentEnvironment;
    }

    /** Starts reporting when the owner has opted in and the saved team is usable. */
    public static void start(MinecraftServer server) {
        stop();
        if (!Config.teamTrackingEnabled || development) {
            inactiveReason = !Config.teamTrackingEnabled ? "off" : "development environment";
            return;
        }
        try {
            if (savedTeam() == null) {
                inactiveReason = "the saved team is incomplete; run /extranerfteam on again";
                return;
            }
            TeamClient client = new TeamClient(
                modSource == null ? null : modSource.toPath(),
                new URL(ENDPOINT),
                new SecureRandom());
            TeamTracking session = new TeamTracking(server, client);
            active = session;
            FMLCommonHandler.instance()
                .bus()
                .register(session);
            session.registered = true;
            inactiveReason = "";
            GTNHExtraNerf.LOG.info("Team tracking is on for {}. Turn it off with /extranerfteam off", teamName());
        } catch (Exception failure) {
            stop();
            inactiveReason = failure.getClass()
                .getSimpleName();
            GTNHExtraNerf.LOG.warn("Team tracking could not start: {}. Gameplay remains available", inactiveReason);
        }
    }

    public static void stop() {
        TeamTracking previous = active;
        active = null;
        if (previous != null) previous.close();
        inactiveReason = "stopped";
    }

    /** Every player who joins is told that the server is listed as a team. */
    @SubscribeEvent
    public void playerJoined(PlayerEvent.PlayerLoggedInEvent event) {
        if (closed || active != this || !(event.player instanceof EntityPlayerMP)) return;
        event.player.addChatMessage(joinNotice().setChatStyle(new ChatStyle().setColor(EnumChatFormatting.YELLOW)));
    }

    public static ChatComponentTranslation joinNotice() {
        return new ChatComponentTranslation("gtnhextranerf.team.notice", SITE, POLICY_URL);
    }

    /** Shown to the operator who turns team tracking off: the stored data stays until it is requested to be deleted. */
    public static ChatComponentTranslation removalNotice(String teamId) {
        return new ChatComponentTranslation("gtnhextranerf.team.removed", CONTACT, teamId);
    }

    @SubscribeEvent
    public void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || active != this) return;
        if (!Config.teamTrackingEnabled) {
            stop();
            inactiveReason = "off";
            return;
        }
        try {
            assertOwner();
            request(nowMillis(), this::capture);
        } catch (RuntimeException failure) {
            stop();
            inactiveReason = failure.getClass()
                .getSimpleName();
            if (failure instanceof ServerTelemetry.WorldNotEligible) GTNHExtraNerf.LOG
                .info("Team tracking stopped: this world earned a certificate before it had run 5 hours");
            else GTNHExtraNerf.LOG.warn("Team tracking stopped after {}. Gameplay remains available", inactiveReason);
        }
    }

    public boolean request(long nowMillis, Supplier<Snapshot> snapshot) {
        assertOwner();
        if (!Config.teamTrackingEnabled) {
            close();
            return false;
        }
        TelemetryClient.Response response = lastResponse;
        if (response != observedResponse) {
            observedResponse = response;
            if (response != null) blockedUntil = nowMillis + response.retryAfterSeconds * 1000;
        }
        if (closed || nowMillis < blockedUntil || nowMillis < nextDue || !inFlight.compareAndSet(false, true))
            return false;
        try {
            Snapshot captured = snapshot.get();
            nextDue = nowMillis + intervalMillis;
            executor.execute(() -> {
                try {
                    if (closed) return;
                    TelemetryClient.Response delivery = sender.send(captured, () -> closed);
                    if (delivery == null) return;
                    lastStatus = "HTTP " + delivery.status;
                    lastResponse = delivery;
                } catch (Exception failure) {
                    lastStatus = failure.getClass()
                        .getSimpleName();
                    lastResponse = new TelemetryClient.Response(0, 60);
                } finally {
                    inFlight.set(false);
                }
            });
            return true;
        } catch (RuntimeException failure) {
            inFlight.set(false);
            throw failure;
        }
    }

    /** Runs on the server thread: the saved team plus the players online right now. */
    public Snapshot capture() {
        assertOwner();
        Member leader = savedTeam();
        if (leader == null) throw new IllegalStateException("Saved team is incomplete");
        List<Member> online = onlinePlayers(server);
        for (Member member : online) {
            if (member.id.equals(leader.id) && !member.name.equals(leader.name)) {
                Config.updateTeamLeaderName(member.name);
                leader = member;
                break;
            }
        }
        WorldServer overworld = DimensionManager.getWorld(0);
        if (overworld == null) throw new IllegalStateException("Team overworld unavailable");
        ProgressionData progression = (ProgressionData) overworld.mapStorage
            .loadData(ProgressionData.class, ProgressionData.NAME);
        // The same decision as telemetry: a world that earned a certificate too early is not listed either.
        if (progression != null && progression.isTelemetryBlocked()) throw new ServerTelemetry.WorldNotEligible();
        return new Snapshot(
            UUID.fromString(Config.teamId),
            leader,
            ServerTelemetry.highestAvailableTier(progression),
            !server.isDedicatedServer(),
            online);
    }

    /** The saved leader, or null when the saved team ID, leader UUID or name cannot be used. */
    static Member savedTeam() {
        try {
            UUID.fromString(Config.teamId);
            return new Member(UUID.fromString(Config.teamLeaderId), Config.teamLeaderName);
        } catch (IllegalArgumentException invalid) {
            return null;
        }
    }

    /** Players with a valid Minecraft name, capped at the report limit. */
    public static List<Member> onlinePlayers(MinecraftServer server) {
        List<Member> members = new ArrayList<>();
        for (Object entry : server.getConfigurationManager().playerEntityList) {
            if (members.size() >= Snapshot.MAXIMUM_PLAYERS) break;
            EntityPlayerMP player = (EntityPlayerMP) entry;
            UUID id = player.getGameProfile()
                .getId();
            String name = player.getCommandSenderName();
            if (id != null && Member.validName(name)) members.add(new Member(id, name));
        }
        return members;
    }

    /**
     * Lists this server as a team, named after {@code sender} or, for the console, the first player online. Shared by
     * the command and the consent window. False when there is nobody to name the team after.
     */
    public static boolean enable(MinecraftServer server, EntityPlayerMP sender) {
        Member commandSender = null;
        if (sender != null) {
            UUID id = sender.getGameProfile()
                .getId();
            if (id != null && Member.validName(sender.getCommandSenderName()))
                commandSender = new Member(id, sender.getCommandSenderName());
        }
        Member leader = chooseLeader(commandSender, onlinePlayers(server));
        if (leader == null) return false;
        // The ID is created once and then kept, also across opting out and in again.
        String teamId = isUuid(Config.teamId) ? Config.teamId
            : UUID.randomUUID()
                .toString();
        Config.enableTeamTracking(teamId, leader.id.toString(), leader.name);
        start(server);
        return true;
    }

    private static boolean isUuid(String value) {
        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException invalid) {
            return false;
        }
    }

    /** The player who typed the command, or the first online player when the console did. */
    public static Member chooseLeader(Member commandSender, List<Member> online) {
        if (commandSender != null) return commandSender;
        return online.isEmpty() ? null : online.get(0);
    }

    public static String teamName(String leaderName) {
        return leaderName + "'s team";
    }

    public static String teamName() {
        return teamName(Config.teamLeaderName);
    }

    public static IChatComponent status() {
        TeamTracking current = active;
        IChatComponent saved = Config.teamId.isEmpty() ? new ChatComponentTranslation("gtnhextranerf.team.saved.none")
            : new ChatComponentTranslation("gtnhextranerf.team.saved.team", teamName(), Config.teamId);
        if (current == null) return "off".equals(inactiveReason) || !Config.teamTrackingEnabled
            ? new ChatComponentTranslation("gtnhextranerf.team.status.off", saved)
            : new ChatComponentTranslation("gtnhextranerf.team.status.inactive", inactiveReason, saved);
        return new ChatComponentTranslation(
            "gtnhextranerf.team.status.active",
            saved,
            current.lastStatus,
            current.inFlight.get(),
            Math.max(0, (Math.max(current.nextDue, current.blockedUntil) - nowMillis()) / 1000));
    }

    private void assertOwner() {
        if (Thread.currentThread() != owner)
            throw new IllegalStateException("Team tracking requires its server thread");
    }

    private static long nowMillis() {
        return System.nanoTime() / 1000000;
    }

    @Override
    public void close() {
        closed = true;
        if (active == this) {
            active = null;
            inactiveReason = Config.teamTrackingEnabled ? "stopped" : "off";
        }
        if (registered) {
            FMLCommonHandler.instance()
                .bus()
                .unregister(this);
            registered = false;
        }
        executor.shutdownNow();
    }

    /** The team's permanent ID, leader, progression tier and the players online when the report was captured. */
    public static final class Snapshot {

        public static final int MAXIMUM_PLAYERS = 256;
        public final UUID teamId;
        public final Member leader;
        public final String highestTier;
        /** True on an integrated server, that is when the world is played in singleplayer. */
        public final boolean singleplayer;
        public final List<Member> players;

        public Snapshot(UUID teamId, Member leader, String highestTier, boolean singleplayer, List<Member> players) {
            if (players.size() > MAXIMUM_PLAYERS) throw new IllegalArgumentException("Too many team players");
            if (highestTier == null || !highestTier.matches("[a-z][a-z0-9_]{0,15}"))
                throw new IllegalArgumentException("Invalid progression tier");
            Set<UUID> seen = new HashSet<>();
            for (Member member : players)
                if (!seen.add(member.id)) throw new IllegalArgumentException("Duplicate team player");
            this.teamId = Objects.requireNonNull(teamId);
            this.leader = Objects.requireNonNull(leader);
            this.highestTier = highestTier;
            this.singleplayer = singleplayer;
            this.players = Collections.unmodifiableList(new ArrayList<>(players));
        }
    }

    public static final class Member {

        private static final Pattern NAME = Pattern.compile("[A-Za-z0-9_]{1,16}");
        public final UUID id;
        public final String name;

        public Member(UUID id, String name) {
            if (!validName(name)) throw new IllegalArgumentException("Invalid player name");
            this.id = Objects.requireNonNull(id);
            this.name = name;
        }

        public static boolean validName(String name) {
            return name != null && NAME.matcher(name)
                .matches();
        }
    }
}
