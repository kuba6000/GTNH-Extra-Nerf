package pl.kuba6000.gtnhextranerf.integration;

import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.ItemInWorldManager;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

import com.mojang.authlib.GameProfile;

import pl.kuba6000.gtnhextranerf.Config;
import pl.kuba6000.gtnhextranerf.teams.TeamCommand;
import pl.kuba6000.gtnhextranerf.teams.TeamTracking;
import pl.kuba6000.gtnhextranerf.telemetry.TelemetryConsent;

/** Exercises leader selection through the team command on a running server. */
final class TeamLeaderIntegrationTests {

    private TeamLeaderIntegrationTests() {}

    static void verify(MinecraftServer server) {
        WorldServer world = DimensionManager.getWorld(0);
        TelemetryConsent consent = (TelemetryConsent) world.mapStorage
            .loadData(TelemetryConsent.class, TelemetryConsent.NAME);
        if (consent == null) throw new AssertionError("World consent record is missing");
        NBTTagCompound saved = new NBTTagCompound();
        consent.writeToNBT(saved);
        RecordingPlayer owner = new RecordingPlayer(server, world, "LeaderOwner");
        RecordingPlayer guest = new RecordingPlayer(server, world, "LeaderGuest");
        try {
            TeamTracking.stop();
            TeamTracking.initialize(null, true);
            Config.disableTeamTracking();
            server.getConfigurationManager().playerEntityList.add(owner);
            server.getConfigurationManager().playerEntityList.add(guest);
            TeamCommand command = new TeamCommand();

            command.processCommand(server, new String[] { "on" });
            check(!Config.teamTrackingEnabled, "The console enabled team tracking without specifying a leader");
            command.processCommand(server, new String[] { "on", "NotOnline" });
            check(!Config.teamTrackingEnabled, "The console selected a player who is not online");
            check(owner.messages == 0 && guest.messages == 0, "A failed command notified connected players");

            command.processCommand(server, new String[] { "on", guest.getCommandSenderName() });
            check(Config.teamTrackingEnabled, "The console could not choose an online leader");
            check(
                Config.teamLeaderId.equals(
                    guest.getGameProfile()
                        .getId()
                        .toString()),
                "The chosen leader was ignored");
            command.processCommand(server, new String[] { "on" });
            check(
                Config.teamLeaderId.equals(
                    guest.getGameProfile()
                        .getId()
                        .toString()),
                "A missing name changed the leader");

            command.processCommand(owner, new String[] { "on" });
            check(
                Config.teamLeaderId.equals(
                    owner.getGameProfile()
                        .getId()
                        .toString()),
                "The player was not the leader");
            command.processCommand(owner, new String[] { "on", "leaderguest" });
            check(
                Config.teamLeaderId.equals(
                    guest.getGameProfile()
                        .getId()
                        .toString()),
                "The named player was not the leader");
        } finally {
            server.getConfigurationManager().playerEntityList.remove(owner);
            server.getConfigurationManager().playerEntityList.remove(guest);
            TeamTracking.stop();
            consent.readFromNBT(saved);
            consent.markDirty();
            Config.loadWorld();
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final class RecordingPlayer extends EntityPlayerMP {

        private int messages;

        private RecordingPlayer(MinecraftServer server, WorldServer world, String name) {
            super(server, world, new GameProfile(UUID.randomUUID(), name), new ItemInWorldManager(world));
        }

        @Override
        public void addChatMessage(IChatComponent message) {
            messages++;
        }
    }
}
