package pl.kuba6000.gtnhextranerf.telemetry;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.Test;

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
        assertTrue(data.telemetry());
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
        assertTrue(loaded.telemetry());
        assertTrue(loaded.team());
        assertEquals(TEAM, loaded.teamId());
        assertEquals(LEADER, loaded.leaderId());
        assertEquals("Kuba", loaded.leaderName());
        assertFalse(loaded.carriedTeam(), "carrying over is a fact of one load, never saved");
    }
}
