package pl.kuba6000.gtnhextranerf.teams;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URL;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import pl.kuba6000.gtnhextranerf.Config;
import pl.kuba6000.gtnhextranerf.LangFile;
import pl.kuba6000.gtnhextranerf.telemetry.TelemetryClient;

class TeamTrackingTest {

    private static final UUID TEAM = UUID.fromString("0f8fad5b-d9cb-469f-a165-70867728950e");
    private static final UUID SESSION = UUID.fromString("62420a97-276b-4afd-a226-558f247802ac");
    private static final UUID ALEX = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final UUID STEVE = UUID.fromString("22222222-2222-4222-8222-222222222222");
    private static final TeamTracking.Member LEADER = new TeamTracking.Member(ALEX, "Alex");
    private static final TeamTracking.Member FRIEND = new TeamTracking.Member(STEVE, "Steve_2");

    private boolean originallyEnabled;

    @BeforeEach
    void optIn() {
        originallyEnabled = Config.teamTrackingEnabled;
        Config.teamTrackingEnabled = true;
    }

    @AfterEach
    void restoreConfiguration() {
        Config.teamTrackingEnabled = originallyEnabled;
    }

    @Test
    void theTeamIsNamedAfterItsLeader() {
        assertEquals("Kuba's team", TeamTracking.teamName("Kuba"));
    }

    @Test
    void reportCarriesTeamIdLeaderAndPlayers() throws Exception {
        TeamClient client = new TeamClient(null, new URL("http://127.0.0.1:1"), new SecureRandom());
        JsonObject report = new JsonParser()
            .parse(
                client.encode(
                    new TeamTracking.Snapshot(TEAM, LEADER, "lv", false, Arrays.asList(LEADER, FRIEND), SESSION, 2),
                    () -> false))
            .getAsJsonObject();
        JsonObject expected = new JsonParser()
            .parse(
                "{\"teamId\":\"" + TEAM
                    + "\",\"leader\":{\"id\":\""
                    + ALEX
                    + "\",\"name\":\"Alex\"},\"highestTier\":\"lv\",\"singleplayer\":false,\"players\":[{\"id\":\""
                    + ALEX
                    + "\",\"name\":\"Alex\"},{\"id\":\""
                    + STEVE
                    + "\",\"name\":\"Steve_2\"}],\"jarProof\":null,\"sessionId\":\""
                    + SESSION
                    + "\",\"onlinePlayers\":2}")
            .getAsJsonObject();
        assertEquals(expected, report);
    }

    @Test
    void teamReportCarriesTheCurrentRunAndTotalOnlinePlayersForStatistics() throws Exception {
        TeamClient client = new TeamClient(null, new URL("http://127.0.0.1:1"), new SecureRandom());
        TeamTracking.Snapshot snapshot = new TeamTracking.Snapshot(
            TEAM,
            LEADER,
            "lv",
            false,
            Arrays.asList(LEADER, FRIEND),
            SESSION,
            4);
        JsonObject report = new JsonParser().parse(client.encode(snapshot, () -> false))
            .getAsJsonObject();
        assertEquals(
            SESSION.toString(),
            report.get("sessionId")
                .getAsString());
        assertEquals(
            4,
            report.get("onlinePlayers")
                .getAsInt());
    }

    @Test
    void aSingleplayerWorldIsMarkedInTheReport() throws Exception {
        TeamClient client = new TeamClient(null, new URL("http://127.0.0.1:1"), new SecureRandom());
        JsonObject report = new JsonParser()
            .parse(
                client.encode(
                    new TeamTracking.Snapshot(TEAM, LEADER, "lv", true, Arrays.asList(LEADER), SESSION, 1),
                    () -> false))
            .getAsJsonObject();
        assertTrue(
            report.get("singleplayer")
                .getAsBoolean());
    }

    @Test
    void joiningPlayersAreToldAboutTeamTrackingAndWhereToReadMore() throws Exception {
        String notice = LangFile.render(TeamTracking.joinNotice());
        assertTrue(notice.contains("team tracking"));
        assertTrue(notice.contains("Minecraft name"));
        assertTrue(notice.contains("https://extranerf.kuba6000.pl/team-tracking"));
        String objection = LangFile.render(TeamTracking.objectionNotice());
        assertTrue(objection.contains("right to object"));
        assertTrue(objection.contains("https://extranerf.kuba6000.pl/team-tracking#removal"));
    }

    @Test
    void theCommandUsageAndTheMissingLeaderMessageAreTranslationKeys() throws Exception {
        assertTrue(
            LangFile.text(new TeamCommand().getCommandUsage(null))
                .startsWith("/extranerfteam"));
        assertFalse(
            LangFile.text("gtnhextranerf.team.needs_player")
                .isEmpty());
    }

    @Test
    void anInvalidTierIsRefused() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new TeamTracking.Snapshot(TEAM, LEADER, "LV!", false, new ArrayList<>(), SESSION, 0));
    }

    @Test
    void anEmptyServerStillReportsItsTeam() throws Exception {
        TeamClient client = new TeamClient(null, new URL("http://127.0.0.1:1"), new SecureRandom());
        JsonObject report = new JsonParser()
            .parse(
                client.encode(
                    new TeamTracking.Snapshot(TEAM, LEADER, "stone", false, new ArrayList<>(), SESSION, 0),
                    () -> false))
            .getAsJsonObject();
        assertEquals(
            0,
            report.getAsJsonArray("players")
                .size());
    }

    @Test
    void invalidTeamsAreRefusedBeforeAnythingIsSent() {
        assertThrows(IllegalArgumentException.class, () -> new TeamTracking.Member(ALEX, "bad name"));
        assertThrows(IllegalArgumentException.class, () -> new TeamTracking.Member(ALEX, ""));
        assertThrows(IllegalArgumentException.class, () -> new TeamTracking.Member(ALEX, "SeventeenCharacter"));
        assertThrows(
            IllegalArgumentException.class,
            () -> new TeamTracking.Snapshot(
                TEAM,
                LEADER,
                "lv",
                false,
                Arrays.asList(LEADER, new TeamTracking.Member(ALEX, "AlexAgain")),
                SESSION,
                2));
        List<TeamTracking.Member> crowd = new ArrayList<>();
        for (int i = 0; i <= TeamTracking.Snapshot.MAXIMUM_PLAYERS; i++)
            crowd.add(new TeamTracking.Member(UUID.randomUUID(), "P" + i));
        assertThrows(
            IllegalArgumentException.class,
            () -> new TeamTracking.Snapshot(TEAM, LEADER, "lv", false, crowd, SESSION, crowd.size()));
    }

    @Test
    void capturesOnTheServerThreadAndSendsInTheBackgroundOnlyWhenDue() throws Exception {
        Thread serverThread = Thread.currentThread();
        AtomicInteger captures = new AtomicInteger();
        AtomicReference<Thread> networkThread = new AtomicReference<>();
        CountDownLatch delivered = new CountDownLatch(1);
        ExecutorService executor = Executors.newSingleThreadExecutor(r -> new Thread(r, "team-test-worker"));
        TeamTracking tracking = new TeamTracking((captured, cancelled) -> {
            networkThread.set(Thread.currentThread());
            delivered.countDown();
            return new TelemetryClient.Response(204, 0);
        }, executor, 1000, 100);
        try {
            java.util.function.Supplier<TeamTracking.Snapshot> snapshot = () -> {
                assertSame(serverThread, Thread.currentThread());
                captures.incrementAndGet();
                return new TeamTracking.Snapshot(TEAM, LEADER, "lv", false, Arrays.asList(LEADER), SESSION, 1);
            };
            assertFalse(tracking.request(99, snapshot));
            assertTrue(tracking.request(100, snapshot));
            assertTrue(delivered.await(5, TimeUnit.SECONDS));
            assertFalse(tracking.request(101, snapshot));
            assertEquals(1, captures.get());
            assertNotSame(serverThread, networkThread.get());
        } finally {
            tracking.close();
        }
    }

    @Test
    void turningTeamTrackingOffStopsCapturingAndSending() {
        AtomicInteger captures = new AtomicInteger();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        TeamTracking tracking = new TeamTracking((captured, cancelled) -> {
            fail("nothing may be sent");
            return null;
        }, executor, 1000, 0);
        Config.teamTrackingEnabled = false;
        try {
            assertFalse(tracking.request(1000, () -> {
                captures.incrementAndGet();
                return null;
            }));
            assertEquals(0, captures.get());
            assertTrue(executor.isShutdown());
        } finally {
            tracking.close();
        }
    }
}
