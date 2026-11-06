package pl.kuba6000.gtnhextranerf.loot.runtime;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;

import cpw.mods.fml.common.Loader;
import eu.usrv.enhancedlootbags.EnhancedLootBags;
import eu.usrv.enhancedlootbags.core.serializer.LootGroups.LootGroup;
import eu.usrv.enhancedlootbags.core.serializer.LootGroups.LootGroup.Drop;
import pl.kuba6000.gtnhextranerf.GTNHExtraNerf;
import pl.kuba6000.gtnhextranerf.loader.LootLoader;
import pl.kuba6000.gtnhextranerf.loot.BagVariant;
import pl.kuba6000.gtnhextranerf.loot.GlobalItemLimit;
import pl.kuba6000.gtnhextranerf.loot.GlobalLootLimits;
import pl.kuba6000.gtnhextranerf.loot.ItemKey;
import pl.kuba6000.gtnhextranerf.loot.LootReward;
import pl.kuba6000.gtnhextranerf.loot.NativeLootTables;
import pl.kuba6000.gtnhextranerf.loot.modules.MeControllerLootModule;
import pl.kuba6000.gtnhextranerf.loot.storage.FileLootLedger;

/** Save-scoped guarantees and limits around ELB's native opening and delivery. */
public final class ServerLootRuntime {

    private static ServerLootRuntime active;
    private final Thread owner = Thread.currentThread();
    private final MinecraftServer server;
    private final FileLootLedger ledger;
    private final GlobalLootLimits limits;
    private Opening opening;
    private boolean failed;

    private ServerLootRuntime(MinecraftServer server, FileLootLedger ledger) {
        this.server = server;
        this.ledger = ledger;
        limits = new GlobalLootLimits(
            ledger,
            new GlobalItemLimit(
                MeControllerLootModule.LIMIT_ID,
                MeControllerLootModule.ITEM,
                MeControllerLootModule.GLOBAL_ALLOWANCE));
    }

    public static void start(MinecraftServer server) {
        if (!Loader.isModLoaded("enhancedlootbags")) return;
        if (LootLoader.developmentUnavailable()) {
            GTNHExtraNerf.LOG.warn("Development world started without lootbag opening: startup loot validation failed");
            return;
        }
        if (active != null) throw new IllegalStateException("Previous loot runtime was not closed");
        FileLootLedger ledger = null;
        try {
            LootLoader.verify(EnhancedLootBags.LootGroupHandler.getLootGroups());
            Path directory = DimensionManager.getCurrentSaveRootDirectory()
                .toPath()
                .resolve("data/gtnhextranerf/loot");
            ledger = Files.exists(directory) ? FileLootLedger.open(directory) : FileLootLedger.create(directory);
            if (ledger.discardedTail() > 0) GTNHExtraNerf.LOG.warn(
                "Loot journal ended in an unfinished record ({} bytes, from a crash or a backup taken mid-write); it was cut off",
                ledger.discardedTail());
            ((NativeLootStorage) EnhancedLootBags.LootGroupHandler).resetNativeLootStorage();
            active = new ServerLootRuntime(server, ledger);
            LootAllowanceSync.broadcast();
        } catch (IOException | RuntimeException failure) {
            if (ledger != null) try {
                ledger.close();
            } catch (IOException closeFailure) {
                failure.addSuppressed(closeFailure);
            }
            throw new IllegalStateException("GTNH Extra Nerf integrity check failed: loot world state", failure);
        }
    }

    public static void stop() {
        ServerLootRuntime previous = active;
        active = null;
        LootAllowanceSync.broadcast();
        if (previous != null) {
            ((NativeLootStorage) EnhancedLootBags.LootGroupHandler).resetNativeLootStorage();
            try {
                previous.ledger.close();
            } catch (IOException failure) {
                GTNHExtraNerf.LOG.error("Cannot close global loot ledger", failure);
            }
        }
    }

    public static ItemStack open(ItemStack bag, World world, EntityPlayer player, Supplier<ItemStack> nativeOpening) {
        if (world.isRemote || (player.capabilities.isCreativeMode && player.isSneaking())) return nativeOpening.get();
        ServerLootRuntime runtime = active;
        if (runtime == null || runtime.failed
            || runtime.server != MinecraftServer.getServer()
            || Thread.currentThread() != runtime.owner
            || runtime.opening != null
            || player.worldObj != world
            || player.isDead
            || player.getHeldItem() != bag
            || bag.stackSize <= 0) {
            player.addChatMessage(new ChatComponentTranslation("gtnhextranerf.loot.unavailable"));
            return bag;
        }
        int fortune = EnchantmentHelper.getEnchantmentLevel(Enchantment.fortune.effectId, bag);
        runtime.opening = new Opening(
            player.getUniqueID(),
            BagVariant.of(bag.getItemDamage(), Math.min(3, Math.max(0, fortune))));
        try {
            return nativeOpening.get();
        } catch (RuntimeException failure) {
            runtime.failed = true;
            LootAllowanceSync.broadcast();
            GTNHExtraNerf.LOG.error("Native loot opening failed; disabled for this server session", failure);
            player.addChatMessage(new ChatComponentTranslation("gtnhextranerf.loot.unavailable"));
            return bag;
        } finally {
            try {
                for (Issued issued : runtime.opening.issued) {
                    runtime.limits.outcome(issued.id, issued.delivered == issued.amount);
                    if (issued.delivered == issued.amount && issued.item.equals(MeControllerLootModule.ITEM))
                        player.addChatMessage(new ChatComponentTranslation("gtnhextranerf.loot.controller_received"));
                }
            } catch (IOException | RuntimeException failure) {
                runtime.failed = true;
                LootAllowanceSync.broadcast();
                GTNHExtraNerf.LOG.error("Cannot record global loot delivery; allowance remains spent", failure);
            } finally {
                runtime.opening = null;
            }
        }
    }

    public static boolean allowed(Drop drop) {
        ServerLootRuntime runtime = current();
        return runtime.limits.available(NativeLootTables.item(drop), drop.getIsRandomAmount() ? 1 : drop.getAmount());
    }

    /** Uses one native draw slot, and the same durable allowance as randomly selected controllers. */
    public static ItemStack guaranteedController(EntityPlayer player, LootGroup group) {
        ServerLootRuntime runtime = current();
        if (!runtime.limits.available(MeControllerLootModule.ITEM, 1)) return null;
        for (Drop drop : group.getDrops()) {
            if (drop.getChance() <= 0 || !NativeLootTables.item(drop)
                .equals(MeControllerLootModule.ITEM)) continue;
            ItemStack stack = drop.getItemStack(1);
            if (stack == null) throw new IllegalStateException("Guaranteed controller cannot be materialized");
            ItemStack claimed = claim(drop, stack);
            // The world-wide first controller takes precedence over legacy per-player counts.
            if (claimed != null && drop.getLimitedDropCount() > 0)
                EnhancedLootBags.LootGroupHandler.updateDropCount(player, group, drop);
            return claimed;
        }
        return null;
    }

    /** Called only after ELB has materialized an actual stack, before ELB can spawn it. */
    public static ItemStack claim(Drop drop, ItemStack stack) {
        if (stack == null) return null;
        ServerLootRuntime runtime = current();
        ItemKey item = key(stack);
        if (!runtime.limits.limited(item)) return stack;
        try {
            Optional<UUID> id = runtime.limits.claim(
                runtime.opening.player,
                runtime.opening.bag,
                new LootReward(
                    item,
                    stack.stackSize,
                    drop.getNBTTag() == null ? "" : drop.getNBTTag(),
                    stack.getMaxStackSize()));
            if (!id.isPresent()) return null;
            runtime.opening.issued.add(new Issued(id.get(), item, stack.stackSize));
            LootAllowanceSync.broadcast();
            return stack;
        } catch (IOException failure) {
            throw new IllegalStateException("Cannot persist global loot claim", failure);
        }
    }

    public static void spawned(EntityItem entity, boolean accepted) {
        if (!accepted) return;
        ServerLootRuntime runtime = current();
        ItemStack stack = entity.getEntityItem();
        ItemKey key = key(stack);
        int remaining = stack.stackSize;
        for (Issued issued : runtime.opening.issued) if (issued.item.equals(key)) {
            int count = Math.min(remaining, issued.amount - issued.delivered);
            issued.delivered += count;
            remaining -= count;
        }
    }

    /** Preserve player counts recorded by the previous implementation, without taking ownership of ELB's DB. */
    public static int previousPlayerCount(EntityPlayer player, LootGroup group, Drop drop) {
        ServerLootRuntime runtime = active;
        if (runtime == null || runtime.failed || player.worldObj.isRemote || Thread.currentThread() != runtime.owner)
            return 0;
        long count = runtime.ledger.snapshot()
            .consumedUnits(
                "elb-player/" + player.getUniqueID() + "/" + group.getGroupID() + "/" + drop.getIdentifier());
        return (int) Math.min(Integer.MAX_VALUE, count);
    }

    /** Remaining controller allowance, or -1 when the server cannot currently report it. */
    public static int controllerAllowance() {
        ServerLootRuntime runtime = active;
        if (runtime == null || runtime.failed || Thread.currentThread() != runtime.owner) return -1;
        try {
            long used = runtime.ledger.snapshot()
                .consumedUnits(MeControllerLootModule.LIMIT_ID);
            return (int) Math.max(0, MeControllerLootModule.GLOBAL_ALLOWANCE - used);
        } catch (RuntimeException failure) {
            GTNHExtraNerf.LOG.warn("Cannot read global loot allowance for preview", failure);
            return -1;
        }
    }

    private static ServerLootRuntime current() {
        ServerLootRuntime runtime = active;
        if (runtime == null || runtime.failed || runtime.opening == null || Thread.currentThread() != runtime.owner)
            throw new IllegalStateException("Global loot access outside native opening");
        return runtime;
    }

    private static ItemKey key(ItemStack stack) {
        return ItemKey.of(Item.itemRegistry.getNameForObject(stack.getItem()), stack.getItemDamage());
    }

    private static final class Opening {

        final UUID player;
        final BagVariant bag;
        final List<Issued> issued = new ArrayList<>();

        Opening(UUID player, BagVariant bag) {
            this.player = player;
            this.bag = bag;
        }
    }

    private static final class Issued {

        final UUID id;
        final ItemKey item;
        final int amount;
        int delivered;

        Issued(UUID id, ItemKey item, int amount) {
            this.id = id;
            this.item = item;
            this.amount = amount;
        }
    }
}
