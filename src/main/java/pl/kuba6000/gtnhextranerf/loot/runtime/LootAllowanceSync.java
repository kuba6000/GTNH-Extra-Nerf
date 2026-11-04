package pl.kuba6000.gtnhextranerf.loot.runtime;

import net.minecraft.entity.player.EntityPlayerMP;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;
import io.netty.buffer.ByteBuf;
import pl.kuba6000.gtnhextranerf.GTNHExtraNerf;
import pl.kuba6000.gtnhextranerf.loot.modules.MeControllerLootModule;

/** Sends the server's save-scoped controller allowance without changing ELB's own packets. */
public final class LootAllowanceSync {

    private static SimpleNetworkWrapper channel;

    private LootAllowanceSync() {}

    public static void initialize() {
        channel = NetworkRegistry.INSTANCE.newSimpleChannel("gtnhextra_loot");
        channel.registerMessage(Handler.class, Message.class, 0, Side.CLIENT);
        FMLCommonHandler.instance()
            .bus()
            .register(new LootAllowanceSync());
    }

    @SubscribeEvent
    public void playerJoined(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.player instanceof EntityPlayerMP)) return;
        try {
            channel.sendTo(new Message(ServerLootRuntime.controllerAllowance()), (EntityPlayerMP) event.player);
        } catch (RuntimeException failure) {
            GTNHExtraNerf.LOG.warn("Cannot send loot allowance to joining player", failure);
        }
    }

    public static void broadcast() {
        if (channel == null) return;
        try {
            channel.sendToAll(new Message(ServerLootRuntime.controllerAllowance()));
        } catch (RuntimeException failure) {
            // A failed notification must never undo or disrupt a durable loot claim.
            GTNHExtraNerf.LOG.warn("Cannot broadcast loot allowance", failure);
        }
    }

    public static final class Message implements IMessage {

        private int remaining;

        public Message() {}

        private Message(int remaining) {
            this.remaining = remaining;
        }

        @Override
        public void fromBytes(ByteBuf buffer) {
            int value = buffer.readByte();
            remaining = value >= 0 && value <= MeControllerLootModule.GLOBAL_ALLOWANCE ? value : -1;
        }

        @Override
        public void toBytes(ByteBuf buffer) {
            buffer.writeByte(remaining);
        }
    }

    public static final class Handler implements IMessageHandler<Message, IMessage> {

        @Override
        public IMessage onMessage(Message message, MessageContext context) {
            GTNHExtraNerf.proxy.receiveControllerAllowance(message.remaining, context);
            return null;
        }
    }
}
