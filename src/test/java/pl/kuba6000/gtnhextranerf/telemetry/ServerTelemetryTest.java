package pl.kuba6000.gtnhextranerf.telemetry;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pl.kuba6000.gtnhextranerf.Config;
import pl.kuba6000.gtnhextranerf.LangFile;

class ServerTelemetryTest {

    private boolean originallyEnabled;

    @BeforeEach
    void enableTelemetry() {
        originallyEnabled = Config.telemetryEnabled;
        Config.telemetryEnabled = true;
    }

    @AfterEach
    void restoreConfiguration() {
        Config.telemetryEnabled = originallyEnabled;
    }

    @Test
    void theJoinNoticeNamesTheOptOutCommandAndThePrivacyPolicy() throws Exception {
        String notice = LangFile.render(ServerTelemetry.joinNotice());
        assertTrue(notice.contains("/extranerftelemetry off"));
        assertTrue(notice.contains("https://extranerf.kuba6000.pl/privacy"));
    }

    @Test
    void theStatusIsATranslatedMessageWithTheStateAsArguments() throws Exception {
        String status = LangFile.render(ServerTelemetry.status());
        assertTrue(status.startsWith("enabled=") || status.startsWith("active; last="), status);
    }

    @Test
    void theCommandUsageIsATranslationKey() throws Exception {
        assertTrue(
            LangFile.text(new TelemetryCommand().getCommandUsage(null))
                .startsWith("/extranerftelemetry"));
    }

    @Test
    void capturesOnTheServerThreadAndSendsInTheBackgroundOnlyWhenDue() throws Exception {
        Thread serverThread = Thread.currentThread();
        AtomicInteger captures = new AtomicInteger();
        AtomicReference<Thread> networkThread = new AtomicReference<>();
        CountDownLatch delivered = new CountDownLatch(1);
        ExecutorService executor = Executors.newSingleThreadExecutor(r -> new Thread(r, "telemetry-test-worker"));
        TelemetryClient.Sender transport = (captured, cancelled) -> {
            networkThread.set(Thread.currentThread());
            delivered.countDown();
            return new TelemetryClient.Response(204, 0);
        };
        ServerTelemetry dispatcher = new ServerTelemetry(transport, executor, 1000, 100);
        Supplier<ServerTelemetry.Snapshot> snapshot = () -> {
            assertSame(serverThread, Thread.currentThread());
            captures.incrementAndGet();
            return new ServerTelemetry.Snapshot(UUID.randomUUID(), 2, "lv", false);
        };
        try {
            assertFalse(dispatcher.request(99, snapshot));
            assertTrue(dispatcher.request(100, snapshot));
            assertTrue(delivered.await(5, TimeUnit.SECONDS));
            assertFalse(dispatcher.request(101, snapshot));
            assertEquals(1, captures.get());
            assertNotSame(serverThread, networkThread.get());
        } finally {
            dispatcher.close();
        }
    }

    @Test
    void optOutDoesNotCaptureWorldStateOrSubmitNetworkWork() throws Exception {
        AtomicInteger sends = new AtomicInteger();
        ServerTelemetry dispatcher = new ServerTelemetry((captured, cancelled) -> {
            sends.incrementAndGet();
            return new TelemetryClient.Response(204, 0);
        }, Executors.newSingleThreadExecutor(), 1000, 0);
        try {
            Config.telemetryEnabled = false;
            assertFalse(
                dispatcher.request(100, () -> { throw new AssertionError("Disabled telemetry read the world"); }));
            assertEquals(0, sends.get());
        } finally {
            dispatcher.close();
        }
    }

    @Test
    void closingAnAttemptCancelsASendThatHasNotStarted() throws Exception {
        AtomicInteger sends = new AtomicInteger();
        CountDownLatch workerOccupied = new CountDownLatch(1);
        CountDownLatch releaseWorker = new CountDownLatch(1);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            workerOccupied.countDown();
            try {
                releaseWorker.await();
            } catch (InterruptedException ignored) {
                Thread.currentThread()
                    .interrupt();
            }
        });
        assertTrue(workerOccupied.await(5, TimeUnit.SECONDS));
        ServerTelemetry dispatcher = new ServerTelemetry((captured, cancelled) -> {
            sends.incrementAndGet();
            return new TelemetryClient.Response(204, 0);
        }, executor, 1000, 0);
        try {
            assertTrue(
                dispatcher.request(100, () -> new ServerTelemetry.Snapshot(UUID.randomUUID(), 1, "stone", true)));
            dispatcher.close();
            releaseWorker.countDown();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
            assertEquals(0, sends.get());
        } finally {
            releaseWorker.countDown();
            dispatcher.close();
        }
    }
}
