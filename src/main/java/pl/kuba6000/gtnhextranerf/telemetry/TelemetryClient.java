package pl.kuba6000.gtnhextranerf.telemetry;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.function.BooleanSupplier;

import com.google.common.io.BaseEncoding;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import pl.kuba6000.gtnhextranerf.net.SecureEndpoint;

/** Plain JSON transport with an optional salted JAR proof. Owned by the telemetry worker. */
public final class TelemetryClient {

    private final Path source;
    private final URL endpoint;
    private final SecureRandom random;
    private boolean hashAttempted;
    private byte[] jarDigest;

    public TelemetryClient(Path source, URL endpoint, SecureRandom random) {
        SecureEndpoint.require(endpoint);
        this.source = source;
        this.endpoint = endpoint;
        this.random = random;
    }

    /**
     * The salted JAR proof, or JSON null when the JAR cannot be read. Shared by every report that carries one.
     */
    public JsonElement jarProof(BooleanSupplier cancelled) {
        try {
            if (!hashAttempted) {
                hashAttempted = true;
                if (source == null || !Files.isRegularFile(source)) return JsonNull.INSTANCE;
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                try (InputStream input = Files.newInputStream(source)) {
                    byte[] buffer = new byte[8192];
                    int count;
                    while ((count = input.read(buffer)) != -1) {
                        if (cancelled.getAsBoolean()) return JsonNull.INSTANCE;
                        digest.update(buffer, 0, count);
                    }
                }
                jarDigest = digest.digest();
            }
            if (jarDigest == null) return JsonNull.INSTANCE;
            byte[] salt = new byte[16];
            random.nextBytes(salt);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(salt);
            JsonObject proof = new JsonObject();
            proof.addProperty(
                "salt",
                BaseEncoding.base16()
                    .lowerCase()
                    .encode(salt));
            proof.addProperty(
                "sha256",
                BaseEncoding.base16()
                    .lowerCase()
                    .encode(digest.digest(jarDigest)));
            return proof;
        } catch (IOException | SecurityException ignored) {
            return JsonNull.INSTANCE;
        } catch (NoSuchAlgorithmException failure) {
            throw new IllegalStateException(failure);
        }
    }

    public String encode(ServerTelemetry.Snapshot snapshot, BooleanSupplier cancelled) {
        JsonObject payload = new JsonObject();
        payload.addProperty("sessionId", snapshot.sessionId.toString());
        payload.addProperty("onlinePlayers", snapshot.onlinePlayers);
        payload.addProperty("highestTier", snapshot.highestTier);
        payload.addProperty("singleplayer", snapshot.singleplayer);
        payload.add("jarProof", jarProof(cancelled));
        return payload.toString();
    }

    public Response send(ServerTelemetry.Snapshot snapshot, BooleanSupplier cancelled) throws IOException {
        if (cancelled.getAsBoolean()) return null;
        byte[] body = encode(snapshot, cancelled).getBytes(StandardCharsets.UTF_8);
        return post(endpoint, body, cancelled);
    }

    /** One JSON POST with the project's fixed headers and timeouts; no redirects are followed. */
    public static Response post(URL endpoint, byte[] body, BooleanSupplier cancelled) throws IOException {
        if (cancelled.getAsBoolean()) return null;
        HttpURLConnection connection = (HttpURLConnection) endpoint.openConnection();
        try {
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setUseCaches(false);
            connection.setAllowUserInteraction(false);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("User-Agent", "GTNH-Extra-Nerf");
            connection.setDoOutput(true);
            connection.setFixedLengthStreamingMode(body.length);
            if (cancelled.getAsBoolean()) return null;
            try (OutputStream output = connection.getOutputStream()) {
                if (cancelled.getAsBoolean()) return null;
                output.write(body);
            }
            int status = connection.getResponseCode();
            long retryAfter = status == 429 || status == 503 ? 60 : 0;
            String retryHeader = connection.getHeaderField("Retry-After");
            if (retryHeader != null) try {
                retryAfter = Long.parseLong(retryHeader.trim());
            } catch (NumberFormatException ignored) {
                // Date-form headers retain the conservative default.
            }
            return new Response(status, retryAfter);
        } finally {
            connection.disconnect();
        }
    }

    @FunctionalInterface
    public interface Sender {

        Response send(ServerTelemetry.Snapshot snapshot, BooleanSupplier cancelled) throws Exception;
    }

    public static final class Response {

        public final int status;
        public final long retryAfterSeconds;
        private final long receivedAtNanos = System.nanoTime();

        public Response(int status, long retryAfterSeconds) {
            this.status = status;
            this.retryAfterSeconds = Math.max(0, Math.min(86400, retryAfterSeconds));
        }

        /** Time since the response was received, unaffected by changes to the system clock. */
        public long ageSeconds() {
            return Math.max(0, (System.nanoTime() - receivedAtNanos) / 1_000_000_000L);
        }
    }
}
