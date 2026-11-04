package pl.kuba6000.gtnhextranerf.telemetry;

import java.io.IOException;
import java.io.UncheckedIOException;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentTranslation;

import com.cleanroommc.modularui.factory.ClientGUI;
import com.cleanroommc.modularui.value.sync.SyncHandler;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import pl.kuba6000.gtnhextranerf.GTNHExtraNerf;
import pl.kuba6000.gtnhextranerf.progression.ProgressionServerTasks;
import pl.kuba6000.gtnhextranerf.teams.TeamTracking;

/** Carries the choice from the consent window to the server, which checks it and applies it. */
public final class ConsentSyncHandler extends SyncHandler<ConsentSyncHandler> {

    public static final String SYNC_ID = "gtnhextranerf.consent";
    private static final int CHOOSE = 0;
    private static final int CLOSE = 1;

    private final EntityPlayerMP serverPlayer;

    public ConsentSyncHandler() {
        allowC2S();
        serverPlayer = null;
    }

    public ConsentSyncHandler(EntityPlayerMP player) {
        allowC2S();
        serverPlayer = player;
    }

    @SideOnly(Side.CLIENT)
    public void choose(int choice) {
        syncToServer(CHOOSE, buffer -> buffer.writeByte(choice));
    }

    @Override
    public void readOnServer(int id, PacketBuffer buffer) throws IOException {
        if (serverPlayer == null || id != CHOOSE) return;
        int choice = buffer.readUnsignedByte();
        ProgressionServerTasks.enqueue(() -> {
            if (!isValid() || getSyncManager().getModularSyncManager()
                .isClosed()
                || serverPlayer.openContainer != getSyncManager().getModularSyncManager()
                    .getContainer())
                return;
            try {
                ConsentPrompt.answer(MinecraftServer.getServer(), serverPlayer, choice);
            } catch (UncheckedIOException failure) {
                ServerTelemetry.stop();
                TeamTracking.stop();
                GTNHExtraNerf.LOG.error("Could not save the reporting choice; reporting is stopped", failure);
                serverPlayer.addChatMessage(new ChatComponentTranslation("gtnhextranerf.telemetry.save_failed"));
            } finally {
                // The window goes away whatever happened, also when someone else answered first.
                syncToClient(CLOSE);
            }
        });
    }

    @Override
    public void readOnClient(int id, PacketBuffer buffer) {
        if (id == CLOSE) closeWindow();
    }

    @SideOnly(Side.CLIENT)
    private static void closeWindow() {
        ClientGUI.close();
    }
}
