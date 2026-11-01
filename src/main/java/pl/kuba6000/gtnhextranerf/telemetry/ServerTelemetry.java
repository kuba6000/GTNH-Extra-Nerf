package pl.kuba6000.gtnhextranerf.telemetry;

import java.io.File;
import java.net.URL;
import java.security.SecureRandom;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

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
import pl.kuba6000.gtnhextranerf.progression.ProgressionTier;

/** One logical-server session owns Minecraft state and scheduling; HTTP receives only immutable data. */
public final class ServerTelemetry implements AutoCloseable {

    public static final String ENDPOINT = "https://extranerf.kuba6000.pl/iamalive";
    static final String PRIVACY_URL = "https://extranerf.kuba6000.pl/privacy";
    private static final long INTERVAL = 60 * 60 * 1000L;
    private static final long FIRST_REPORT_DELAY = 10 * 60 * 1000L;
    private static volatile ServerTelemetry active;
    private static UUID serverRunId;
    private static File modSource;
    private static boolean development;
    private static String inactiveReason = "not started";
    private final Thread owner = Thread.currentThread();
    private final TelemetryClient.Sender sender;
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

    public ServerTelemetry(MinecraftServer server, TelemetryClient client) {
        this(client::send, Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "ExtraNerf-Telemetry");
            thread.setDaemon(true);
            return thread;
        }), INTERVAL, nowMillis() + FIRST_REPORT_DELAY);
        this.server = Objects.requireNonNull(server);
    }

    public ServerTelemetry(TelemetryClient.Sender sender, ExecutorService executor, long intervalMillis,
        long firstDue) {
        if (intervalMillis < 1) throw new IllegalArgumentException("Telemetry interval must be positive");
        this.sender = Objects.requireNonNull(sender);
        this.executor = Objects.requireNonNull(executor);
        this.intervalMillis = intervalMillis;
        nextDue = firstDue;
    }

    public static void initialize(File source, boolean developmentEnvironment) {
        modSource = source;
        development = developmentEnvironment;
    }

    public static void startServer(MinecraftServer server) {
        serverRunId = UUID.randomUUID();
        start(server);
    }

    public static void stopServer() {
        stop();
        serverRunId = null;
    }

    public static void start(MinecraftServer server) {
        stop();
        if (!Config.telemetryEnabled || development) {
            inactiveReason = !Config.telemetryEnabled ? "opted out" : "development environment";
            return;
        }
        try {
            TelemetryClient client = new TelemetryClient(
                modSource == null ? null : modSource.toPath(),
                new URL(ENDPOINT),
                new SecureRandom());
            ServerTelemetry session = new ServerTelemetry(server, client);
            active = session;
            FMLCommonHandler.instance()
                .bus()
                .register(session);
            session.registered = true;
            inactiveReason = "";
            GTNHExtraNerf.LOG.info(
                "Telemetry enabled. Disable with /extranerftelemetry off or telemetry.enabled=false in {}",
                Config.configurationFile()
                    .getName());
            GTNHExtraNerf.LOG.info(
                "We'd appreciate you keeping telemetry enabled. It is used only for community statistics about active servers, online players, and world progression. "
                    + "Privacy policy: "
                    + PRIVACY_URL);
        } catch (Exception failure) {
            stop();
            inactiveReason = failure.getClass()
                .getSimpleName();
            GTNHExtraNerf.LOG.warn("Telemetry could not start: {}. Gameplay remains available", inactiveReason);
        }
    }

    public static void stop() {
        ServerTelemetry previous = active;
        active = null;
        if (previous != null) previous.close();
        inactiveReason = "stopped";
    }

    /** Whoever may run /extranerftelemetry (operators, and the world owner in singleplayer) is told it is on. */
    @SubscribeEvent
    public void playerJoined(PlayerEvent.PlayerLoggedInEvent event) {
        if (closed || active != this || !(event.player instanceof EntityPlayerMP)) return;
        if (!new TelemetryCommand().canCommandSenderUseCommand(event.player)) return;
        event.player.addChatMessage(joinNotice().setChatStyle(new ChatStyle().setColor(EnumChatFormatting.YELLOW)));
    }

    public static ChatComponentTranslation joinNotice() {
        return new ChatComponentTranslation("gtnhextranerf.telemetry.notice", PRIVACY_URL);
    }

    @SubscribeEvent
    public void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || active != this) return;
        if (!Config.telemetryEnabled) {
            stop();
            inactiveReason = "opted out";
            return;
        }
        try {
            assertOwner();
            request(nowMillis(), this::capture);
        } catch (RuntimeException failure) {
            stop();
            inactiveReason = failure.getClass()
                .getSimpleName();
            if (failure instanceof WorldNotEligible)
                GTNHExtraNerf.LOG.info("Telemetry stopped: this world earned a certificate before it had run 5 hours");
            else GTNHExtraNerf.LOG.warn("Telemetry stopped after {}. Gameplay remains available", inactiveReason);
        }
    }

    public boolean request(long nowMillis, Supplier<Snapshot> snapshot) {
        assertOwner();
        if (!Config.telemetryEnabled) {
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

    public Snapshot capture() {
        assertOwner();
        WorldServer overworld = DimensionManager.getWorld(0);
        if (overworld == null) throw new IllegalStateException("Telemetry overworld unavailable");
        ProgressionData progression = (ProgressionData) overworld.mapStorage
            .loadData(ProgressionData.class, ProgressionData.NAME);
        if (progression != null && progression.isTelemetryBlocked()) throw new WorldNotEligible();
        return new Snapshot(
            Objects.requireNonNull(serverRunId, "Telemetry server run has not started"),
            server.getConfigurationManager().playerEntityList.size(),
            highestAvailableTier(progression),
            !server.isDedicatedServer());
    }

    /** The highest tier whose machines the world's certificates unlock; also used by team tracking. */
    public static String highestAvailableTier(ProgressionData progression) {
        if (progression == null || !progression.isUnlocked(ProgressionTier.STEAM)) return "stone";
        String highest = "steam";
        for (int voltage = 1;; voltage++) {
            ProgressionTier tier = ProgressionTier.forMachine(voltage, false);
            if (tier == null || tier == ProgressionTier.MAX || progression.missingForMachine(voltage, false) != null)
                return highest;
            highest = tier.id();
        }
    }

    /**
     * The world earned a certificate too early, so neither telemetry nor team tracking reports from it; see
     * {@code TelemetryEligibility}.
     */
    public static final class WorldNotEligible extends IllegalStateException {

        public WorldNotEligible() {
            super("World not eligible for reports");
        }
    }

    public static IChatComponent status() {
        ServerTelemetry current = active;
        if (current == null) return new ChatComponentTranslation(
            "gtnhextranerf.telemetry.status.inactive",
            Config.telemetryEnabled,
            inactiveReason);
        return new ChatComponentTranslation(
            "gtnhextranerf.telemetry.status.active",
            current.lastStatus,
            current.inFlight.get(),
            Math.max(0, (Math.max(current.nextDue, current.blockedUntil) - nowMillis()) / 1000));
    }

    private void assertOwner() {
        if (Thread.currentThread() != owner) throw new IllegalStateException("Telemetry requires its server thread");
    }

    private static long nowMillis() {
        return System.nanoTime() / 1000000;
    }

    @Override
    public void close() {
        closed = true;
        if (active == this) {
            active = null;
            inactiveReason = Config.telemetryEnabled ? "stopped" : "opted out";
        }
        if (registered) {
            FMLCommonHandler.instance()
                .bus()
                .unregister(this);
            registered = false;
        }
        executor.shutdownNow();
    }

    public static final class Snapshot {

        /** The UUID identifies the current server run and is never saved. */
        public final UUID sessionId;
        public final int onlinePlayers;
        public final String highestTier;
        public final boolean singleplayer;

        public Snapshot(UUID sessionId, int onlinePlayers, String highestTier, boolean singleplayer) {
            if (onlinePlayers < 0 || onlinePlayers > 1024) throw new IllegalArgumentException("Invalid player count");
            if (highestTier == null || !highestTier.matches("[a-z][a-z0-9_]{0,15}"))
                throw new IllegalArgumentException("Invalid progression tier");
            this.sessionId = Objects.requireNonNull(sessionId);
            this.onlinePlayers = onlinePlayers;
            this.highestTier = highestTier;
            this.singleplayer = singleplayer;
        }
    }
}
