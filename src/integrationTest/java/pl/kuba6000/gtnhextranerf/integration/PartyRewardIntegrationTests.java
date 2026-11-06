package pl.kuba6000.gtnhextranerf.integration;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.ItemInWorldManager;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.config.Configuration;

import com.mojang.authlib.GameProfile;

import betterquesting.api.enums.EnumPartyStatus;
import betterquesting.api.properties.NativeProps;
import betterquesting.api.questing.IQuest;
import betterquesting.api.questing.party.IParty;
import betterquesting.api.utils.BigItemStack;
import betterquesting.questing.QuestDatabase;
import betterquesting.questing.party.PartyManager;
import betterquesting.storage.QuestSettings;
import bq_standard.rewards.RewardItem;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import drethic.questbook.config.QBConfig;

final class PartyRewardIntegrationTests {

    private PartyRewardIntegrationTests() {}

    static void verify(WorldServer world) throws Exception {
        Configuration savedConfig = QBConfig.qbconfig;
        boolean savedSpawn = QBConfig.spawnWithBook;
        boolean savedSync = QBConfig.syncQuestRewards;
        boolean savedParties = QuestSettings.INSTANCE.getProperty(NativeProps.PARTY_ENABLE);
        Path directory = Files.createTempDirectory("extranerf-party-rewards-");
        Path config = directory.resolve("betterquesting/questbook.cfg");
        Files.createDirectories(config.getParent());
        int partyId = PartyManager.INSTANCE.nextID();
        UUID questId = UUID.randomUUID();
        try {
            QuestSettings.INSTANCE.setProperty(NativeProps.PARTY_ENABLE, true);
            EntityPlayerMP first = player(world, "RewardFirst");
            EntityPlayerMP second = player(world, "RewardSecond");
            UUID absent = UUID.randomUUID();
            IParty party = PartyManager.INSTANCE.createNew(partyId);
            party.setStatus(first.getUniqueID(), EnumPartyStatus.OWNER);
            party.setStatus(second.getUniqueID(), EnumPartyStatus.MEMBER);
            party.setStatus(absent, EnumPartyStatus.MEMBER);
            for (int attempt = 0; attempt < 2; attempt++) {
                Files.write(
                    config,
                    "general {\n B:syncQuestRewards=false\n B:spawnWithBook=false\n}\n"
                        .getBytes(StandardCharsets.UTF_8));
                QBConfig.init(new FMLPreInitializationEvent(null, directory.toFile()));
                IQuest quest = QuestDatabase.INSTANCE.createNew(questId);
                quest.setProperty(NativeProps.REPEAT_TIME, -1);
                RewardItem reward = new RewardItem();
                reward.setIgnoreDisabled(true);
                reward.items.add(new BigItemStack(new ItemStack(Items.diamond)));
                quest.getRewards()
                    .add(0, reward);
                for (UUID member : party.getMembers()) quest.setComplete(member, System.currentTimeMillis());
                check(quest.canClaim(first, false), "First player could not claim a completed quest");
                check(quest.canClaim(second, false), "Second player was blocked before any claim");
                quest.claimReward(first, false);
                check(
                    !quest.canClaim(second, false),
                    "Party member can claim a second copy after config load " + attempt);
                check(quest.hasClaimed(absent), "Absent party member was not marked claimed");
                check(first.inventory.hasItem(Items.diamond), "Claimant did not receive the item");
                check(!second.inventory.hasItem(Items.diamond), "Non-claimant received an extra reward");
                check(!QBConfig.spawnWithBook, "Unrelated config option changed");
                Configuration onDisk = new Configuration(config.toFile());
                onDisk.load();
                check(
                    !onDisk.get("general", "syncQuestRewards", true)
                        .getBoolean(),
                    "Mixin overwrote the config file");
                first.inventory.clearInventory(Items.diamond, -1);
            }
        } finally {
            QuestDatabase.INSTANCE.remove(questId);
            PartyManager.INSTANCE.removeID(partyId);
            QuestSettings.INSTANCE.setProperty(NativeProps.PARTY_ENABLE, savedParties);
            QBConfig.qbconfig = savedConfig;
            QBConfig.spawnWithBook = savedSpawn;
            QBConfig.syncQuestRewards = savedSync;
            Files.deleteIfExists(config);
            Files.deleteIfExists(config.getParent());
            Files.deleteIfExists(directory);
        }
    }

    private static EntityPlayerMP player(WorldServer world, String name) {
        return new EntityPlayerMP(
            MinecraftServer.getServer(),
            world,
            new GameProfile(UUID.randomUUID(), name),
            new ItemInWorldManager(world));
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
