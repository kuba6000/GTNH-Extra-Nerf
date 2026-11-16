package pl.kuba6000.gtnhextranerf.audit;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.LongSupplier;

/** Owns lifecycle and synchronization; callback execution itself is never under the session lock. */
public final class AuditSession {

    private final List<AuditDefinition> definitions;
    private final Map<AuditDefinition, Accumulator> statistics = new IdentityHashMap<>();
    private final Map<Thread, Deque<AuditHandle>> stacks = new LinkedHashMap<>();
    private final LongSupplier clock;
    private AuditReport report;

    public AuditSession(Collection<? extends AuditDefinition> definitions) {
        this(definitions, System::nanoTime);
    }

    public AuditSession(Collection<? extends AuditDefinition> definitions, LongSupplier clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
        this.definitions = new ArrayList<>(definitions);
        Set<String> ids = new HashSet<>();
        for (AuditDefinition definition : this.definitions) {
            if (!ids.add(Objects.requireNonNull(definition.id(), "Audit id")))
                throw new IllegalArgumentException("Duplicate audit id: " + definition.id());
            statistics.put(definition, new Accumulator());
        }
    }

    public synchronized AuditHandle open(AuditDefinition definition) {
        if (report != null) throw new IllegalStateException("Audit session is finished");
        if (!statistics.containsKey(definition))
            throw new IllegalArgumentException("Definition does not belong to this session");
        AuditHandle handle = new AuditHandle(this, definition, clock.getAsLong());
        stacks.computeIfAbsent(Thread.currentThread(), ignored -> new ArrayDeque<>())
            .push(handle);
        return handle;
    }

    public synchronized boolean isFinished() {
        return report != null;
    }

    public synchronized AuditHandle current() {
        Deque<AuditHandle> stack = stacks.get(Thread.currentThread());
        if (stack == null || stack.isEmpty()) throw new IllegalStateException("No active audit scope");
        return stack.peek();
    }

    public void run(AuditDefinition definition, Consumer<AuditHandle> action) {
        Objects.requireNonNull(action, "action");
        auditResult(definition, handle -> {
            action.accept(handle);
            return null;
        });
    }

    public <T> T auditResult(AuditDefinition definition, Function<AuditHandle, T> action) {
        Objects.requireNonNull(action, "action");
        try (AuditHandle handle = open(definition)) {
            try {
                T result = action.apply(handle);
                if (handle.outcome == null) handle.success();
                return result;
            } catch (RuntimeException | Error failure) {
                try {
                    handle.failure(failure);
                } catch (RuntimeException | Error recordingFailure) {
                    if (recordingFailure != failure) failure.addSuppressed(recordingFailure);
                }
                throw failure;
            }
        }
    }

    private void requireCurrent(AuditHandle handle) {
        if (handle.closed || handle.owner != Thread.currentThread() || current() != handle)
            throw new IllegalStateException("Audit handle must be active, current and used by its owning thread");
    }

    synchronized void record(AuditHandle handle, int modified, int added, int removed) {
        requireCurrent(handle);
        if (handle.outcome != null) throw new IllegalStateException("Audit outcome already set");
        int nextModified = Math.addExact(handle.modified, modified);
        int nextAdded = Math.addExact(handle.added, added);
        int nextRemoved = Math.addExact(handle.removed, removed);
        handle.modified = nextModified;
        handle.added = nextAdded;
        handle.removed = nextRemoved;
    }

    synchronized void outcome(AuditHandle handle, AuditStatus status, String reason) {
        requireCurrent(handle);
        if (handle.outcome != null && status != AuditStatus.FAILED)
            throw new IllegalStateException("Audit outcome already set");
        handle.outcome = status;
        handle.reason = reason;
    }

    synchronized void close(AuditHandle handle) {
        requireCurrent(handle);
        long elapsed = Math.max(0, clock.getAsLong() - handle.started);
        Deque<AuditHandle> stack = stacks.get(handle.owner);

        if (handle.outcome == null) {
            handle.outcome = AuditStatus.FAILED;
            handle.reason = "Execution closed without an explicit outcome";
        }
        Accumulator value = statistics.get(handle.definition);
        int nextModified = Math.addExact(value.modified, handle.modified);
        int nextAdded = Math.addExact(value.added, handle.added);
        int nextRemoved = Math.addExact(value.removed, handle.removed);
        stack.pop();
        if (!stack.isEmpty()) stack.peek().nestedNanos += elapsed;
        value.modified = nextModified;
        value.added = nextAdded;
        value.removed = nextRemoved;
        value.nanos += Math.max(0, elapsed - handle.nestedNanos);
        if (handle.outcome == AuditStatus.SUCCESS) value.successes++;
        else {
            if (handle.outcome == AuditStatus.SKIPPED) value.skips++;
            else value.failures++;
            value.diagnostics.add(handle.reason);
        }
        handle.closed = true;
    }

    public synchronized AuditReport finish() {
        if (report != null) return report;
        if (stacks.values()
            .stream()
            .anyMatch(stack -> !stack.isEmpty()))
            throw new IllegalStateException("Cannot finish audit with active scopes");
        List<AuditResult> results = new ArrayList<>();
        for (AuditDefinition definition : definitions) {
            Accumulator value = statistics.get(definition);
            results.add(
                new AuditResult(
                    definition,
                    new AuditCounts(value.modified, value.added, value.removed),
                    value.successes,
                    value.failures,
                    value.skips,
                    value.nanos,
                    value.diagnostics));
        }
        report = new AuditReport(results);
        statistics.clear();
        stacks.clear();
        return report;
    }

    private static final class Accumulator {

        int modified;
        int added;
        int removed;
        int successes;
        int failures;
        int skips;
        long nanos;
        final List<String> diagnostics = new ArrayList<>();
    }
}
