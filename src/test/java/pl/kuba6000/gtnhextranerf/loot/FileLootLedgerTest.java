package pl.kuba6000.gtnhextranerf.loot;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import pl.kuba6000.gtnhextranerf.loot.storage.FileLootLedger;

class FileLootLedgerTest {

    @TempDir
    Path temporary;

    @Test
    void anUnfinishedLastRecordIsCutOffAndEarlierClaimsSurvive() throws Exception {
        Path directory = temporary.resolve("torn");
        ItemKey item = ItemKey.of("appliedenergistics2:tile.BlockController", 0);
        GlobalItemLimit limit = new GlobalItemLimit("gtnhextranerf:me_controller", item, 1);
        try (FileLootLedger ledger = FileLootLedger.create(directory)) {
            new GlobalLootLimits(ledger, limit).claim(new UUID(0, 1), BagVariant.of(38, 3), new LootReward(item, 1));
        }
        Path file = directory.resolve("claims.journal");
        long complete = Files.size(file);
        // A crash or a backup copied mid-write: a frame that announces 100 bytes but holds 4, then only 3 bytes.
        for (byte[] tail : new byte[][] { { 0, 0, 0, 100, 1, 2, 3, 4, 5, 6, 7, 8 }, { 0, 0, 0 } }) {
            Files.write(file, tail, StandardOpenOption.APPEND);
            try (FileLootLedger ledger = FileLootLedger.open(directory)) {
                assertEquals(tail.length, ledger.discardedTail());
                assertFalse(new GlobalLootLimits(ledger, limit).available(item, 1));
            }
            assertEquals(complete, Files.size(file));
        }
    }

    @Test
    void aZeroFilledOrChecksumDamagedLastRecordIsCutOff() throws Exception {
        Path directory = temporary.resolve("zeroTail");
        ItemKey item = ItemKey.of("appliedenergistics2:tile.BlockController", 0);
        GlobalItemLimit limit = new GlobalItemLimit("gtnhextranerf:me_controller", item, 1);
        try (FileLootLedger ledger = FileLootLedger.create(directory)) {
            new GlobalLootLimits(ledger, limit).claim(new UUID(0, 1), BagVariant.of(38, 3), new LootReward(item, 1));
        }
        Path file = directory.resolve("claims.journal");
        long complete = Files.size(file);
        // Power loss: zero bytes after the last record, and a complete-length frame whose checksum does not match.
        for (byte[] tail : new byte[][] { new byte[64], { 0, 0, 0, 4, 0, 0, 0, 0, 9, 9, 9, 9 } }) {
            Files.write(file, tail, StandardOpenOption.APPEND);
            try (FileLootLedger ledger = FileLootLedger.open(directory)) {
                assertEquals(tail.length, ledger.discardedTail());
                assertFalse(new GlobalLootLimits(ledger, limit).available(item, 1));
            }
            assertEquals(complete, Files.size(file));
        }
    }

    @Test
    void damageFollowedByMoreDataIsFatal() throws Exception {
        Path directory = temporary.resolve("middle");
        try (FileLootLedger ignored = FileLootLedger.create(directory)) {}
        Path file = directory.resolve("claims.journal");
        byte[] header = Files.readAllBytes(file);
        // Zero length followed by a non-zero byte, and a checksum mismatch with bytes after the frame.
        for (byte[] tail : new byte[][] { { 0, 0, 0, 0, 0, 0, 0, 0, 1 },
            { 0, 0, 0, 4, 0, 0, 0, 0, 9, 9, 9, 9, 0, 0, 0, 0, 0, 0, 0, 0 } }) {
            Files.write(file, header);
            Files.write(file, tail, StandardOpenOption.APPEND);
            assertThrows(IOException.class, () -> FileLootLedger.open(directory));
            assertEquals(header.length + tail.length, Files.size(file));
        }
    }

    @Test
    void aJournalWithoutACompleteHeaderIsNeverRepaired() throws Exception {
        Path directory = temporary.resolve("headless");
        Files.createDirectories(directory);
        Path file = directory.resolve("claims.journal");
        Files.write(file, new byte[] { 0, 0, 0, 40, 0 });
        assertThrows(IOException.class, () -> FileLootLedger.open(directory));
        assertEquals(5, Files.size(file));
    }

    @Test
    void checksumCorruptionAndMissingFilesCannotResetAllowances() throws Exception {
        Path directory = temporary.resolve("corrupt");
        try (FileLootLedger ignored = FileLootLedger.create(directory)) {}
        Path file = directory.resolve("claims.journal");
        byte[] bytes = Files.readAllBytes(file);
        bytes[bytes.length - 1] ^= 1;
        Files.write(file, bytes);
        assertThrows(IOException.class, () -> FileLootLedger.open(directory));
        assertThrows(IOException.class, () -> FileLootLedger.open(temporary.resolve("missing")));
    }

    @Test
    void independentWorldsHaveDifferentIdentitiesAndTheFileHasAnExclusiveOwner() throws Exception {
        Path first = temporary.resolve("first");
        try (FileLootLedger a = FileLootLedger.create(first);
            FileLootLedger b = FileLootLedger.create(temporary.resolve("second"))) {
            assertNotEquals(a.ledgerId(), b.ledgerId());
            assertThrows(IOException.class, () -> FileLootLedger.open(first));
        }
    }

    @Test
    void anOpenJournalCanBeReadByABackup() throws Exception {
        Path directory = temporary.resolve("backup");
        try (FileLootLedger ignored = FileLootLedger.create(directory)) {
            byte[] copy = Files.readAllBytes(directory.resolve("claims.journal"));
            assertTrue(copy.length > 0);
        }
    }
}
