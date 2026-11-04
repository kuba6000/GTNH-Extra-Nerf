package pl.kuba6000.gtnhextranerf.integration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.event.ClickEvent;
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

/** Exercises the command and the messages received by players already on a server. */
final class TeamNoticeIntegrationTests {

    private TeamNoticeIntegrationTests() {}

    static void verify(MinecraftServer server) {
        WorldServer world = DimensionManager.getWorld(0);
        TelemetryConsent consent = (TelemetryConsent) world.mapStorage
            .loadData(TelemetryConsent.class, TelemetryConsent.NAME);
        if (consent == null) throw new AssertionError("World consent record is missing");
        NBTTagCompound saved = new NBTTagCompound();
        consent.writeToNBT(saved);
        RecordingPlayer owner = new RecordingPlayer(server, world, "NoticeOwner");
        RecordingPlayer guest = new RecordingPlayer(server, world, "NoticeGuest");
        try {
            TeamTracking.stop();
            TeamTracking.initialize(null, true);
            Config.disableTeamTracking();
            server.getConfigurationManager().playerEntityList.add(owner);
            server.getConfigurationManager().playerEntityList.add(guest);

            new TeamCommand().processCommand(owner, new String[] { "on" });
            check(Config.teamTrackingEnabled, "The team tracking command did not enable reporting");
            check(owner.hasPolicyNotice(), "The player enabling team tracking did not receive the notice");
            check(guest.hasPolicyNotice(), "A connected player did not receive the notice");
            check(
                owner.hasObjectionNotice(),
                "The player enabling team tracking was not told about the right to object");
            check(guest.hasObjectionNotice(), "A connected player was not told about the right to object");
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

        private final List<IChatComponent> messages = new ArrayList<>();

        private RecordingPlayer(MinecraftServer server, WorldServer world, String name) {
            super(server, world, new GameProfile(UUID.randomUUID(), name), new ItemInWorldManager(world));
        }

        @Override
        public void addChatMessage(IChatComponent message) {
            messages.add(message);
        }

        private boolean hasPolicyNotice() {
            for (IChatComponent message : messages) {
                String text = message.getUnformattedText();
                if (text.contains(TeamTracking.POLICY_URL) && text.toLowerCase(Locale.ROOT)
                    .contains("team tracking") && hasClickablePolicy(message)) return true;
            }
            return false;
        }

        private boolean hasObjectionNotice() {
            for (IChatComponent message : messages) {
                String text = message.getUnformattedText();
                if (text.contains(TeamTracking.POLICY_URL) && text.toLowerCase(Locale.ROOT)
                    .contains("object") && hasClickablePolicy(message)) return true;
            }
            return false;
        }

        private boolean hasClickablePolicy(IChatComponent message) {
            for (Object entry : message) {
                IChatComponent part = (IChatComponent) entry;
                ClickEvent click = part.getChatStyle()
                    .getChatClickEvent();
                if (click != null && click.getAction() == ClickEvent.Action.OPEN_URL
                    && click.getValue()
                        .startsWith(TeamTracking.POLICY_URL))
                    return true;
            }
            return false;
        }
    }
}
