package pl.kuba6000.gtnhextranerf.client.nei;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import net.minecraft.client.resources.I18n;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.NetworkManager;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import pl.kuba6000.gtnhextranerf.loot.modules.MeControllerLootModule;

/** Client display state is unknown until a snapshot arrives from the current server. */
public final class LootAllowancePreview {

    public static final LootAllowancePreview INSTANCE = new LootAllowancePreview();
    private static final AtomicReference<Snapshot> STATE = new AtomicReference<>(new Snapshot(null, -1));

    private LootAllowancePreview() {}

    @SubscribeEvent
    public void connected(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        STATE.set(new Snapshot(event.manager, -1));
    }

    @SubscribeEvent
    public void disconnected(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        STATE.updateAndGet(previous -> previous.connection == event.manager ? new Snapshot(null, -1) : previous);
    }

    public static void receive(NetworkManager connection, int value) {
        if (connection == null) return;
        int valid = value >= 0 && value <= MeControllerLootModule.GLOBAL_ALLOWANCE ? value : -1;
        STATE.updateAndGet(previous -> previous.connection == connection ? new Snapshot(connection, valid) : previous);
    }

    public static boolean isController(ItemStack stack) {
        return stack != null && stack.getItemDamage() == MeControllerLootModule.ITEM.metadata()
            && MeControllerLootModule.ITEM.registryName()
                .equals(Item.itemRegistry.getNameForObject(stack.getItem()));
    }

    public static List<String> tooltip() {
        int snapshot = STATE.get().remaining;
        List<String> lines = new ArrayList<>();
        lines.add(I18n.format("gtnhextranerf.loot.preview.amount", 1));
        if (snapshot < 0) lines.add(I18n.format("gtnhextranerf.loot.preview.unknown"));
        else {
            lines.add(
                I18n.format("gtnhextranerf.loot.preview.remaining", snapshot, MeControllerLootModule.GLOBAL_ALLOWANCE));
            if (snapshot == 0) lines.add(I18n.format("gtnhextranerf.loot.preview.exhausted"));
            lines.add(I18n.format("gtnhextranerf.loot.preview.last_update"));
        }
        lines.add(I18n.format("gtnhextranerf.loot.preview.conditions"));
        return lines;
    }

    private static final class Snapshot {

        final NetworkManager connection;
        final int remaining;

        Snapshot(NetworkManager connection, int remaining) {
            this.connection = connection;
            this.remaining = remaining;
        }
    }
}
