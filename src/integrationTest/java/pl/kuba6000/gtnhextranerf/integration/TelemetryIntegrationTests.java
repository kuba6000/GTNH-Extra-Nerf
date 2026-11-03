package pl.kuba6000.gtnhextranerf.integration;

import java.io.File;
import java.net.URL;
import java.security.SecureRandom;
import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppedEvent;
import pl.kuba6000.gtnhextranerf.CommonProxy;
import pl.kuba6000.gtnhextranerf.Config;
import pl.kuba6000.gtnhextranerf.progression.MachineProgression;
import pl.kuba6000.gtnhextranerf.progression.ProgressionData;
import pl.kuba6000.gtnhextranerf.progression.ProgressionTier;
import pl.kuba6000.gtnhextranerf.progression.TelemetryEligibility;
import pl.kuba6000.gtnhextranerf.teams.TeamClient;
import pl.kuba6000.gtnhextranerf.teams.TeamTracking;
import pl.kuba6000.gtnhextranerf.telemetry.ServerTelemetry;
import pl.kuba6000.gtnhextranerf.telemetry.TelemetryClient;

/** Real Forge configuration and server lifecycle; never connects to the production endpoint. */
public final class TelemetryIntegrationTests {

    static void verify(MinecraftServer server) throws Exception {
        // Prevent production HTTP during fixture lifecycle restarts.
        ServerTelemetry.stop();
        ServerTelemetry.initialize(null, true);
        File config = Config.configurationFile();
        boolean originallyEnabled = Config.telemetryEnabled;
        WorldServer world = DimensionManager.getWorld(0);
        ProgressionData progression = (ProgressionData) world.mapStorage
            .loadData(ProgressionData.class, ProgressionData.NAME);
        if (progression == null) {
            progression = new ProgressionData();
            world.mapStorage.setData(ProgressionData.NAME, progression);
        }
        NBTTagCompound savedProgression = new NBTTagCompound();
        progression.writeToNBT(savedProgression);
        progression.readFromNBT(new NBTTagCompound());
        progression.markDirty();
        try {
            check(
                server.getCommandManager()
                    .executeCommand(server, "extranerftelemetry status") == 1,
                "telemetry status command was unavailable in the runtime JAR");
            check(
                server.getCommandManager()
                    .executeCommand(server, "extranerftelemetry off") == 1,
                "telemetry opt-out command failed");
            Config.synchronizeConfiguration(config);
            check(!Config.telemetryEnabled, "telemetry opt-out did not survive configuration reload");
            Config.setTelemetryEnabled(true);
            Config.synchronizeConfiguration(config);
            check(Config.telemetryEnabled, "telemetry could not be enabled again");

            // Capture through the actual service, with a loopback-only client that is never scheduled.
            TelemetryClient client = new TelemetryClient(null, new URL("http://127.0.0.1:1"), new SecureRandom());
            try (ServerTelemetry source = new ServerTelemetry(server, client)) {
                verifyYoungWorldIsNotReported(world, source);
                // From here on the world is old enough for any certificate.
                world.getWorldInfo()
                    .incrementTotalWorldTime(TelemetryEligibility.MINIMUM_WORLD_TICKS);
                resetProgression(world);
                check(source.capture().highestTier.equals("stone"), "empty progression was not Stone");
                MachineProgression.certify(world, ProgressionTier.LV);
                check(source.capture().highestTier.equals("stone"), "missing Steam did not block the reported tier");
                MachineProgression.certify(world, ProgressionTier.STEAM);
                ServerTelemetry.Snapshot snapshot = source.capture();
                check(
                    snapshot.highestTier.equals("lv"),
                    "snapshot did not reflect the server's highest available tier");
                check(
                    snapshot.onlinePlayers == server.getConfigurationManager().playerEntityList.size(),
                    "snapshot did not use the connected server-player count");
                MachineProgression.certify(world, ProgressionTier.HV);
                check(source.capture().highestTier.equals("lv"), "missing MV did not block HV in telemetry");
                MachineProgression.certify(world, ProgressionTier.MV);
                check(source.capture().highestTier.equals("hv"), "restoring MV did not unlock reported HV");
                MachineProgression.revoke(world, ProgressionTier.LV);
                check(source.capture().highestTier.equals("steam"), "revocation did not lower the reported tier");
                for (ProgressionTier tier : ProgressionTier.values()) MachineProgression.certify(world, tier);
                check(source.capture().highestTier.equals("uxv"), "MAX must not be reported by the client");
                check(snapshot.singleplayer == !server.isDedicatedServer(), "snapshot used the wrong server mode");
                UUID id = snapshot.sessionId;
                check(id.version() == 4 && id.variant() == 2, "server identity was not a random UUID v4");
                check(source.capture().sessionId.equals(id), "server identity changed between reports");
                ServerTelemetry.stop();
                ServerTelemetry.start(server);
                try (ServerTelemetry resumed = new ServerTelemetry(server, client)) {
                    check(resumed.capture().sessionId.equals(id), "toggling telemetry changed the server identity");
                }
                CommonProxy lifecycle = new CommonProxy();
                lifecycle.serverStopped(new FMLServerStoppedEvent());
                lifecycle.serverStarting(new FMLServerStartingEvent(server));
                try (ServerTelemetry restarted = new ServerTelemetry(server, client)) {
                    UUID nextId = restarted.capture().sessionId;
                    check(!nextId.equals(id), "server restart reused the previous telemetry UUID");
                    check(restarted.capture().sessionId.equals(nextId), "new server identity changed between reports");
                }
                check(
                    world.mapStorage.loadData(ProgressionData.class, ProgressionData.NAME) != null,
                    "telemetry replaced the progression record");
            }
        } finally {
            progression.readFromNBT(savedProgression);
            progression.markDirty();
            Config.setTelemetryEnabled(originallyEnabled);
            Config.synchronizeConfiguration(config);
        }
    }

    /** A world that earns a certificate above Steam before it has run 5 hours never reports, and stays that way. */
    private static void verifyYoungWorldIsNotReported(WorldServer world, ServerTelemetry source) throws Exception {
        long previous = world.getTotalWorldTime();
        try {
            world.getWorldInfo()
                .incrementTotalWorldTime(TelemetryEligibility.MINIMUM_WORLD_TICKS - 1);
            resetProgression(world);
            MachineProgression.certify(world, ProgressionTier.STEAM);
            check(source.capture().highestTier.equals("steam"), "Steam in a young world must not block telemetry");
            MachineProgression.certify(world, ProgressionTier.LV);
            boolean refused = false;
            try {
                source.capture();
            } catch (ServerTelemetry.WorldNotEligible expected) {
                refused = true;
            }
            check(refused, "a certificate earned in a young world did not turn telemetry off");
            // The decision is saved with the world and does not expire when the world gets older.
            world.getWorldInfo()
                .incrementTotalWorldTime(TelemetryEligibility.MINIMUM_WORLD_TICKS * 2);
            NBTTagCompound saved = new NBTTagCompound();
            progression(world).writeToNBT(saved);
            ProgressionData reloaded = new ProgressionData();
            reloaded.readFromNBT(saved);
            check(reloaded.isTelemetryBlocked(), "the telemetry decision was not saved with the world");
            try {
                source.capture();
                check(false, "an old world must not undo the decision");
            } catch (ServerTelemetry.WorldNotEligible expected) {
                // Expected.
            }
            // Team tracking follows the same decision.
            verifyTeamTracking(world, true);
            // Exactly five hours is old enough.
            world.getWorldInfo()
                .incrementTotalWorldTime(TelemetryEligibility.MINIMUM_WORLD_TICKS);
            resetProgression(world);
            MachineProgression.certify(world, ProgressionTier.STEAM);
            MachineProgression.certify(world, ProgressionTier.LV);
            check(source.capture().highestTier.equals("lv"), "a world of exactly 5 hours was refused");
            verifyTeamTracking(world, false);
        } finally {
            world.getWorldInfo()
                .incrementTotalWorldTime(previous);
            resetProgression(world);
        }
    }

    private static void verifyTeamTracking(WorldServer world, boolean refused) throws Exception {
        boolean wasEnabled = Config.teamTrackingEnabled;
        Config.enableTeamTracking(
            UUID.randomUUID()
                .toString(),
            UUID.randomUUID()
                .toString(),
            "Tester");
        try (TeamTracking tracking = new TeamTracking(
            MinecraftServer.getServer(),
            new TeamClient(null, new URL("http://127.0.0.1:1"), new SecureRandom()))) {
            boolean blocked = false;
            try {
                tracking.capture();
            } catch (ServerTelemetry.WorldNotEligible expected) {
                blocked = true;
            }
            check(
                blocked == refused,
                refused ? "team tracking reported from a blocked world" : "team tracking was refused");
        } finally {
            if (!wasEnabled) Config.disableTeamTracking();
        }
    }

    private static ProgressionData progression(WorldServer world) {
        return (ProgressionData) world.mapStorage.loadData(ProgressionData.class, ProgressionData.NAME);
    }

    /** Empties the world's certificates and the telemetry decision: the real state has no such reset. */
    private static void resetProgression(WorldServer world) {
        ProgressionData data = progression(world);
        if (data == null) return;
        data.readFromNBT(new NBTTagCompound());
        data.markDirty();
    }

    private static void check(boolean result, String message) {
        if (!result) throw new AssertionError(message);
    }
}
