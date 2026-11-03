package pl.kuba6000.gtnhextranerf.recipes.crafting;

import java.util.IdentityHashMap;
import java.util.Map;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/** Sends final workbench inventories after vanilla finishes its click transaction. */
public final class ArcaneWorkbenchSync {

    public static final ArcaneWorkbenchSync INSTANCE = new ArcaneWorkbenchSync();
    private final Map<EntityPlayerMP, Container> pending = new IdentityHashMap<>();

    private ArcaneWorkbenchSync() {}

    public static void request(EntityPlayerMP player, Container container) {
        INSTANCE.pending.put(player, container);
    }

    @SubscribeEvent
    public void afterTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        for (Map.Entry<EntityPlayerMP, Container> entry : pending.entrySet()) {
            EntityPlayerMP player = entry.getKey();
            if (player.openContainer == entry.getValue()) {
                player.sendContainerToPlayer(entry.getValue());
            }
        }
        pending.clear();
    }

    public static void reset() {
        INSTANCE.pending.clear();
    }
}
