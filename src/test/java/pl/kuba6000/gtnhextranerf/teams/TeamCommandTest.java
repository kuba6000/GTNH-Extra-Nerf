package pl.kuba6000.gtnhextranerf.teams;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.SaveHandler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import pl.kuba6000.gtnhextranerf.Config;
import pl.kuba6000.gtnhextranerf.LangFile;
import pl.kuba6000.gtnhextranerf.telemetry.ServerTelemetry;
import pl.kuba6000.gtnhextranerf.telemetry.TelemetryCommand;
import pl.kuba6000.gtnhextranerf.telemetry.TelemetryConsent;

class TeamCommandTest {

    private static final Pattern UUID_IN_CHAT = Pattern
        .compile("(?i)\\b[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\b");

    @Test
    void telemetryStatusDistinguishesAnUnansweredWorldFromEnabledAndDisabledReporting(@TempDir Path directory)
        throws Exception {
        SaveHandler save = new SaveHandler(directory.toFile(), "world", false);
        Config.loadWorld(save, new MapStorage(save), TelemetryConsent.Legacy.NONE);
        try {
            ServerTelemetry.stop();
            Sender sender = new Sender();
            TelemetryCommand command = new TelemetryCommand();
            command.processCommand(sender, new String[] { "status" });
            String unanswered = sender.render();

            Config.setTelemetryEnabled(false);
            sender.messages.clear();
            command.processCommand(sender, new String[] { "status" });
            String off = sender.render();
            assertNotEquals(unanswered, off);

            Config.setTelemetryEnabled(true);
            sender.messages.clear();
            command.processCommand(sender, new String[] { "status" });
            assertNotEquals(off, sender.render());
        } finally {
            ServerTelemetry.stop();
            Config.unloadWorld();
        }
    }

    @Test
    void statusAndOptOutKeepTheTeamIdOutOfChatAndExplainWhereToFindIt(@TempDir Path directory) throws Exception {
        String teamId = "5e9e2e20-9db0-4b4b-9d0a-1a6a8f5a7b11";
        SaveHandler save = new SaveHandler(directory.toFile(), "world", false);
        Config.loadWorld(save, new MapStorage(save), TelemetryConsent.Legacy.NONE);
        try {
            Config.enableTeamTracking(teamId, "0b1c43f9-77a0-4c7b-b3c3-5a1d6a1f3e22", "Kuba");
            TeamTracking.stop();
            Sender sender = new Sender();
            TeamCommand command = new TeamCommand();
            command.processCommand(sender, new String[] { "status" });
            assertFalse(
                sender.render()
                    .contains(teamId));
            assertFalse(
                UUID_IN_CHAT.matcher(sender.render())
                    .find());

            sender.messages.clear();
            command.processCommand(sender, new String[] { "off" });
            String reply = sender.render();
            assertFalse(reply.contains(teamId));
            assertFalse(
                UUID_IN_CHAT.matcher(reply)
                    .find());
            assertTrue(reply.contains("gtnhextranerf-team-id.txt"));
            assertTrue(reply.contains("extranerf@kuba6000.pl"));
        } finally {
            TeamTracking.stop();
            Config.unloadWorld();
        }
    }

    private static final class Sender implements ICommandSender {

        private final List<IChatComponent> messages = new ArrayList<>();

        String render() throws Exception {
            StringBuilder text = new StringBuilder();
            for (IChatComponent message : messages) text.append(LangFile.render(message))
                .append('\n');
            return text.toString();
        }

        @Override
        public String getCommandSenderName() {
            return "Tester";
        }

        @Override
        public IChatComponent func_145748_c_() {
            return new ChatComponentText(getCommandSenderName());
        }

        @Override
        public void addChatMessage(IChatComponent message) {
            messages.add(message);
        }

        @Override
        public boolean canCommandSenderUseCommand(int level, String command) {
            return true;
        }

        @Override
        public ChunkCoordinates getPlayerCoordinates() {
            return new ChunkCoordinates(0, 0, 0);
        }

        @Override
        public World getEntityWorld() {
            return null;
        }
    }
}
