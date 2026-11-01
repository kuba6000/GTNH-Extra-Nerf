package pl.kuba6000.gtnhextranerf.teams;

import java.util.List;
import java.util.UUID;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;

import pl.kuba6000.gtnhextranerf.Config;
import pl.kuba6000.gtnhextranerf.telemetry.TelemetryCommand;

/** Owner-only opt-in for team tracking. Separate from /extranerftelemetry and never sends anything synchronously. */
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
        String action = arguments.length == 0 ? "status" : arguments[0];
        if ("status".equals(action)) {
            reply(sender, TeamTracking.status());
        } else if ("on".equals(action)) {
            enable(sender);
        } else if ("off".equals(action)) {
            Config.disableTeamTracking();
            TeamTracking.stop();
            reply(sender, TeamTracking.status());
            // The operator needs the ID to ask for deletion of the data that was already sent.
            if (!Config.teamId.isEmpty()) reply(sender, TeamTracking.removalNotice(Config.teamId));
        } else {
            reply(sender, new ChatComponentTranslation(getCommandUsage(sender)));
        }
    }

    private void enable(ICommandSender sender) {
        MinecraftServer server = MinecraftServer.getServer();
        TeamTracking.Member commandSender = null;
        if (sender instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP) sender;
            UUID id = player.getGameProfile()
                .getId();
            if (id != null && TeamTracking.Member.validName(player.getCommandSenderName()))
                commandSender = new TeamTracking.Member(id, player.getCommandSenderName());
        }
        TeamTracking.Member leader = TeamTracking.chooseLeader(commandSender, TeamTracking.onlinePlayers(server));
        if (leader == null) {
            reply(sender, new ChatComponentTranslation("gtnhextranerf.team.needs_player"));
            return;
        }
        // The ID is created once and then kept, also across opting out and in again.
        String teamId = isUuid(Config.teamId) ? Config.teamId
            : UUID.randomUUID()
                .toString();
        Config.enableTeamTracking(teamId, leader.id.toString(), leader.name);
        TeamTracking.start(server);
        reply(sender, TeamTracking.status());
    }

    private static boolean isUuid(String value) {
        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException invalid) {
            return false;
        }
    }

    private static void reply(ICommandSender sender, IChatComponent message) {
        sender.addChatMessage(message);
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] arguments) {
        return arguments.length == 1 ? getListOfStringsMatchingLastWord(arguments, "status", "on", "off") : null;
    }
}
