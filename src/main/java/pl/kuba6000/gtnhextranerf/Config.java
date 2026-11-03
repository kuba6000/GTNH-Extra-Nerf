package pl.kuba6000.gtnhextranerf;

import java.io.File;

import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.config.Configuration;

import pl.kuba6000.gtnhextranerf.telemetry.TelemetryConsent;

/**
 * Telemetry and team tracking are decided per world: the values below describe the world that is loaded and are filled
 * from its {@link TelemetryConsent} record. The configuration file only carries what older versions saved there, once,
 * into the first world that is loaded.
 */
public class Config {

    /** True once the owners of this world have answered; until then nothing is sent, whatever else is set. */
    public static volatile boolean telemetryAsked = false;
    /** The effective telemetry setting: the world was asked and the answer was yes. */
    public static volatile boolean telemetryEnabled = false;
    public static volatile boolean teamTrackingEnabled = false;
    public static volatile String teamId = "";
    public static volatile String teamLeaderId = "";
    public static volatile String teamLeaderName = "";
    private static Configuration current;
    private static File currentFile;
    private static boolean legacyTelemetryOptedOut;
    private static boolean legacyTeamEnabled;
    private static String legacyTeamId = "";
    private static String legacyTeamLeaderId = "";
    private static String legacyTeamLeaderName = "";
    private static TelemetryConsent consent;

    public static void synchronizeConfiguration(File configFile) {
        Configuration configuration = new Configuration(configFile);
        legacyTelemetryOptedOut = !configuration.getBoolean(
            "enabled",
            "telemetry",
            true,
            "Legacy. Telemetry is decided per world now: the first time a world is loaded after an update, "
                + "false here is kept as a refusal; anything else is asked in game. "
                + "Use /extranerftelemetry on|off to change a world. Privacy policy: https://extranerf.kuba6000.pl/privacy");
        legacyTeamEnabled = configuration.getBoolean(
            "enabled",
            "team_tracking",
            false,
            "Legacy. A team that was enabled here is moved into the first world that is loaded and cleared from this file. "
                + "Use /extranerfteam on|off to change a world.");
        legacyTeamId = configuration
            .getString("id", "team_tracking", "", "Legacy. Moved into a world together with the settings above.");
        legacyTeamLeaderId = configuration
            .getString("leader", "team_tracking", "", "Legacy. Moved into a world together with the settings above.");
        legacyTeamLeaderName = configuration.getString(
            "leaderName",
            "team_tracking",
            "",
            "Legacy. Moved into a world together with the settings above.");
        current = configuration;
        currentFile = configFile;

        if (configuration.hasChanged()) {
            configuration.save();
        }
    }

    /** Loads the record of the world that is starting; the first load after an update carries the old settings over. */
    public static void loadWorld() {
        WorldServer overworld = DimensionManager.getWorld(0);
        if (overworld == null) throw new IllegalStateException("The consent overworld is unavailable");
        TelemetryConsent data = TelemetryConsent.load(
            overworld,
            new TelemetryConsent.Legacy(
                legacyTelemetryOptedOut,
                legacyTeamEnabled,
                legacyTeamId,
                legacyTeamLeaderId,
                legacyTeamLeaderName));
        if (data.carriedTeam()) clearLegacyTeam();
        consent = data;
        apply();
    }

    /** Called when the server stops, so that the next world starts from nothing. */
    public static void unloadWorld() {
        consent = null;
        telemetryAsked = false;
        telemetryEnabled = false;
        teamTrackingEnabled = false;
        teamId = "";
        teamLeaderId = "";
        teamLeaderName = "";
    }

    private static void apply() {
        telemetryAsked = consent.asked();
        telemetryEnabled = consent.asked() && consent.telemetry();
        teamTrackingEnabled = consent.asked() && consent.team();
        teamId = consent.teamId();
        teamLeaderId = consent.leaderId();
        teamLeaderName = consent.leaderName();
    }

    private static TelemetryConsent world() {
        if (consent == null) throw new IllegalStateException("No world has been loaded");
        return consent;
    }

    /** Owner command or answer, called on the server thread. Whichever way it goes, the world has been asked. */
    public static void setTelemetryEnabled(boolean enabled) {
        world().answerTelemetry(enabled);
        apply();
    }

    /** Opt-in command, called on the server thread. The team ID is created once and then kept forever. */
    public static void enableTeamTracking(String newTeamId, String leaderId, String leaderName) {
        world().enableTeam(newTeamId, leaderId, leaderName);
        apply();
    }

    public static void disableTeamTracking() {
        world().disableTeam();
        apply();
    }

    /** Keeps the team name current when the leader changes their Minecraft name. */
    public static void updateTeamLeaderName(String leaderName) {
        world().renameLeader(leaderName);
        apply();
    }

    /** The team has moved into a world; another world must not list itself under the same ID. */
    private static void clearLegacyTeam() {
        legacyTeamEnabled = false;
        legacyTeamId = "";
        legacyTeamLeaderId = "";
        legacyTeamLeaderName = "";
        if (current == null) return;
        current.get("team_tracking", "enabled", false)
            .set(false);
        current.get("team_tracking", "id", "")
            .set("");
        current.get("team_tracking", "leader", "")
            .set("");
        current.get("team_tracking", "leaderName", "")
            .set("");
        current.save();
    }

    public static File configurationFile() {
        if (currentFile == null) throw new IllegalStateException("Configuration has not been initialized");
        return currentFile;
    }
}
