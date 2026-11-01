package pl.kuba6000.gtnhextranerf.update;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import pl.kuba6000.gtnhextranerf.GTNHExtraNerf;
import pl.kuba6000.gtnhextranerf.net.SecureEndpoint;
import pl.kuba6000.gtnhextranerf.telemetry.TelemetryCommand;

/**
 * Looks for a newer release at startup and every twelve hours, independently of telemetry. The worker only reads one
 * small public file; the result is announced in the console and to operators when they join.
 */
public final class UpdateChecker implements AutoCloseable {

    public static final String ENDPOINT = "https://extranerf.kuba6000.pl/version.json";
    public static final String DOWNLOAD_URL = "https://extranerf.kuba6000.pl/download";
    private static final long INTERVAL_SECONDS = TimeUnit.HOURS.toSeconds(12);
    private static final int MAXIMUM_RESPONSE_BYTES = 1024;
    private static volatile UpdateChecker active;

    private final String currentVersion;
    private final Fetcher fetcher;
    private final ScheduledExecutorService worker;
    private volatile String newerVersion;
    private volatile boolean closed;
    private String loggedVersion; // worker thread only
    private boolean registered;

    @FunctionalInterface
    public interface Fetcher {

        /** Returns the latest published release tag, or null when the answer is unusable. */
        String fetch() throws IOException;
    }

    UpdateChecker(String currentVersion, Fetcher fetcher, ScheduledExecutorService worker, long initialDelaySeconds,
        long intervalSeconds) {
        this.currentVersion = Objects.requireNonNull(currentVersion);
        this.fetcher = Objects.requireNonNull(fetcher);
        this.worker = Objects.requireNonNull(worker);
        worker.scheduleWithFixedDelay(this::check, initialDelaySeconds, intervalSeconds, TimeUnit.SECONDS);
    }

    /** Starts for a logical server. Development builds and prereleases have no comparable tag and stay silent. */
    public static void start(String currentVersion) {
        stop();
        if (!ReleaseVersion.isRelease(currentVersion)) return;
        try {
            Fetcher fetcher = httpFetcher(new URL(ENDPOINT));
            ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor(task -> {
                Thread thread = new Thread(task, "ExtraNerf-UpdateCheck");
                thread.setDaemon(true);
                return thread;
            });
            UpdateChecker checker = new UpdateChecker(currentVersion, fetcher, worker, 0, INTERVAL_SECONDS);
            FMLCommonHandler.instance()
                .bus()
                .register(checker);
            checker.registered = true;
            active = checker;
        } catch (Exception failure) {
            GTNHExtraNerf.LOG.debug(
                "Update check could not start: {}",
                failure.getClass()
                    .getSimpleName());
        }
    }

    public static void stop() {
        UpdateChecker previous = active;
        active = null;
        if (previous != null) previous.close();
    }

    static Fetcher httpFetcher(URL endpoint) {
        SecureEndpoint.require(endpoint);
        return () -> {
            HttpURLConnection connection = (HttpURLConnection) endpoint.openConnection();
            try {
                connection.setInstanceFollowRedirects(false);
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                connection.setUseCaches(false);
                connection.setAllowUserInteraction(false);
                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", "GTNH-Extra-Nerf");
                connection.setRequestProperty("Accept", "application/json");
                if (connection.getResponseCode() != 200) return null;
                try (InputStream input = connection.getInputStream()) {
                    ByteArrayOutputStream received = new ByteArrayOutputStream();
                    byte[] buffer = new byte[256];
                    int count;
                    while ((count = input.read(buffer)) != -1) {
                        received.write(buffer, 0, count);
                        if (received.size() > MAXIMUM_RESPONSE_BYTES) return null;
                    }
                    return parseResponse(new String(received.toByteArray(), StandardCharsets.UTF_8));
                }
            } finally {
                connection.disconnect();
            }
        };
    }

    /** The file is {"version":"<release tag>"}; anything else is ignored. */
    static String parseResponse(String body) {
        try {
            JsonElement root = new JsonParser().parse(body);
            if (!root.isJsonObject()) return null;
            JsonObject object = root.getAsJsonObject();
            JsonElement version = object.get("version");
            if (version == null || !version.isJsonPrimitive()
                || !version.getAsJsonPrimitive()
                    .isString())
                return null;
            String tag = version.getAsString();
            return ReleaseVersion.isRelease(tag) ? tag : null;
        } catch (RuntimeException invalid) {
            return null;
        }
    }

    /** Runs on the worker. A failed or unusable check stays silent: updating is a convenience, not a requirement. */
    void check() {
        if (closed) return;
        String latest;
        try {
            latest = fetcher.fetch();
        } catch (IOException | RuntimeException failure) {
            return;
        }
        if (latest == null) return; // An unusable answer says nothing about the release; keep what is known.
        if (!ReleaseVersion.isNewer(currentVersion, latest)) {
            newerVersion = null;
            return;
        }
        newerVersion = latest;
        if (!latest.equals(loggedVersion)) {
            loggedVersion = latest;
            GTNHExtraNerf.LOG.warn(message(latest, currentVersion));
        }
    }

    /** The release tag that is newer than the running mod, or null when the mod is up to date or unknown. */
    public String newerVersion() {
        return newerVersion;
    }

    public static ChatComponentTranslation message(String latest, String current) {
        return new ChatComponentTranslation("gtnhextranerf.update.available", latest, current, DOWNLOAD_URL);
    }

    /** Operators and the integrated-server owner are told; other players are not. */
    static boolean mayReceive(boolean operator, boolean dedicated, String owner, String name) {
        return operator || TelemetryCommand.isIntegratedOwner(dedicated, owner, name);
    }

    @SubscribeEvent
    public void playerJoined(PlayerEvent.PlayerLoggedInEvent event) {
        String latest = newerVersion;
        if (latest == null || closed || !(event.player instanceof EntityPlayerMP)) return;
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null) return;
        EntityPlayerMP player = (EntityPlayerMP) event.player;
        boolean operator = server.getConfigurationManager()
            .func_152596_g(player.getGameProfile());
        if (!mayReceive(operator, server.isDedicatedServer(), server.getServerOwner(), player.getCommandSenderName()))
            return;
        player.addChatMessage(
            message(latest, currentVersion).setChatStyle(new ChatStyle().setColor(EnumChatFormatting.YELLOW)));
    }

    @Override
    public void close() {
        closed = true;
        if (registered) {
            FMLCommonHandler.instance()
                .bus()
                .unregister(this);
            registered = false;
        }
        worker.shutdownNow();
    }
}
