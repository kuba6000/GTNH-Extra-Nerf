package pl.kuba6000.gtnhextranerf.telemetry;

import static org.junit.jupiter.api.Assertions.*;

import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.SaveHandler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import pl.kuba6000.gtnhextranerf.Config;

class TelemetryConsentTest {

    private static final String TEAM = "5e9e2e20-9db0-4b4b-9d0a-1a6a8f5a7b11";
    private static final String LEADER = "0b1c43f9-77a0-4c7b-b3c3-5a1d6a1f3e22";

    @Test
    void aWorldWithoutAnyOlderSettingIsAsked() {
        TelemetryConsent data = TelemetryConsent.initial(TelemetryConsent.Legacy.NONE);
        assertFalse(data.asked());
        assertFalse(data.team());
        assertFalse(data.carriedTeam());
    }

    @Test
    void theOldDefaultOfTelemetryOnIsNotAnAnswer() {
        // Older versions wrote "enabled=true" for everyone, so it says nothing about what the owner wants.
        TelemetryConsent data = TelemetryConsent.initial(new TelemetryConsent.Legacy(false, false, "", "", ""));
        assertFalse(data.asked());
    }

    @Test
    void anOlderRefusalIsKeptAndNotAskedAgain() {
        TelemetryConsent data = TelemetryConsent.initial(new TelemetryConsent.Legacy(true, false, "", "", ""));
        assertTrue(data.asked());
        assertFalse(data.telemetry());
        assertFalse(data.team());
    }

    @Test
    void anOlderTeamIsMovedIntoTheWorldAndCountsAsAnAnswer() {
        TelemetryConsent data = TelemetryConsent
            .initial(new TelemetryConsent.Legacy(false, true, TEAM, LEADER, "Kuba"));
        assertTrue(data.asked());
        assertFalse(data.telemetry());
        assertTrue(data.team());
        assertEquals(TEAM, data.teamId());
        assertEquals(LEADER, data.leaderId());
        assertEquals("Kuba", data.leaderName());
        assertTrue(data.carriedTeam());
    }

    @Test
    void anOlderTeamKeepsAnOlderRefusalOfTelemetry() {
        TelemetryConsent data = TelemetryConsent.initial(new TelemetryConsent.Legacy(true, true, TEAM, LEADER, "Kuba"));
        assertTrue(data.asked());
        assertFalse(data.telemetry());
        assertTrue(data.team());
    }

    @Test
    void anOlderDisabledTeamKeepsItsIdentityWithoutTurningItOn() {
        TelemetryConsent data = TelemetryConsent
            .initial(new TelemetryConsent.Legacy(false, false, TEAM, LEADER, "Kuba"));
        assertFalse(data.team());
        assertFalse(data.asked());
        assertEquals(TEAM, data.teamId());
        assertTrue(data.carriedTeam());
    }

    @Test
    void anIncompleteOlderTeamIsNotCarriedOver() {
        for (TelemetryConsent.Legacy broken : new TelemetryConsent.Legacy[] {
            new TelemetryConsent.Legacy(false, true, "", LEADER, "Kuba"),
            new TelemetryConsent.Legacy(false, true, TEAM, "not-a-uuid", "Kuba"),
            new TelemetryConsent.Legacy(false, true, TEAM, LEADER, " ") }) {
            TelemetryConsent data = TelemetryConsent.initial(broken);
            assertFalse(data.asked());
            assertFalse(data.team());
            assertFalse(data.carriedTeam());
        }
    }

    @Test
    void answeringAsksNoMore() {
        TelemetryConsent data = TelemetryConsent.initial(TelemetryConsent.Legacy.NONE);
        data.answerTelemetry(true);
        assertTrue(data.asked());
        assertTrue(data.telemetry());
    }

    @Test
    void selectingEitherReportingModeDisablesTheOther() {
        TelemetryConsent data = TelemetryConsent.initial(TelemetryConsent.Legacy.NONE);
        data.answerTelemetry(true);
        data.enableTeam(TEAM, LEADER, "Kuba");
        assertTrue(data.team());
        assertFalse(data.telemetry());

        data.answerTelemetry(true);
        assertTrue(data.telemetry());
        assertFalse(data.team());
        assertEquals(TEAM, data.teamId());
    }

    @Test
    void refusalSurvivesReloadWithoutWaitingForWorldAutosave(@TempDir Path directory) {
        SaveHandler save = new SaveHandler(directory.toFile(), "world", false);
        TelemetryConsent data = TelemetryConsent.load(save, new MapStorage(save), TelemetryConsent.Legacy.NONE);
        data.answerTelemetry(true);
        data.answerTelemetry(false);

        TelemetryConsent reloaded = TelemetryConsent.load(save, new MapStorage(save), TelemetryConsent.Legacy.NONE);
        assertTrue(reloaded.asked());
        assertFalse(reloaded.telemetry());
    }

    @Test
    void teamIdCanBeReadFromTheWorldFolderAfterDisablingAndReloading(@TempDir Path directory) throws Exception {
        SaveHandler save = new SaveHandler(directory.toFile(), "world", false);
        TelemetryConsent data = TelemetryConsent.load(save, new MapStorage(save), TelemetryConsent.Legacy.NONE);
        Path readableId = save.getWorldDirectory()
            .toPath()
            .resolve("gtnhextranerf-team-id.txt");
        assertFalse(Files.exists(readableId));

        data.enableTeam(TEAM, LEADER, "Kuba");
        assertTrue(new String(Files.readAllBytes(readableId), StandardCharsets.UTF_8).contains(TEAM));
        data.disableTeam();
        TelemetryConsent.load(save, new MapStorage(save), TelemetryConsent.Legacy.NONE);
        assertTrue(new String(Files.readAllBytes(readableId), StandardCharsets.UTF_8).contains(TEAM));
    }

    @Test
    void inaccessibleWorldStorageStopsReportingWithoutCrashingWorldStartup(@TempDir Path directory) throws Exception {
        SaveHandler save = new SaveHandler(directory.toFile(), "world", false);
        Files.createDirectory(
            save.getMapFileFromName(TelemetryConsent.NAME)
                .toPath());
        try {
            assertDoesNotThrow(() -> Config.loadWorld(save, new MapStorage(save), TelemetryConsent.Legacy.NONE));
            assertFalse(Config.telemetryEnabled);
            assertFalse(Config.teamTrackingEnabled);
            assertFalse(Config.worldAvailable());
        } finally {
            Config.unloadWorld();
        }
    }

    @Test
    void aDamagedWorldDecisionIsNotOverwrittenAndReportingStaysOff(@TempDir Path directory) throws Exception {
        byte[][] damagedFiles = { "not a valid world decision".getBytes(StandardCharsets.UTF_8),
            CompressedStreamTools.compress(new NBTTagCompound()) };
        for (int index = 0; index < damagedFiles.length; index++) {
            SaveHandler save = new SaveHandler(directory.toFile(), "world" + index, false);
            Path decision = save.getMapFileFromName(TelemetryConsent.NAME)
                .toPath();
            byte[] damaged = damagedFiles[index];
            Files.write(decision, damaged);
            try {
                assertDoesNotThrow(() -> Config.loadWorld(save, new MapStorage(save), TelemetryConsent.Legacy.NONE));
                assertFalse(Config.worldAvailable());
                assertFalse(Config.telemetryEnabled);
                assertFalse(Config.teamTrackingEnabled);
                assertArrayEquals(damaged, Files.readAllBytes(decision));
            } finally {
                Config.unloadWorld();
            }
        }
    }

    @Test
    void aWorldWhoseSessionLockWasTakenCannotOverwriteTheNewOwner(@TempDir Path directory) throws Exception {
        SaveHandler oldOwner = new SaveHandler(directory.toFile(), "world", false);
        TelemetryConsent old = TelemetryConsent.load(oldOwner, new MapStorage(oldOwner), TelemetryConsent.Legacy.NONE);
        old.answerTelemetry(true);
        Thread.sleep(5);
        SaveHandler newOwner = new SaveHandler(directory.toFile(), "world", false);
        assertThrows(UncheckedIOException.class, () -> old.answerTelemetry(false));
        TelemetryConsent reloaded = TelemetryConsent
            .load(newOwner, new MapStorage(newOwner), TelemetryConsent.Legacy.NONE);
        assertTrue(reloaded.telemetry());
    }

    @Test
    void anOlderWorldWithBothModesEnabledKeepsOnlyTeamTracking() {
        NBTTagCompound old = new NBTTagCompound();
        old.setBoolean("asked", true);
        old.setBoolean("telemetry", true);
        old.setBoolean("team", true);
        old.setString("teamId", TEAM);
        old.setString("leaderId", LEADER);
        old.setString("leaderName", "Kuba");
        TelemetryConsent reloaded = new TelemetryConsent();
        reloaded.readFromNBT(old);
        assertTrue(reloaded.team());
        assertFalse(reloaded.telemetry());
    }

    @Test
    void turningTeamTrackingOnAnswersTheQuestionAndLeavingItKeepsTheTeamId() {
        TelemetryConsent data = TelemetryConsent.initial(TelemetryConsent.Legacy.NONE);
        String id = UUID.randomUUID()
            .toString();
        data.enableTeam(id, LEADER, "Kuba");
        assertTrue(data.asked());
        assertTrue(data.team());
        assertFalse(data.telemetry(), "team tracking does not turn minimal statistics on");
        data.disableTeam();
        assertFalse(data.team());
        assertEquals(id, data.teamId());
    }

    @Test
    void theRecordSurvivesSavingTheWorld() {
        TelemetryConsent data = TelemetryConsent
            .initial(new TelemetryConsent.Legacy(false, true, TEAM, LEADER, "Kuba"));
        NBTTagCompound tag = new NBTTagCompound();
        data.writeToNBT(tag);
        TelemetryConsent loaded = new TelemetryConsent();
        loaded.readFromNBT(tag);
        assertTrue(loaded.asked());
        assertFalse(loaded.telemetry());
        assertTrue(loaded.team());
        assertEquals(TEAM, loaded.teamId());
        assertEquals(LEADER, loaded.leaderId());
        assertEquals("Kuba", loaded.leaderName());
        assertFalse(loaded.carriedTeam(), "carrying over is a fact of one load, never saved");
    }
}
