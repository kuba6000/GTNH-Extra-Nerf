package pl.kuba6000.gtnhextranerf.telemetry;

import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentTranslation;

import pl.kuba6000.gtnhextranerf.Config;

/** Owner-only diagnostic and opt-out command; no synchronous network operation. */
// Declare the interface explicitly: RFG must remap its inherited abstract methods in runtime JARs.
public final class TelemetryCommand extends CommandBase implements ICommand {

    @Override
    public String getCommandName() {
        return "extranerftelemetry";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "gtnhextranerf.telemetry.usage";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 4;
    }

    /** Like vanilla /seed, singleplayer needs no cheats; the owner check keeps LAN guests out. */
    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        if (super.canCommandSenderUseCommand(sender)) return true;
        MinecraftServer server = MinecraftServer.getServer();
        return server != null && sender instanceof EntityPlayerMP
            && isIntegratedOwner(server.isDedicatedServer(), server.getServerOwner(), sender.getCommandSenderName());
    }

    public static boolean isIntegratedOwner(boolean dedicated, String owner, String sender) {
        return !dedicated && owner != null && owner.equalsIgnoreCase(sender);
    }

    @Override
    public void processCommand(ICommandSender sender, String[] arguments) {
        String action = arguments.length == 0 ? "status" : arguments[0];
        if ("status".equals(action)) {
            sender.addChatMessage(ServerTelemetry.status());
        } else if ("off".equals(action)) {
            Config.setTelemetryEnabled(false);
            ServerTelemetry.stop();
            sender.addChatMessage(new ChatComponentTranslation("gtnhextranerf.telemetry.off"));
        } else if ("on".equals(action)) {
            Config.setTelemetryEnabled(true);
            ServerTelemetry.start(MinecraftServer.getServer());
            sender.addChatMessage(ServerTelemetry.status());
        } else {
            sender.addChatMessage(new ChatComponentTranslation(getCommandUsage(sender)));
        }
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] arguments) {
        return arguments.length == 1 ? getListOfStringsMatchingLastWord(arguments, "status", "off", "on") : null;
    }
}
