package pl.kuba6000.gtnhextranerf.integration;

import java.util.UUID;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.NetworkManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.ItemInWorldManager;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.util.ForgeDirection;

import com.cleanroommc.modularui.api.RecipeViewerSettings;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.ModularSyncManager;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.mojang.authlib.GameProfile;

import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.metatileentity.BaseMetaPipeEntity;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEBasicMachine;
import gregtech.api.metatileentity.implementations.MTEBasicMachineBronze;
import gregtech.api.metatileentity.implementations.MTEFluidPipe;
import gregtech.api.util.GTOreDictUnificator;
import io.netty.channel.embedded.EmbeddedChannel;
import pl.kuba6000.gtnhextranerf.progression.CertificateStatusPanel;
import pl.kuba6000.gtnhextranerf.progression.MachineProgression;
import pl.kuba6000.gtnhextranerf.progression.ProgressionCertificates;
import pl.kuba6000.gtnhextranerf.progression.ProgressionServerTasks;
import pl.kuba6000.gtnhextranerf.progression.ProgressionTier;

/** Real native damage and processing on a disposable dedicated server. */
final class SteamIntegrationTests {

    private SteamIntegrationTests() {}

    static void verify(WorldServer world) {
        // Forge FakePlayer ignores damage, so use a server player with an in-memory connection.
        EntityPlayerMP player = new EntityPlayerMP(
            MinecraftServer.getServer(),
            world,
            new GameProfile(UUID.randomUUID(), "PressureTest"),
            new ItemInWorldManager(world));
        NetworkManager network = new NetworkManager(false);
        EmbeddedChannel channel = new EmbeddedChannel(network);
        player.playerNetServerHandler = new NetHandlerPlayServer(MinecraftServer.getServer(), network, player);
        // Let vanilla's initial spawn protection expire through normal player ticks.
        for (int tick = 0; tick < 61; tick++) player.onUpdate();
        player.capabilities.isCreativeMode = false;
        player.capabilities.disableDamage = false;
        BaseMetaTileEntity bronze = ProgressionIntegrationTests.furnace(world, ItemList.Machine_Bronze_Furnace, 32);
        BaseMetaTileEntity steel = ProgressionIntegrationTests.furnace(world, ItemList.Machine_HP_Furnace, 33);
        BaseMetaTileEntity electric = ProgressionIntegrationTests.furnace(world, ItemList.Machine_MV_E_Furnace, 34);
        MTEBasicMachine steam = (MTEBasicMachine) bronze.getMetaTileEntity();
        check(!MachineProgression.isLocked(steam), "Steam certificate did not cover Bronze machines");
        check(
            !MachineProgression.isLocked((MTEBasicMachine) steel.getMetaTileEntity()),
            "Steam certificate did not cover Steel machines");
        PanelSyncManager guiSync = new PanelSyncManager(new ModularSyncManager(false), false);
        ((MTEBasicMachineBronze) steam).buildUI(
            new PosGuiData(player, bronze.xCoord, bronze.yCoord, bronze.zCoord),
            guiSync,
            new UISettings(RecipeViewerSettings.DUMMY));
        IntSyncValue status = (IntSyncValue) guiSync.findSyncHandlerNullable(CertificateStatusPanel.SYNC_ID, 0);
        check(
            status != null && status.getIntValue() == 0,
            "actual Bronze MUI2 GUI did not register certificate status");
        ItemStack spare = ProgressionCertificates.issue(world, ProgressionTier.STEAM);

        ItemStack stack = GTOreDictUnificator.get(OrePrefixes.pipeTiny, Materials.Steel, 1);
        check(
            ((ItemBlock) stack.getItem()).placeBlockAt(stack, player, world, 36, 240, 8, 1, .5f, .5f, .5f, 0),
            "could not place damage-test pipe");
        BaseMetaPipeEntity tile = (BaseMetaPipeEntity) world.getTileEntity(36, 240, 8);
        MTEFluidPipe pipe = (MTEFluidPipe) tile.getMetaTileEntity();
        pipe.mFluids[0] = Materials.Steam.getGas(1_000);
        player.setPosition(36.5, 240, 8.5);
        player.capabilities.disableDamage = true;
        pipe.onEntityCollidedWithBlock(world, 36, 240, 8, player);
        drain();
        check(!MachineProgression.isLocked(steam), "protected player damage attempt revoked certification");
        player.capabilities.disableDamage = false;
        EntityPig pig = new EntityPig(world);
        pipe.onEntityCollidedWithBlock(world, 36, 240, 8, pig);
        drain();
        check(!MachineProgression.isLocked(steam), "mob damage revoked certification");
        player.hurtResistantTime = 0;
        player.setHealth(20);
        float health = player.getHealth();
        pipe.onEntityCollidedWithBlock(world, 36, 240, 8, player);
        check(
            player.getHealth() < health,
            "native hot pipe did not damage the player (temperature " + pipe.mFluids[0].getFluid()
                .getTemperature(pipe.mFluids[0]) + ")");
        check(!MachineProgression.isLocked(steam), "damage mutated certification before the queued server tick");
        drain();
        check(
            MachineProgression.missingTier(steam) == ProgressionTier.STEAM,
            "actual pipe damage did not revoke Steam certification");
        status.updateCacheFromSource(false);
        check(
            ProgressionTier.fromSyncId(status.getIntValue()) == ProgressionTier.STEAM,
            "open Bronze GUI did not observe Steam revocation");
        check(
            MachineProgression.missingTier((MTEBasicMachine) electric.getMetaTileEntity()) == ProgressionTier.STEAM,
            "Steam accident did not pause electric machines");
        BaseMetaTileEntity nether = ProgressionIntegrationTests
            .furnace(DimensionManager.getWorld(-1), ItemList.Machine_Bronze_Furnace, 37);
        check(
            MachineProgression.isLocked((MTEBasicMachine) nether.getMetaTileEntity()),
            "Steam revocation differs across dimensions");
        long steamBefore = bronze.getStoredSteam();
        bronze.updateEntityProfiled();
        steel.updateEntityProfiled();
        electric.updateEntityProfiled();
        check(
            steam.mProgresstime == 0 && bronze.getStoredSteam() == steamBefore,
            "revoked Steam machine processed or consumed steam");
        check(
            ((MTEBasicMachine) steel.getMetaTileEntity()).mProgresstime == 0,
            "revoked Steel machine kept processing");
        check(
            ((MTEBasicMachine) electric.getMetaTileEntity()).mProgresstime == 0,
            "electric machine bypassed Steam revocation");
        activate(world, player, spare);
        check(MachineProgression.isLocked(steam) && spare.stackSize == 1, "old spare restored Steam certification");
        activate(world, player, ProgressionCertificates.issue(world, ProgressionTier.STEAM));
        check(
            !MachineProgression.isLocked((MTEBasicMachine) electric.getMetaTileEntity()),
            "Steam renewal lost existing electrical certification");

        // Exercise the native Steel exhaust with an actual server player in its vent block.
        steel.setFrontFacing(ForgeDirection.EAST);
        int ventX = steel.getOffsetX(steel.getFrontFacing(), 1);
        int ventY = steel.getOffsetY(steel.getFrontFacing(), 1);
        int ventZ = steel.getOffsetZ(steel.getFrontFacing(), 1);
        world.setBlockToAir(ventX, ventY, ventZ);
        player.setPosition(ventX + .5, ventY, ventZ + .5);
        check(world.spawnEntityInWorld(player), "could not place player in the native exhaust area");
        // Full-pack join handlers reapply the world's game mode; configure the injury after joining.
        player.theItemInWorldManager.setGameType(WorldSettings.GameType.SURVIVAL);
        player.capabilities.disableDamage = false;
        player.capabilities.isCreativeMode = false;
        check(
            world
                .getEntitiesWithinAABB(
                    EntityLivingBase.class,
                    AxisAlignedBB.getBoundingBox(ventX, ventY, ventZ, ventX + 1, ventY + 1, ventZ + 1))
                .contains(player),
            "native exhaust query cannot see the test player (player box " + player.boundingBox
                + ", vent "
                + ventX
                + ","
                + ventY
                + ","
                + ventZ
                + ", facing "
                + steel.getFrontFacing()
                + ")");
        player.hurtResistantTime = 0;
        player.setHealth(20);
        MTEBasicMachineBronze vent = (MTEBasicMachineBronze) steel.getMetaTileEntity();
        check(vent.getSteamDamage() > 0, "exhaust fixture must use a machine that deals native damage");
        vent.endProcess();
        health = player.getHealth();
        check(vent.allowToCheckRecipe(), "native Steam exhaust could not vent");
        check(
            player.getHealth() < health,
            "native Steam exhaust did not damage player (damage " + vent.getSteamDamage()
                + ", invulnerable "
                + player.isEntityInvulnerable()
                + ", protected "
                + player.capabilities.disableDamage
                + ", fire immune "
                + player.isImmuneToFire()
                + ")");
        drain();
        check(
            MachineProgression.missingTier(steam) == ProgressionTier.STEAM,
            "actual exhaust damage did not revoke Steam certification");
        world.removeEntity(player);
        activate(world, player, ProgressionCertificates.issue(world, ProgressionTier.STEAM));
        bronze.updateEntityProfiled();
        check(steam.mProgresstime > 0, "recertified Steam machine did not resume");
        player.inventory.setInventorySlotContents(player.inventory.currentItem, null);
        world.setBlockToAir(36, 240, 8);
        channel.finish();
    }

    private static void activate(WorldServer world, EntityPlayerMP player, ItemStack certificate) {
        player.inventory.setInventorySlotContents(player.inventory.currentItem, certificate);
        certificate.getItem()
            .onItemRightClick(certificate, world, player);
    }

    private static void drain() {
        new ProgressionServerTasks().onTick(new TickEvent.ServerTickEvent(TickEvent.Phase.END));
    }

    private static void check(boolean success, String detail) {
        if (!success) throw new AssertionError(detail);
    }
}
