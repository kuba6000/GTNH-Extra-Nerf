package pl.kuba6000.gtnhextranerf.teams;

import java.io.UncheckedIOException;
import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;

import pl.kuba6000.gtnhextranerf.Config;
import pl.kuba6000.gtnhextranerf.GTNHExtraNerf;
import pl.kuba6000.gtnhextranerf.telemetry.ServerTelemetry;
import pl.kuba6000.gtnhextranerf.telemetry.TelemetryCommand;

/** Owner-only team reporting command; it selects an alternative to minimal telemetry. */
// Declare the interface explicitly: RFG must remap its inherited abstract methods in runtime JARs.
public final class TeamCommand extends CommandBase implements ICommand {

    @Override
    public String getCommandName() {
        return "extranerfteam";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "gtnhextranerf.team.usage";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 4;
    }

    /** Operators and the console may opt in; in singleplayer the world owner may, without cheats. */
    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        if (super.canCommandSenderUseCommand(sender)) return true;
        MinecraftServer server = MinecraftServer.getServer();
        return server != null && sender instanceof EntityPlayerMP
            && TelemetryCommand
                .isIntegratedOwner(server.isDedicatedServer(), server.getServerOwner(), sender.getCommandSenderName());
    }

    @Override
    public void processCommand(ICommandSender sender, String[] arguments) {
        if (!Config.worldAvailable()) {
            sender.addChatMessage(new ChatComponentTranslation("gtnhextranerf.telemetry.save_failed"));
            return;
        }
        try {
            processAction(sender, arguments);
        } catch (UncheckedIOException failure) {
            ServerTelemetry.stop();
            TeamTracking.stop();
            GTNHExtraNerf.LOG.error("Could not save the reporting choice; reporting is stopped", failure);
            sender.addChatMessage(new ChatComponentTranslation("gtnhextranerf.telemetry.save_failed"));
        }
    }

    private void processAction(ICommandSender sender, String[] arguments) {
        String action = arguments.length == 0 ? "status" : arguments[0];
        if ("status".equals(action)) {
            reply(sender, TeamTracking.status());
        } else if ("on".equals(action)) {
            enable(sender, arguments);
        } else if ("off".equals(action)) {
            Config.disableTeamTracking();
            TeamTracking.stop();
            // The operator needs the ID to ask for deletion of the data that was already sent.
            reply(sender, Config.teamId.isEmpty() ? TeamTracking.status() : TeamTracking.removalNotice());
        } else {
            reply(sender, new ChatComponentTranslation(getCommandUsage(sender)));
        }
    }

    private void enable(ICommandSender sender, String[] arguments) {
        if (arguments.length > 2) {
            reply(sender, new ChatComponentTranslation(getCommandUsage(sender)));
            return;
        }
        MinecraftServer server = MinecraftServer.getServer();
        EntityPlayerMP player;
        if (arguments.length == 2) {
            player = server.getConfigurationManager()
                .func_152612_a(arguments[1]);
            if (player == null) {
                reply(sender, new ChatComponentTranslation("gtnhextranerf.team.player_not_online", arguments[1]));
                return;
            }
        } else if (sender instanceof EntityPlayerMP) {
            player = (EntityPlayerMP) sender;
        } else {
            reply(sender, new ChatComponentTranslation("gtnhextranerf.team.leader_required"));
            return;
        }
        if (!TeamTracking.enable(server, player)) {
            reply(sender, new ChatComponentTranslation("gtnhextranerf.team.needs_player"));
            return;
        }
        reply(sender, TeamTracking.status());
    }

    private static void reply(ICommandSender sender, IChatComponent message) {
        sender.addChatMessage(message);
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] arguments) {
        if (arguments.length == 1) return getListOfStringsMatchingLastWord(arguments, "status", "on", "off");
        MinecraftServer server = MinecraftServer.getServer();
        if (arguments.length == 2 && "on".equals(arguments[0]) && server != null)
            return getListOfStringsMatchingLastWord(arguments, server.getAllUsernames());
        return null;
    }
}
