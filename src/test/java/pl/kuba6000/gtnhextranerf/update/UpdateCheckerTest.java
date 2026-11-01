package pl.kuba6000.gtnhextranerf.update;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import pl.kuba6000.gtnhextranerf.LangFile;

class UpdateCheckerTest {

    private static UpdateChecker checker(String current, UpdateChecker.Fetcher fetcher) {
        ScheduledExecutorService idle = Executors.newSingleThreadScheduledExecutor();
        // The first scheduled run is a day away; the tests call check() themselves.
        return new UpdateChecker(current, fetcher, idle, 86400, 86400);
    }

    @Test
    void parsesOnlyAReleaseTagInAJsonObject() {
        assertEquals("0.0.7", UpdateChecker.parseResponse("{\"version\":\"0.0.7\"}"));
        assertEquals("0.0.7", UpdateChecker.parseResponse("{\"version\":\"0.0.7\",\"other\":1}"));
        for (String body : new String[] { "", "not json", "[]", "{}", "{\"version\":7}", "{\"version\":null}",
            "{\"version\":\"0.0.7-beta\"}", "{\"version\":\"<script>\"}", "\"0.0.7\"" })
            assertNull(UpdateChecker.parseResponse(body), body);
    }

    @Test
    void reportsANewerReleaseAndClearsItWhenCurrent() {
        String[] answer = { "0.0.7" };
        UpdateChecker checker = checker("0.0.6", () -> answer[0]);
        try {
            assertNull(checker.newerVersion());
            checker.check();
            assertEquals("0.0.7", checker.newerVersion());
            answer[0] = null; // an unusable answer keeps what is known
            checker.check();
            assertEquals("0.0.7", checker.newerVersion());
            answer[0] = "0.0.6";
            checker.check();
            assertNull(checker.newerVersion());
        } finally {
            checker.close();
        }
    }

    @Test
    void networkFailuresAreSilentAndDevelopmentBuildsNeverReportAnUpdate() {
        UpdateChecker failing = checker("0.0.6", () -> { throw new IOException("offline"); });
        UpdateChecker development = checker("0.0.6-86-g02984ed", () -> "9.9.9");
        try {
            assertDoesNotThrow(failing::check);
            assertNull(failing.newerVersion());
            development.check();
            assertNull(development.newerVersion());
        } finally {
            failing.close();
            development.close();
        }
    }

    @Test
    void closedCheckersDoNothing() {
        AtomicInteger calls = new AtomicInteger();
        UpdateChecker checker = checker("0.0.6", () -> {
            calls.incrementAndGet();
            return "0.0.7";
        });
        checker.close();
        checker.check();
        assertEquals(0, calls.get());
        assertNull(checker.newerVersion());
    }

    @Test
    void messageNamesBothVersionsAndTheDownloadPage() throws Exception {
        String message = LangFile.render(UpdateChecker.message("0.0.7", "0.0.6"));
        assertTrue(message.contains("0.0.7"));
        assertTrue(message.contains("0.0.6"));
        assertTrue(message.contains("https://extranerf.kuba6000.pl/download"));
    }

    @Test
    void onlyOperatorsAndTheIntegratedOwnerAreTold() {
        assertTrue(UpdateChecker.mayReceive(true, true, null, "Someone"));
        assertFalse(UpdateChecker.mayReceive(false, true, "Owner", "Owner"));
        assertTrue(UpdateChecker.mayReceive(false, false, "Owner", "owner"));
        assertFalse(UpdateChecker.mayReceive(false, false, "Owner", "Guest"));
        assertFalse(UpdateChecker.mayReceive(false, false, null, "Guest"));
    }

    @Test
    void readsTheVersionOverHttpWithoutFollowingRedirectsOrAcceptingLargeAnswers() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicInteger redirected = new AtomicInteger();
        StringBuilder padding = new StringBuilder();
        for (int i = 0; i < 2000; i++) padding.append('a');
        server.createContext("/ok", exchange -> reply(exchange, 200, "{\"version\":\"0.0.7\"}"));
        server.createContext("/missing", exchange -> reply(exchange, 404, "{\"version\":\"0.0.7\"}"));
        server.createContext(
            "/large",
            exchange -> reply(exchange, 200, "{\"version\":\"0.0.7\",\"x\":\"" + padding + "\"}"));
        server.createContext("/moved", exchange -> {
            exchange.getResponseHeaders()
                .add("Location", "/ok-after-redirect");
            exchange.sendResponseHeaders(303, -1);
            exchange.close();
        });
        server.createContext("/ok-after-redirect", exchange -> {
            redirected.incrementAndGet();
            reply(exchange, 200, "{\"version\":\"0.0.7\"}");
        });
        server.start();
        try {
            String base = "http://127.0.0.1:" + server.getAddress()
                .getPort();
            assertEquals(
                "0.0.7",
                UpdateChecker.httpFetcher(new URL(base + "/ok"))
                    .fetch());
            assertNull(
                UpdateChecker.httpFetcher(new URL(base + "/missing"))
                    .fetch());
            assertNull(
                UpdateChecker.httpFetcher(new URL(base + "/large"))
                    .fetch());
            assertNull(
                UpdateChecker.httpFetcher(new URL(base + "/moved"))
                    .fetch());
            assertEquals(0, redirected.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void refusesPlainHttpToARemoteHost() {
        assertThrows(
            IllegalArgumentException.class,
            () -> UpdateChecker.httpFetcher(new URL("http://extranerf.kuba6000.pl/version.json")));
    }

    private static void reply(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody()
            .write(bytes);
        exchange.close();
    }
}
