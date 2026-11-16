package pl.kuba6000.gtnhextranerf.telemetry;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import pl.kuba6000.gtnhextranerf.LangFile;

class TelemetryCommandTest {

    @Test
    void integratedServerOwnerMayUseTheCommandWithoutCheats() {
        assertTrue(TelemetryCommand.isIntegratedOwner(false, "Steve", "Steve"));
        assertTrue(TelemetryCommand.isIntegratedOwner(false, "Steve", "steve"));
    }

    @Test
    void turningStatisticsOffWhileTeamTrackingIsOnSaysHowToStopTeamReports() throws Exception {
        assertTrue(
            LangFile.text(TelemetryCommand.offMessage(true))
                .contains("/extranerfteam off"));
        assertFalse(
            LangFile.text(TelemetryCommand.offMessage(false))
                .contains("/extranerfteam"));
    }

    @Test
    void lanGuestsAndDedicatedServersStillNeedOperatorPermission() {
        assertFalse(TelemetryCommand.isIntegratedOwner(false, "Steve", "Alex"));
        assertFalse(TelemetryCommand.isIntegratedOwner(true, "Steve", "Steve"));
        assertFalse(TelemetryCommand.isIntegratedOwner(false, null, "Steve"));
    }
}
