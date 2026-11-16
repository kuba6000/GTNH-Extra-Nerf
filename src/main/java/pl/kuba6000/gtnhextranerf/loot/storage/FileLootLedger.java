package pl.kuba6000.gtnhextranerf.loot.storage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.zip.CRC32;

import pl.kuba6000.gtnhextranerf.loot.BagVariant;
import pl.kuba6000.gtnhextranerf.loot.ItemKey;
import pl.kuba6000.gtnhextranerf.loot.LootPlan;
import pl.kuba6000.gtnhextranerf.loot.LootReward;
import pl.kuba6000.gtnhextranerf.loot.LootState;

/**
 * Exclusive, forced append journal. Corruption is fatal and never silently resets counts. The one exception is a
 * damaged final record that nothing valid can follow, left by a crash, power loss or a backup copied mid-write: a frame
 * running past the end of the file, an invalid length followed only by zero bytes, or a checksum mismatch on a frame
 * ending exactly at the end of the file. That write never finished, so it is cut off.
 */
public final class FileLootLedger implements AutoCloseable {

    private static final long OWNER_LOCK_POSITION = Long.MAX_VALUE - 1;
    private static final int MAGIC = 0x45584E4C;
    private static final int VERSION = 1;
    private static final int MAX_FRAME = 16 * 1024 * 1024;
    private static final int MAX_STRING = 1024 * 1024;
    private final FileChannel channel;
    private final FileLock lock;
    private final Map<UUID, LootClaim> claims = new LinkedHashMap<>();
    private LootState state = LootState.empty();
    private long sequence;
    private UUID ledgerId;
    private boolean healthy = true;
    private long discardedTail;

    private FileLootLedger(FileChannel channel, FileLock lock) {
        this.channel = channel;
        this.lock = lock;
    }

    /** Only for an explicitly new ledger directory. Existing/partly initialized directories are never reused. */
    public static FileLootLedger create(Path directory) throws IOException {
        Path absolute = directory.toAbsolutePath();
        Files.createDirectories(absolute.getParent());
        Files.createDirectory(absolute);
        return connect(absolute, true);
    }

    /** Requires an existing journal. A missing file is an error, never an empty save. */
    public static FileLootLedger open(Path directory) throws IOException {
        return connect(directory, false);
    }

    private static FileLootLedger connect(Path directory, boolean create) throws IOException {
        Path file = directory.resolve("claims.journal");
        FileChannel channel = create
            ? FileChannel.open(file, StandardOpenOption.CREATE_NEW, StandardOpenOption.READ, StandardOpenOption.WRITE)
            : FileChannel.open(file, StandardOpenOption.READ, StandardOpenOption.WRITE);
        try {
            FileLock lock;
            try {
                // Lock one byte the journal never reaches instead of the whole file: Windows locks are mandatory,
                // and a whole-file lock stops backup tools in the same game from reading the world.
                lock = channel.tryLock(OWNER_LOCK_POSITION, 1, false);
            } catch (OverlappingFileLockException failure) {
                throw new IOException("Loot ledger is already open", failure);
            }
            if (lock == null) throw new IOException("Loot ledger is already open");
            FileLootLedger ledger = new FileLootLedger(channel, lock);
            if (create) {
                ledger.ledgerId = UUID.randomUUID();
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                DataOutputStream out = new DataOutputStream(bytes);
                out.writeByte(0);
                out.writeLong(0);
                out.writeInt(MAGIC);
                out.writeInt(VERSION);
                writeUuid(out, ledger.ledgerId);
                ledger.append(bytes.toByteArray());
            } else ledger.replay();
            return ledger;
        } catch (IOException | RuntimeException failure) {
            try {
                channel.close();
            } catch (IOException closeFailure) {
                failure.addSuppressed(closeFailure);
            }
            throw failure;
        }
    }

    public synchronized LootState snapshot() {
        if (!healthy || !channel.isOpen()) throw new IllegalStateException("Loot ledger unavailable");
        return state;
    }

    public synchronized List<LootClaim> unresolved() {
        List<LootClaim> result = new ArrayList<>();
        for (LootClaim claim : claims.values()) if (claim.outcome() != LootClaim.Outcome.DELIVERED) result.add(claim);
        return Collections.unmodifiableList(result);
    }

    public UUID ledgerId() {
        return ledgerId;
    }

    /** Bytes of a damaged final record that were cut off when the journal was opened, or 0. */
    public long discardedTail() {
        return discardedTail;
    }

    /** Returns only after the allowance claim is forced to storage. Delivery must happen afterward. */
    public synchronized void claim(UUID opening, UUID player, String policyRevision, LootPlan plan) throws IOException {
        usable();
        Objects.requireNonNull(opening, "Opening ID");
        Objects.requireNonNull(player, "Player UUID");
        Objects.requireNonNull(policyRevision, "Policy revision");
        if (policyRevision.isEmpty()) throw new IllegalArgumentException("Policy revision is required");
        if (claims.containsKey(opening)) throw new IllegalStateException("Opening was already claimed");
        if (plan.beforeState() != state) throw new IllegalStateException("Stale or foreign loot plan");
        Map<String, Long> next = plan.stateAfterCommit()
            .consumedUnits();
        for (Map.Entry<String, Long> previous : state.consumedUnits()
            .entrySet()) {
            if (next.getOrDefault(previous.getKey(), -1L) < previous.getValue())
                throw new IllegalArgumentException("A claim cannot replenish an allowance");
        }
        Map<String, Long> changes = new LinkedHashMap<>();
        next.forEach(
            (key, count) -> {
                if (!Objects.equals(
                    state.consumedUnits()
                        .get(key),
                    count)) changes.put(key, count);
            });
        if (changes.size() > 100_000 || plan.rewards()
            .isEmpty()
            || plan.rewards()
                .size() > 100_000)
            throw new IOException("Loot claim exceeds journal collection limits");
        long nextSequence = Math.addExact(sequence, 1);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytes);
        out.writeByte(1);
        out.writeLong(nextSequence);
        writeUuid(out, opening);
        writeUuid(out, player);
        writeString(out, policyRevision);
        out.writeInt(
            plan.bag()
                .groupId());
        out.writeInt(
            plan.bag()
                .fortune());
        out.writeInt(changes.size());
        for (Map.Entry<String, Long> change : changes.entrySet()) {
            writeString(out, change.getKey());
            out.writeLong(change.getValue());
        }
        out.writeInt(
            plan.rewards()
                .size());
        for (LootReward reward : plan.rewards()) writeReward(out, reward);
        append(bytes.toByteArray());
        try {
            sequence = nextSequence;
            state = plan.stateAfterCommit();
            claims.put(
                opening,
                new LootClaim(opening, player, policyRevision, plan.bag(), plan.rewards(), LootClaim.Outcome.PENDING));
        } catch (RuntimeException | Error failure) {
            healthy = false;
            throw failure;
        }
    }

    /** Recording an outcome does not refund counts and does not deliver any item. */
    public synchronized void recordOutcome(UUID opening, LootClaim.Outcome outcome) throws IOException {
        usable();
        Objects.requireNonNull(outcome, "Outcome");
        LootClaim previous = claims.get(opening);
        if (previous == null || previous.outcome() != LootClaim.Outcome.PENDING || outcome == LootClaim.Outcome.PENDING)
            throw new IllegalStateException("Invalid delivery outcome transition");
        long nextSequence = Math.addExact(sequence, 1);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytes);
        out.writeByte(2);
        out.writeLong(nextSequence);
        writeUuid(out, opening);
        out.writeByte(outcome == LootClaim.Outcome.DELIVERED ? 1 : 2);
        append(bytes.toByteArray());
        try {
            sequence = nextSequence;
            claims.put(opening, previous.withOutcome(outcome));
        } catch (RuntimeException | Error failure) {
            healthy = false;
            throw failure;
        }
    }

    private void append(byte[] payload) throws IOException {
        usable();
        if (payload.length > MAX_FRAME) throw new IOException("Loot journal record too large");
        ByteBuffer frame = ByteBuffer.allocate(payload.length + 8);
        frame.putInt(payload.length)
            .putInt(checksum(payload))
            .put(payload)
            .flip();
        try {
            channel.position(channel.size());
            while (frame.hasRemaining())
                if (channel.write(frame) <= 0) throw new IOException("Loot journal write stalled");
            channel.force(true);
        } catch (IOException | RuntimeException | Error failure) {
            healthy = false;
            throw failure;
        }
    }

    private void replay() throws IOException {
        channel.position(0);
        boolean header = false;
        long tornAt = -1;
        try {
            while (channel.position() < channel.size()) {
                long frameStart = channel.position();
                if (channel.size() - frameStart < 8) {
                    tornAt = frameStart;
                    break;
                }
                ByteBuffer prefix = ByteBuffer.allocate(8);
                readFully(prefix);
                prefix.flip();
                int length = prefix.getInt();
                int crc = prefix.getInt();
                if (length <= 0 || length > MAX_FRAME) {
                    // A zero-filled tail (e.g. preallocated blocks after power loss) holds no record at all.
                    if (zeroFrom(frameStart)) {
                        tornAt = frameStart;
                        break;
                    }
                    throw new IOException("Invalid loot journal frame");
                }
                // A frame running past the end of the file can only be the last write, interrupted.
                if (length > channel.size() - channel.position()) {
                    tornAt = frameStart;
                    break;
                }
                ByteBuffer payload = ByteBuffer.allocate(length);
                readFully(payload);
                if (checksum(payload.array()) != crc) {
                    // Only the very last frame can be a write that never finished.
                    if (channel.position() == channel.size()) {
                        tornAt = frameStart;
                        break;
                    }
                    throw new IOException("Loot journal checksum mismatch");
                }
                DataInputStream in = new DataInputStream(new ByteArrayInputStream(payload.array()));
                int type = in.readUnsignedByte();
                long number = in.readLong();
                if (!header) {
                    if (type != 0 || number != 0 || in.readInt() != MAGIC || in.readInt() != VERSION)
                        throw new IOException("Unsupported loot journal header");
                    ledgerId = readUuid(in);
                    header = true;
                } else {
                    if (number != Math.addExact(sequence, 1)) throw new IOException("Loot journal sequence gap");
                    if (type == 1) readClaim(in);
                    else if (type == 2) readOutcome(in);
                    else throw new IOException("Unknown loot journal record");
                    sequence = number;
                }
                if (in.available() != 0) throw new IOException("Trailing loot journal data");
            }
            if (!header) throw new IOException("Missing loot journal header");
        } catch (RuntimeException failure) {
            throw new IOException("Invalid loot journal data", failure);
        }
        if (tornAt >= 0) {
            discardedTail = channel.size() - tornAt;
            channel.truncate(tornAt);
            channel.force(true);
        }
    }

    private void readClaim(DataInputStream in) throws IOException {
        UUID opening = readUuid(in);
        UUID player = readUuid(in);
        String policy = readString(in);
        if (policy.isEmpty() || claims.containsKey(opening)) throw new IOException("Invalid duplicate/unnamed claim");
        BagVariant bag = BagVariant.of(in.readInt(), in.readInt());
        Map<String, Long> next = new LinkedHashMap<>(state.consumedUnits());
        int size = count(in);
        Map<String, Long> changes = new LinkedHashMap<>();
        for (int i = 0; i < size; i++) {
            String key = readString(in);
            long value = in.readLong();
            if (value < 0 || value < next.getOrDefault(key, 0L) || changes.putIfAbsent(key, value) != null)
                throw new IOException("Invalid counter transition");
        }
        next.putAll(changes);
        int rewardsCount = count(in);
        if (rewardsCount == 0) throw new IOException("Empty loot claim");
        List<LootReward> rewards = new ArrayList<>();
        for (int i = 0; i < rewardsCount; i++) {
            ItemKey item = ItemKey.of(readString(in), in.readInt());
            int amount = in.readInt();
            int maxStack = in.readInt();
            rewards.add(new LootReward(item, amount, readString(in), maxStack));
        }
        state = LootState.fromConsumed(next);
        claims.put(opening, new LootClaim(opening, player, policy, bag, rewards, LootClaim.Outcome.PENDING));
    }

    private void readOutcome(DataInputStream in) throws IOException {
        UUID opening = readUuid(in);
        int status = in.readUnsignedByte();
        LootClaim previous = claims.get(opening);
        if (previous == null || previous.outcome() != LootClaim.Outcome.PENDING || (status != 1 && status != 2))
            throw new IOException("Invalid outcome record");
        claims.put(opening, previous.withOutcome(status == 1 ? LootClaim.Outcome.DELIVERED : LootClaim.Outcome.FAILED));
    }

    /** True if every byte from {@code start} to the end of the file is zero. Leaves the position at the end. */
    private boolean zeroFrom(long start) throws IOException {
        channel.position(start);
        ByteBuffer buffer = ByteBuffer.allocate(8192);
        while (channel.read(buffer) > 0) {
            buffer.flip();
            while (buffer.hasRemaining()) if (buffer.get() != 0) return false;
            buffer.clear();
        }
        return true;
    }

    private void readFully(ByteBuffer target) throws IOException {
        while (target.hasRemaining()) if (channel.read(target) <= 0) throw new EOFException("Incomplete loot journal");
    }

    private static int count(DataInputStream in) throws IOException {
        int count = in.readInt();
        if (count < 0 || count > 100_000 || count > in.available())
            throw new IOException("Invalid journal collection length");
        return count;
    }

    private static void writeReward(DataOutputStream out, LootReward reward) throws IOException {
        writeString(
            out,
            reward.item()
                .registryName());
        out.writeInt(
            reward.item()
                .metadata());
        out.writeInt(reward.amount());
        out.writeInt(reward.maxStackSize());
        writeString(out, reward.nbt());
    }

    private static void writeString(DataOutputStream out, String value) throws IOException {
        ByteBuffer bytes = StandardCharsets.UTF_8.newEncoder()
            .encode(java.nio.CharBuffer.wrap(value));
        if (bytes.remaining() > MAX_STRING) throw new IOException("Loot journal string too large");
        byte[] copy = new byte[bytes.remaining()];
        bytes.get(copy);
        out.writeInt(copy.length);
        out.write(copy);
    }

    private static String readString(DataInputStream in) throws IOException {
        int size = in.readInt();
        if (size < 0 || size > MAX_STRING || size > in.available())
            throw new IOException("Invalid journal string length");
        byte[] bytes = new byte[size];
        in.readFully(bytes);
        return StandardCharsets.UTF_8.newDecoder()
            .decode(ByteBuffer.wrap(bytes))
            .toString();
    }

    private static void writeUuid(DataOutputStream out, UUID uuid) throws IOException {
        out.writeLong(uuid.getMostSignificantBits());
        out.writeLong(uuid.getLeastSignificantBits());
    }

    private static UUID readUuid(DataInputStream in) throws IOException {
        return new UUID(in.readLong(), in.readLong());
    }

    private static int checksum(byte[] bytes) {
        CRC32 crc = new CRC32();
        crc.update(bytes);
        return (int) crc.getValue();
    }

    private void usable() throws IOException {
        if (!healthy || !channel.isOpen() || !lock.isValid()) throw new IOException("Loot ledger closed or failed");
    }

    @Override
    public synchronized void close() throws IOException {
        healthy = false;
        try {
            if (lock.isValid()) lock.release();
        } finally {
            channel.close();
        }
    }
}
