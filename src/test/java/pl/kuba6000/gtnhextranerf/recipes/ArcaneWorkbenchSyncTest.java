package pl.kuba6000.gtnhextranerf.recipes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import cpw.mods.fml.common.gameevent.TickEvent;
import pl.kuba6000.gtnhextranerf.recipes.crafting.ArcaneWorkbenchSync;

class ArcaneWorkbenchSyncTest {

    @AfterEach
    void clear() {
        ArcaneWorkbenchSync.reset();
    }

    @Test
    void sendsOneSnapshotAfterClickProcessingAtTickEnd() {
        RecordingPlayer player = new ObjenesisStd().newInstance(RecordingPlayer.class);
        player.openContainer = container();
        ArcaneWorkbenchSync.request(player, player.openContainer);
        ArcaneWorkbenchSync.request(player, player.openContainer);
        assertEquals(0, player.snapshots);
        ArcaneWorkbenchSync.INSTANCE.afterTick(new TickEvent.ServerTickEvent(TickEvent.Phase.START));
        assertEquals(0, player.snapshots);
        ArcaneWorkbenchSync.INSTANCE.afterTick(new TickEvent.ServerTickEvent(TickEvent.Phase.END));
        assertEquals(1, player.snapshots);
        ArcaneWorkbenchSync.INSTANCE.afterTick(new TickEvent.ServerTickEvent(TickEvent.Phase.END));
        assertEquals(1, player.snapshots);
    }

    @Test
    void closingTheWindowOrStoppingTheServerDiscardsPendingSnapshots() {
        RecordingPlayer player = new ObjenesisStd().newInstance(RecordingPlayer.class);
        player.openContainer = container();
        ArcaneWorkbenchSync.request(player, player.openContainer);
        player.openContainer = container();
        ArcaneWorkbenchSync.INSTANCE.afterTick(new TickEvent.ServerTickEvent(TickEvent.Phase.END));
        assertEquals(0, player.snapshots);
        ArcaneWorkbenchSync.request(player, player.openContainer);
        ArcaneWorkbenchSync.reset();
        ArcaneWorkbenchSync.INSTANCE.afterTick(new TickEvent.ServerTickEvent(TickEvent.Phase.END));
        assertEquals(0, player.snapshots);
    }

    private static Container container() {
        return new Container() {

            @Override
            public boolean canInteractWith(EntityPlayer player) {
                return true;
            }
        };
    }

    private static final class RecordingPlayer extends EntityPlayerMP {

        private int snapshots;

        private RecordingPlayer() {
            super(null, null, null, null);
        }

        @Override
        public void sendContainerToPlayer(Container container) {
            snapshots++;
        }
    }
}
