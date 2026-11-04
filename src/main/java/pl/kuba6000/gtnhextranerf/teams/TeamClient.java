package pl.kuba6000.gtnhextranerf.teams;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.function.BooleanSupplier;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import pl.kuba6000.gtnhextranerf.net.SecureEndpoint;
import pl.kuba6000.gtnhextranerf.telemetry.TelemetryClient;

/** Encodes and posts one team report. */
public final class TeamClient {

    private final URL endpoint;
    private final TelemetryClient proof;

    public TeamClient(Path source, URL endpoint, SecureRandom random) {
        SecureEndpoint.require(endpoint);
        this.endpoint = endpoint;
        this.proof = new TelemetryClient(source, endpoint, random);
    }

    public String encode(TeamTracking.Snapshot snapshot, BooleanSupplier cancelled) {
        JsonObject payload = new JsonObject();
        payload.addProperty("teamId", snapshot.teamId.toString());
        payload.add("leader", member(snapshot.leader));
        payload.addProperty("highestTier", snapshot.highestTier);
        payload.addProperty("singleplayer", snapshot.singleplayer);
        payload.addProperty("sessionId", snapshot.sessionId.toString());
        payload.addProperty("onlinePlayers", snapshot.onlinePlayers);
        JsonArray players = new JsonArray();
        for (TeamTracking.Member player : snapshot.players) players.add(member(player));
        payload.add("players", players);
        payload.add("jarProof", proof.jarProof(cancelled));
        return payload.toString();
    }

    private static JsonObject member(TeamTracking.Member member) {
        JsonObject object = new JsonObject();
        object.addProperty("id", member.id.toString());
        object.addProperty("name", member.name);
        return object;
    }

    public TelemetryClient.Response send(TeamTracking.Snapshot snapshot, BooleanSupplier cancelled) throws IOException {
        if (cancelled.getAsBoolean()) return null;
        byte[] body = encode(snapshot, cancelled).getBytes(StandardCharsets.UTF_8);
        return TelemetryClient.post(endpoint, body, cancelled);
    }
}
