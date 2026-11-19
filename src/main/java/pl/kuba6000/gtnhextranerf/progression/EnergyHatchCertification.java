package pl.kuba6000.gtnhextranerf.progression;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.event.ClickEvent;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.GregTechAPI;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatchDynamo;
import gregtech.api.metatileentity.implementations.MTEHatchEnergy;
import gregtech.api.metatileentity.implementations.MTEWirelessEnergy;
import gregtech.common.covers.CoverEnergyWireless;
import pl.kuba6000.gtnhextranerf.GTNHExtraNerf;
import pl.kuba6000.gtnhextranerf.audit.AuditManager;
import pl.kuba6000.gtnhextranerf.audit.AuditScope;
import tectech.thing.metaTileEntity.hatch.MTEHatchEnergyMulti;
import tectech.thing.metaTileEntity.hatch.MTEHatchWirelessMulti;

/** Hatch gates share world certification; discovery never depends on a controller's GUI. */
public final class EnergyHatchCertification {

    public static final String OPEN_COMMAND = "extranerf-certificate";

    public interface PortHook {
    }

    public interface BufferHook {
    }

    public interface WirelessInputHook {
    }

    public static boolean isEnergyHatch(IMetaTileEntity machine) {
        return machine instanceof MTEHatchEnergy || machine instanceof MTEHatchDynamo
            || machine instanceof MTEHatchEnergyMulti;
    }

    public static boolean isLocked(IMetaTileEntity machine) {
        return isEnergyHatch(machine) && MachineProgression.isLocked((MetaTileEntity) machine);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPlaced(BlockEvent.PlaceEvent event) {
        if (event.world.isRemote || event.player instanceof FakePlayer) return;
        // Wait for the placement transaction and other listeners to finish before inspecting the new tile.
        ProgressionServerTasks
            .enqueue(() -> { if (!event.isCanceled()) warn(event.player, event.world, event.x, event.y, event.z); });
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent event) {
        if (event.world.isRemote || event.entityPlayer instanceof FakePlayer
            || event.action != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK
            || event.entityPlayer.getHeldItem() != null) return;
        ProgressionServerTasks.enqueue(
            () -> { if (!event.isCanceled()) warn(event.entityPlayer, event.world, event.x, event.y, event.z); });
    }

    private static void warn(EntityPlayer player, World world, int x, int y, int z) {
        TileEntity placed = world.getTileEntity(x, y, z);
        if (!(placed instanceof BaseMetaTileEntity)) return;
        IMetaTileEntity machine = ((BaseMetaTileEntity) placed).getMetaTileEntity();
        if (!isEnergyHatch(machine)) return;
        ProgressionTier missing = MachineProgression.missingTier((MetaTileEntity) machine);
        if (missing == null) return;
        ChatComponentTranslation message = new ChatComponentTranslation(
            "gtnhextranerf.progression.hatch.disabled",
            machine.getLocalName(),
            new ChatComponentTranslation("item.gtnhextranerf." + missing.id() + "_certificate.name"));
        message.getChatStyle()
            .setColor(EnumChatFormatting.RED);
        ChatComponentTranslation link = new ChatComponentTranslation("gtnhextranerf.progression.hatch.open");
        link.getChatStyle()
            .setColor(EnumChatFormatting.AQUA)
            .setUnderlined(true)
            .setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + OPEN_COMMAND + " " + missing.id()));
        player.addChatMessage(
            message.appendText(" ")
                .appendSibling(link));
    }

    public static void verify() {
        AuditManager.run(AuditScope.VERIFY_ENERGY_HATCH_CERTIFICATION, audit -> {
            if (!PortHook.class.isAssignableFrom(BaseMetaTileEntity.class)
                || !BufferHook.class.isAssignableFrom(MetaTileEntity.class)
                || !WirelessInputHook.class.isAssignableFrom(CoverEnergyWireless.class)
                || !WirelessInputHook.class.isAssignableFrom(MTEWirelessEnergy.class)
                || !WirelessInputHook.class.isAssignableFrom(MTEHatchWirelessMulti.class))
                throw new IllegalStateException("Energy hatch certification hooks are missing");
            int hatches = 0;
            for (IMetaTileEntity prototype : GregTechAPI.METATILEENTITIES) if (isEnergyHatch(prototype)) hatches++;
            if (hatches == 0) throw new IllegalStateException("No native energy or dynamo hatches registered");
            audit.success();
        });
    }

    /** Chat links navigate on the client; payments and unlocks still use the existing server-authoritative UI. */
    @SideOnly(Side.CLIENT)
    public static final class OpenCertificateCommand extends CommandBase {

        @Override
        public String getCommandName() {
            return OPEN_COMMAND;
        }

        @Override
        public String getCommandUsage(ICommandSender sender) {
            return "/" + OPEN_COMMAND + " <tier>";
        }

        @Override
        public int getRequiredPermissionLevel() {
            return 0;
        }

        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            if (args.length == 1) for (ProgressionTier tier : ProgressionTier.values()) if (tier.id()
                .equals(args[0])) {
                    GTNHExtraNerf.proxy.showCertificate(tier);
                    return;
                }
            throw new WrongUsageException(getCommandUsage(sender));
        }
    }
}
